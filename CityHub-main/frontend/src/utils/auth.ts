import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import router from '@/router'

// 行内鉴权：未登录时提示并跳转登录页（对应原 requireAuth）
export function requireAuth(): boolean {
  const auth = useAuthStore()
  if (auth.isLoggedIn) return true
  useToastStore().show('请先登录', 'error')
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  return false
}
