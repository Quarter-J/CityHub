import { request } from './request'
import type { Blog, ScrollResult, FollowUser } from '@/types'

export const blogAPI = {
  save: (blog: unknown) => request<null>({ url: '/blog', method: 'POST', data: blog }),

  like: (id: number | string) => request<null>({ url: `/blog/like/${id}`, method: 'PUT' }),

  my: (current = 1) => request<Blog[]>({ url: '/blog/of/me', params: { current } }),

  hot: (current = 1) => request<Blog[]>({ url: '/blog/hot', params: { current } }),

  detail: (id: number | string) => request<Blog>({ url: `/blog/${id}` }),

  likes: (id: number | string) => request<FollowUser[]>({ url: `/blog/likes/${id}` }),

  byUser: (id: number | string, current = 1) =>
    request<Blog[]>({ url: '/blog/of/user', params: { id, current } }),

  followFeed: (lastId: number, offset = 0) =>
    request<ScrollResult<Blog>>({ url: '/blog/of/follow', params: { lastId, offset } }),

  ofLiked: (current = 1) => request<Blog[]>({ url: '/blog/of/liked', params: { current } }),

  ofShop: (shopId: number | string, current = 1) =>
    request<Blog[]>({ url: '/blog/of/shop', params: { shopId, current } }),
}
