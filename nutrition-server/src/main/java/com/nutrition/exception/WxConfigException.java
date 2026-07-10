package com.nutrition.exception;

/**
 * 微信配置缺失异常
 * 当微信小程序 AppID 或 AppSecret 未配置时抛出
 */
public class WxConfigException extends RuntimeException {

    public WxConfigException(String message) {
        super(message);
    }

    public WxConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
