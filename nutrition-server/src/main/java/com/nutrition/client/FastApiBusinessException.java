package com.nutrition.client;

/**
 * FastApi 业务异常
 * 用于区分网络异常和业务逻辑异常
 */
public class FastApiBusinessException extends RuntimeException {

    public FastApiBusinessException(String message) {
        super(message);
    }

    public FastApiBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
