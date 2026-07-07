package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 营养师AI对话记录实体类
 * 对应数据库表 nutritionist_chat
 */
@Data
@TableName("nutritionist_chat")
public class NutritionistChat extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("role")
    @Schema(description = "角色 user用户 / assistantAI")
    private String role;

    @TableField("content")
    @Schema(description = "对话文本")
    private String content;

    @TableField("file_ids")
    @Schema(description = "图片附件ID JSON数组")
    private String fileIds;
}