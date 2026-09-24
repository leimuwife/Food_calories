package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.service.FoodNutritionService;
import com.nutrition.vo.FoodCategoryVO;
import com.nutrition.vo.FoodSearchResultVO;
import com.nutrition.vo.FoodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 食物字典控制器
 * 提供食物搜索、详情和分类查询能力。
 */
@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
@Tag(name = "食物字典", description = "食物搜索、食物详情和食物分类")
public class FoodController {

    /** 食物营养服务 */
    private final FoodNutritionService foodNutritionService;

    /**
     * 分页搜索食物。
     *
     * @param keyword  食物名称关键词，可为空
     * @param category 食物分类，可为空
     * @param page     页码，从1开始
     * @param pageSize 每页条数
     * @return 食物分页结果
     */
    @GetMapping("/search")
    @Operation(summary = "搜索食物", description = "按名称关键词和分类分页查询食物")
    public Result<FoodSearchResultVO> searchFood(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return Result.ok(foodNutritionService.searchFood(keyword, category, page, pageSize));
    }

    /**
     * 查询食物详情。
     *
     * @param id 食物ID
     * @return 食物营养详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询食物详情", description = "按食物ID查询完整营养信息")
    public Result<FoodVO> getFoodDetail(@PathVariable("id") Long id) {
        return Result.ok(foodNutritionService.getFoodDetail(id));
    }

    /**
     * 查询全部食物分类。
     *
     * @return 去重后的分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "查询食物分类", description = "查询全部未删除食物的分类列表")
    public Result<List<FoodCategoryVO>> listCategories() {
        return Result.ok(foodNutritionService.listCategories());
    }
}
