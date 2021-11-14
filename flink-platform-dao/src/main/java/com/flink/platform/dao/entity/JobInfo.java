package com.flink.platform.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.flink.platform.common.enums.DeployMode;
import com.flink.platform.common.enums.ExecutionMode;
import com.flink.platform.common.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/** job config info. */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_job_info")
public class JobInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** unique code. */
    private String code;

    /** job name. */
    private String name;

    /** job desc. */
    private String description;

    /** job type. */
    private JobType type;

    /** version. */
    private String version;

    /** config for run job. */
    private String config;

    /** deploy mode. */
    private DeployMode deployMode;

    /** execution mode. */
    private ExecutionMode execMode;

    /** sql or jar path. */
    private String subject;

    /** use json to explain sql logic in subject column. */
    private String sqlPlan;

    /** main args. */
    private String mainArgs;

    /** main class. */
    private String mainClass;

    /** catalogs. */
    private String catalogs;

    /** external jars. */
    private String extJars;

    /** -1: delete, 0: close, 1: open. */
    private Integer status;

    /** cron expression for the job. */
    private String cronExpr;

    /**
     * route url. <br>
     * example: http://127.0.0.1
     */
    private String routeUrl;

    /** create user. */
    private String createUser;

    /** create time. */
    private LocalDateTime createTime;

    /** update user. */
    private String updateUser;

    /** update time. */
    private LocalDateTime updateTime;
}