package com.nutrition.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置类
 * 用于调用微信接口的 HTTP 客户端配置
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 连接超时时间（秒）
     */
    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    /**
     * 读取超时时间（秒）
     */
    private static final int READ_TIMEOUT_SECONDS = 10;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                .build();
    }
}
