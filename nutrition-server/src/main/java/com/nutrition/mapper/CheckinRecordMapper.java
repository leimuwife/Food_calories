package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.CheckinRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 打卡记录 Mapper 接口
 * 提供打卡记录的数据访问操作
 */
@Mapper
public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {

    /**
     * 查询用户指定月份的所有打卡日期
     *
     * @param userId 用户ID
     * @param startDate 月份开始日期
     * @param endDate 月份结束日期
     * @return 打卡日期列表
     */
    List<LocalDate> selectCheckinDatesByMonth(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 根据用户ID和打卡日期查询记录
     *
     * @param userId 用户ID
     * @param checkinDate 打卡日期
     * @return 打卡记录
     */
    CheckinRecord selectByUserIdAndDate(@Param("userId") Long userId, @Param("checkinDate") LocalDate checkinDate);
}
