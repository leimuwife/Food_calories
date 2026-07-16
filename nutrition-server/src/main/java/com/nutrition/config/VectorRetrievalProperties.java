package com.nutrition.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量检索配置属性类
 * 统一管理向量检索相关的配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "vector.retrieval")
public class VectorRetrievalProperties {

    /**
     * 向量集群名称
     */
    private String clusterName;

    /**
     * 向量集合名称
     */
    private String collectionName = "food_nutrition_knowledge";

    /**
     * 默认召回条数
     */
    private Integer defaultTopN = 3;

    /**
     * 默认最低相似度阈值
     */
    private Double defaultMinSimilarity = 0.75;

    /**
     * 批量处理大小
     */
    private Integer batchSize = 10;

    /**
     * 重试次数
     */
    private Integer retryCount = 2;

    /**
     * 缓存有效期（天）
     */
    private Integer cacheTtlDays = 7;

    /**
     * 是否启用启动时向量入库
     */
    private Boolean enableEmbedding = false;

    /**
     * 向量服务API密钥（DashVector独立服务）
     */
    private String apiKey;

    /**
     * DashVector集群公网访问Endpoint
     */
    private String endpoint;

    @PostConstruct
    public void init() {
        if (endpoint != null) {
            endpoint = endpoint.trim();
            if (endpoint.endsWith(",")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1).trim();
            }
        }
    }
}