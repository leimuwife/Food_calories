package com.nutrition.vo;

import lombok.Data;
import java.util.List;

/**
 * 每日饮食数据视图对象
 * 用于返回用户每日饮食记录及汇总信息
 */
@Data
public class DailyDietVO {

    private List<DietRecordVO> records;

    private DailySummaryVO summary;
}
