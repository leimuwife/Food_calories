package com.nutrition.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 餐型枚举类
 */
@Schema(description = "餐型枚举")
public enum MealType {

    @Schema(description = "早餐")
    BREAKFAST("breakfast", "早餐"),

    @Schema(description = "午餐")
    LUNCH("lunch", "午餐"),

    @Schema(description = "晚餐")
    DINNER("dinner", "晚餐"),

    @Schema(description = "夜宵")
    SNACK("snack", "夜宵");

    private final String code;
    private final String description;

    MealType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MealType fromCode(String code) {
        for (MealType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知餐型: " + code);
    }
}