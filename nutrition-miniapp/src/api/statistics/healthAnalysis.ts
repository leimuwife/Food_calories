import request from '../request'
import type { HealthAnalysisReportVO, HealthCalorieTrendVO, HealthGoalType } from '../types'

/** 查询最近7天和30天每日热量趋势 */
export function getHealthCalorieTrend() {
  return request<HealthCalorieTrendVO>({
    url: '/api/health-analysis/calories',
    method: 'GET',
    showLoading: false,
  })
}

/** 查询指定目标最近一次已保存报告 */
export function getLatestHealthReport(goalType: HealthGoalType) {
  return request<HealthAnalysisReportVO | null>({
    url: '/api/health-analysis/report/latest',
    method: 'GET',
    params: { goalType },
    showLoading: false,
  })
}

/** 生成并保存健康分析报告 */
export function generateHealthReport(goalType: HealthGoalType) {
  return request<HealthAnalysisReportVO>({
    url: '/api/health-analysis/report',
    method: 'POST',
    data: { goalType },
    showLoading: false,
    timeout: 180000,
  })
}
