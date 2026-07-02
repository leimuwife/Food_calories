package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.FoodDict;
import com.nutrition.service.FoodService;
import com.nutrition.vo.FoodCategoryVO;
import com.nutrition.vo.FoodSearchResultVO;
import com.nutrition.vo.FoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 食物数据控制器
 */
@RestController
@RequestMapping("/food")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/search")
    public Result<FoodSearchResultVO> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String category) {
        FoodSearchResultVO result = foodService.search(keyword, category);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<FoodVO> getDetail(@PathVariable Long id) {
        FoodVO food = foodService.getDetail(id);
        return Result.ok(food);
    }

    @GetMapping("/categories")
    public Result<List<FoodCategoryVO>> getCategories() {
        List<FoodCategoryVO> categories = foodService.getCategories();
        return Result.ok(categories);
    }

    @PostMapping("/import")
    public Result<Integer> batchImport(@RequestBody List<FoodDict> foodList) {
        int count = foodService.batchImport(foodList);
        return Result.ok("成功导入 " + count + " 条数据", count);
    }
}
