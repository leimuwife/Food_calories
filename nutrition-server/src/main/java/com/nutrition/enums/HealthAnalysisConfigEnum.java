package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 健康分析配置枚举
 * 统一维护日期范围、AI尝试次数和重试等待时间。
 */
@Getter
@RequiredArgsConstructor
public enum HealthAnalysisConfigEnum {

    /** 最近天数 */
    RECENT_DAYS(7),

    /** 长期趋势天数 */
    TREND_DAYS(30),

    /** AI 最大尝试次数 */
    MAX_AI_ATTEMPTS(3),

    /** 重试基础等待时间，单位：毫秒 */
    RETRY_BASE_DELAY_MILLIS(500);

    /** 配置值 */
    private final int value;
}
