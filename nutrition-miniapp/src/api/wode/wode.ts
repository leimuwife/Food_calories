import request from '../request'
import type { ProfileUpdateParam } from '../types'

export function updateProfile(data: ProfileUpdateParam) {
  return request({
    url: '/api/user/profile',
    method: 'PUT',
    data,
  })
}

export function updateNutritionGoal(data: {
  dailyCalorieGoal?: number
  dailyProteinGoal?: number
  dailyFatGoal?: number
  dailyCarbsGoal?: number
}) {
  return request({
    url: '/api/user/goal',
    method: 'PUT',
    data,
  })
}