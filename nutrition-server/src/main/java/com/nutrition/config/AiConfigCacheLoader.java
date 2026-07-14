package com.nutrition.config;

import com.nutrition.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * AI配置缓存加载器
 * 服务启动时自动加载启用的AI配置到Redis缓存
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiConfigCacheLoader implements CommandLineRunner {

    private final AiConfigService aiConfigService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始加载AI配置缓存...");
        try {
            aiConfigService.refreshCache();
            var config = aiConfigService.getEnabledConfig();
            if (config != null) {
                log.info("AI配置缓存加载成功: modelName={}, modelType={}",
                        config.getModelName(), config.getModelType());
            } else {
                log.warn("未找到启用的AI配置，请在后台配置并启用");
            }
        } catch (Exception e) {
            log.error("AI配置缓存加载失败: error={}", e.getMessage(), e);
        }
    }
}