package com.nutrition.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python-FastAPI AI服务配置属性
 * 配置项从 application.yml 中 fastapi 节点读取
 */
@Data
@Component
@ConfigurationProperties(prefix = "fastapi")
public class FastApiProperties {

    /** FastAPI服务基础URL，如 http://localhost:8004 */
    private String baseUrl = "http://localhost:8004";

    /** API鉴权密钥（与Python端API_SECRET_KEY保持一致） */
    private String apiSecretKey = "your-secret-key-change-in-production";

    /** 热量估算接口路径 */
    private String estimatePath = "/api/ai/estimate-calorie";

    /** 对话接口路径 */
    private String chatPath = "/api/ai/chat";

    /** 知识库文档上传接口路径（调用Python） */
    private String knowledgeUploadPath = "/api/rag/document/upload";

    /** 知识库文档删除接口路径（调用Python） */
    private String knowledgeDeletePath = "/api/rag/document/delete";

    /** 知识库检索接口路径 */
    private String knowledgeSearchPath = "/api/rag/search/query";

    /** 连接超时（秒） */
    private int connectTimeout = 10;

    /** 读取超时（秒） */
    private int readTimeout = 60;
}
