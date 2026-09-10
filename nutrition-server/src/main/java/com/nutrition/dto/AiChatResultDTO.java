package com.nutrition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI营养师对话结果DTO
 * 承载Python ReAct引擎返回的最终回答及会话标识：
 * 新建对话时 sessionId 由Java雪花算法生成并经Python回传，
 * 调用方需将其返回给前端，后续多轮对话由前端原样携带
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResultDTO {

    /** AI最终回答文本 */
    private String response;

    /** 会话ID（新建对话时为新生成的雪花ID；继续对话时为入参ID） */
    private String sessionId;

    /** 是否为本次新建的会话 */
    private Boolean newSession;
}
