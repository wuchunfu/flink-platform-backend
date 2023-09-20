package com.flink.platform.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flink.platform.common.constants.Constant;
import com.flink.platform.common.enums.ExecutionStatus;
import com.flink.platform.dao.entity.JobFlowRun;
import com.flink.platform.dao.entity.JobRunInfo;
import com.flink.platform.dao.entity.User;
import com.flink.platform.dao.service.JobFlowRunService;
import com.flink.platform.dao.service.JobRunInfoService;
import com.flink.platform.web.entity.response.ResultInfo;
import com.flink.platform.web.service.KillJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.flink.platform.common.enums.ExecutionStatus.KILLABLE;
import static com.flink.platform.common.enums.ExecutionStatus.getNonTerminals;
import static com.flink.platform.common.enums.ResponseStatus.ERROR_PARAMETER;
import static com.flink.platform.common.enums.ResponseStatus.FLOW_ALREADY_TERMINATED;
import static com.flink.platform.common.enums.ResponseStatus.KILL_FLOW_EXCEPTION_FOUND;
import static com.flink.platform.common.enums.ResponseStatus.NO_RUNNING_JOB_FOUND;
import static com.flink.platform.common.enums.ResponseStatus.USER_HAVE_NO_PERMISSION;
import static com.flink.platform.common.util.DateUtil.GLOBAL_DATE_TIME_FORMAT;
import static com.flink.platform.web.entity.response.ResultInfo.failure;
import static com.flink.platform.web.entity.response.ResultInfo.success;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

/** crud job flow. */
@Slf4j
@RestController
@RequestMapping("/jobFlowRun")
public class JobFlowRunController {

    @Autowired private JobFlowRunService jobFlowRunService;

    @Autowired private JobRunInfoService jobRunInfoService;

    @Autowired private KillJobService killJobService;

    @GetMapping(value = "/get/{flowRunId}")
    public ResultInfo<JobFlowRun> get(@PathVariable long flowRunId) {
        JobFlowRun jobFlowRun = jobFlowRunService.getById(flowRunId);
        return success(jobFlowRun);
    }

    @GetMapping(value = "/page")
    public ResultInfo<IPage<JobFlowRun>> page(
            @RequestAttribute(value = Constant.SESSION_USER) User loginUser,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "status", required = false) ExecutionStatus status,
            @RequestParam(name = "tag", required = false) String tagCode,
            @DateTimeFormat(pattern = GLOBAL_DATE_TIME_FORMAT)
                    @RequestParam(name = "startTime", required = false)
                    LocalDateTime startTime,
            @DateTimeFormat(pattern = GLOBAL_DATE_TIME_FORMAT)
                    @RequestParam(name = "endTime", required = false)
                    LocalDateTime endTime,
            @RequestParam(name = "sort", required = false) String sort) {
        Page<JobFlowRun> pager = new Page<>(page, size);

        LambdaQueryWrapper<JobFlowRun> queryWrapper =
                new QueryWrapper<JobFlowRun>()
                        .lambda()
                        .eq(JobFlowRun::getUserId, loginUser.getId())
                        .eq(nonNull(status), JobFlowRun::getStatus, status)
                        .like(isNotEmpty(name), JobFlowRun::getName, name)
                        .like(isNotEmpty(tagCode), JobFlowRun::getTags, tagCode)
                        .between(
                                nonNull(startTime) && nonNull(endTime),
                                JobFlowRun::getCreateTime,
                                startTime,
                                endTime);
        if ("-id".equals(sort)) {
            queryWrapper.orderByDesc(JobFlowRun::getId);
        }

        IPage<JobFlowRun> iPage = jobFlowRunService.page(pager, queryWrapper);
        return success(iPage);
    }

    @GetMapping(value = "/kill/{flowRunId}")
    public ResultInfo<Long> kill(
            @RequestAttribute(value = Constant.SESSION_USER) User loginUser,
            @PathVariable Long flowRunId) {
        JobFlowRun jobFlowRun = jobFlowRunService.getById(flowRunId);
        ExecutionStatus status = jobFlowRun.getStatus();
        if (status != null && status.isTerminalState()) {
            return failure(FLOW_ALREADY_TERMINATED);
        }

        // Set the status of the workflow to KILLABLE.
        JobFlowRun newJobFlowRun = new JobFlowRun();
        newJobFlowRun.setId(jobFlowRun.getId());
        newJobFlowRun.setStatus(KILLABLE);
        jobFlowRunService.updateById(newJobFlowRun);

        // Get unfinished jobs and kill them concurrently.
        List<JobRunInfo> unfinishedJobs =
                jobRunInfoService.list(
                        new QueryWrapper<JobRunInfo>()
                                .lambda()
                                .eq(JobRunInfo::getFlowRunId, flowRunId)
                                .eq(JobRunInfo::getUserId, loginUser.getId())
                                .in(JobRunInfo::getStatus, getNonTerminals()));
        if (CollectionUtils.isEmpty(unfinishedJobs)) {
            return failure(NO_RUNNING_JOB_FOUND);
        }

        boolean isSuccess =
                unfinishedJobs.parallelStream()
                        .map(
                                jobRun -> {
                                    try {
                                        return killJobService.killRemoteJob(jobRun);
                                    } catch (Exception e) {
                                        log.error("kill job: {} failed.", jobRun.getId(), e);
                                        return false;
                                    }
                                })
                        .reduce((bool1, bool2) -> bool1 && bool2)
                        .orElse(false);

        return isSuccess ? success(flowRunId) : failure(KILL_FLOW_EXCEPTION_FOUND);
    }

    @GetMapping(value = "/purge/{flowRunId}")
    public ResultInfo<Long> purge(
            @RequestAttribute(value = Constant.SESSION_USER) User loginUser,
            @PathVariable long flowRunId) {
        JobFlowRun jobFlowRun = jobFlowRunService.getById(flowRunId);
        if (jobFlowRun == null) {
            return failure(ERROR_PARAMETER);
        }

        if (!loginUser.getId().equals(jobFlowRun.getUserId())) {
            return failure(USER_HAVE_NO_PERMISSION);
        }

        jobFlowRunService.deleteAllById(flowRunId, loginUser.getId());
        return success(flowRunId);
    }
}
