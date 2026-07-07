import request from '../request'
import type { DailyDietVO, MonthlySummaryVO } from '../types'

export function getTodayRecords(date: string) {
  return request<DailyDietVO>({
    url: '/api/diet/record',
    method: 'GET',
    data: { date },
  })
}

export function getMonthlySummary(year: number, month: number) {
  return request<MonthlySummaryVO>({
    url: '/api/statistics/monthly',
    method: 'GET',
    data: { year, month },
  })
}