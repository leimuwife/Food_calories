package com.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建AI聊天会话请求DTO
 */
@Data
@Schema(description = "创建AI聊天会话请求")
public class ChatSessionCreateDTO {

    @Schema(description = "用户ID（必传）")
    private Long userId;
}
