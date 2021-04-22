package com.itiger.persona.common.util;

import java.util.function.Supplier;

/**
 * check whether throw exception
 *
 * @author tiny.wang
 */
public class Preconditions {

    public static <T extends Throwable> void checkThrow(boolean condition, Supplier<T> supplier) throws T {
        if (condition) {
            throw supplier.get();
        }
    }
}