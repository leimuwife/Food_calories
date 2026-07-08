package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.DietRecord;
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
     * 根据用户ID和日期查询饮食记录
     *
     * @param userId 用户ID
     * @param date   查询日期
     * @return 饮食记录列表
     */
    List<DietRecord> selectByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 根据用户ID和日期范围查询饮食记录
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 饮食记录列表
     */
    List<DietRecord> selectByUserIdAndDateRange(@Param("userId") Long userId, 
                                                  @Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
}