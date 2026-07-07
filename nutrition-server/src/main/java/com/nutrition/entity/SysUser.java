package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@Data
@TableName("sys_user")
public class SysUser extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 微信 openid */
    @Schema(description = "微信 openid")
    private String openid;

    /** 昵称 */
    @Schema(description = "昵称")
    private String nickname;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 用户名（账号登录） */
    @Schema(description = "用户名")
    private String username;

    /** 密码哈希 */
    @Schema(description = "密码哈希")
    private String passwordHash;
}
