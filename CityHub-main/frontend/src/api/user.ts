import { request } from './request'
import type { UserDTO, UserInfo } from '@/types'

export const userAPI = {
  sendCode: (phone: string) =>
    request<null>({ url: '/user/code', method: 'POST', params: { phone } }),

  login: (data: { phone: string; code: string }) =>
    request<string>({ url: '/user/login', method: 'POST', data }),

  logout: () => request<null>({ url: '/user/logout', method: 'POST' }),

  me: () => request<UserDTO>({ url: '/user/me' }),

  info: (id: number | string) => request<UserInfo>({ url: `/user/info/${id}` }),

  userById: (id: number | string) => request<UserDTO>({ url: `/user/${id}` }),

  update: (data: { nickName?: string; icon?: string }) =>
    request<null>({ url: '/user/update', method: 'PUT', data }),

  updateInfo: (data: { city?: string; introduce?: string }) =>
    request<null>({ url: '/user/info', method: 'PUT', data }),

  sign: () => request<null>({ url: '/user/sign', method: 'POST' }),

  signCount: () => request<number>({ url: '/user/sign/count' }),

  signStatus: () => request<boolean>({ url: '/user/sign/status' }),
}
