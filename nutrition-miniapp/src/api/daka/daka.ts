import request from '../request'
import type { MonthlySummaryVO } from '../types'

export function getMonthlySummary(year: number, month: number) {
  return request<MonthlySummaryVO>({
    url: '/api/statistics/monthly',
    method: 'GET',
    data: { year, month },
  })
}

export function checkin(date: string) {
  return request({
    url: '/api/checkin',
    method: 'POST',
    data: { date },
  })
}

export function cancelCheckin(date: string) {
  return request({
    url: `/api/checkin/${date}`,
    method: 'DELETE',
  })
}

export function getCheckinDates(year: number, month: number) {
  return request<{ dates: string[] }>({
    url: '/api/checkin/monthly',
    method: 'GET',
    data: { year, month },
  })
}