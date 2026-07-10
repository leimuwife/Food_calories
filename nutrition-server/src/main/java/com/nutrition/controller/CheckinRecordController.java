package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.param.CheckinParam;
import com.nutrition.service.CheckinRecordService;
import com.nutrition.vo.CheckinMonthlyVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;

/**
 * 打卡记录控制器
 * 提供打卡相关的 REST API 接口
 */
@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CheckinRecordController {

    private final CheckinRecordService checkinRecordService;

    /**
     * 用户打卡
     * POST /api/checkin
     *
     * @param request 请求对象，用于获取用户ID
     * @param param   打卡请求参数，包含打卡日期
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> checkin(HttpServletRequest request, @Valid @RequestBody CheckinParam param) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDate date = LocalDate.parse(param.getDate());

        checkinRecordService.checkin(userId, date);
        return Result.ok("打卡成功", null);
    }

    /**
     * 取消打卡
     * DELETE /api/checkin/{date}
     *
     * @param request 请求对象，用于获取用户ID
     * @param date    打卡日期（路径参数）
     * @return 操作结果
     */
    @DeleteMapping("/{date}")
    public Result<Void> cancelCheckin(HttpServletRequest request, @PathVariable("date") String date) {
        Long userId = (Long) request.getAttribute("userId");
        LocalDate checkinDate = LocalDate.parse(date);

        checkinRecordService.cancelCheckin(userId, checkinDate);
        return Result.ok("取消成功", null);
    }

    /**
     * 查询月度打卡日期
     * GET /api/checkin/monthly
     *
     * @param request 请求对象，用于获取用户ID
     * @param year    年份（1900-2100）
     * @param month   月份（1-12）
     * @return 月度打卡日期列表
     */
    @GetMapping("/monthly")
    public Result<CheckinMonthlyVO> getMonthlyCheckinDates(
            HttpServletRequest request,
            @RequestParam("year") @NotNull(message = "年份不能为空") @Min(value = 1900, message = "年份必须大于等于1900") @Max(value = 2100, message = "年份必须小于等于2100") Integer year,
            @RequestParam("month") @NotNull(message = "月份不能为空") @Min(value = 1, message = "月份必须大于等于1") @Max(value = 12, message = "月份必须小于等于12") Integer month) {

        Long userId = (Long) request.getAttribute("userId");
        CheckinMonthlyVO vo = checkinRecordService.getMonthlyCheckinDates(userId, year, month);

        return Result.ok(vo);
    }
}
