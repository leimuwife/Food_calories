package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.service.DietRecordService;
import com.nutrition.vo.DailyDietVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;

/**
 * 饮食记录控制器
 * 处理饮食记录相关的HTTP请求
 */
@RestController
@RequestMapping("/diet")
@RequiredArgsConstructor
public class DietRecordController {

    private final DietRecordService dietRecordService;

    /**
     * 查询用户指定日期的饮食记录
     * 
     * @param request HTTP请求对象，用于获取当前用户ID
     * @param date    查询日期，格式：YYYY-MM-DD
     * @return 当日饮食数据
     */
    @GetMapping("/record")
    public Result<DailyDietVO> getDailyDiet(HttpServletRequest request, 
                                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long userId = (Long) request.getAttribute("userId");
        DailyDietVO result = dietRecordService.getDailyDiet(userId, date);
        return Result.ok("查询成功", result);
    }
}