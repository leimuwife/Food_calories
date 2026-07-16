package com.nutrition.config;

import com.aliyun.dashvector.DashVectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DashVector客户端配置类
 * 使用阿里云官方SDK创建客户端实例
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DashVectorClientConfig {

    private final VectorRetrievalProperties vectorProperties;

    @Bean
    public DashVectorClient dashVectorClient() {
        String apiKey = vectorProperties.getApiKey();
        String endpoint = vectorProperties.getEndpoint();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DashVector API Key未配置，客户端将无法正常工作");
            return null;
        }

        if (endpoint == null || endpoint.isEmpty()) {
            log.warn("DashVector Endpoint未配置，客户端将无法正常工作");
            return null;
        }

        endpoint = cleanEndpoint(endpoint);
        log.info("初始化DashVector客户端: endpoint={}", endpoint);

        try {
            return new DashVectorClient(apiKey, endpoint);
        } catch (Exception e) {
            log.error("创建DashVector客户端失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    private String cleanEndpoint(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        
        String cleaned = endpoint.trim();
        
        cleaned = cleaned.replaceAll("`", "");
        
        if (cleaned.startsWith("https://")) {
            cleaned = cleaned.substring("https://".length());
        } else if (cleaned.startsWith("http://")) {
            cleaned = cleaned.substring("http://".length());
        }
        
        return cleaned;
    }
}