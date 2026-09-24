package com.nutrition.vo;

import lombok.Data;

/**
 * 用户信息视图对象
 */
@Data
public class UserVO {

    private Long id;

    private String openid;

    /** 登录用户名 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /** 头像附件ID */
    private String fileIds;

    /** 头像完整URL */
    private String avatarUrl;
}
