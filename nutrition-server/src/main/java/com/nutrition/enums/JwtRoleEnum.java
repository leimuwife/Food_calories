package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JWT 角色枚举。
 * 用于区分普通用户和管理员接口权限。
 */
@Getter
@RequiredArgsConstructor
public enum JwtRoleEnum {

    /** 普通用户 */
    USER("USER"),

    /** 系统管理员 */
    ADMIN("ADMIN"); 

    /** 角色编码 */
    private final String code;
}
