package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.service.StatisticsService;
import com.nutrition.vo.DailySummaryVO;
import com.nutrition.vo.MonthlySummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计分析控制器
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/daily")
    public Result<DailySummaryVO> getDailySummary(HttpServletRequest request,
                                                   @RequestParam String date) {
        Long userId = (Long) request.getAttribute("userId");
        DailySummaryVO result = statisticsService.getDailySummary(userId, date);
        return Result.ok(result);
    }

    @GetMapping("/monthly")
    public Result<MonthlySummaryVO> getMonthlySummary(HttpServletRequest request,
                                                       @RequestParam int year,
                                                       @RequestParam int month) {
        Long userId = (Long) request.getAttribute("userId");
        MonthlySummaryVO result = statisticsService.getMonthlySummary(userId, year, month);
        return Result.ok(result);
    }

    @GetMapping("/export")
    public Result<Map<String, String>> exportCSV(HttpServletRequest request,
                                                  @RequestParam String startDate,
                                                  @RequestParam String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        String csv = statisticsService.exportCSV(userId, startDate, endDate);
        Map<String, String> data = new java.util.HashMap<>();
        data.put("csvContent", csv);
        return Result.ok(data);
    }
}
