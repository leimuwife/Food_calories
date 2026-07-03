import { useUserStore } from '@/stores/user'
import type { FoodVO, DailyDietVO, DailySummaryVO } from '../types'

const BASE_URL = 'http://localhost:8088'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: AnyObject
  header?: Record<string, string>
  showLoading?: boolean
}

interface ApiResponse<T = unknown> {
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

export interface HomePageData {
  todayRecords: DailyDietVO
  dailySummary: DailySummaryVO
}

export interface FoodSearchResult {
  list: FoodVO[]
  total: number
}

export function searchFood(keyword: string): Promise<ApiResponse<FoodSearchResult>> {
  return request<FoodSearchResult>({
    url: '/api/food/search',
    method: 'GET',
    data: { keyword },
    showLoading: false,
  })
}

export function getTodayRecords(date: string): Promise<ApiResponse<DailyDietVO>> {
  return request<DailyDietVO>({
    url: '/api/diet/record',
    method: 'GET',
    data: { date },
    showLoading: false,
  })
}

export function getDailySummary(date: string): Promise<ApiResponse<DailySummaryVO>> {
  return request<DailySummaryVO>({
    url: '/api/statistics/daily',
    method: 'GET',
    data: { date },
    showLoading: false,
  })
}

export default {
  searchFood,
  getTodayRecords,
  getDailySummary,
}
