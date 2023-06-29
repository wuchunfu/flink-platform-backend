package com.flink.platform.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flink.platform.common.enums.ExecutionStatus;
import com.flink.platform.common.enums.JobType;
import com.flink.platform.common.exception.UnrecoverableException;
import com.flink.platform.common.util.JsonUtil;
import com.flink.platform.dao.entity.JobRunInfo;
import com.flink.platform.dao.service.JobRunInfoService;
import com.flink.platform.web.command.CommandBuilder;
import com.flink.platform.web.command.CommandExecutor;
import com.flink.platform.web.command.JobCallback;
import com.flink.platform.web.command.JobCommand;
import com.flink.platform.web.command.flink.FlinkCommand;
import com.flink.platform.web.config.AppRunner;
import com.flink.platform.web.enums.Placeholder;
import com.flink.platform.web.enums.Variable;
import com.flink.platform.web.util.ExceptionUtil;
import com.flink.platform.web.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flink.platform.common.constants.Constant.HOST_IP;
import static com.flink.platform.common.enums.ExecutionStatus.CREATED;
import static com.flink.platform.common.enums.ExecutionStatus.ERROR;

/** Process job service. */
@Slf4j
@Service
public class ProcessJobService {

    private final JobRunInfoService jobRunInfoService;

    private final List<CommandBuilder> jobCommandBuilders;

    private final List<CommandExecutor> jobCommandExecutors;

    @Autowired
    public ProcessJobService(
            JobRunInfoService jobRunInfoService,
            List<CommandBuilder> jobCommandBuilders,
            List<CommandExecutor> jobCommandExecutors) {
        this.jobRunInfoService = jobRunInfoService;
        this.jobCommandBuilders = jobCommandBuilders;
        this.jobCommandExecutors = jobCommandExecutors;
    }

    public Long processJob(final long jobRunId, final int retries) {
        int errorTimes = 0;
        while (AppRunner.isRunning()) {
            try {
                processJob(jobRunId);
                break;
            } catch (Exception e) {
                log.error("Process job run: {} failed, retry times: {}.", jobRunId, errorTimes, e);
                if (++errorTimes > retries || e instanceof UnrecoverableException) {
                    JobRunInfo jobRun = new JobRunInfo();
                    jobRun.setId(jobRunId);
                    jobRun.setStatus(ERROR);
                    jobRun.setBackInfo(
                            JsonUtil.toJsonString(
                                    new JobCallback(ExceptionUtil.stackTrace(e), null)));
                    jobRunInfoService.updateById(jobRun);
                    break;
                }

                ThreadUtil.sleepRetry(errorTimes);
            }
        }

        return jobRunId;
    }

    public void processJob(final long jobRunId) throws Exception {
        JobCommand jobCommand = null;
        JobRunInfo jobRunInfo = null;

        try {
            // step 1: get job info
            jobRunInfo =
                    jobRunInfoService.getOne(
                            new QueryWrapper<JobRunInfo>()
                                    .lambda()
                                    .eq(JobRunInfo::getId, jobRunId)
                                    .eq(JobRunInfo::getStatus, CREATED));
            if (jobRunInfo == null) {
                throw new UnrecoverableException(
                        String.format("The job run: %s is no longer exists.", jobRunId));
            }

            // step 2: replace variables in the sql statement
            JobRunInfo finalJobRun = jobRunInfo;
            Map<String, Object> variableMap = new HashMap<>();
            Arrays.stream(Placeholder.values())
                    .filter(placeholder -> finalJobRun.getSubject().contains(placeholder.wildcard))
                    .map(placeholder -> placeholder.provider.apply(finalJobRun))
                    .forEach(variableMap::putAll);

            MapUtils.emptyIfNull(jobRunInfo.getVariables())
                    .forEach(
                            (name, value) -> {
                                Variable sqlVar = Variable.matchPrefix(name);
                                variableMap.put(name, sqlVar.provider.apply(value));
                            });
            // replace variable with actual value
            for (Map.Entry<String, Object> entry : variableMap.entrySet()) {
                String originSubject = jobRunInfo.getSubject();
                String distSubject =
                        originSubject.replace(entry.getKey(), entry.getValue().toString());
                jobRunInfo.setSubject(distSubject);
            }

            JobType jobType = jobRunInfo.getType();
            String version = jobRunInfo.getVersion();

            // step 3: build job command, create a SqlContext if needed
            jobCommand =
                    jobCommandBuilders.stream()
                            .filter(builder -> builder.isSupported(jobType, version))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new UnrecoverableException(
                                                    "No available job command builder"))
                            .buildCommand(jobRunInfo.getFlowRunId(), jobRunInfo);

            // step 4: submit job
            LocalDateTime submitTime = LocalDateTime.now();
            final JobCommand command = jobCommand;
            JobCallback callback =
                    jobCommandExecutors.stream()
                            .filter(executor -> executor.isSupported(jobType))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new UnrecoverableException(
                                                    "No available job command executor"))
                            .exec(command);

            // step 5: write job run info to db
            ExecutionStatus executionStatus = callback.getStatus();
            JobRunInfo newJobRun = new JobRunInfo();
            newJobRun.setId(jobRunInfo.getId());
            newJobRun.setSubject(jobRunInfo.getSubject());
            newJobRun.setStatus(executionStatus);
            newJobRun.setVariables(variableMap);
            newJobRun.setBackInfo(JsonUtil.toJsonString(callback));
            newJobRun.setSubmitTime(submitTime);
            newJobRun.setHost(HOST_IP);
            if (executionStatus.isTerminalState()) {
                newJobRun.setStopTime(LocalDateTime.now());
            }
            jobRunInfoService.updateById(newJobRun);

            // step 6: print job command info
            log.info("Job run: {} submitted, time: {}", jobRunId, System.currentTimeMillis());

        } finally {
            if (jobRunInfo != null
                    && jobRunInfo.getType() == JobType.FLINK_SQL
                    && jobCommand != null) {
                try {
                    FlinkCommand flinkCommand = (FlinkCommand) jobCommand;
                    if (flinkCommand.getMainArgs() != null) {
                        Files.deleteIfExists(Paths.get(flinkCommand.getMainArgs()));
                    }
                } catch (Exception e) {
                    log.warn("Delete sql context file failed", e);
                }
            }
        }
    }
}
