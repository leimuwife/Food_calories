package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.dto.AiChatResultDTO;
import com.nutrition.dto.CalorieEstimateResultDTO;
import com.nutrition.service.AiFoodEstimateService;
import com.nutrition.service.AiModelService;
import com.nutrition.vo.CalorieEstimateVO;
import com.nutrition.vo.ChatResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
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
    @Operation(summary = "AI热量估算", description = "根据食物名称、描述和重量估算总热量，仅返回数值用于输入框回填")
    public Result<CalorieEstimateVO> estimateCalorie(@RequestParam String foodName,
                                                      @RequestParam(required = false) String foodDesc,
                                                      @RequestParam(required = false) Integer weight) {
        log.info("AI热量估算请求: foodName={}, foodDesc={}, weight={}", foodName, foodDesc, weight);

        try {
            CalorieEstimateResultDTO result = aiFoodEstimateService.estimateCalorie(foodName, foodDesc, weight);

            CalorieEstimateVO vo = CalorieEstimateVO.builder()
                    .totalCalorie(result.getTotalCalorie())
                    .build();

            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI热量估算异常: foodName={}, foodDesc={}, weight={}, error={}", foodName, foodDesc, weight, e.getMessage(), e);
            return Result.fail("暂时无法估算，请手动填写食材热量");
        }
    }

    /**
     * AI营养师对话接口（POST）
     * 首次对话请求体不带sessionId，Python回调Java雪花算法生成正式会话，
     * 生成的sessionId随响应回传；前端保存后，后续多轮对话在请求体中原样携带。
     * userId由JWT过滤器解析后放入请求属性，不信任前端传值。
     *
     * @param request 请求体，content为提问内容（必填），sessionId为会话标识（新建时不传）
     * @return AI回答内容、会话ID及是否新建会话
     */
    @PostMapping("/chat")
    @Operation(summary = "AI营养师对话", description = "与AI营养师进行营养健康问答对话，首次对话自动生成会话ID并回传")
    public Result<ChatResponseVO> chat(@RequestBody java.util.Map<String, String> request,
                                       HttpServletRequest httpRequest) {
        String message = request.get("content");
        if (message == null || message.trim().isEmpty()) {
            return Result.badRequest("提问内容不能为空");
        }

        // 会话ID空白视为新建对话；用户身份只从JWT上下文取
        String sessionId = StringUtils.hasText(request.get("sessionId")) ? request.get("sessionId").trim() : null;
        Long userId = (Long) httpRequest.getAttribute("userId");

        log.info("AI营养师对话请求: userId={}, sessionId={}, message={}", userId, sessionId, message);

        try {
            AiChatResultDTO result = aiModelService.chat(message, sessionId, userId);

            ChatResponseVO vo = ChatResponseVO.builder()
                    .response(result.getResponse())
                    .sessionId(result.getSessionId())
                    .newSession(result.getNewSession())
                    .build();

            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI营养师对话异常: userId={}, sessionId={}, message={}, error={}",
                    userId, sessionId, message, e.getMessage(), e);
            return Result.fail("对话服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI配置连通性测试接口
     * 复用完整对话链路（含会话创建），能正常返回回答即说明Python服务与模型配置可用
     *
     * @param message 测试问题
     * @return AI回答内容
     */
    @GetMapping("/test")
    @Operation(summary = "AI配置测试", description = "测试AI模型配置连通性")
    public Result<ChatResponseVO> test(@RequestParam String message, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        log.info("AI配置测试请求: userId={}, message={}", userId, message);

        try {
            AiChatResultDTO result = aiModelService.test(message, userId);

            ChatResponseVO vo = ChatResponseVO.builder()
                    .response(result.getResponse())
                    .sessionId(result.getSessionId())
                    .newSession(result.getNewSession())
                    .build();

            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI配置测试异常: userId={}, message={}, error={}", userId, message, e.getMessage(), e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }
}