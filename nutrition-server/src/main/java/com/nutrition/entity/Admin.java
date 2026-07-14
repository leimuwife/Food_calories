package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体类
 * 对应数据库表 sys_admin
 */
@Data
@TableName("sys_admin")
public class Admin extends Common {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("username")
    @Schema(description = "管理员账号")
    private String username;

    @TableField("password")
    @Schema(description = "密码（BCrypt加密）")
    private String password;

    @TableField("nickname")
    @Schema(description = "管理员昵称")
    private String nickname;

    @TableField("file_ids")
    @Schema(description = "头像附件ID列表")
    private String fileIds;

    @TableField("phone")
    @Schema(description = "联系电话")
    private String phone;

}