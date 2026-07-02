package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.DietRecord;
import com.nutrition.vo.DietRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录数据访问层
 * 负责饮食记录表的数据库操作
 */
@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    /**
     * 按用户和日期查询记录
     * @param userId 用户ID
     * @param date 记录日期
     * @return 饮食记录列表
     */
    List<DietRecordVO> findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 按用户和日期范围查询记录
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 饮食记录列表
     */
    List<DietRecordVO> findByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 按月查询用户所有记录
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 饮食记录列表
     */
    List<DietRecordVO> findByUserAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
}
