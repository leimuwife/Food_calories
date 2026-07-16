package com.nutrition.config;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorCollection;
import com.aliyun.dashvector.common.DashVectorException;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.DocOpResult;
import com.aliyun.dashvector.models.Vector;
import com.aliyun.dashvector.models.requests.InsertDocRequest;
import com.aliyun.dashvector.models.requests.QueryDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import com.nutrition.entity.AiConfig;
import com.nutrition.service.AiConfigService;
import com.nutrition.util.AesUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 食物知识向量化入库工具类
 * 支持项目启动可选执行/手动调用执行，完成全量知识库向量化入库
 * 使用阿里云百炼text-embedding-v1模型进行文本向量化
 * 向量数据存储到阿里云DashVector独立向量服务（使用官方SDK）
 * 
 * API Key来源：
 * - text-embedding-v1向量化模型：数据库ai_config表（与大模型共用）
 * - DashVector向量库：配置文件vector.retrieval.api-key
 */
@Component
@Slf4j
public class FoodKnowledgeEmbeddingRunner implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final VectorRetrievalProperties vectorProperties;
    private final AiConfigService aiConfigService;
    private final AesUtil aesUtil;
    private final RestTemplate restTemplate;
    private final DashVectorClient dashVectorClient;

    public FoodKnowledgeEmbeddingRunner(ObjectMapper objectMapper,
                                         VectorRetrievalProperties vectorProperties,
                                         AiConfigService aiConfigService,
                                         AesUtil aesUtil,
                                         @org.springframework.beans.factory.annotation.Qualifier("aiRestTemplate") RestTemplate restTemplate,
                                         DashVectorClient dashVectorClient) {
        this.objectMapper = objectMapper;
        this.vectorProperties = vectorProperties;
        this.aiConfigService = aiConfigService;
        this.aesUtil = aesUtil;
        this.restTemplate = restTemplate;
        this.dashVectorClient = dashVectorClient;
    }

    private static final String KNOWLEDGE_FILE_PATH = "foodData/food_knowledge.jsonl";
    private static final String EMBEDDING_MODEL = "text-embedding-v1";
    private static final String EMBEDDING_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding-v1/embeddings";

    private static final int QUERY_LIMIT = 100;

    private int totalCount = 0;
    private int successCount = 0;
    private int failCount = 0;
    private int skipCount = 0;

    @Override
    public void run(ApplicationArguments args) {
        boolean enableEmbedding = Boolean.TRUE.equals(vectorProperties.getEnableEmbedding());
        if (!enableEmbedding) {
            log.info("向量入库未启用，如需启动时执行请在application.yml中设置 vector.retrieval.enable-embedding=true 或环境变量 VECTOR_ENABLE_EMBEDDING=true");
            return;
        }

        executeEmbedding();
    }

    public void executeEmbedding() {
        log.info("========== 开始执行食物知识向量化入库 ==========");

        String embeddingApiKey = getEmbeddingApiKeyFromDatabase();
        String vectorDbApiKey = vectorProperties.getApiKey();
        String endpoint = vectorProperties.getEndpoint();
        String collectionName = vectorProperties.getCollectionName();

        log.info("向量化模型API Key: sk-{}***", embeddingApiKey != null && embeddingApiKey.length() > 4 ? embeddingApiKey.substring(2, 6) : "null");
        log.info("向量库API Key: sk-{}***", vectorDbApiKey != null && vectorDbApiKey.length() > 4 ? vectorDbApiKey.substring(2, 6) : "null");
        log.info("DashVector Endpoint: {}", endpoint);

        if (embeddingApiKey == null || embeddingApiKey.isEmpty()) {
            log.error("向量化模型API密钥获取失败，请检查数据库ai_config表是否配置了有效的API Key");
            return;
        }

        if (vectorDbApiKey == null || vectorDbApiKey.isEmpty()) {
            log.error("向量库API密钥未配置，请在application.yml中设置 vector.retrieval.api-key 或环境变量 VECTOR_API_KEY");
            return;
        }

        if (endpoint == null || endpoint.isEmpty()) {
            log.error("DashVector集群Endpoint未配置，请在application.yml中设置 vector.retrieval.endpoint 或环境变量 VECTOR_ENDPOINT");
            return;
        }

        if (dashVectorClient == null) {
            log.error("DashVector客户端初始化失败，请检查配置");
            return;
        }

        log.info("向量库Endpoint: {}, 集合: {}", endpoint, collectionName);

        List<KnowledgeItem> knowledgeItems = readKnowledgeFile();

        if (knowledgeItems.isEmpty()) {
            log.warn("知识库文件为空或读取失败");
            logStatistics();
            return;
        }

        totalCount = knowledgeItems.size();
        log.info("知识库文件读取完成，共{}条数据", totalCount);

        Set<Long> existingIds = queryExistingIds();
        log.info("已存在的food_id数量: {}", existingIds.size());

        List<KnowledgeItem> newItems = knowledgeItems.stream()
                .filter(item -> !existingIds.contains(item.getId()))
                .toList();

        log.info("待入库的新数据数量: {}", newItems.size());

        if (newItems.isEmpty()) {
            log.info("所有数据已存在于向量库中，无需重复入库");
            skipCount = totalCount;
            logStatistics();
            return;
        }

        int batchSize = vectorProperties.getBatchSize();
        for (int i = 0; i < newItems.size(); i += batchSize) {
            int end = Math.min(i + batchSize, newItems.size());
            List<KnowledgeItem> batch = newItems.subList(i, end);
            
            log.info("处理第{}批数据，范围: {}-{}，数量: {}",
                    (i / batchSize) + 1, i + 1, end, batch.size());

            processBatch(batch, embeddingApiKey);

            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logStatistics();
        log.info("========== 食物知识向量化入库完成 ==========");
    }

    private String getEmbeddingApiKeyFromDatabase() {
        AiConfig config = aiConfigService.getEnabledConfigEntity();
        if (config == null) {
            log.error("未找到启用的AI配置，请在数据库ai_config表中配置并启用");
            return null;
        }

        try {
            String decryptedKey = aesUtil.decrypt(config.getApiKey());
            if (decryptedKey != null && !decryptedKey.isEmpty()) {
                log.debug("从数据库获取向量化模型API Key成功，模型名称: {}", config.getModelName());
                return decryptedKey;
            } else {
                log.error("数据库中API Key为空或解密后为空");
                return null;
            }
        } catch (Exception e) {
            log.error("解密数据库API密钥失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    private List<KnowledgeItem> readKnowledgeFile() {
        List<KnowledgeItem> items = new ArrayList<>();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(KNOWLEDGE_FILE_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode node = objectMapper.readTree(line);
                    KnowledgeItem item = new KnowledgeItem();
                    item.setId(node.has("id") ? node.get("id").asLong() : null);
                    item.setFoodName(node.has("food_name") ? node.get("food_name").asText() : null);
                    item.setContent(node.has("content") ? node.get("content").asText() : null);
                    
                    if (item.getId() != null && item.getFoodName() != null && item.getContent() != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    log.warn("解析JSON行失败: line={}, error={}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("读取知识库文件失败: error={}", e.getMessage(), e);
        }
        
        return items;
    }

    private Set<Long> queryExistingIds() {
        Set<Long> existingIds = new HashSet<>();
        
        try {
            String collectionName = vectorProperties.getCollectionName();
            DashVectorCollection collection = dashVectorClient.get(collectionName);
            
            if (collection == null) {
                log.warn("集合不存在: {}", collectionName);
                return existingIds;
            }

            List<Float> dummyVector = new ArrayList<>();
            dummyVector.add(0.0f);
            
            QueryDocRequest request = QueryDocRequest.builder()
                    .vector(Vector.builder().value(dummyVector).build())
                    .topk(QUERY_LIMIT)
                    .build();

            Response<List<Doc>> response = collection.query(request);
            
            if (response != null && response.isSuccess() && response.getOutput() != null) {
                for (Doc doc : response.getOutput()) {
                    Map<String, Object> fields = doc.getFields();
                    if (fields != null && fields.containsKey("food_id")) {
                        try {
                            existingIds.add(Long.parseLong(String.valueOf(fields.get("food_id"))));
                        } catch (NumberFormatException e) {
                            log.warn("解析food_id失败: {}", fields.get("food_id"));
                        }
                    }
                }
            }
        } catch (DashVectorException e) {
            log.error("查询已存在food_id失败: error={}", e.getMessage(), e);
            log.warn("无法查询已存在数据，将尝试全量入库");
        }
        
        return existingIds;
    }

    private void processBatch(List<KnowledgeItem> batch, String embeddingApiKey) {
        List<String> texts = batch.stream()
                .map(KnowledgeItem::getContent)
                .toList();

        List<List<Double>> embeddings = generateEmbeddings(texts, embeddingApiKey);
        
        if (embeddings == null || embeddings.size() != batch.size()) {
            log.error("批量生成向量失败，批次数据全部标记为失败");
            failCount += batch.size();
            return;
        }

        String collectionName = vectorProperties.getCollectionName();
        DashVectorCollection collection = dashVectorClient.get(collectionName);

        if (collection == null) {
            log.error("集合不存在: {}", collectionName);
            failCount += batch.size();
            return;
        }

        for (int i = 0; i < batch.size(); i++) {
            KnowledgeItem item = batch.get(i);
            List<Double> embedding = embeddings.get(i);
            
            if (embedding != null) {
                try {
                    List<Float> floatVector = embedding.stream()
                            .map(Double::floatValue)
                            .toList();

                    Map<String, Object> fields = new HashMap<>();
                    fields.put("food_id", item.getId());
                    fields.put("food_name", item.getFoodName());
                    fields.put("content", item.getContent());

                    Doc doc = Doc.builder()
                            .id(String.valueOf(item.getId()))
                            .vector(Vector.builder().value(floatVector).build())
                            .fields(fields)
                            .build();

                    InsertDocRequest request = InsertDocRequest.builder()
                            .docs(Collections.singletonList(doc))
                            .build();

                    Response<List<DocOpResult>> response = collection.insert(request);
                    
                    if (response != null && response.isSuccess()) {
                        successCount++;
                        log.debug("向量入库成功: food_name={}, food_id={}", item.getFoodName(), item.getId());
                    } else {
                        failCount++;
                        log.warn("向量入库失败: food_name={}, food_id={}, message={}", 
                                item.getFoodName(), item.getId(), 
                                response != null ? response.getMessage() : "unknown");
                    }
                } catch (DashVectorException e) {
                    failCount++;
                    log.error("向量入库异常: food_name={}, food_id={}, error={}", 
                            item.getFoodName(), item.getId(), e.getMessage(), e);
                }
            } else {
                failCount++;
                log.warn("向量生成失败: food_name={}, food_id={}", item.getFoodName(), item.getId());
            }
        }
    }

    private List<List<Double>> generateEmbeddings(List<String> texts, String apiKey) {
        int retryCount = vectorProperties.getRetryCount();
        
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                HttpHeaders headers = buildHeaders(apiKey);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", EMBEDDING_MODEL);
                requestBody.put("input", texts);
                
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_API_URL, request, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode data = root.get("data");
                    
                    if (data != null && data.isArray()) {
                        List<List<Double>> embeddings = new ArrayList<>();
                        for (JsonNode item : data) {
                            JsonNode embeddingNode = item.get("embedding");
                            if (embeddingNode != null && embeddingNode.isArray()) {
                                List<Double> embedding = new ArrayList<>();
                                for (JsonNode value : embeddingNode) {
                                    embedding.add(value.asDouble());
                                }
                                embeddings.add(embedding);
                            } else {
                                embeddings.add(null);
                            }
                        }
                        return embeddings;
                    }
                }
                
                log.warn("向量生成API响应异常: attempt={}, status={}", attempt + 1, response.getStatusCode());
                
            } catch (Exception e) {
                log.warn("向量生成失败: attempt={}, error={}", attempt + 1, e.getMessage());
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

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    private void logStatistics() {
        log.info("========== 向量入库统计 ==========");
        log.info("  - 总处理条数: {}", totalCount);
        log.info("  - 成功条数: {}", successCount);
        log.info("  - 失败条数: {}", failCount);
        log.info("  - 跳过条数: {}", skipCount);
        log.info("  - 成功率: {}%", totalCount > 0 ? String.format("%.2f", (successCount * 100.0 / totalCount)) : "N/A");
        log.info("==================================");
    }

    @lombok.Data
    private static class KnowledgeItem {
        private Long id;
        private String foodName;
        private String content;
    }
}