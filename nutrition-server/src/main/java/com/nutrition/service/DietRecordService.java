package com.nutrition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.entity.DietRecord;
import com.nutrition.vo.DailyDietVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    /**
     * 添加饮食记录
     *
     * @param userId 用户ID
     * @param param  添加饮食记录请求参数
     * @return 饮食记录ID
     * @throws IOException 文件上传异常
     */
    Long addDietRecord(Long userId, com.nutrition.param.DietRecordParam param) throws IOException;

    /**
     * 添加饮食记录（带文件上传）
     *
     * @param userId 用户ID
     * @param param  添加饮食记录请求参数
     * @param file   食物图片文件（可选）
     * @return 饮食记录ID
     * @throws IOException 文件上传异常
     */
    Long addDietRecord(Long userId, com.nutrition.param.DietRecordParam param, MultipartFile file) throws IOException;

    /**
     * 删除饮食记录
     *
     * @param userId   用户ID
     * @param recordId 饮食记录ID
     */
    void deleteDietRecord(Long userId, Long recordId);

    /**
     * 清除指定日期的饮食缓存
     *
     * @param userId 用户ID
     * @param date   日期字符串，格式：YYYY-MM-DD
     */
    void clearDietCache(Long userId, String date);
}