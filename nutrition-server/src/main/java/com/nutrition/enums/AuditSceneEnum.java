package com.nutrition.enums;

import lombok.Getter;

/**
 * 审核业务场景枚举
 * 对应数据库 scene 字段
 */
@Getter
public enum AuditSceneEnum {

    MOMENT(1, "朋友圈动态"),
    COMMENT(2, "评论"),
    DIET_REMARK(3, "饮食备注/描述"),
    PROFILE(4, "个人资料");

    private final int code;
    private final String description;

    AuditSceneEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AuditSceneEnum fromCode(int code) {
        for (AuditSceneEnum scene : values()) {
            if (scene.code == code) {
                return scene;
            }
        }
        throw new IllegalArgumentException("Invalid audit scene code: " + code);
    }
}
