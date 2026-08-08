import request from '../request'
import type { DietRecordParam } from '../types'
import { useUserStore } from '@/stores/user'

const BASE_URL = 'http://localhost:8088'

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