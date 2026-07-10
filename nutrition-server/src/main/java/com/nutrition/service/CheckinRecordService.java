package com.nutrition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.entity.CheckinRecord;
import com.nutrition.vo.CheckinMonthlyVO;

import java.time.LocalDate;

/**
 * 打卡记录服务接口
 * 提供打卡相关的业务操作
 */
public interface CheckinRecordService extends IService<CheckinRecord> {

    /**
     * 用户打卡
     * 根据用户ID和日期创建打卡记录，已打卡则不重复创建
     *
     * @param userId 用户ID
     * @param date   打卡日期
     */
    void checkin(Long userId, LocalDate date);

    /**
     * 取消打卡
     * 根据用户ID和日期删除打卡记录（软删除）
     *
     * @param userId 用户ID
     * @param date   打卡日期
     */
    void cancelCheckin(Long userId, LocalDate date);

    /**
     * 查询用户月度打卡日期
     * 返回指定月份的所有打卡日期列表
     *
     * @param userId 用户ID
     * @param year   年份
     * @param month  月份
     * @return 月度打卡VO，包含打卡日期列表
     */
    CheckinMonthlyVO getMonthlyCheckinDates(Long userId, Integer year, Integer month);
}
