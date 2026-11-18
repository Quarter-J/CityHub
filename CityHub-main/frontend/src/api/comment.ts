import { request } from './request'
import type { BlogComment } from '@/types'

export const commentAPI = {
  list: (blogId: number | string, current = 1) =>
    request<BlogComment[]>({ url: `/blog-comments/of/blog/${blogId}`, params: { current } }),

  add: (data: { blogId: number; content: string }) =>
    request<null>({ url: '/blog-comments', method: 'POST', data }),

  remove: (id: number) => request<null>({ url: `/blog-comments/${id}`, method: 'DELETE' }),
}
