package com.nutrition.vo;

import lombok.Data;

/**
 * 登录结果视图对象
 * 用于返回用户登录成功后的token和用户信息
 */
@Data
public class LoginResultVO {

    private String token;

    private UserVO user;
}
