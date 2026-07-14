import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const BASE_URL = '/api'

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error: any) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code !== '200' && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_nickname')
        localStorage.removeItem('admin_fileIds')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error: any) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export interface ApiResponse<T = any> {
  code: string | number
  message: string
  data: T
}

export default service