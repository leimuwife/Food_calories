import request from '../request'
import type { DietRecordParam, FoodCategoryVO, FoodSearchResult, FoodVO } from '../types'
import { useUserStore } from '@/stores/user'

// 生产环境使用同域 /api（Nginx 反向代理），留空即请求当前站点域名，避免写死 localhost
const BASE_URL = ''

export async function addDietRecord(data: DietRecordParam, filePath?: string): Promise<{ recordId: number }> {
  if (filePath && filePath.trim()) {
    const userStore = useUserStore()
    const header: Record<string, string> = {}
    if (userStore.token) {
      header['Authorization'] = `Bearer ${userStore.token}`
    }
    
    return new Promise<{ recordId: number }>((resolve, reject) => {
      uni.uploadFile({
        url: BASE_URL + '/api/diet/record',
        filePath: filePath,
        name: 'file',
        header: header,
        formData: {
          data: JSON.stringify(data),
        },
        success: (res) => {
          try {
            const result = JSON.parse(res.data)
            if (result.code === 200) {
              resolve(result.data)
            } else {
              reject(new Error(result.message || '保存失败'))
            }
          } catch (e) {
            reject(new Error('解析响应失败'))
          }
        },
        fail: (err) => {
          reject(new Error('网络异常，请检查网络连接'))
        },
      })
    })
  }
  
  const result = await request<{ recordId: number }>({
    url: '/api/diet/record',
    method: 'POST',
    data,
    showLoading: false,
  })
  return result.data
}

export function estimateCalories(foodName: string, foodDesc: string, weight?: number) {
  return request<{ totalCalorie: number }>({
    url: '/api/ai/estimate-calorie',
    method: 'GET',
    params: { foodName, foodDesc, weight },
  })
}

/** 分页搜索食物 */
export function searchFood(keyword = '', category?: string, page = 1, pageSize = 20) {
  return request<FoodSearchResult>({
    url: '/api/food/search',
    method: 'GET',
    params: {
      keyword,
      ...(category ? { category } : {}),
      page,
      pageSize,
    },
    showLoading: false,
  })
}

/** 查询食物详情 */
export function getFoodDetail(id: string | number) {
  return request<FoodVO>({
    url: `/api/food/${id}`,
    method: 'GET',
    showLoading: false,
  })
}

/** 查询食物分类 */
export function getFoodCategories() {
  return request<FoodCategoryVO[]>({
    url: '/api/food/categories',
    method: 'GET',
    showLoading: false,
  })
}

export function deleteDietRecord(recordId: string | number) {
  return request({
    url: `/api/diet/record/${recordId}`,
    method: 'DELETE',
  })
}

export function getDietItemDetail(itemId: string | number) {
  return request<{ foodName: string; foodDesc: string; weight: number; calories: number; remark: string; imageUrls: string[] }>({
    url: `/api/diet/item/${itemId}`,
    method: 'GET',
  })
}

export async function updateDietItem(data: DietRecordParam, filePath?: string): Promise<{ itemId: number }> {
  if (filePath && filePath.trim()) {
    const userStore = useUserStore()
    const header: Record<string, string> = {}
    if (userStore.token) {
      header['Authorization'] = `Bearer ${userStore.token}`
    }
    
    return new Promise<{ itemId: number }>((resolve, reject) => {
      uni.uploadFile({
        url: BASE_URL + '/api/diet/item',
        filePath: filePath,
        name: 'file',
        header: header,
        formData: {
          data: JSON.stringify(data),
        },
        success: (res) => {
          try {
            const result = JSON.parse(res.data)
            if (result.code === 200) {
              resolve(result.data)
            } else {
              reject(new Error(result.message || '更新失败'))
            }
          } catch (e) {
            reject(new Error('解析响应失败'))
          }
        },
        fail: (err) => {
          reject(new Error('网络异常，请检查网络连接'))
        },
      })
    })
  }
  
  const result = await request<{ itemId: number }>({
    url: '/api/diet/item',
    method: 'PUT',
    data,
    showLoading: false,
  })
  return result.data
}
