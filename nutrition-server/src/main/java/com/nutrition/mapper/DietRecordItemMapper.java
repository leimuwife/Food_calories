package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.DietRecordItem;
import com.nutrition.vo.DietItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 饮食记录明细数据访问层
 * 负责饮食记录明细表的数据库操作
 */
@Mapper
public interface DietRecordItemMapper extends BaseMapper<DietRecordItem> {

    /**
     * 按记录ID查询明细
     * @param recordId 记录ID
     * @return 明细列表
     */
    List<DietItemVO> findByRecordId(@Param("recordId") Long recordId);

    /**
     * 按月查询用户所有明细（用于统计分析）
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 明细列表
     */
    List<DietItemVO> findByUserAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    /**
     * 按记录ID删除所有明细
     * @param recordId 记录ID
     */
    void deleteByRecordId(@Param("recordId") Long recordId);
}
