package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("chat_message")
public class AIChatMessage extends Common{
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("session_id")
    @Schema(description = "会话ID")
    private Long sessionId;

    @TableField("role")
    @Schema(description = "消息类型:role 枚举可选值：user、tool_call、tool_result、ai_answer")
    private Long role;

    @TableField("content")
    @Schema(description = "消息内容")
    private String content;

}
