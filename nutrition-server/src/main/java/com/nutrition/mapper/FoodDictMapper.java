package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.FoodDict;
import com.nutrition.vo.FoodCategoryVO;
import com.nutrition.vo.FoodVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 食物字典数据访问层
 * 负责食物字典表的数据库操作
 */
@Mapper
public interface FoodDictMapper extends BaseMapper<FoodDict> {

    /**
     * 按名称和分类搜索食物
     * @param keyword 食物名称关键词
     * @param category 食物分类
     * @return 食物列表
     */
    List<FoodVO> search(@Param("keyword") String keyword, @Param("category") String category);

    /**
     * 获取所有食物分类
     * @return 分类列表
     */
    List<FoodCategoryVO> getAllCategories();
}
