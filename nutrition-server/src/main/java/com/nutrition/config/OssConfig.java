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

    /**
     * 是否为私有 Bucket 模式。
     * false（默认）：上传时把对象 ACL 设为 PublicRead，返回可直接访问的公开 URL（保持现状）。
     * true：不设置公开读，读取时由后端生成短期签名 URL。需同时在阿里云控制台把 Bucket 读写权限设为「私有」。
     */
    private boolean privateBucket = false;

    /** 私有模式下签名 URL 的有效期（秒），默认 1 小时 */
    private long signedUrlExpireSeconds = 3600;

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
