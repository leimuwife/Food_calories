package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 验证码 Redis Key 枚举
 * 统一管理验证码缓存键前缀，避免不同模块重复定义。
 */
@Getter
@RequiredArgsConstructor
public enum CaptchaRedisKeyEnum {

    /** 验证码缓存键前缀 */
    PREFIX("captcha:");

    /** Redis Key 前缀 */
    private final String prefix;
}
