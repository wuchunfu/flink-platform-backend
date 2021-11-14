package com.flink.platform.dao.entity;

import com.flink.platform.udf.common.SqlColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Label parser. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabelParser {

    private String functionName;

    private Class<?> functionClass;

    private Class<?> dataClass;

    private List<SqlColumn> dataColumns;
}