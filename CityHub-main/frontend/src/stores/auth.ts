import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { userAPI } from '@/api/user'
import type { UserDTO } from '@/types'

const TOKEN_KEY = 'cityhub_token'
const USER_KEY = 'cityhub_user'

function readUser(): UserDTO | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserDTO
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<UserDTO | null>(readUser())

  const isLoggedIn = computed(() => !!token.value)

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  function setUser(value: UserDTO) {
    user.value = value
    localStorage.setItem(USER_KEY, JSON.stringify(value))
  }

  function clear() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function login(phone: string, code: string) {
    const res = await userAPI.login({ phone, code })
    if (res.data) setToken(res.data)
    try {
      const meRes = await userAPI.me()
      if (meRes.data) setUser(meRes.data)
    } catch {
      // 忽略 me 失败，不影响登录流程
    }
  }

  async function logout() {
    try {
      await userAPI.logout()
    } catch {
      // 忽略登出接口失败
    }
    clear()
  }

  return { token, user, isLoggedIn, setToken, setUser, clear, login, logout }
})
