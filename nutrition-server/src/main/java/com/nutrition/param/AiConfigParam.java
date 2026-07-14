package com.nutrition.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI配置参数类
 */
@Data
public class AiConfigParam {

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @NotBlank(message = "API地址不能为空")
    private String apiUrl;

    @NotBlank(message = "API密钥不能为空")
    private String apiKey;
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String systemPrompt;

    @DecimalMin(value = "0.00", message = "温度参数最小为0")
    @DecimalMax(value = "1.00", message = "温度参数最大为1")
    private BigDecimal temperature = new BigDecimal("0.20");

    @Min(value = 1, message = "最大token数至少为1")
    private Integer maxTokens = 800;
}