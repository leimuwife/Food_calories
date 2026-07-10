package com.nutrition.exception;

/**
 * 微信网络请求异常
 * 当调用微信接口发生网络错误、超时、连接异常时抛出
 */
public class WxNetworkException extends RuntimeException {

    public WxNetworkException(String message) {
        super(message);
    }

    public WxNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
