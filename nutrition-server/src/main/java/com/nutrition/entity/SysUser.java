package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信 openid */
    private String openid;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 用户名（账号登录） */
    private String username;

    /** 密码哈希 */
    private String passwordHash;

    /** 每日热量目标 (kcal) */
    private Integer dailyCalorieGoal;

    /** 每日蛋白质目标 (g) */
    private Integer dailyProteinGoal;

    /** 每日脂肪目标 (g) */
    private Integer dailyFatGoal;

    /** 每日碳水目标 (g) */
    private Integer dailyCarbsGoal;

    /**
     * 附件 ID 数组 JSON 字符串
     */
    @TableField("file_ids")
    private String fileIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
