package com.nutrition.service;

import com.nutrition.entity.DietRecord;
import com.nutrition.param.DietRecordParam;
import com.nutrition.vo.DailyDietVO;
import com.nutrition.vo.DietRecordVO;

import java.util.List;

public interface DietService {

    DietRecord addRecord(Long userId, DietRecordParam param);

    DailyDietVO getRecordsByDate(Long userId, String dateStr);

    List<DietRecordVO> getRecordsByRange(Long userId, String startDate, String endDate);

    void deleteRecord(Long userId, Long recordId);

    void updateItemWeight(Long userId, Long itemId, int weight);

    void deleteItem(Long userId, Long itemId);

    void copyRecordToDate(Long userId, Long recordId, String targetDate);

    DietRecordVO convertToVO(DietRecord record);
}
