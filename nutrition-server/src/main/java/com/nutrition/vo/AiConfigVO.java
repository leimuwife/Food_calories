package com.nutrition.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI配置视图对象
 * apiKey字段返回脱敏后的值
 */
@Data
public class AiConfigVO {

    private Long id;

    private String modelName;

    private String modelType;

    private String apiUrl;

    private String apiKey;

    private String nickname;

    private String systemPrompt;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Integer isEnabled;
}