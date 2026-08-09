package com.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 会话消息批量落盘请求DTO（Python flush_session_to_mysql 回调）
 */
@Data
@Schema(description = "会话消息批量落盘请求")
public class ChatMessageFlushDTO {

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "用户ID（会话不存在需创建时使用）")
    private Long userId;

    @Schema(description = "消息列表")
    private List<ChatMessageItemDTO> messages;
}
