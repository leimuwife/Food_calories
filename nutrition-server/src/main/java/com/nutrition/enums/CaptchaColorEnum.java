package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.Color;

/**
 * 验证码颜色枚举
 * 统一维护验证码背景色和数字颜色，避免绘图工具类散落颜色常量。
 */
@Getter
@RequiredArgsConstructor
public enum CaptchaColorEnum {

    /** 浅粉色背景 */
    BACKGROUND(255, 249, 250),

    /** 主色数字 */
    TEXT_PRIMARY(214, 51, 132),

    /** 辅助数字色 */
    TEXT_SECONDARY(180, 60, 150),

    /** 深色数字 */
    TEXT_TERTIARY(120, 70, 160);

    /** 红色通道 */
    private final int red;

    /** 绿色通道 */
    private final int green;

    /** 蓝色通道 */
    private final int blue;

    /**
     * 转换为 AWT 颜色对象。
     *
     * @return AWT 颜色对象
     */
    public Color toColor() {
        return new Color(red, green, blue);
    }
}
