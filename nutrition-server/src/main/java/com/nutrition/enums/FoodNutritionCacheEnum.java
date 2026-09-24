package com.nutrition.enums;

import lombok.Getter;

/**
 * 食物营养缓存配置枚举
 * 统一维护食物营养 Redis Hash 的键名和过期时间。
 */
@Getter
public enum FoodNutritionCacheEnum {

    /** 食物营养 Redis Hash 键 */
    HASH_KEY("food:nutrition"),

    /** 缓存过期天数 */
    EXPIRE_DAYS(7);

    /** 字符串配置值 */
    private final String stringValue;

    /** 数值配置值 */
    private final long numberValue;

    /**
     * 创建字符串类型配置。
     *
     * @param value 配置值
     */
    FoodNutritionCacheEnum(String value) {
        this.stringValue = value;
        this.numberValue = 0L;
    }

    /**
     * 创建数值类型配置。
     *
     * @param value 配置值
     */
    FoodNutritionCacheEnum(long value) {
        this.stringValue = null;
        this.numberValue = value;
    }
}
