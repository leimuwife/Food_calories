package com.nutrition.vo;

import lombok.Data;

/**
 * 用户信息视图对象
 */
@Data
public class UserVO {

    private Long id;

    private String openid;

    private String nickname;

    private String email;
}
