package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI聊天会话实体类
 * 对应数据库表 chat_session
 */
@Data
@TableName("chat_session")
public class NutritionistChat extends Common {

    @TableId(value = "session_id", type = IdType.ASSIGN_ID)
    @Schema(description = "会话唯一标识，前端携带")
    private Long sessionId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

}
