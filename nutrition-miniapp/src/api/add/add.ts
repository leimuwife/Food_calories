import request from '../request'
import type { DietRecordParam } from '../types'
import { useUserStore } from '@/stores/user'

const BASE_URL = 'http://localhost:8088'

export async function addDietRecord(data: DietRecordParam, filePath?: string): Promise<{ recordId: number }> {
  if (filePath) {
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
              uni.showToast({ title: result.message || '保存失败', icon: 'none' })
              reject(new Error(result.message || '保存失败'))
            }
          } catch (e) {
            uni.showToast({ title: '解析响应失败', icon: 'none' })
            reject(new Error('解析响应失败'))
          }
        },
        fail: (err) => {
          uni.showToast({ title: '网络异常', icon: 'none' })
          reject(err)
        },
      })
    })
  }
  
  const result = await request<{ recordId: number }>({
    url: '/api/diet/record',
    method: 'POST',
    data,
  })
  return result.data
}

export function estimateCalories(description: string) {
  return request<{ calories: number }>({
    url: '/api/nutritionist/estimate-calories',
    method: 'POST',
    data: { description },
  })
}

export function deleteDietRecord(recordId: string | number) {
  return request({
    url: `/api/diet/record/${recordId}`,
    method: 'DELETE',
  })
}