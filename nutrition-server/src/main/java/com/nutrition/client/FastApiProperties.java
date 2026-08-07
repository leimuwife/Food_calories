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

    /** FastAPI服务基础URL，如 http://localhost:8000 */
    private String baseUrl = "http://localhost:8000";

    /** 热量估算接口路径 */
    private String estimatePath = "/api/ai/estimate-calorie";

    /** 对话接口路径 */
    private String chatPath = "/api/ai/chat";

    /** 知识库文档上传接口路径 */
    private String knowledgeUploadPath = "/api/knowledge/upload";

    /** 连接超时（秒） */
    private int connectTimeout = 10;

    /** 读取超时（秒） */
    private int readTimeout = 60;
}
