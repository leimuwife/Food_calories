package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.param.AiConfigParam;
import com.nutrition.service.AiConfigService;
import com.nutrition.service.AiModelService;
import com.nutrition.vo.AiConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI配置管理控制器
 */
@RestController
@RequestMapping("/ai/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI配置管理", description = "AI大模型配置管理接口")
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final AiModelService aiModelService;

    @GetMapping("/list")
    @Operation(summary = "查询配置列表", description = "查询所有AI模型配置")
    public Result<List<AiConfigVO>> list() {
        List<AiConfigVO> list = aiConfigService.listAll();
        return Result.ok(list);
    }

    @PostMapping
    @Operation(summary = "新增配置", description = "新增AI模型配置")
    public Result<Long> add(@Valid @RequestBody AiConfigParam param) {
        Long id = aiConfigService.add(param);
        return Result.ok("新增成功", id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新配置", description = "更新AI模型配置")
    public Result<Void> update(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Valid @RequestBody AiConfigParam param) {
        aiConfigService.update(id, param);
        return Result.ok("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置", description = "删除AI模型配置")
    public Result<Void> delete(@Parameter(description = "配置ID") @PathVariable Long id) {
        aiConfigService.delete(id);
        return Result.ok("删除成功", null);
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用配置", description = "启用指定的AI模型配置")
    public Result<Void> enable(@Parameter(description = "配置ID") @PathVariable Long id) {
        aiConfigService.enable(id);
        return Result.ok("启用成功", null);
    }

    @PostMapping("/test")
    @Operation(summary = "测试配置连通性", description = "传入测试问题，验证配置是否可用")
    public Result<Map<String, String>> test(@RequestBody Map<String, String> request) {
        String testMessage = request.get("message");
        if (testMessage == null || testMessage.isEmpty()) {
            testMessage = "你好，请介绍一下你自己";
        }

        String response = aiModelService.test(testMessage);

        return Result.ok(Map.of(
                "message", testMessage,
                "response", response
        ));
    }
}