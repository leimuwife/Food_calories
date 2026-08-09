package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息VO（历史记录查询返回）
 */
@Data
@Builder
@Schema(description = "聊天消息")
public class ChatMessageVO {

    @Schema(description = "角色：user/ai_thought/tool_call/tool_result/ai_answer")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
