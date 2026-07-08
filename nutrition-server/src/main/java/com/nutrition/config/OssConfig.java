package com.nutrition.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String domain;

    @Bean
    public OSS ossClient() {
        if (endpoint == null || endpoint.isEmpty()) {
            log.warn("OSS配置未完成，将使用本地存储");
            return null;
        }
        log.info("初始化阿里云OSS客户端: endpoint={}, bucket={}", endpoint, bucketName);
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}