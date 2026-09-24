package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 验证码配置枚举
 * 统一维护验证码长度、有效期和图片绘制参数，避免业务代码出现魔法数字。
 */
@Getter
@RequiredArgsConstructor
public enum CaptchaConfigEnum {

    /** 验证码数字位数 */
    CODE_LENGTH(4),

    /** 验证码有效期，单位：分钟 */
    EXPIRE_MINUTES(5),

    /** 验证码图片宽度，单位：像素 */
    IMAGE_WIDTH(160),

    /** 验证码图片高度，单位：像素 */
    IMAGE_HEIGHT(48),

    /** 干扰线数量 */
    NOISE_LINE_COUNT(6),

    /** 随机颜色通道最小值 */
    COLOR_CHANNEL_MIN(80),

    /** 随机颜色通道取值范围 */
    COLOR_CHANNEL_RANGE(176);

    /** 配置值 */
    private final int value;
}
