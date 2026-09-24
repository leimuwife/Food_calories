package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 健康分析报告状态枚举
 * 区分本次 AI 生成报告和 AI 失败后的历史报告降级结果。
 */
@Getter
@RequiredArgsConstructor
public enum HealthReportStatusEnum {

    /** 本次 AI 成功生成 */
    CURRENT("current", "最新报告"),

    /** AI 服务失败后返回同目标历史报告 */
    FALLBACK("fallback", "历史报告");

    /** 状态编码 */
    private final String code;

    /** 中文说明 */
    private final String label;
}
