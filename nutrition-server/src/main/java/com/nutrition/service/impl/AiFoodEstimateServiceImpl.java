package com.nutrition.service.impl;

import com.nutrition.config.AiEstimatePromptConfig;
import com.nutrition.config.VectorRetrievalProperties;
import com.nutrition.dto.CalorieEstimateResultDTO;
import com.nutrition.dto.KnowledgeDTO;
import com.nutrition.dto.NutritionDTO;
import com.nutrition.entity.AiConfig;
import com.nutrition.service.AiConfigService;
import com.nutrition.service.AiFoodEstimateService;
import com.nutrition.service.FoodKnowledgeRetrievalService;
import com.nutrition.service.FoodNutritionService;
import com.nutrition.util.AesUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI食物热量估算服务实现类
 * 与AI营养师对话接口完全隔离，独立业务逻辑
 * 核心能力：用户输入食物描述 → 并行检索营养数据+知识库 → 单次大模型调用 → 返回纯热量数值
 */
@Service
@Slf4j
public class AiFoodEstimateServiceImpl implements AiFoodEstimateService {

    private final AiConfigService aiConfigService;
    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FoodNutritionService foodNutritionService;
    private final FoodKnowledgeRetrievalService foodKnowledgeRetrievalService;
    private final AiEstimatePromptConfig promptConfig;
    private final VectorRetrievalProperties vectorProperties;
    private final RestTemplate aiRestTemplate;

    public AiFoodEstimateServiceImpl(AiConfigService aiConfigService,
                                     AesUtil aesUtil,
                                     ObjectMapper objectMapper,
                                     RedisTemplate<String, Object> redisTemplate,
                                     FoodNutritionService foodNutritionService,
                                     FoodKnowledgeRetrievalService foodKnowledgeRetrievalService,
                                     AiEstimatePromptConfig promptConfig,
                                     VectorRetrievalProperties vectorProperties,
                                     @org.springframework.beans.factory.annotation.Qualifier("aiRestTemplate") RestTemplate aiRestTemplate) {
        this.aiConfigService = aiConfigService;
        this.aesUtil = aesUtil;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.foodNutritionService = foodNutritionService;
        this.foodKnowledgeRetrievalService = foodKnowledgeRetrievalService;
        this.promptConfig = promptConfig;
        this.vectorProperties = vectorProperties;
        this.aiRestTemplate = aiRestTemplate;
    }

    private static final String REDIS_CACHE_PREFIX = "ai:estimate:";
    private static final String RATE_LIMIT_PREFIX = "ai:estimate:rate:";

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+\\.?\\d*");

    private static final Map<String, BigDecimal> PREDEFINED_RECIPES;

    static {
        Map<String, BigDecimal> recipes = new HashMap<>();
        recipes.put("蛋糕", new BigDecimal("350"));
        recipes.put("面包", new BigDecimal("280"));
        recipes.put("米饭", new BigDecimal("116"));
        recipes.put("面条", new BigDecimal("130"));
        recipes.put("饺子", new BigDecimal("200"));
        recipes.put("包子", new BigDecimal("250"));
        recipes.put("馒头", new BigDecimal("280"));
        recipes.put("油条", new BigDecimal("380"));
        recipes.put("鸡蛋", new BigDecimal("143"));
        recipes.put("牛奶", new BigDecimal("54"));
        recipes.put("酸奶", new BigDecimal("72"));
        recipes.put("苹果", new BigDecimal("52"));
        recipes.put("香蕉", new BigDecimal("91"));
        recipes.put("西瓜", new BigDecimal("25"));
        recipes.put("草莓", new BigDecimal("32"));
        recipes.put("西红柿", new BigDecimal("18"));
        recipes.put("黄瓜", new BigDecimal("15"));
        recipes.put("胡萝卜", new BigDecimal("41"));
        recipes.put("土豆", new BigDecimal("77"));
        recipes.put("瘦肉", new BigDecimal("143"));
        recipes.put("肥肉", new BigDecimal("615"));
        recipes.put("鱼", new BigDecimal("120"));
        recipes.put("虾", new BigDecimal("80"));
        recipes.put("鸡肉", new BigDecimal("167"));
        recipes.put("牛肉", new BigDecimal("125"));
        recipes.put("猪肉", new BigDecimal("143"));
        recipes.put("羊肉", new BigDecimal("118"));
        recipes.put("奶油", new BigDecimal("879"));
        recipes.put("奶酪", new BigDecimal("328"));
        recipes.put("巧克力", new BigDecimal("586"));
        recipes.put("饼干", new BigDecimal("433"));
        recipes.put("薯片", new BigDecimal("536"));
        recipes.put("冰淇淋", new BigDecimal("127"));
        recipes.put("可乐", new BigDecimal("42"));
        recipes.put("果汁", new BigDecimal("45"));
        recipes.put("咖啡", new BigDecimal("1"));
        PREDEFINED_RECIPES = Collections.unmodifiableMap(recipes);
    }

