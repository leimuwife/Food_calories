package com.nutrition.enums;

import lombok.Getter;

/**
 * 审核结果建议枚举
 * 对应微信接口返回的 suggest 字段
 */
@Getter
public enum AuditSuggestEnum {

    PASS("pass", "放行", "内容合规，放行业务"),
    RISKY("risky", "待复审", "标记人工复审，拦截发布"),
    BLOCK("block", "违规拦截", "直接判定违规，返回前端提示修改内容");

    private final String code;
    private final String label;
    private final String description;

    AuditSuggestEnum(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public static AuditSuggestEnum fromCode(String code) {
        for (AuditSuggestEnum suggest : values()) {
            if (suggest.code.equalsIgnoreCase(code)) {
                return suggest;
            }
        }
        throw new IllegalArgumentException("Invalid audit suggest code: " + code);
    }
}
