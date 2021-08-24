package com.flink.platform.web.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author tiny.wang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompositeSqlWhere extends SqlWhere {

    private String relation;

    private List<SqlWhere> conditions;

}