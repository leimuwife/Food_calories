package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户账号状态枚举。
 * 与 sys_user.delete_flag 字段保持一致。
 */
@Getter
@RequiredArgsConstructor
public enum UserAccountStatusEnum {

    /** 正常可用 */
    ENABLED(0),

    /** 已禁用，登录查询会被逻辑删除条件过滤 */
    DISABLED(1);

    /** 数据库状态值 */
    private final int value;
}
