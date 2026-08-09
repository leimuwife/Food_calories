package com.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单条聊天消息DTO（Python回调批量落盘用）
 */
@Data
@Schema(description = "聊天消息")
public class ChatMessageItemDTO {

    @Schema(description = "角色：user/ai_thought/tool_call/tool_result/ai_answer")
    private String role;

    @Schema(description = "消息内容")
    private String content;
}