    @Override
    public CalorieEstimateResultDTO estimateCalorie(String foodDesc, Integer weight) {
        long startTime = System.currentTimeMillis();
        
        if (foodDesc == null || foodDesc.trim().isEmpty()) {
            log.warn("AI热量估算：食物描述为空");
            return CalorieEstimateResultDTO.builder().totalCalorie(BigDecimal.ZERO).build();
        }

        final String trimmedFoodDesc = foodDesc.trim();
        log.info("AI热量估算开始: foodDesc={}, weight={}", trimmedFoodDesc, weight);

        String cacheKey = buildCacheKey(foodDesc, weight);

        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                BigDecimal calorie = new BigDecimal(cachedValue.toString());
                log.info("AI热量估算：Redis缓存命中，耗时={}ms，calorie={}", 
                        System.currentTimeMillis() - startTime, calorie);
                return CalorieEstimateResultDTO.builder().totalCalorie(calorie).build();
            }
        } catch (Exception e) {
            log.warn("AI热量估算：Redis缓存查询失败，继续执行估算: error={}", e.getMessage());
        }

        if (!checkRateLimit()) {
            log.warn("AI热量估算：请求频率超限");
            return CalorieEstimateResultDTO.builder().totalCalorie(BigDecimal.ZERO).build();
        }

        List<String> extractedFoods = extractFoodNames(trimmedFoodDesc);
        log.debug("AI热量估算：提取食材列表={}", extractedFoods);

        Map<String, NutritionDTO> nutritionData = Collections.emptyMap();
        List<KnowledgeDTO> knowledgeData = Collections.emptyList();
        boolean redisSuccess = true;
        boolean vectorSuccess = true;

        CompletableFuture<Map<String, NutritionDTO>> nutritionTask = CompletableFuture.supplyAsync(() -> {
            try {
                if (!extractedFoods.isEmpty()) {
                    return foodNutritionService.batchGetNutrition(extractedFoods);
                }
            } catch (Exception e) {
                log.error("AI热量估算：Redis食材查询失败: error={}", e.getMessage(), e);
            }
            return Collections.emptyMap();
        });

        CompletableFuture<List<KnowledgeDTO>> knowledgeTask = CompletableFuture.supplyAsync(() -> {
            try {
                return foodKnowledgeRetrievalService.retrieve(trimmedFoodDesc);
            } catch (Exception e) {
                log.error("AI热量估算：向量检索失败: error={}", e.getMessage(), e);
            }
            return Collections.emptyList();
        });

        try {
            CompletableFuture.allOf(nutritionTask, knowledgeTask)
                    .get(5, TimeUnit.SECONDS);

            nutritionData = nutritionTask.get();
            knowledgeData = knowledgeTask.get();
            redisSuccess = !nutritionData.isEmpty();
            vectorSuccess = !knowledgeData.isEmpty();

        } catch (TimeoutException e) {
            log.warn("AI热量估算：并行检索超时");
        } catch (InterruptedException | ExecutionException e) {
            log.error("AI热量估算：并行检索异常: error={}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        long retrievalTime = System.currentTimeMillis() - startTime;
        log.info("AI热量估算：并行检索完成，耗时={}ms，redisHit={}种，vectorHit={}条", 
                retrievalTime, nutritionData.size(), knowledgeData.size());

        BigDecimal totalCalorie = executeEstimation(trimmedFoodDesc, weight, nutritionData, knowledgeData, 
                redisSuccess, vectorSuccess);

        if (totalCalorie.compareTo(BigDecimal.ZERO) > 0) {
            try {
                int cacheTtlHours = 24;
                redisTemplate.opsForValue().set(cacheKey, totalCalorie.toString(), 
                        cacheTtlHours, TimeUnit.HOURS);
                log.debug("AI热量估算结果已缓存: key={}, ttl={}小时", cacheKey, cacheTtlHours);
            } catch (Exception e) {
                log.warn("AI热量估算：缓存写入失败: error={}", e.getMessage());
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("AI热量估算完成: foodDesc={}, calorie={}, 总耗时={}ms", 
                trimmedFoodDesc, totalCalorie, totalTime);

        return CalorieEstimateResultDTO.builder().totalCalorie(totalCalorie).build();
    }

    private BigDecimal executeEstimation(String foodDesc, Integer weight, Map<String, NutritionDTO> nutritionData,
                                          List<KnowledgeDTO> knowledgeData, boolean redisSuccess, boolean vectorSuccess) {
        AiConfig config = aiConfigService.getEnabledConfigEntity();
        if (config == null) {
            log.error("AI热量估算：未找到启用的AI配置，执行降级计算");
            return fallbackCalculate(foodDesc, weight, nutritionData, knowledgeData);
        }

        String apiKey = decryptApiKey(config.getApiKey());
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("AI热量估算：API密钥无效，执行降级计算");
            return fallbackCalculate(foodDesc, weight, nutritionData, knowledgeData);
        }

        try {
            String nutritionJson = serializeNutritionData(nutritionData);
            String knowledgeText = serializeKnowledgeData(knowledgeData);

            String weightStr = weight != null ? weight + "克" : "未指定";
            String userPrompt = promptConfig.getUserPromptTemplate()
                    .replace("{foodDesc}", foodDesc)
                    .replace("{weight}", weightStr)
                    .replace("{nutritionData}", nutritionJson)
                    .replace("{knowledgeData}", knowledgeText);

            String systemPrompt = promptConfig.getSystemPrompt();

            long modelStartTime = System.currentTimeMillis();
            String response = callModel(apiKey, config.getApiUrl(), "qwen-plus", systemPrompt, userPrompt);
            long modelTime = System.currentTimeMillis() - modelStartTime;
            
            log.info("AI热量估算：大模型调用完成，耗时={}ms，response={}", modelTime, response);

            BigDecimal calorie = parseCalorieResponse(response);

            if (validateCalorie(calorie)) {
                return calorie.setScale(1, RoundingMode.HALF_UP);
            } else {
                log.warn("AI热量估算：数值验证失败(calorie={})，执行降级计算", calorie);
                return fallbackCalculate(foodDesc, weight, nutritionData, knowledgeData);
            }

        } catch (Exception e) {
            log.error("AI热量估算：大模型调用失败: error={}", e.getMessage(), e);
            log.warn("AI热量估算：大模型异常，执行降级计算");
            return fallbackCalculate(foodDesc, weight, nutritionData, knowledgeData);
        }
    }

    private BigDecimal fallbackCalculate(String foodDesc, Integer weight, Map<String, NutritionDTO> nutritionData,
                                          List<KnowledgeDTO> knowledgeData) {
        log.info("AI热量估算：执行降级计算");

        if (!nutritionData.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal weightFactor = weight != null && weight > 0 
                    ? new BigDecimal(weight).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    : new BigDecimal("0.5");
            for (NutritionDTO dto : nutritionData.values()) {
                BigDecimal calorie = dto.getCalorie() != null ? dto.getCalorie() : BigDecimal.ZERO;
                total = total.add(calorie.multiply(weightFactor));
            }
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                log.info("AI热量估算：降级计算成功(使用Redis数据)，calorie={}", total);
                return total.setScale(1, RoundingMode.HALF_UP);
            }
        }

        if (!knowledgeData.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (KnowledgeDTO knowledge : knowledgeData) {
                BigDecimal calorie = extractCalorieFromKnowledge(knowledge.getContent());
                total = total.add(calorie);
            }
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                log.info("AI热量估算：降级计算成功(使用RAG知识)，calorie={}", total);
                return total.setScale(1, RoundingMode.HALF_UP);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : PREDEFINED_RECIPES.entrySet()) {
            if (foodDesc.contains(entry.getKey())) {
                total = total.add(entry.getValue().multiply(new BigDecimal("0.5")));
            }
        }

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            log.info("AI热量估算：降级计算成功(使用预制配方)，calorie={}", total);
            return total.setScale(1, RoundingMode.HALF_UP);
        }

        log.warn("AI热量估算：所有降级策略均未获得有效结果");
        return BigDecimal.ZERO;
    }

    private boolean checkRateLimit() {
        String clientKey = "default";
        String rateKey = RATE_LIMIT_PREFIX + clientKey;
        int limit = 8;

        try {
            Long count = redisTemplate.opsForValue().increment(rateKey);
            if (count == 1) {
                redisTemplate.expire(rateKey, 60, TimeUnit.SECONDS);
            }
            return count != null && count <= limit;
        } catch (Exception e) {
            log.warn("AI热量估算：限流检查失败，允许请求: error={}", e.getMessage());
            return true;
        }
    }

    private List<String> extractFoodNames(String foodDesc) {
        List<String> foods = new ArrayList<>();
        
        for (String key : PREDEFINED_RECIPES.keySet()) {
            if (foodDesc.contains(key)) {
                foods.add(key);
            }
        }

        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
        Matcher matcher = pattern.matcher(foodDesc);
        while (matcher.find()) {
            String word = matcher.group();
            if (!foods.contains(word) && word.length() >= 2) {
                foods.add(word);
            }
        }

        return foods.stream().distinct().limit(20).toList();
    }

    private String serializeNutritionData(Map<String, NutritionDTO> nutritionData) {
        if (nutritionData == null || nutritionData.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        nutritionData.forEach((name, dto) -> {
            sb.append(name).append(": 热量=").append(dto.getCalorie())
              .append("kcal, 蛋白质=").append(dto.getProtein())
              .append("g, 脂肪=").append(dto.getFat())
              .append("g, 碳水=").append(dto.getCarbohydrate()).append("g\n");
        });
        return sb.toString();
    }

    private String serializeKnowledgeData(List<KnowledgeDTO> knowledgeData) {
        if (knowledgeData == null || knowledgeData.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        for (KnowledgeDTO knowledge : knowledgeData) {
            sb.append("【").append(knowledge.getFoodName())
              .append("】").append(knowledge.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String callModel(String apiKey, String apiUrl, String modelName,
                             String systemPrompt, String userPrompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 100);

        var messages = new ArrayList<Map<String, String>>();
        var systemMsg = new HashMap<String, String>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        var userMsg = new HashMap<String, String>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String fullUrl = apiUrl;
        if (!apiUrl.contains("/chat/completions") && !apiUrl.contains("/v1/chat")) {
            fullUrl = apiUrl + "/chat/completions";
        }

        ResponseEntity<String> response = aiRestTemplate.postForEntity(fullUrl, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText().trim();
                }
            }
            throw new RuntimeException("API响应格式错误: " + response.getBody());
        } else {
            throw new RuntimeException("API调用失败，状态码: " + response.getStatusCode());
        }
    }

    private BigDecimal parseCalorieResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(response);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group());
            } catch (NumberFormatException e) {
                log.warn("AI热量估算：解析数值失败: response={}", response);
                return BigDecimal.ZERO;
            }
        }

        log.warn("AI热量估算：未找到有效数值: response={}", response);
        return BigDecimal.ZERO;
    }

    private boolean validateCalorie(BigDecimal calorie) {
        if (calorie == null) {
            return false;
        }
        int minCalorie = 0;
        int maxCalorie = 10000;
        return calorie.compareTo(BigDecimal.valueOf(minCalorie)) > 0 
                && calorie.compareTo(BigDecimal.valueOf(maxCalorie)) < 0;
    }

    private BigDecimal extractCalorieFromKnowledge(String content) {
        if (content == null || content.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Pattern caloriePattern = Pattern.compile("能量\\s*[:：]\\s*(\\d+(\\.\\d+)?)\\s*千卡");
        Matcher matcher = caloriePattern.matcher(content);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        Pattern kcalPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*kcal");
        matcher = kcalPattern.matcher(content);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return BigDecimal.ZERO;
    }

    private String buildCacheKey(String foodDesc, Integer weight) {
        try {
            String cacheInput = foodDesc + ":" + (weight != null ? weight : "null");
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(cacheInput.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return REDIS_CACHE_PREFIX + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return REDIS_CACHE_PREFIX + (foodDesc + ":" + weight).hashCode();
        }
    }

    private String decryptApiKey(String encryptedKey) {
        try {
            return aesUtil.decrypt(encryptedKey);
        } catch (Exception e) {
            log.error("解密API密钥失败: error={}", e.getMessage(), e);
            return null;
        }
    }
}