package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.DietItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 饮食项数据访问层
 * 负责饮食项表的数据库操作
 */
@Mapper
public interface DietItemMapper extends BaseMapper<DietItem> {

    /**
     * 根据记录ID查询饮食项列表
     *
     * @param recordId 记录ID
     * @return 饮食项列表
     */
    List<DietItem> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据记录ID列表查询饮食项列表
     *
     * @param recordIds 记录ID列表
     * @return 饮食项列表
     */
    List<DietItem> selectByRecordIds(@Param("recordIds") List<Long> recordIds);

    /**
     * 统计记录的总热量
     *
     * @param recordId 记录ID
     * @return 总热量
     */
    BigDecimal sumCaloriesByRecordId(@Param("recordId") Long recordId);

    /**
     * 批量插入饮食项
     *
     * @param items 饮食项列表
     */
    void batchInsert(@Param("items") List<DietItem> items);
}