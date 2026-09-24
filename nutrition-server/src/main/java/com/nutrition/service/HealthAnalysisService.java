package com.nutrition.service;

import com.nutrition.enums.HealthGoalTypeEnum;
import com.nutrition.vo.HealthAnalysisReportVO;
import com.nutrition.vo.HealthCalorieTrendVO;

/**
 * 健康分析业务接口。
 * 提供热量趋势查询、报告生成和最近报告查询。
 */
public interface HealthAnalysisService {

    /**
     * 查询当前用户最近7天和30天热量趋势。
     *
     * @param userId 用户ID
     * @return 热量趋势
     */
    HealthCalorieTrendVO getCalorieTrend(Long userId);

    /**
     * 查询指定目标最近一次报告。
     *
     * @param userId 用户ID
     * @param goal   目标类型
     * @return 最近报告；不存在时返回 null
     */
    HealthAnalysisReportVO getLatestReport(Long userId, HealthGoalTypeEnum goal);

    /**
     * 生成并保存健康分析报告。
     *
     * @param userId 用户ID
     * @param goal   目标类型
     * @return 新报告或同目标历史降级报告
     */
    HealthAnalysisReportVO generateReport(Long userId, HealthGoalTypeEnum goal);
}
