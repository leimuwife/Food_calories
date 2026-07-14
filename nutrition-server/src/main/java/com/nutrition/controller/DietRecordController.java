package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.param.DietRecordParam;
import com.nutrition.service.DietRecordService;
import com.nutrition.vo.DailyDietVO;
import com.nutrition.vo.DietItemVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 饮食记录控制器
 * 处理饮食记录相关的HTTP请求
 */
@RestController
@RequestMapping("/diet")
@RequiredArgsConstructor
@Slf4j
public class DietRecordController {

    private final DietRecordService dietRecordService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

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

    /**
     * 添加饮食记录
     * @param request
     * @param dataJson
     * @param file
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/record", consumes = "multipart/form-data")
    public Result<Map<String, Object>> addDietRecordWithFile(HttpServletRequest request,
                                                            @RequestParam("data") String dataJson,
                                                            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        DietRecordParam param = objectMapper.readValue(dataJson, DietRecordParam.class);
        validateParam(param);

        Long recordId = dietRecordService.addDietRecord(userId, param, file);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", recordId);

        return Result.ok("添加成功", result);
    }

    /**
     * 添加饮食记录
     * @param request
     * @param param
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/record", consumes = "application/json")
    public Result<Map<String, Object>> addDietRecord(HttpServletRequest request,
                                                     @Valid @RequestBody DietRecordParam param) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        Long recordId = dietRecordService.addDietRecord(userId, param, null);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", recordId);

        return Result.ok("添加成功", result);
    }

    @DeleteMapping("/record/{id}")
    public Result<String> deleteDietRecord(HttpServletRequest request,
                                         @PathVariable("id") String recordIdStr) {
        Long userId = (Long) request.getAttribute("userId");
        Long recordId = Long.parseLong(recordIdStr);
        dietRecordService.deleteDietRecord(userId, recordId);
        return Result.ok("删除成功");
    }

    @PostMapping("/record/cache/clear")
    public Result<String> clearDietCache(HttpServletRequest request,
                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long userId = (Long) request.getAttribute("userId");
        dietRecordService.clearDietCache(userId, date.toString());
        return Result.ok("缓存已清除");
    }

    @GetMapping("/item/{id}")
    public Result<DietItemVO> getDietItemDetail(HttpServletRequest request,
                                               @PathVariable("id") String itemIdStr) {
        Long userId = (Long) request.getAttribute("userId");
        Long itemId = Long.parseLong(itemIdStr);
        DietItemVO itemVO = dietRecordService.getDietItemDetail(userId, itemId);
        return Result.ok(itemVO);
    }

    @PutMapping(value = "/item", consumes = "multipart/form-data")
    public Result<Map<String, Object>> updateDietItemWithFile(HttpServletRequest request,
                                                             @RequestParam("data") String dataJson,
                                                             @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        DietRecordParam param = objectMapper.readValue(dataJson, DietRecordParam.class);
        validateParam(param);

        Long itemId = dietRecordService.updateDietItem(userId, param, file);

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);

        return Result.ok("更新成功", result);
    }

    @PutMapping(value = "/item", consumes = "application/json")
    public Result<Map<String, Object>> updateDietItem(HttpServletRequest request,
                                                      @Valid @RequestBody DietRecordParam param) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        Long itemId = dietRecordService.updateDietItem(userId, param, null);

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);

        return Result.ok("更新成功", result);
    }

    private void validateParam(DietRecordParam param) {
        var violations = validator.validate(param);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.error("参数校验失败: {}", message);
            throw new com.nutrition.common.BusinessException(400, message);
        }
    }
}