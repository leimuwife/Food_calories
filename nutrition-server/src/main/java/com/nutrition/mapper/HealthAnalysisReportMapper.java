package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.dto.DailyCalorieDTO;
import com.nutrition.entity.HealthAnalysisReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康分析数据访问层。
 * 负责报告持久化及每日热量聚合查询。
 */
@Mapper
public interface HealthAnalysisReportMapper extends BaseMapper<HealthAnalysisReport> {

    /**
     * 汇总指定用户在日期范围内的每日热量。
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日热量聚合列表
     */
    List<DailyCalorieDTO> sumDailyCalories(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
}