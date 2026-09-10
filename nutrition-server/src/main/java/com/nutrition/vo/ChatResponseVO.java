package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI对话返回VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI对话返回")
public class ChatResponseVO {

    @Schema(description = "AI回答内容")
    private String response;

    @Schema(description = "会话ID；新建对话时由系统生成并回传，前端后续多轮对话需原样携带")
    private String sessionId;

    @Schema(description = "本次是否为新建会话")
    private Boolean newSession;
}
