package com.nutrition.enums;

import lombok.Getter;

/**
 * 反馈状态枚举
 * 0待处理 1处理中 2已完结
 */
@Getter
public enum FeedbackStatusEnum {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完结");

    private final int code;
    private final String desc;

    FeedbackStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return 枚举值
     */
    public static FeedbackStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (FeedbackStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }

    /**
     * 根据状态码获取描述
     * @param code 状态码
     * @return 描述
     */
    public static String getDescByCode(Integer code) {
        return fromCode(code).getDesc();
    }
}