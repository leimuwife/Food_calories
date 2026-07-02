import { useUserStore } from '@/stores/user'
import type {
  LoginResultVO,
  UserVO,
  FoodVO,
  FoodCategoryVO,
  FoodSearchResultVO,
  DietRecordVO,
  DietItemVO,
  DailyDietVO,
  DailySummaryVO,
  MonthlySummaryVO,
  RegisterParam,
  DietRecordParam,
  GoalUpdateParam,
  ProfileUpdateParam,
} from './types'

const BASE_URL = 'http://localhost:8088'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: AnyObject
  header?: Record<string, string>
  showLoading?: boolean
}

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

function request<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const { url, method = 'GET', data, header = {}, showLoading = true } = options

  if (showLoading) {
    uni.showLoading({ title: '加载中...', mask: true })
  }

  const userStore = useUserStore()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...header,
  }

  if (userStore.token) {
    headers['Authorization'] = `Bearer ${userStore.token}`
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: headers,
      timeout: 15000,
      success: (res) => {
        const statusCode = res.statusCode
        const responseData = res.data as ApiResponse<T>

        if (statusCode === 200 && responseData.code === 200) {
          resolve(responseData)
        } else if (statusCode === 401) {
          userStore.logout()
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          reject(new Error('Unauthorized'))
        } else {
          uni.showToast({
            title: responseData.message || '请求失败',
            icon: 'none',
          })
          reject(new Error(responseData.message || 'Request failed'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请检查网络', icon: 'none' })
        reject(err)
      },
      complete: () => {
        if (showLoading) {
          uni.hideLoading()
        }
      },
    })
  })
}

export function wxLogin(code: string) {
  return request<LoginResultVO>({
    url: '/api/auth/wx-login',
    method: 'POST',
    data: { code },
  })
}

export function accountLogin(username: string, password: string) {
  return request<LoginResultVO>({
    url: '/api/auth/login',
    method: 'POST',
    data: { username, password },
  })
}

export function register(data: RegisterParam) {
  return request<LoginResultVO>({
    url: '/api/auth/register',
    method: 'POST',
    data,
  })
}

export function updateProfile(data: ProfileUpdateParam) {
  return request({
    url: '/api/user/profile',
    method: 'PUT',
    data,
  })
}

export function updateNutritionGoal(data: GoalUpdateParam) {
  return request({
    url: '/api/user/goal',
    method: 'PUT',
    data,
  })
}

export function searchFood(keyword: string, category?: string) {
  return request<FoodSearchResultVO>({
    url: '/api/food/search',
    method: 'GET',
    data: { keyword, category },
  })
}

export function getFoodDetail(foodId: number) {
  return request<FoodVO>({
    url: `/api/food/${foodId}`,
    method: 'GET',
  })
}

export function getFoodCategories() {
  return request<FoodCategoryVO[]>({
    url: '/api/food/categories',
    method: 'GET',
  })
}

export function addDietRecord(data: DietRecordParam) {
  return request<{ recordId: number }>({
    url: '/api/diet/record',
    method: 'POST',
    data,
  })
}

export function getTodayRecords(date: string) {
  return request<DailyDietVO>({
    url: '/api/diet/record',
    method: 'GET',
    data: { date },
  })
}

export function getRecordsByRange(startDate: string, endDate: string) {
  return request<DietRecordVO[]>({
    url: '/api/diet/records/range',
    method: 'GET',
    data: { startDate, endDate },
  })
}

export function deleteDietRecord(recordId: number) {
  return request({
    url: `/api/diet/record/${recordId}`,
    method: 'DELETE',
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
  return request<DailySummaryVO>({
    url: '/api/statistics/daily',
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

export function exportData(startDate: string, endDate: string) {
  return request<{ csvContent: string }>({
    url: '/api/statistics/export',
    method: 'GET',
    data: { startDate, endDate },
  })
}

export default {
  wxLogin,
  accountLogin,
  register,
  updateProfile,
  updateNutritionGoal,
  searchFood,
  getFoodDetail,
  getFoodCategories,
  addDietRecord,
  getTodayRecords,
  getRecordsByRange,
  deleteDietRecord,
  updateDietItem,
  deleteDietItem,
  copyRecordToToday,
  getDailySummary,
  getMonthlySummary,
  exportData,
}
