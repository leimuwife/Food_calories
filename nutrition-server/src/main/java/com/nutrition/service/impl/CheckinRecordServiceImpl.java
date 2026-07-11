package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.entity.CheckinRecord;
import com.nutrition.mapper.CheckinRecordMapper;
import com.nutrition.service.CheckinRecordService;
import com.nutrition.vo.CheckinMonthlyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 打卡记录服务实现类
 * 实现打卡相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord> implements CheckinRecordService {

    /**
     * 用户打卡
     * 根据用户ID和日期创建打卡记录，已打卡则不重复创建
     *
     * @param userId 用户ID
     * @param date   打卡日期
     */
    @Override
    @Transactional
    public void checkin(Long userId, LocalDate date) {
        CheckinRecord existingRecord = getBaseMapper().selectByUserIdAndDate(userId, date);
        if (existingRecord != null) {
            log.info("用户{}已在{}打卡，无需重复打卡", userId, date);
            return;
        }

        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setCheckinDate(date);

        save(record);
        log.info("用户{}打卡成功: date={}, recordId={}", userId, date, record.getId());
    }

    /**
     * 取消打卡
     * 根据用户ID和日期删除打卡记录（软删除）
     *
     * @param userId 用户ID
     * @param date   打卡日期
     */
    @Override
    @Transactional
    public void cancelCheckin(Long userId, LocalDate date) {
        CheckinRecord record = getBaseMapper().selectByUserIdAndDate(userId, date);
        if (record == null) {
            log.error("取消打卡失败: 用户{}在{}没有打卡记录", userId, date);
            throw new BusinessException(BizMsgEnum.CHECKIN_RECORD_NOT_EXIST);
        }

        LambdaUpdateWrapper<CheckinRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CheckinRecord::getId, record.getId())
                .eq(CheckinRecord::getUserId, userId)
                .set(CheckinRecord::getDeleteFlag, 1);

        update(wrapper);
        log.info("用户{}取消打卡成功: date={}", userId, date);
    }

    /**
     * 查询用户月度打卡日期
     * 返回指定月份的所有打卡日期列表
     *
     * @param userId 用户ID
     * @param year   年份
     * @param month  月份
     * @return 月度打卡VO，包含打卡日期列表
     */
    @Override
    @Transactional(readOnly = true)
    public CheckinMonthlyVO getMonthlyCheckinDates(Long userId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<LocalDate> checkinDates = getBaseMapper().selectCheckinDatesByMonth(userId, startDate, endDate);

        List<String> dateStrings = checkinDates.stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());

        CheckinMonthlyVO vo = new CheckinMonthlyVO();
        vo.setDates(dateStrings);

        log.info("查询用户{}月度打卡日期成功: year={}, month={}, count={}", userId, year, month, dateStrings.size());
        return vo;
    }
}
