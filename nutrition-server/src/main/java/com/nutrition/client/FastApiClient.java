package com.nutrition.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Python-FastAPI AI服务调用客户端
 * 预留调用Python-FastAPI服务的能力，当前阶段仅提供方法签名，不执行真实调用
 * 后续正式接入AI服务时，将RestTemplate配置指向FastAPI地址即可
 *
 * <p>使用方式：
 * <pre>{@code
 * @Autowired
 * private FastApiClient fastApiClient;
 *
 * // 热量估算
 * BigDecimal calorie = fastApiClient.estimateCalorie("200g水煮西兰花", 200);
 *
 * // AI对话
 * String response = fastApiClient.chat("今天吃什么比较健康？");
 * }</pre>
 */
@Component
@Slf4j
public class FastApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FastApiProperties properties;

    public FastApiClient(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         FastApiProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 调用Python-FastAPI热量估算接口
     *
     * @param foodDesc 食物描述
     * @param weight   重量（克）
     * @return 估算热量（千卡）
     */
    public BigDecimal estimateCalorie(String foodDesc, Integer weight) {
        // TODO: 取消注释以下代码，正式接入Python-FastAPI AI服务
        // String url = buildUrl(properties.getEstimatePath());
        // Map<String, Object> params = new HashMap<>();
        // params.put("foodDesc", foodDesc);
        // if (weight != null) {
        //     params.put("weight", weight);
        // }
        // Map<String, Object> result = postForObject(url, params, new TypeReference<>() {});
        // return new BigDecimal(result.get("totalCalorie").toString());

        log.debug("FastApiClient.estimateCalorie 预留方法，当前未执行真实调用");
        return BigDecimal.ZERO;
    }

    /**
     * 调用Python-FastAPI对话接口
     *
     * @param message 用户消息
     * @return AI回复内容
     */
    public String chat(String message) {
        // TODO: 取消注释以下代码，正式接入Python-FastAPI AI服务
        // String url = buildUrl(properties.getChatPath());
        // Map<String, Object> params = new HashMap<>();
        // params.put("content", message);
        // Map<String, Object> result = postForObject(url, params, new TypeReference<>() {});
        // return result.get("response").toString();

        log.debug("FastApiClient.chat 预留方法，当前未执行真实调用");
        return null;
    }

    /**
     * 调用Python-FastAPI知识库文档上传接口
     *
     * @param fileName 文件名
     * @param content  文件内容（文本）
     * @return 上传结果标识
     */
    public String uploadKnowledgeDocument(String fileName, String content) {
        // TODO: 取消注释以下代码，正式接入Python-FastAPI AI服务
        // String url = buildUrl(properties.getKnowledgeUploadPath());
        // Map<String, Object> params = new HashMap<>();
        // params.put("fileName", fileName);
        // params.put("content", content);
        // Map<String, Object> result = postForObject(url, params, new TypeReference<>() {});
        // return result.get("uploadId").toString();

        log.debug("FastApiClient.uploadKnowledgeDocument 预留方法，当前未执行真实调用");
        return null;
    }

    /**
     * 通用POST请求方法
     */
    private <T> T postForObject(String url, Object requestBody, TypeReference<T> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), typeRef);
            }
            throw new RuntimeException("FastAPI调用失败: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("FastAPI调用异常: url={}, error={}", url, e.getMessage(), e);
            throw new RuntimeException("FastAPI服务调用失败", e);
        }
    }

    private String buildUrl(String path) {
        String base = properties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
