package com.nutrition.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.FoodDict;
import com.nutrition.mapper.FoodDictMapper;
import com.nutrition.service.FoodService;
import com.nutrition.vo.FoodCategoryVO;
import com.nutrition.vo.FoodSearchResultVO;
import com.nutrition.vo.FoodVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 食物字典业务层实现类
 * 负责食物数据的搜索、查询和批量导入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodServiceImpl extends ServiceImpl<FoodDictMapper, FoodDict> implements FoodService {

    @Override
    public FoodSearchResultVO search(String keyword, String category) {
        List<FoodVO> voList = this.baseMapper.search(keyword, category);

        FoodSearchResultVO result = new FoodSearchResultVO();
        result.setList(voList);
        result.setTotal(voList.size());
        return result;
    }

    @Override
    public FoodVO getDetail(Long foodId) {
        FoodDict food = this.getById(foodId);
        if (food == null) {
            throw new BusinessException(404, "食物不存在");
        }
        return convertToVO(food);
    }

    @Override
    public List<FoodCategoryVO> getCategories() {
        return this.baseMapper.getAllCategories();
    }

    @Override
    public int batchImport(List<FoodDict> foodList) {
        int count = 0;
        for (FoodDict food : foodList) {
            if (StrUtil.isNotBlank(food.getFoodName())) {
                this.save(food);
                count++;
            }
        }
        log.info("批量导入食物数据完成，共导入 {} 条", count);
        return count;
    }

    @Override
    public FoodVO convertToVO(FoodDict food) {
        FoodVO vo = new FoodVO();
        vo.setId(food.getId());
        vo.setFoodName(food.getFoodName());
        vo.setCategory(food.getCategory());
        vo.setCaloriesPer100g(food.getCaloriesPer100g());
        vo.setProteinPer100g(food.getProteinPer100g());
        vo.setFatPer100g(food.getFatPer100g());
        vo.setCarbsPer100g(food.getCarbsPer100g());
        vo.setFiberPer100g(food.getFiberPer100g());
        vo.setEdiblePortion(food.getEdiblePortion());
        vo.setDataSource(food.getDataSource());
        return vo;
    }
}
