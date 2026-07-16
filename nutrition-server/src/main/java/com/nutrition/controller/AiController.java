package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.dto.CalorieEstimateResultDTO;
import com.nutrition.service.AiFoodEstimateService;
import com.nutrition.service.AiModelService;
import com.nutrition.vo.CalorieEstimateVO;
import com.nutrition.vo.ChatResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI服务控制器
 * 包含两个完全独立的接口：
 * 1. AI热量估算接口：返回纯数值，用于前端输入框回填
 * 2. AI营养师对话接口：返回完整对话文本，支持多轮问答
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI服务", description = "AI热量估算与营养师对话接口")
public class AiController {

    private final AiFoodEstimateService aiFoodEstimateService;
    private final AiModelService aiModelService;

    /**
     * AI热量估算接口
     * 用户输入食物描述和重量，一键估算总热量，仅返回数值用于前端输入框回填
     * 与AI营养师对话接口完全隔离，独立业务逻辑
     *
     * @param foodDesc 用户输入的食物描述（如"一块含西瓜、面包、动物奶油的蛋糕"）
     * @param weight   用户输入的食物重量（单位：克），可选参数
     * @return 热量估算结果，仅包含总热量数值（BigDecimal保留1位小数）
     */
    @GetMapping("/estimate-calorie")
    @Operation(summary = "AI热量估算", description = "根据食物描述和重量估算总热量，仅返回数值用于输入框回填")
    public Result<CalorieEstimateVO> estimateCalorie(@RequestParam String foodDesc, 
                                                      @RequestParam(required = false) Integer weight) {
        log.info("AI热量估算请求: foodDesc={}, weight={}", foodDesc, weight);

        try {
            CalorieEstimateResultDTO result = aiFoodEstimateService.estimateCalorie(foodDesc, weight);

            CalorieEstimateVO vo = CalorieEstimateVO.builder()
                    .totalCalorie(result.getTotalCalorie())
                    .build();

            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI热量估算异常: foodDesc={}, weight={}, error={}", foodDesc, weight, e.getMessage(), e);
            return Result.fail("暂时无法估算，请手动填写食材热量");
        }
    }

    /**
     * AI营养师对话接口（POST）
     * 用户提问后执行RAG向量检索，拼接知识库进Prompt
     * 大模型输出完整自然语言回答，末尾强制拼接固定免责声明
     * 不缓存对话结果，保留多轮上下文记忆（通过前端管理）
     * 不复用热量估算专用Prompt，使用营养师角色对话Prompt
     *
     * @param request 包含 content 字段的请求体
     * @return AI回答内容，包含完整对话文本和免责声明
     */
    @PostMapping("/chat")
    @Operation(summary = "AI营养师对话", description = "与AI营养师进行营养健康问答对话")
    public Result<ChatResponseVO> chat(@RequestBody java.util.Map<String, String> request) {
        String message = request.get("content");
        if (message == null || message.trim().isEmpty()) {
            return Result.badRequest("提问内容不能为空");
        }

        log.info("AI营养师对话请求: message={}", message);

        try {
            String response = aiModelService.chat(message);

            ChatResponseVO vo = ChatResponseVO.builder()
                    .response(response)
                    .build();

            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI营养师对话异常: message={}, error={}", message, e.getMessage(), e);
            return Result.fail("对话服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI配置连通性测试接口
     *
     * @param message 测试问题
     * @return AI回答内容
     */
    @GetMapping("/test")
    @Operation(summary = "AI配置测试", description = "测试AI模型配置连通性")
    public Result<ChatResponseVO> test(@RequestParam String message) {
        log.info("AI配置测试请求: message={}", message);

        try {
            String response = aiModelService.test(message);

            ChatResponseVO vo = ChatResponseVO.builder()
                    .response(response)
                    .build();

            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI配置测试异常: message={}, error={}", message, e.getMessage(), e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }
}