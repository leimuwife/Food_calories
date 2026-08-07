package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * AI模型配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_config")
public class AiConfig extends Common {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型名称 */
    private String modelName;

    /** 昵称 */
    private String nickname;

    /** 模型类型 */
    private String modelType;

    /** API地址 */
    private String apiUrl;

    /** API密钥（AES加密存储） */
    private String apiKey;

    /** 系统提示词 */
    private String systemPrompt;

    /** 温度参数 */
    private BigDecimal temperature;

    /** 最大Token数 */
    private Integer maxTokens;

    /** 是否启用 0-未启用 1-已启用 */
    @TableField("is_enabled")
    private Integer isEnabled;
}
