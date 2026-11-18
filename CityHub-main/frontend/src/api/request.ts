import axios, { type AxiosRequestConfig } from 'axios'
import type { Result } from '@/types'

export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8081'
const TOKEN_KEY = 'cityhub_token'

const instance = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

// 请求拦截器：注入 token
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

// 响应拦截器：HTTP 错误统一转成带 errorMsg 的 Error
instance.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data
    const msg =
      (data && typeof data === 'object' && 'errorMsg' in data && data.errorMsg) ||
      error.message ||
      '请求失败'
    return Promise.reject(new Error(msg))
  },
)

// 统一封装：success 为 true 时返回 Result，否则抛出 errorMsg
export async function request<T = unknown>(config: AxiosRequestConfig): Promise<Result<T>> {
  const response = await instance.request<Result<T>>(config)
  const body = response.data
  if (body && body.success) return body
  throw new Error(body?.errorMsg || '请求失败')
}

export default instance
