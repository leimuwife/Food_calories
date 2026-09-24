package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 食物数据来源枚举
 * 统一维护返回给前端的数据来源说明。
 */
@Getter
@RequiredArgsConstructor
public enum FoodDataSourceEnum {

    /** 中国食物成分表 */
    CHINESE_FOOD_COMPOSITION_TABLE("中国食物成分表");

    /** 数据来源名称 */
    private final String name;
}
