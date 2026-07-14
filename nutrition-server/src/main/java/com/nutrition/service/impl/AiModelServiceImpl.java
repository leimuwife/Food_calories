package com.nutrition.service.impl;

import com.nutrition.common.BusinessException;
import com.nutrition.entity.AiConfig;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.service.AiConfigService;
import com.nutrition.service.AiModelService;
import com.nutrition.util.AesUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiModelServiceImpl implements AiModelService {

    private final AiConfigService aiConfigService;
    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper;
    private final RestTemplate aiRestTemplate;

    @Override
    public String chat(String userMessage) {
        AiConfig config = aiConfigService.getEnabledConfigEntity();
        if (config == null) {
            throw new BusinessException(BizMsgEnum.AI_CONFIG_NOT_ENABLED);
        }

        return doChat(config, userMessage);
    }

    @Override
    public String test(String testMessage) {
        AiConfig config = aiConfigService.getEnabledConfigEntity();
        if (config == null) {
            throw new BusinessException(BizMsgEnum.AI_CONFIG_NOT_ENABLED);
        }

        return doChat(config, testMessage);
    }

    private String doChat(AiConfig config, String userMessage) {
        String encryptedKey = config.getApiKey();
        String apiKey = aesUtil.decrypt(encryptedKey);
        String apiUrl = config.getApiUrl();
        String modelName = config.getModelName();

        double temperature = config.getTemperature() != null ? config.getTemperature().doubleValue() : 0.7;
        int maxTokens = config.getMaxTokens() != null ? config.getMaxTokens() : 800;

        String systemPrompt = config.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = "你是一位专业的营养健康顾问，请用简洁、专业的语言回答用户关于饮食营养、健康管理方面的问题。";
        }

        String maskedApiKey = maskApiKey(apiKey);
        log.info("AI模型调用参数: model={}, apiUrl={}, apiKey={}, temperature={}, maxTokens={}",
                modelName, apiUrl, maskedApiKey, temperature, maxTokens);

        try {
            String response = callApiWithRestTemplate(apiUrl, apiKey, modelName, systemPrompt, userMessage, temperature, maxTokens);
            log.debug("AI对话完成: model={}, messageLength={}, responseLength={}",
                    modelName, userMessage.length(), response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.error("AI模型调用失败: model={}, apiUrl={}, apiKey={}, error={}", modelName, apiUrl, maskedApiKey, e.getMessage(), e);
            throw new BusinessException(BizMsgEnum.AI_MODEL_CALL_FAILED);
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "******";
        }
        return apiKey.substring(0, 4) + "******" + apiKey.substring(apiKey.length() - 4);
    }

    private String callApiWithRestTemplate(String apiUrl, String apiKey, String modelName,
                                          String systemPrompt, String userMessage,
                                          double temperature, int maxTokens) {
        RestTemplate restTemplate = aiRestTemplate;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        var messages = new java.util.ArrayList<Map<String, String>>();
        var systemMsg = new java.util.HashMap<String, String>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        var userMsg = new java.util.HashMap<String, String>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String fullUrl = apiUrl;
        if (!apiUrl.contains("/chat/completions") && !apiUrl.contains("/v1/chat")) {
            fullUrl = apiUrl + "/chat/completions";
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, request, String.class);
            
            log.debug("API响应状态: {}, 响应体: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").asText();
                    }
                }
                throw new RuntimeException("API响应格式错误: " + response.getBody());
            } else {
                String errorBody = response.getBody();
                log.error("API调用失败: 状态码={}, 响应体={}", response.getStatusCode(), errorBody);
                throw new RuntimeException("API调用失败，状态码: " + response.getStatusCode() + ", 响应: " + errorBody);
            }
        } catch (Exception e) {
            log.error("RestTemplate调用失败: url={}, error={}", fullUrl, e.getMessage(), e);
            throw new RuntimeException("API调用失败: " + e.getMessage(), e);
        }
    }
}