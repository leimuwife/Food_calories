package com.nutrition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.entity.DietRecord;
import com.nutrition.vo.DailyDietVO;

import java.time.LocalDate;

/**
 * 饮食记录服务接口
 * 提供饮食记录相关的业务操作
 */
public interface DietRecordService extends IService<DietRecord> {

    /**
     * 查询用户指定日期的饮食记录
     *
     * @param userId 用户ID
     * @param date   查询日期
     * @return 当日饮食数据
     */
    DailyDietVO getDailyDiet(Long userId, LocalDate date);
}