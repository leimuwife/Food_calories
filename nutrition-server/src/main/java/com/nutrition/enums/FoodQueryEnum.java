package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 食物库查询配置枚举
 * 统一维护分页和查询参数边界，避免服务层出现魔法数字。
 */
@Getter
@RequiredArgsConstructor
public enum FoodQueryEnum {

    /** 默认每页条数 */
    DEFAULT_PAGE_SIZE(20),

    /** 最大每页条数 */
    MAX_PAGE_SIZE(100);

    /** 配置值 */
    private final int value;
}
