package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 创建AI聊天会话结果VO
 */
@Data
@Builder
@Schema(description = "创建AI聊天会话结果")
public class ChatSessionCreateVO {

    @Schema(description = "会话ID（Java雪花生成，Python以字符串使用）")
    private Long sessionId;

    @Schema(description = "用户ID")
    private Long userId;
}
