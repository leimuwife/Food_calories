package com.nutrition.exception;

/**
 * 微信接口业务异常
 * 当微信接口返回业务错误码（errcode != 0）时抛出
 */
public class WxBusinessException extends RuntimeException {

    private final int errorCode;

    public WxBusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WxBusinessException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
