package com.nutrition.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.HealthAnalysisReport;
import com.nutrition.enums.HealthGoalTypeEnum;
import com.nutrition.enums.HealthReportStatusEnum;
import com.nutrition.mapper.HealthAnalysisReportMapper;
import com.nutrition.service.HealthAnalysisAiService;
import com.nutrition.service.impl.HealthAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 健康分析业务重试与降级测试。
 */
@ExtendWith(MockitoExtension.class)
class HealthAnalysisServiceImplTest {

    @Mock
    private HealthAnalysisReportMapper reportMapper;

    @Mock
    private HealthAnalysisAiService aiService;

    @Mock
    private ObjectMapper objectMapper;

    private HealthAnalysisServiceImpl service;

    /** 初始化被测服务。 */
    @BeforeEach
    void setUp() {
        service = new HealthAnalysisServiceImpl(reportMapper, aiService, objectMapper);
        when(reportMapper.sumDailyCalories(anyLong(), any(), any())).thenReturn(List.of());
    }

    /** 验证 AI 成功后保存并返回最新报告。 */
    @Test
    void shouldGenerateAndSaveCurrentReport() throws Exception {
        when(aiService.generateReport(anyString(), eq(1L))).thenReturn("测试分析报告");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        doAnswer(invocation -> {
            HealthAnalysisReport report = invocation.getArgument(0);
            report.setId(10L);
            return 1;
        }).when(reportMapper).insert(any(HealthAnalysisReport.class));

        var result = service.generateReport(1L, HealthGoalTypeEnum.WEIGHT_LOSS);

        assertEquals(HealthReportStatusEnum.CURRENT, result.getReportStatus());
        assertEquals("测试分析报告", result.getReportContent());
        assertEquals(10L, result.getReportId());
    }

    /** 验证第三次调用成功时不会再执行降级。 */
    @Test
    void shouldRetryUntilThirdAttemptSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        when(aiService.generateReport(anyString(), eq(1L))).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("temporary failure");
            }
            return "第三次成功";
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        doAnswer(invocation -> {
            HealthAnalysisReport report = invocation.getArgument(0);
            report.setId(11L);
            return 1;
        }).when(reportMapper).insert(any(HealthAnalysisReport.class));

        var result = service.generateReport(1L, HealthGoalTypeEnum.FITNESS);

        assertEquals(3, attempts.get());
        assertEquals(HealthReportStatusEnum.CURRENT, result.getReportStatus());
    }

    /** 验证三次失败后返回同目标历史报告。 */
    @Test
    void shouldFallbackToLatestReportAfterThreeFailures() {
        when(aiService.generateReport(anyString(), eq(1L))).thenThrow(new RuntimeException("ai down"));
        HealthAnalysisReport historical = reportEntity(20L);
        when(reportMapper.selectOne(any())).thenReturn(historical);

        var result = service.generateReport(1L, HealthGoalTypeEnum.WEIGHT_GAIN);

        assertEquals(HealthReportStatusEnum.FALLBACK, result.getReportStatus());
        assertEquals(20L, result.getReportId());
    }

    /** 验证没有历史报告时返回业务异常。 */
    @Test
    void shouldThrowWhenAiFailsAndNoHistoryExists() {
        when(aiService.generateReport(anyString(), eq(1L))).thenThrow(new RuntimeException("ai down"));
        when(reportMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.generateReport(1L, HealthGoalTypeEnum.NORMAL_DIET));
    }

    /**
     * 创建历史报告测试数据。
     *
     * @param id 报告ID
     * @return 报告实体
     */
    private HealthAnalysisReport reportEntity(Long id) {
        HealthAnalysisReport report = new HealthAnalysisReport();
        report.setId(id);
        report.setUserId(1L);
        report.setGoalType(HealthGoalTypeEnum.WEIGHT_GAIN.getCode());
        report.setReportContent("历史报告");
        report.setLast7Avg(new BigDecimal("1800.0"));
        report.setLast30Avg(new BigDecimal("1900.0"));
        report.setCreateTime(LocalDateTime.now());
        return report;
    }
}
