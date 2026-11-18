import { API_BASE } from '@/api/request'

// 头像上传返回相对路径 /avatars/xxx.png，需拼接后端地址与 /uploads 前缀
export function uploadsUrl(path: string): string {
  if (!path) return ''
  if (/^https?:\/\//.test(path)) return path
  return `${API_BASE}/uploads${path.startsWith('/') ? path : '/' + path}`
}
