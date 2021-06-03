package com.itiger.persona.flink.udf.business;

import com.itiger.persona.common.enums.DataType;
import com.itiger.persona.common.util.FunctionUtil;
import com.itiger.persona.common.util.JsonUtil;
import com.itiger.persona.flink.udf.entity.LabelWrapper;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.types.inference.TypeInference;

import java.time.Instant;
import java.util.Optional;

/**
 * @author tiny.wang
 */
public class ValidUserLabelValueFunction extends ScalarFunction {

    public Object eval(String json, String type) {
        val userLabel = FunctionUtil.getOrDefault(() -> JsonUtil.toBean(json, LabelWrapper.class), null);

        // 判断有效时间
        if (userLabel != null && userLabel.getValid_time() != -1 && userLabel.getValid_time() < Instant.now().toEpochMilli()) {
            return null;
        }

        Object valueObj = userLabel != null ? userLabel.getValue() : null;
        if (valueObj == null) {
            return null;
        }

        String value = valueObj.toString();
        DataType dataType = DataType.of(type);
        switch (dataType) {
            case INT:
                return NumberUtils.toInt(value);
            case LONG:
                return NumberUtils.toLong(value);
            case FLOAT:
                return NumberUtils.toFloat(value);
            case DOUBLE:
                return NumberUtils.toDouble(value);
            case BOOL:
                return BooleanUtils.toBoolean(value);
            case STRING:
            default:
                return value;
        }
    }

    @Override
    public TypeInference getTypeInference(DataTypeFactory typeFactory) {
        return TypeInference.newBuilder()
                .typedArguments(DataTypes.STRING(), DataTypes.STRING())
                .outputTypeStrategy(callContext -> {
                    if (!callContext.isArgumentLiteral(1) || callContext.isArgumentNull(1)) {
                        throw callContext.newValidationError("Literal expected for second argument.");
                    }
                    DataType dataType = callContext.getArgumentValue(1, String.class)
                            .map(DataType::of).orElse(DataType.STRING);
                    switch (dataType) {
                        case INT:
                            return Optional.of(DataTypes.INT().nullable());
                        case LONG:
                            return Optional.of(DataTypes.BIGINT().nullable());
                        case FLOAT:
                            return Optional.of(DataTypes.FLOAT().nullable());
                        case DOUBLE:
                            return Optional.of(DataTypes.DOUBLE().nullable());
                        case BOOL:
                            return Optional.of(DataTypes.BOOLEAN().nullable());
                        case STRING:
                        default:
                            return Optional.of(DataTypes.STRING().nullable());
                    }
                })
                .build();
    }
}