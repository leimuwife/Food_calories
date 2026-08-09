package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI聊天会话列表项VO（前端会话列表展示）
 */
@Data
@Builder
@Schema(description = "AI聊天会话列表项")
public class ChatSessionVO {

    @Schema(description = "会话ID（雪花生成）")
    private Long sessionId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "最近一条用户消息（列表标题预览）")
    private String lastMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
