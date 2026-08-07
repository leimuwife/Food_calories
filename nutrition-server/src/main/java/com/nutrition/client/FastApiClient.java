package com.nutrition.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.enums.BizMsgEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Python-FastAPI AI服务调用客户端
 * 负责调用Python RAG知识库的上传、删除、检索接口
 *
 * <p>特性：
 * 1. 专用 RestTemplate，配置独立超时
 * 2. 响应结构统一封装为 PythonApiResponseVO
 * 3. 网络异常自动重试，业务异常快速失败
 * 4. 精细化日志：成功/警告/错误分级
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FastApiClient {

    private final ObjectMapper objectMapper;
    private final FastApiProperties properties;

    @Qualifier("fastApiRestTemplate")
    private final RestTemplate restTemplate;

    // ==================== 请求头构造 ====================

    /**
     * 构造统一鉴权请求头
     */
    private HttpHeaders buildAuthHeaders(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.set("Authorization", "Bearer " + properties.getApiSecretKey());
        return headers;
    }

    // ==================== URL工具 ====================

    /**
     * 拼接完整URL，处理多余斜杠
     */
    private String buildUrl(String path) {
        String base = properties.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    // ==================== 响应解析 ====================

    /**
     * 解析Python返回的通用响应结构
     */
    private <T> PythonApiResponseVO<T> parseResponse(String responseBody, TypeReference<PythonApiResponseVO<T>> typeRef) {
        try {
            return objectMapper.readValue(responseBody, typeRef);
        } catch (Exception e) {
            throw new FastApiBusinessException(
                    BizMsgEnum.RAG_PYTHON_RESPONSE_PARSE_FAILED.getMessage() + ": " + e.getMessage(), e);
        }
    }

    // ==================== 核心业务方法 ====================

    /**
     * 调用Python知识库文档上传接口（multipart/form-data）
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean uploadKnowledgeFile(MultipartFile file, Long docId, String fileMd5) {
        Assert.notNull(file, BizMsgEnum.RAG_FILE_NOT_NULL.getMessage());
        Assert.notNull(docId, BizMsgEnum.RAG_DOC_ID_NOT_NULL.getMessage());
        Assert.hasText(fileMd5, BizMsgEnum.RAG_FILE_MD5_NOT_EMPTY.getMessage());

        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        if (fileSize <= 0) {
            log.error("{}: fileName={}, docId={}", BizMsgEnum.RAG_FILE_EMPTY.getMessage(), originalFilename, docId);
            throw new FastApiBusinessException(BizMsgEnum.RAG_FILE_EMPTY.getMessage());
        }
        if (fileSize > 50 * 1024 * 1024) {
            log.error("{}: fileName={}, size={}MB", BizMsgEnum.RAG_FILE_TOO_LARGE.getMessage(), originalFilename, fileSize / 1024 / 1024);
            throw new FastApiBusinessException(BizMsgEnum.RAG_FILE_TOO_LARGE.getMessage());
        }

        String url = buildUrl(properties.getKnowledgeUploadPath());
        log.info("调用Python知识库上传接口: fileName={}, size={}KB, docId={}, url={}",
                originalFilename, fileSize / 1024, docId, url);

        try {
            HttpHeaders headers = buildAuthHeaders(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("doc_id", String.valueOf(docId));
            body.add("file_md5", fileMd5);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            PythonApiResponseVO<Map<String, Object>> result = parseResponse(
                    response.getBody(), new TypeReference<>() {});

            if (result.isSuccess()) {
                log.info("Python知识库上传成功: fileName={}, docId={}", originalFilename, docId);
                return true;
            } else {
                log.warn("Python知识库上传业务失败: docId={}, code={}, msg={}",
                        docId, result.getCode(), result.getMsg());
                throw new FastApiBusinessException(
                        BizMsgEnum.RAG_PYTHON_UPLOAD_BIZ_FAILED.getMessage() + ": " + result.getMsg());
            }
        } catch (RestClientException e) {
            log.warn("Python知识库上传网络异常(将重试): docId={}, error={}", docId, e.getMessage());
            throw e;
        } catch (FastApiBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Python知识库上传未知异常: docId={}, error={}", docId, e.getMessage(), e);
            throw new FastApiBusinessException(
                    BizMsgEnum.RAG_UPLOAD_FAILED.getMessage() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 调用Python知识库文档删除接口
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean deleteKnowledgeDocument(String docId) {
        Assert.hasText(docId, BizMsgEnum.RAG_DOC_ID_NOT_NULL.getMessage());

        String url = buildUrl(properties.getKnowledgeDeletePath()) + "?doc_id=" + docId;
        log.info("调用Python知识库删除接口: docId={}, url={}", docId, url);

        try {
            HttpHeaders headers = buildAuthHeaders(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, String.class
            );

            PythonApiResponseVO<Map<String, Object>> result = parseResponse(
                    response.getBody(), new TypeReference<>() {});

            if (result.isSuccess()) {
                log.info("Python知识库删除成功: docId={}", docId);
                return true;
            } else {
                log.warn("Python知识库删除业务失败: docId={}, msg={}", docId, result.getMsg());
                return false;
            }
        } catch (RestClientException e) {
            log.warn("Python知识库删除网络异常(将重试): docId={}, error={}", docId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Python知识库删除异常: docId={}, error={}", docId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 调用Python知识库检索接口
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Map<String, Object> searchKnowledge(String query, int topk) {
        Assert.hasText(query, BizMsgEnum.RAG_QUERY_NOT_EMPTY.getMessage());
        Assert.isTrue(topk > 0 && topk <= 50, BizMsgEnum.RAG_TOPK_INVALID.getMessage());

        String url = buildUrl(properties.getKnowledgeSearchPath());
        log.info("调用Python知识库检索接口: query={}, topk={}", query, topk);

        try {
            HttpHeaders headers = buildAuthHeaders(MediaType.APPLICATION_JSON);

            Map<String, Object> params = new HashMap<>();
            params.put("query", query);
            params.put("topk", topk);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            PythonApiResponseVO<Map<String, Object>> result = parseResponse(
                    response.getBody(), new TypeReference<>() {});

            if (result.isSuccess()) {
                log.info("Python知识库检索成功: query={}, results={}", query,
                        result.getData() != null ? result.getData().size() : 0);
                return result.getData();
            } else {
                log.warn("Python知识库检索业务失败: query={}, msg={}", query, result.getMsg());
                throw new FastApiBusinessException(
                        BizMsgEnum.RAG_PYTHON_SEARCH_FAILED.getMessage() + ": " + result.getMsg());
            }
        } catch (RestClientException e) {
            log.warn("Python知识库检索网络异常(将重试): query={}, error={}", query, e.getMessage());
            throw e;
        } catch (FastApiBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Python知识库检索未知异常: query={}, error={}", query, e.getMessage(), e);
            throw new FastApiBusinessException(
                    BizMsgEnum.RAG_PYTHON_SEARCH_CALL_FAILED.getMessage() + ": " + e.getMessage(), e);
        }
    }

    // ==================== 预留方法 ====================

    /**
     * 调用Python热量估算接口（预留）
     */
    public BigDecimal estimateCalorie(String foodDesc, Integer weight) {
        log.debug("FastApiClient.estimateCalorie 预留方法，当前未执行真实调用");
        return BigDecimal.ZERO;
    }

    /**
     * 调用Python对话接口（预留）
     */
    public String chat(String message) {
        log.debug("FastApiClient.chat 预留方法，当前未执行真实调用");
        return null;
    }
}
