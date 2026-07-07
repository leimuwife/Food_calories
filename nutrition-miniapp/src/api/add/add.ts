import request from '../request'
import type { DietRecordParam } from '../types'

export function addDietRecord(data: DietRecordParam) {
  return request<{ recordId: number }>({
    url: '/api/diet/record',
    method: 'POST',
    data,
  })
}

export function estimateCalories(description: string) {
  return request<{ calories: number }>({
    url: '/api/nutritionist/estimate-calories',
    method: 'POST',
    data: { description },
  })
}

export function deleteDietRecord(recordId: number) {
  return request({
    url: `/api/diet/record/${recordId}`,
    method: 'DELETE',
  })
}