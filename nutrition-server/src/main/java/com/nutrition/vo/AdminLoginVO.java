package com.nutrition.vo;

import lombok.Data;

/**
 * 管理员登录结果视图对象
 */
@Data
public class AdminLoginVO {

    /** JWT Token */
    private String token;

    /** 管理员ID */
    private Long id;

    /** 账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像附件ID列表 */
    private String fileIds;

    /** 联系电话 */
    private String phone;
}