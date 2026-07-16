package com.nutrition.service.impl;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorCollection;
import com.aliyun.dashvector.common.DashVectorException;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.Vector;
import com.aliyun.dashvector.models.requests.QueryDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import com.nutrition.config.VectorRetrievalProperties;
import com.nutrition.dto.KnowledgeDTO;
import com.nutrition.entity.AiConfig;
import com.nutrition.service.AiConfigService;
import com.nutrition.service.FoodKnowledgeRetrievalService;
import com.nutrition.util.AesUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 食物知识向量检索服务实现类
 * 使用阿里云百炼text-embedding-v1模型进行文本向量化
 * 使用阿里云DashVector独立向量服务进行语义检索（使用官方SDK）
 * 
 * API Key来源：
 * - text-embedding-v1向量化模型：数据库ai_config表（与大模型共用）
 * - DashVector向量库：配置文件vector.retrieval.api-key
 */
@Service
@Slf4j
public class FoodKnowledgeRetrievalServiceImpl implements FoodKnowledgeRetrievalService {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final VectorRetrievalProperties vectorProperties;
    private final AiConfigService aiConfigService;
    private final AesUtil aesUtil;
    private final RestTemplate restTemplate;
    private final DashVectorClient dashVectorClient;

    public FoodKnowledgeRetrievalServiceImpl(ObjectMapper objectMapper,
                                             StringRedisTemplate stringRedisTemplate,
                                             VectorRetrievalProperties vectorProperties,
                                             AiConfigService aiConfigService,
                                             AesUtil aesUtil,
                                             @org.springframework.beans.factory.annotation.Qualifier("aiRestTemplate") RestTemplate restTemplate,
                                             DashVectorClient dashVectorClient) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.vectorProperties = vectorProperties;
        this.aiConfigService = aiConfigService;
        this.aesUtil = aesUtil;
        this.restTemplate = restTemplate;
        this.dashVectorClient = dashVectorClient;
    }

    private static final String EMBEDDING_MODEL = "text-embedding-v1";
    private static final String EMBEDDING_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding-v1/embeddings";

    @Override
    public List<KnowledgeDTO> retrieve(String query, int topN, double minSimilarity) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("查询文本为空");
            return Collections.emptyList();
        }

        String cacheKey = buildCacheKey(query, topN, minSimilarity);
        String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cachedResult != null) {
            log.debug("向量检索命中缓存: query={}", query);
            try {
                List<KnowledgeDTO> cachedList = objectMapper.readValue(cachedResult, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, KnowledgeDTO.class));
                return cachedList;
            } catch (Exception e) {
                log.warn("解析缓存结果失败: error={}", e.getMessage());
            }
        }

        List<Double> queryVector = generateQueryVector(query);
        
        if (queryVector == null || queryVector.isEmpty()) {
            log.warn("查询向量生成失败");
            return Collections.emptyList();
        }

        List<Map<String, Object>> rawResults = searchVectorDB(queryVector, topN);
        
        if (rawResults == null || rawResults.isEmpty()) {
            log.debug("向量检索无结果");
            return Collections.emptyList();
        }

        List<KnowledgeDTO> results = convertToDTO(rawResults, minSimilarity);
        
        if (!results.isEmpty()) {
            try {
                String jsonResult = objectMapper.writeValueAsString(results);
                int cacheTtlDays = vectorProperties.getCacheTtlDays() != null ? vectorProperties.getCacheTtlDays() : 7;
                stringRedisTemplate.opsForValue().set(cacheKey, jsonResult, cacheTtlDays, TimeUnit.DAYS);
                log.debug("向量检索结果已缓存: query={}, cacheKey={}", query, cacheKey);
            } catch (Exception e) {
                log.warn("缓存向量检索结果失败: error={}", e.getMessage());
            }
        }

        return results;
    }

    @Override
    public List<KnowledgeDTO> retrieve(String query) {
        int defaultTopN = vectorProperties.getDefaultTopN() != null ? vectorProperties.getDefaultTopN() : 3;
        double defaultThreshold = vectorProperties.getDefaultMinSimilarity() != null ? vectorProperties.getDefaultMinSimilarity() : 0.75;
        return retrieve(query, defaultTopN, defaultThreshold);
    }

    private String buildCacheKey(String query, int topN, double minSimilarity) {
        try {
            String rawKey = query + ":" + topN + ":" + minSimilarity;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "vector:retrieval:" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "vector:retrieval:" + query.hashCode();
        }
    }

    private List<Double> generateQueryVector(String query) {
        String apiKey = getEmbeddingApiKeyFromDatabase();
        
        if (apiKey == null) {
            log.error("向量化模型API密钥获取失败");
            return null;
        }

        int retryCount = vectorProperties.getRetryCount() != null ? vectorProperties.getRetryCount() : 2;
        
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                HttpHeaders headers = buildHeaders(apiKey);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", EMBEDDING_MODEL);
                requestBody.put("input", query);
                
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_API_URL, request, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode data = root.get("data");
                    
                    if (data != null && data.isArray() && data.size() > 0) {
                        JsonNode embeddingNode = data.get(0).get("embedding");
                        if (embeddingNode != null && embeddingNode.isArray()) {
                            List<Double> embedding = new ArrayList<>();
                            for (JsonNode value : embeddingNode) {
                                embedding.add(value.asDouble());
                            }
                            log.debug("查询向量生成成功: query={}, dimension={}", query, embedding.size());
                            return embedding;
                        }
                    }
                }
                
                log.warn("查询向量生成API响应异常: attempt={}, status={}", attempt + 1, response.getStatusCode());
                
            } catch (Exception e) {
                log.warn("查询向量生成失败: attempt={}, error={}", attempt + 1, e.getMessage());
            }
            
            if (attempt < retryCount) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000L * (attempt + 1));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return null;
    }

    private String getEmbeddingApiKeyFromDatabase() {
        AiConfig config = aiConfigService.getEnabledConfigEntity();
        if (config == null) {
            log.error("未找到启用的AI配置");
            return null;
        }

        try {
            return aesUtil.decrypt(config.getApiKey());
        } catch (Exception e) {
            log.error("解密数据库API密钥失败: error={}", e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> searchVectorDB(List<Double> queryVector, int topN) {
        try {
            if (dashVectorClient == null) {
                log.error("DashVector客户端未初始化");
                return null;
            }

            String collectionName = vectorProperties.getCollectionName();
            DashVectorCollection collection = dashVectorClient.get(collectionName);

            if (collection == null) {
                log.error("集合不存在: {}", collectionName);
                return null;
            }

            List<Float> floatVector = queryVector.stream()
                    .map(Double::floatValue)
                    .toList();

            QueryDocRequest request = QueryDocRequest.builder()
                    .vector(Vector.builder().value(floatVector).build())
                    .topk(topN)
                    .build();

            Response<List<Doc>> response = collection.query(request);

            if (response != null && response.isSuccess() && response.getOutput() != null) {
                List<Map<String, Object>> searchResults = new ArrayList<>();
                for (Doc doc : response.getOutput()) {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("id", doc.getId());
                    resultMap.put("score", doc.getScore());
                    resultMap.put("metadata", doc.getFields());
                    searchResults.add(resultMap);
                }
                return searchResults;
            }
        } catch (DashVectorException e) {
            log.error("向量检索异常: error={}", e.getMessage(), e);
        }

        return null;
    }

    private List<KnowledgeDTO> convertToDTO(List<Map<String, Object>> rawResults, double minSimilarity) {
        List<KnowledgeDTO> results = new ArrayList<>();
        
        for (Map<String, Object> raw : rawResults) {
            try {
                double score = raw.get("score") != null ? ((Number) raw.get("score")).doubleValue() : 0.0;
                
                if (score < minSimilarity) {
                    log.debug("相似度低于阈值，过滤: score={}, threshold={}", score, minSimilarity);
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) raw.get("metadata");
                
                if (metadata == null) {
                    continue;
                }

                KnowledgeDTO dto = new KnowledgeDTO();
                
                if (metadata.containsKey("food_id")) {
                    dto.setFoodId(Long.parseLong(String.valueOf(metadata.get("food_id"))));
                }
                
                if (metadata.containsKey("food_name")) {
                    dto.setFoodName(String.valueOf(metadata.get("food_name")));
                }
                
                if (metadata.containsKey("content")) {
                    dto.setContent(String.valueOf(metadata.get("content")));
                }
                
                dto.setSimilarity(score);
                results.add(dto);
                
            } catch (Exception e) {
                log.warn("转换检索结果失败: raw={}, error={}", raw, e.getMessage());
            }
        }

        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        
        return results;
    }

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }
}