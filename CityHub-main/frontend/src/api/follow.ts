import { request } from './request'
import type { FollowUser } from '@/types'

export const followAPI = {
  follow: (id: number | string, isFollow: boolean) =>
    request<null>({ url: `/follow/${id}/${isFollow}`, method: 'PUT' }),

  isFollow: (id: number | string) => request<boolean>({ url: `/follow/or/not/${id}` }),

  common: (id: number | string) => request<FollowUser[]>({ url: `/follow/common/${id}` }),

  ofMe: () => request<FollowUser[]>({ url: '/follow/of/me' }),

  fans: (id: number | string) => request<FollowUser[]>({ url: `/follow/fans/${id}` }),
}
