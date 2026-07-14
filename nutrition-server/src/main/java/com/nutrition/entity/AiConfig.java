package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI大模型配置实体类
 * 对应数据库表 ai_config
 */
@Data
@TableName("ai_config")
public class AiConfig extends Common {

    @TableId(type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Long id;

    @TableField("model_name")
    @Schema(description = "模型标识名称，如 qwen-max、deepseek-chat")
    private String modelName;

    @TableField("model_type")
    @Schema(description = "模型厂商类型，如 openai、dashscope")
    private String modelType;

    @TableField("api_url")
    @Schema(description = "大模型接口请求地址")
    private String apiUrl;

    @TableField("api_key")
    @Schema(description = "API密钥（AES加密存储）")
    private String apiKey;

    @TableField("nickname")
    @Schema(description = "模型昵称，用于前端展示")
    private String nickname;

    @TableField("system_prompt")
    @Schema(description = "系统提示词")
    private String systemPrompt;

    @TableField("temperature")
    @Schema(description = "模型温度参数，范围0~1")
    private BigDecimal temperature;

    @TableField("max_tokens")
    @Schema(description = "单次回答最大token数")
    private Integer maxTokens;

    @TableField("is_enabled")
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer isEnabled;

}