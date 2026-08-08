package com.nutrition.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.entity.AiConfig;
import com.nutrition.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI模型配置Redis缓存预热器
 * 项目启动完成后自动执行，将当前启用的AI配置（is_enabled=1）预热到Redis
 * 使用Redis String结构存储，Key为 ai:config:enabled，value为精简配置JSON
 * Python端通过此缓存读取LLM配置（api_key已解密），无需在Python侧配置.env
 * 设置过期时间为7天作为兜底机制，异常时不阻断项目启动
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiConfigCacheRunner implements ApplicationRunner {

    private final AiConfigService aiConfigService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Key - 存储当前启用的AI配置
     */
    private static final String REDIS_KEY = "ai:config:enabled";

    /**
     * Redis过期时间（7天）
     */
    private static final long REDIS_EXPIRE_DAYS = 7;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始执行AI配置Redis缓存预热 ==========");

        try {
            // 查询当前启用的AI配置（apiKey已在Service层解密）
            AiConfig config = aiConfigService.getEnabledConfig();
            if (config == null) {
                log.warn("无启用的AI配置，跳过缓存预热");
                log.info("========== AI配置Redis缓存预热完成（无启用配置） ==========");
                return;
            }

            // 构建精简配置Map（仅保留Python端需要的字段，避免序列化Common基类冗余字段）
            Map<String, Object> configMap = new HashMap<>(8);
            configMap.put("modelName", config.getModelName());
            configMap.put("apiUrl", config.getApiUrl());
            configMap.put("apiKey", config.getApiKey());
            configMap.put("temperature", config.getTemperature());
            configMap.put("maxTokens", config.getMaxTokens());
            configMap.put("systemPrompt", config.getSystemPrompt());

            String jsonValue = serializeConfigMap(configMap);

            // 写入Redis（String结构）
            redisTemplate.opsForValue().set(REDIS_KEY, jsonValue);
            redisTemplate.expire(REDIS_KEY, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);

            log.info("AI配置Redis缓存预热成功: modelName={}, key={}, expire={}天",
                    config.getModelName(), REDIS_KEY, REDIS_EXPIRE_DAYS);
        } catch (Exception e) {
            log.error("AI配置缓存预热异常: error={}", e.getMessage(), e);
            log.warn("缓存预热失败，不阻断项目启动");
        }

        log.info("========== AI配置Redis缓存预热完成 ==========");
    }

    /**
     * 序列化配置Map为JSON字符串
     */
    private String serializeConfigMap(Map<String, Object> configMap) {
        try {
            return objectMapper.writeValueAsString(configMap);
        } catch (JsonProcessingException e) {
            log.error("序列化AI配置失败: error={}", e.getMessage(), e);
            throw new RuntimeException("AI配置序列化失败", e);
        }
    }
}
