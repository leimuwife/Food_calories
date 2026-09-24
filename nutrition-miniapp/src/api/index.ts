import { useUserStore } from '@/stores/user'
import request from './request'

const BASE_URL = 'http://localhost:8088'

import {
  getMonthlySummary as dakaMonthlySummary,
} from './daka/daka'

import {
  getFeedList,
  publishFeed,
  toggleFeedLike,
  addFeedComment,
} from './qingyouquan/qingyouquan'

import {
  updateProfile,
  updateNutritionGoal,
  getFeedbackList,
} from './wode/wode'

import {
  nutritionistChat,
} from './yingyangshi/yingyangshi'

import {
  getTodayRecords,
  getMonthlySummary as historyMonthlySummary,
} from './history/history'

import {
  addDietRecord,
  estimateCalories,
  deleteDietRecord,
} from './add/add'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export function getRecordsByRange(startDate: string, endDate: string) {
  return request({
    url: '/api/diet/records/range',
    method: 'GET',
    data: { startDate, endDate },
  })
}

export function updateDietItem(itemId: number, weight: number) {
  return request({
    url: `/api/diet/item/${itemId}`,
    method: 'PUT',
    data: { weight },
  })
}

export function deleteDietItem(itemId: number) {
  return request({
    url: `/api/diet/item/${itemId}`,
    method: 'DELETE',
  })
}

export function copyRecordToToday(recordId: number, targetDate: string) {
  return request({
    url: `/api/diet/record/${recordId}/copy`,
    method: 'POST',
    data: { targetDate },
  })
}

export function getDailySummary(date: string) {
  return request<{
    totalCalories: number
    totalProtein: number
    totalFat: number
    totalCarbs: number
    calorieGoal: number
    proteinGoal: number
    fatGoal: number
    carbsGoal: number
    mealTypes: string[]
    caloriesByMeal: Record<string, number>
    proteinsByMeal: Record<string, number>
    fatsByMeal: Record<string, number>
    carbsByMeal: Record<string, number>
    meals: Record<string, { items: any[]; calories: number; protein: number; fat: number; carbs: number }>
  }>({
    url: '/api/statistics/daily',
    method: 'GET',
    data: { date },
  })
}

export function exportData(startDate: string, endDate: string) {
  return request({
    url: '/api/statistics/export',
    method: 'GET',
    data: { startDate, endDate },
  })
}

export function uploadAttachment(filePath: string, prefix?: string) {
  const userStore = useUserStore()
  const formData: Record<string, string> = {}
  if (prefix) {
    formData.prefix = prefix
  }
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/api/attachment/upload',
      filePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${userStore.token}`
      },
      formData,
      success: (res) => {
        try {
          const responseData = JSON.parse(res.data)
          if (String(responseData.code) === '200') {
            resolve(responseData.data)
          } else {
            uni.showToast({ title: responseData.message || '上传失败', icon: 'none' })
            reject(new Error(responseData.message))
          }
        } catch {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(new Error('Upload failed'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function getAttachmentUrl(fileId: string) {
  return `/api/attachment/${fileId}/url`
}

export default {
  updateProfile,
  updateNutritionGoal,
  getFeedbackList,
  addDietRecord,
  getTodayRecords,
  getRecordsByRange,
  deleteDietRecord,
  updateDietItem,
  deleteDietItem,
  copyRecordToToday,
  getDailySummary,
  getMonthlySummary: dakaMonthlySummary,
  exportData,
  getFeedList,
  publishFeed,
  toggleFeedLike,
  addFeedComment,
  uploadAttachment,
  getAttachmentUrl,
  estimateCalories,
  nutritionistChat,
}
