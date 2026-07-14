package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.service.AiModelService;
import com.nutrition.vo.NutritionistChatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI营养师对话控制器
 */
@RestController
@RequestMapping("/nutritionist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI营养师", description = "AI营养师对话接口")
public class NutritionistController {

    private final AiModelService aiModelService;

    @PostMapping("/chat")
    @Operation(summary = "AI营养师对话", description = "用户与AI营养师进行对话")
    public Result<NutritionistChatVO> chat(
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.isEmpty()) {
            return Result.badRequest("提问内容不能为空");
        }

        log.info("AI营养师对话请求: contentLength={}", content.length());

        String response = aiModelService.chat(content);

        NutritionistChatVO vo = NutritionistChatVO.buildAssistant(response);

        return Result.ok(vo);
    }
}