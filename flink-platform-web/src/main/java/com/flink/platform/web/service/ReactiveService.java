package com.flink.platform.web.service;

import com.flink.platform.common.enums.DbType;
import com.flink.platform.common.enums.SqlType;
import com.flink.platform.common.exception.JobCommandGenException;
import com.flink.platform.dao.entity.Datasource;
import com.flink.platform.dao.entity.JobInfo;
import com.flink.platform.dao.entity.ds.DatasourceParam;
import com.flink.platform.web.command.CommandBuilder;
import com.flink.platform.web.config.WorkerConfig;
import com.flink.platform.web.entity.vo.ReactiveDataVo;
import com.flink.platform.web.entity.vo.ReactiveExecVo;
import com.flink.platform.web.util.CommandUtil;
import com.flink.platform.web.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;

import static com.flink.platform.common.enums.SqlType.SELECT;
import static com.flink.platform.common.util.SqlUtil.limitRowNum;

/** Manage datasource service. */
@Slf4j
@Service
public class ReactiveService {

    private final ThreadPoolExecutor executor;

    private final Map<String, BlockingQueue<String>> cmdOutputBufferMap;

    private final List<CommandBuilder> commandBuilders;

    @Autowired
    public ReactiveService(WorkerConfig workerConfig, List<CommandBuilder> commandBuilders) {
        this.executor =
                ThreadUtil.newDaemonFixedThreadExecutor(
                        "Reactive-job", workerConfig.getReactiveExecThreads());
        this.cmdOutputBufferMap = new ConcurrentHashMap<>();
        this.commandBuilders = commandBuilders;
    }

    public ReactiveExecVo execFlink(String execId, JobInfo jobInfo, String[] envProps)
            throws Exception {
        String command =
                commandBuilders.stream()
                        .filter(
                                builder ->
                                        builder.isSupported(
                                                jobInfo.getType(), jobInfo.getVersion()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new JobCommandGenException(
                                                "No available job command builder"))
                        .buildCommand(jobInfo)
                        .toCommandString();

        CompletableFuture.runAsync(
                        () -> {
                            try {
                                CommandUtil.exec(command, envProps, collectCmdResult(execId));
                            } catch (Exception e) {
                                StringWriter writer = new StringWriter();
                                e.printStackTrace(new PrintWriter(writer, true));
                                cmdOutputBufferMap.get(execId).add(writer.toString());
                            }
                        },
                        executor)
                .whenComplete(
                        (unused, throwable) -> {
                            try {
                                ThreadUtil.sleep(5000);
                            } finally {
                                cmdOutputBufferMap.remove(execId);
                            }
                        });

        return new ReactiveExecVo(execId);
    }

    public ReactiveDataVo execSql(JobInfo jobInfo, Datasource datasource) throws Exception {
        String sqlContent = jobInfo.getSubject();
        if (isQuery(sqlContent)) {
            sqlContent = limitRowNum(sqlContent);
        }

        try (Connection connection = getConnection(datasource.getType(), datasource.getParams());
                Statement stmt = connection.createStatement()) {
            String[] columnNames;
            List<Object[]> dataList = new ArrayList<>();
            if (stmt.execute(sqlContent)) {
                ResultSet resultSet = stmt.getResultSet();
                ResultSetMetaData metaData = resultSet.getMetaData();

                // metadata.
                int num = metaData.getColumnCount();
                columnNames = new String[num];
                for (int i = 1; i <= num; i++) {
                    columnNames[i - 1] = metaData.getColumnName(i);
                }

                // data list.
                DbType dbType = datasource.getType();
                while (resultSet.next()) {
                    Object[] item = new Object[num];
                    for (int i = 1; i <= num; i++) {
                        item[i - 1] = toJavaObject(dbType, resultSet.getObject(i));
                    }
                    dataList.add(item);
                }
            } else {
                columnNames = new String[] {"success"};
                dataList.add(new Object[] {false});
            }

            return new ReactiveDataVo(columnNames, dataList, null);
        }
    }

    public boolean bufferExists(String execId) {
        return cmdOutputBufferMap.containsKey(execId);
    }

    public List<String> getBufferByExecId(String execId) {
        BlockingQueue<String> printLogQueue = cmdOutputBufferMap.get(execId);
        if (printLogQueue != null) {
            List<String> cmdLogs = new ArrayList<>();
            printLogQueue.drainTo(cmdLogs);
            return cmdLogs;
        } else {
            return null;
        }
    }

    private BiConsumer<CommandUtil.CmdOutType, String> collectCmdResult(String execId) {
        BlockingQueue<String> cmdLogQueue = new ArrayBlockingQueue<>(50_000);
        cmdOutputBufferMap.put(execId, cmdLogQueue);
        return (cmdOutType, line) -> {
            try {
                switch (cmdOutType) {
                    case STD:
                    case ERR:
                        cmdLogQueue.offer(line);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                log.error("Consumer command log failed", e);
            }
        };
    }

    private Connection getConnection(DbType dbType, DatasourceParam params) throws Exception {
        Class.forName(params.getDriver());
        Properties properties = new Properties();
        switch (dbType) {
            case CLICKHOUSE:
                properties.setProperty("user", params.getUsername());
                properties.setProperty("password", params.getPassword());
                break;
            case MYSQL:
            default:
                throw new RuntimeException("unsupported db type: " + dbType);
        }
        properties.putAll(params.getProperties());
        return DriverManager.getConnection(params.getUrl(), properties);
    }

    private Object toJavaObject(DbType dbType, Object dbObject) throws Exception {
        switch (dbType) {
            case CLICKHOUSE:
                if (dbObject instanceof Array) {
                    Object objectArray = ((Array) dbObject).getArray();
                    int arrayLength = java.lang.reflect.Array.getLength(objectArray);
                    Object[] javaObjectArray = new Object[arrayLength];
                    for (int i = 0; i < arrayLength; i++) {
                        javaObjectArray[i] =
                                toJavaObject(dbType, java.lang.reflect.Array.get(objectArray, i));
                    }
                    return javaObjectArray;
                } else {
                    return dbObject;
                }
            case MYSQL:
                return dbObject;
            default:
                throw new RuntimeException("unsupported database type:" + dbType);
        }
    }

    public boolean isQuery(String sql) {
        return SqlType.parse(sql).getType() == SELECT;
    }
}
