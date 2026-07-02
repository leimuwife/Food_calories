package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.DietRecord;
import com.nutrition.param.DietItemUpdateParam;
import com.nutrition.param.DietRecordParam;
import com.nutrition.service.DietService;
import com.nutrition.vo.DailyDietVO;
import com.nutrition.vo.DietRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 饮食记录控制器
 */
@RestController
@RequestMapping("/diet")
@RequiredArgsConstructor
public class DietController {

    private final DietService dietService;

    @PostMapping("/record")
    public Result<Map<String, Object>> addRecord(HttpServletRequest request,
                                                  @Valid @RequestBody DietRecordParam param) {
        Long userId = (Long) request.getAttribute("userId");
        DietRecord record = dietService.addRecord(userId, param);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("recordId", record.getId());
        return Result.ok("添加成功", data);
    }

    @GetMapping("/record")
    public Result<DailyDietVO> getRecordsByDate(HttpServletRequest request,
                                                 @RequestParam String date) {
        Long userId = (Long) request.getAttribute("userId");
        DailyDietVO result = dietService.getRecordsByDate(userId, date);
        return Result.ok(result);
    }

    @GetMapping("/records/range")
    public Result<List<DietRecordVO>> getRecordsByRange(HttpServletRequest request,
                                                         @RequestParam String startDate,
                                                         @RequestParam String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        List<DietRecordVO> records = dietService.getRecordsByRange(userId, startDate, endDate);
        return Result.ok(records);
    }

    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        dietService.deleteRecord(userId, id);
        return Result.ok("删除成功", null);
    }

    @PutMapping("/item/{id}")
    public Result<Void> updateItemWeight(HttpServletRequest request,
                                          @PathVariable Long id,
                                          @Valid @RequestBody DietItemUpdateParam param) {
        Long userId = (Long) request.getAttribute("userId");
        dietService.updateItemWeight(userId, id, param.getWeight());
        return Result.ok("更新成功", null);
    }

    @DeleteMapping("/item/{id}")
    public Result<Void> deleteItem(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        dietService.deleteItem(userId, id);
        return Result.ok("删除成功", null);
    }

    @PostMapping("/record/{id}/copy")
    public Result<Void> copyRecord(HttpServletRequest request,
                                    @PathVariable Long id,
                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String targetDate = body.get("targetDate");
        if (targetDate == null || targetDate.isEmpty()) {
            return Result.badRequest("目标日期不能为空");
        }
        dietService.copyRecordToDate(userId, id, targetDate);
        return Result.ok("复制成功", null);
    }
}
