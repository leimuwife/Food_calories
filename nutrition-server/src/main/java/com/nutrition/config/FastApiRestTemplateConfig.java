package com.nutrition.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * FastApi专用 RestTemplate 配置
 * 独立配置连接超时、读取超时，避免影响其他场景
 */
@Configuration
public class FastApiRestTemplateConfig {

    /**
     * 连接超时（秒）- 网络建连超时
     */
    private static final int CONNECT_TIMEOUT_SECONDS = 10;

    /**
     * 读取超时（秒）- Python AI推理可能耗时较长
     */
    private static final int READ_TIMEOUT_SECONDS = 60;

    @Bean("fastApiRestTemplate")
    public RestTemplate fastApiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                .build();
    }
}
