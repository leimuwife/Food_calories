package com.nutrition.config;

import com.nutrition.client.FastApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * FastApi专用 RestTemplate 配置
 * 独立配置连接超时、读取超时，避免影响其他场景
 *
 * <p>超时参数统一从 fastapi 配置节点（FastApiProperties）读取，
 * 避免与 application.yml 中的配置重复维护。
 * 大JSONL文件上传时Python端处理耗时较长，读取超时需保持较大值（默认120秒）。
 */
@Configuration
@RequiredArgsConstructor
public class FastApiRestTemplateConfig {

    private final FastApiProperties properties;

    @Bean("fastApiRestTemplate")
    public RestTemplate fastApiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeout()))
                .setReadTimeout(Duration.ofSeconds(properties.getReadTimeout()))
                .build();
    }
}
