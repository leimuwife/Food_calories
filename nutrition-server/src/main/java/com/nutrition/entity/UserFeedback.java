package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户问题反馈实体类
 * 对应数据库表 user_feedback
 */
@Data
@TableName("user_feedback")
public class UserFeedback {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键自增ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "关联用户表主键user_id")
    private Long userId;

    @TableField("feedback_content")
    @Schema(description = "用户反馈内容")
    private String feedbackContent;

    @TableField("feedback_status")
    @Schema(description = "处理状态 0待处理 1处理中 2已完结")
    private Integer feedbackStatus;

    @TableField("admin_reply")
    @Schema(description = "后台管理员回复内容")
    private String adminReply;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("delete_flag")
    @Schema(description = "逻辑删除 0未删除 1已删除")
    private Integer deleteFlag;
}