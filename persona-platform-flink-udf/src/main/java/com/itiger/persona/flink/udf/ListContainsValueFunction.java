package com.itiger.persona.flink.udf;

import com.itiger.persona.flink.udf.util.ObjectUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.functions.ScalarFunction;

import java.util.Arrays;
import java.util.List;

import static com.itiger.persona.common.constants.Constant.AND;
import static com.itiger.persona.common.constants.Constant.OR;

/**
 * @author tiny.wang
 */
public class ListContainsValueFunction extends ScalarFunction {

    private static final int ZERO = 0;

    private static final int ONE = 1;

    public int eval(List<String> columnValues, String inputValue) {
        return internalEval(columnValues, inputValue);
    }

    public int internalEval(List<?> columnValues, String inputValue) {
        if (CollectionUtils.isEmpty(columnValues)
                || StringUtils.isBlank(inputValue)) {
            return ZERO;
        }
        // and | or operator
        boolean orOperator = inputValue.contains(OR);
        String[] inputItems;
        if (orOperator) {
            inputItems = inputValue.split("\\|");
        } else {
            inputItems = inputValue.split(AND);
        }
        return orOperator ?
                isAnyContained(columnValues, inputItems) :
                isAllContained(columnValues, inputItems);
    }

    private int isAllContained(List<?> columnValues, String[] inputItems) {
        Object[] cast = ObjectUtil.cast(inputItems, columnValues.get(0).getClass());
        boolean bool = Arrays.stream(cast).allMatch(columnValues::contains);
        return bool ? ONE : ZERO;
    }

    private int isAnyContained(List<?> columnValues, String[] inputItems) {
        Object[] cast = ObjectUtil.cast(inputItems, columnValues.get(0).getClass());
        boolean bool = Arrays.stream(cast).anyMatch(columnValues::contains);
        return bool ? ONE : ZERO;
    }

}
