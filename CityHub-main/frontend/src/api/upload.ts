import { request } from './request'

export const uploadAPI = {
  blogImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<string>({ url: '/upload/blog', method: 'POST', data: formData })
  },

  avatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<string>({ url: '/upload/avatar', method: 'POST', data: formData })
  },

  deleteBlogImage: (name: string) =>
    request<null>({ url: '/upload/blog/delete', params: { name } }),
}
