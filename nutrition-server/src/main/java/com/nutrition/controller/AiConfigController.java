package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.AiConfig;
import com.nutrition.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI配置管理控制器
 * 提供AI模型配置的增删改查、启用禁用、连通性测试等功能
 */
@RestController
@RequestMapping("/ai/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI配置管理", description = "AI模型配置的增删改查与管理接口")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    /**
     * 获取AI配置列表
     * 返回的API密钥已脱敏处理
     */
    @GetMapping("/list")
    @Operation(summary = "获取AI配置列表", description = "获取所有AI模型配置，API密钥已脱敏")
    public Result<List<AiConfig>> list() {
        log.info("获取AI配置列表");
        List<AiConfig> configs = aiConfigService.listAll();
        return Result.ok(configs);
    }

    /**
     * 新增AI配置
     */
    @PostMapping
    @Operation(summary = "新增AI配置", description = "新增AI模型配置，API密钥将自动加密存储")
    public Result<Long> add(@RequestBody AiConfig config) {
        log.info("新增AI配置请求: modelName={}, modelType={}", config.getModelName(), config.getModelType());
        Long id = aiConfigService.add(config);
        return Result.ok(id);
    }

    /**
     * 修改AI配置
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改AI配置", description = "修改AI模型配置")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiConfig config) {
        log.info("修改AI配置请求: id={}, modelName={}", id, config.getModelName());
        aiConfigService.update(id, config);
        return Result.ok();
    }

    /**
     * 删除AI配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除AI配置", description = "删除指定的AI模型配置")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除AI配置请求: id={}", id);
        aiConfigService.delete(id);
        return Result.ok();
    }

    /**
     * 启用AI配置
     */
    @PostMapping("/{id}/enable")
    @Operation(summary = "启用AI配置", description = "启用指定的AI模型配置，同时禁用其他配置")
    public Result<Void> enable(@PathVariable Long id) {
        log.info("启用AI配置请求: id={}", id);
        aiConfigService.enable(id);
        return Result.ok();
    }

    /**
     * 禁用AI配置
     */
    @PostMapping("/{id}/disable")
    @Operation(summary = "禁用AI配置", description = "禁用指定的AI模型配置")
    public Result<Void> disable(@PathVariable Long id) {
        log.info("禁用AI配置请求: id={}", id);
        aiConfigService.disable(id);
        return Result.ok();
    }

    /**
     * 测试AI配置连通性
     */
    @PostMapping("/test")
    @Operation(summary = "测试AI配置连通性", description = "测试AI模型配置的连通性")
    public Result<Map<String, String>> test(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String configIdStr = request.get("configId");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.badRequest("测试消息不能为空");
        }
        
        Long configId;
        if (configIdStr != null && !configIdStr.isEmpty()) {
            configId = Long.parseLong(configIdStr);
        } else {
            // 如果没有指定配置ID，使用当前启用的配置
            AiConfig enabledConfig = aiConfigService.getEnabledConfig();
            if (enabledConfig == null) {
                return Result.fail("请先启用一个AI配置");
            }
            configId = enabledConfig.getId();
        }

        log.info("测试AI配置连通性: configId={}, message={}", configId, message);
        String response = aiConfigService.testConnection(configId, message);
        
        Map<String, String> result = Map.of("message", message, "response", response);
        return Result.ok(result);
    }
}
