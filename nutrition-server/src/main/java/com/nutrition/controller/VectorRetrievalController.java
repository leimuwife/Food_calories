package com.nutrition.controller;

import com.nutrition.config.FoodKnowledgeEmbeddingRunner;
import com.nutrition.dto.KnowledgeDTO;
import com.nutrition.service.FoodKnowledgeRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量检索测试接口
 * 用于验证向量检索功能的正确性
 * 包含基础检索、常见食材验证、复合食物验证、无关内容验证等测试场景
 */
@RestController
@RequestMapping("/vector")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "向量检索测试", description = "RAG向量知识库检索功能测试接口")
public class VectorRetrievalController {

    private final FoodKnowledgeRetrievalService foodKnowledgeRetrievalService;
    private final FoodKnowledgeEmbeddingRunner foodKnowledgeEmbeddingRunner;

    /**
     * 基础语义检索接口
     *
     * @param query 查询文本
     * @param topN 召回条数（默认3）
     * @param minSimilarity 最低相似度阈值（默认0.75）
     * @return 检索结果列表
     */
    @GetMapping("/search")
    @Operation(summary = "语义检索", description = "根据查询文本检索食物知识")
    public Map<String, Object> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topN,
            @RequestParam(defaultValue = "0.75") double minSimilarity) {
        
        log.info("向量检索请求: query={}, topN={}, minSimilarity={}", query, topN, minSimilarity);
        
        List<KnowledgeDTO> results = foodKnowledgeRetrievalService.retrieve(query, topN, minSimilarity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("query", query);
        response.put("topN", topN);
        response.put("minSimilarity", minSimilarity);
        response.put("count", results.size());
        response.put("results", results);
        
        return response;
    }

    /**
     * 手动触发向量入库
     *
     * @return 执行状态
     */
    @PostMapping("/embedding/run")
    @Operation(summary = "执行向量入库", description = "手动触发食物知识向量化入库")
    public Map<String, Object> runEmbedding() {
        log.info("手动触发向量入库");
        
        try {
            foodKnowledgeEmbeddingRunner.executeEmbedding();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "向量入库已执行，请查看控制台日志获取详细统计信息");
            
            return response;
        } catch (Exception e) {
            log.error("向量入库执行失败: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "向量入库执行失败: " + e.getMessage());
            
            return response;
        }
    }

    /**
     * 测试场景1：常见食材验证
     * 输入常见食材名称（如"动物奶油""西红柿"），验证召回结果正确、相似度高于阈值
     *
     * @return 测试结果
     */
    @GetMapping("/test/common-food")
    @Operation(summary = "常见食材测试", description = "验证常见食材名称检索结果正确性")
    public Map<String, Object> testCommonFood() {
        log.info("执行常见食材测试");
        
        Map<String, Object> testResults = new HashMap<>();
        
        String[] testCases = {"动物奶油", "西红柿", "大米", "鸡蛋"};
        double threshold = 0.75;
        
        for (String foodName : testCases) {
            List<KnowledgeDTO> results = foodKnowledgeRetrievalService.retrieve(foodName);
            
            Map<String, Object> caseResult = new HashMap<>();
            caseResult.put("query", foodName);
            caseResult.put("count", results.size());
            caseResult.put("results", results);
            
            boolean allAboveThreshold = results.stream()
                    .allMatch(dto -> dto.getSimilarity() >= threshold);
            boolean success = !results.isEmpty() && allAboveThreshold;
            
            caseResult.put("allAboveThreshold", allAboveThreshold);
            caseResult.put("success", success);
            
            testResults.put(foodName, caseResult);
        }
        
        boolean overallSuccess = testResults.values().stream()
                .allMatch(r -> (Boolean) ((Map<String, Object>) r).get("success"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", overallSuccess);
        response.put("threshold", threshold);
        response.put("testCases", testResults);
        
        return response;
    }

    /**
     * 测试场景2：复合食物验证
     * 输入复合食物（如"奶油蛋糕"），验证能召回相关食材与配方知识
     *
     * @return 测试结果
     */
    @GetMapping("/test/compound-food")
    @Operation(summary = "复合食物测试", description = "验证复合食物名称能召回相关食材知识")
    public Map<String, Object> testCompoundFood() {
        log.info("执行复合食物测试");
        
        Map<String, Object> testResults = new HashMap<>();
        
        String[] testCases = {"奶油蛋糕", "西红柿炒蛋", "蛋炒饭", "红烧肉"};
        String[] expectedKeywords = {"奶油", "西红柿", "鸡蛋", "猪肉"};
        
        for (int i = 0; i < testCases.length; i++) {
            String foodName = testCases[i];
            String expectedKeyword = expectedKeywords[i];
            
            List<KnowledgeDTO> results = foodKnowledgeRetrievalService.retrieve(foodName);
            
            Map<String, Object> caseResult = new HashMap<>();
            caseResult.put("query", foodName);
            caseResult.put("expectedKeyword", expectedKeyword);
            caseResult.put("count", results.size());
            caseResult.put("results", results);
            
            boolean containsKeyword = results.stream()
                    .anyMatch(dto -> dto.getFoodName().contains(expectedKeyword) || 
                            dto.getContent().contains(expectedKeyword));
            boolean success = !results.isEmpty() && containsKeyword;
            
            caseResult.put("containsExpectedKeyword", containsKeyword);
            caseResult.put("success", success);
            
            testResults.put(foodName, caseResult);
        }
        
        boolean overallSuccess = testResults.values().stream()
                .allMatch(r -> (Boolean) ((Map<String, Object>) r).get("success"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", overallSuccess);
        response.put("testCases", testResults);
        
        return response;
    }

    /**
     * 测试场景3：无关内容验证
     * 输入无关内容（如"汽车"），验证返回空结果，无误召回
     *
     * @return 测试结果
     */
    @GetMapping("/test/irrelevant-content")
    @Operation(summary = "无关内容测试", description = "验证无关内容查询返回空结果")
    public Map<String, Object> testIrrelevantContent() {
        log.info("执行无关内容测试");
        
        Map<String, Object> testResults = new HashMap<>();
        
        String[] testCases = {"汽车", "手机", "电脑", "书籍"};
        
        for (String query : testCases) {
            List<KnowledgeDTO> results = foodKnowledgeRetrievalService.retrieve(query);
            
            Map<String, Object> caseResult = new HashMap<>();
            caseResult.put("query", query);
            caseResult.put("count", results.size());
            caseResult.put("results", results);
            
            boolean success = results.isEmpty();
            caseResult.put("success", success);
            
            testResults.put(query, caseResult);
        }
        
        boolean overallSuccess = testResults.values().stream()
                .allMatch(r -> (Boolean) ((Map<String, Object>) r).get("success"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", overallSuccess);
        response.put("testCases", testResults);
        
        return response;
    }

    /**
     * 综合测试
     * 一次性执行所有测试场景
     *
     * @return 综合测试结果
     */
    @GetMapping("/test/all")
    @Operation(summary = "综合测试", description = "一次性执行所有测试场景")
    public Map<String, Object> testAll() {
        log.info("执行向量检索综合测试");
        
        Map<String, Object> response = new HashMap<>();
        
        response.put("commonFood", testCommonFood());
        response.put("compoundFood", testCompoundFood());
        response.put("irrelevantContent", testIrrelevantContent());
        
        boolean commonSuccess = (Boolean) ((Map<String, Object>) response.get("commonFood")).get("success");
        boolean compoundSuccess = (Boolean) ((Map<String, Object>) response.get("compoundFood")).get("success");
        boolean irrelevantSuccess = (Boolean) ((Map<String, Object>) response.get("irrelevantContent")).get("success");
        
        response.put("overallSuccess", commonSuccess && compoundSuccess && irrelevantSuccess);
        
        return response;
    }
}