package com.nutrition.service;

import com.nutrition.vo.DailySummaryVO;
import com.nutrition.vo.MonthlySummaryVO;

public interface StatisticsService {

    DailySummaryVO getDailySummary(Long userId, String dateStr);

    MonthlySummaryVO getMonthlySummary(Long userId, int year, int month);

    String exportCSV(Long userId, String startDate, String endDate);
}
