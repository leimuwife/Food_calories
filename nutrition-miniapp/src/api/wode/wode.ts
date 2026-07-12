import request from '../request'
import type { ProfileUpdateParam, UserFeedback, UserVO } from '../types'

export function getProfile() {
  return request<UserVO>({
    url: '/api/user/profile',
    method: 'GET',
  })
}

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

export function getFeedbackList() {
  return request<UserFeedback[]>({
    url: '/api/user/feedback/list',
    method: 'GET',
  })
}