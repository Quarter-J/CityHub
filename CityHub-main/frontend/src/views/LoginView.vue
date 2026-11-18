<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userAPI } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useThemeStore } from '@/stores/theme'
import BaseIcon from '@/components/base/BaseIcon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const theme = useThemeStore()

const phone = ref('')
const code = ref('')
const countdown = ref(0)
let timer: number | null = null

function onPhoneInput(e: Event) {
  const input = e.target as HTMLInputElement
  phone.value = input.value.replace(/\D/g, '')
}

function sendCode() {
  if (!/^1\d{10}$/.test(phone.value)) {
    toast.show('请输入正确的手机号', 'error')
    return
  }
  if (countdown.value > 0) return

  userAPI
    .sendCode(phone.value)
    .then(() => {
      toast.show('验证码已发送', 'success')
      countdown.value = 60
      updateCodeBtn()
    })
    .catch((err) => toast.show((err as Error).message || '发送失败', 'error'))
}

function updateCodeBtn() {
  if (countdown.value > 0) {
    countdown.value--
    timer = window.setTimeout(updateCodeBtn, 1000)
  }
}

async function doLogin() {
  if (!/^1\d{10}$/.test(phone.value)) {
    toast.show('请输入正确的手机号', 'error')
    return
  }
  if (!code.value) {
    toast.show('请输入验证码', 'error')
    return
  }

  try {
    await auth.login(phone.value, code.value)
    toast.show('登录成功', 'success')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (err) {
    toast.show((err as Error).message || '登录失败', 'error')
  }
}

onUnmounted(() => {
  if (timer) window.clearTimeout(timer)
})
</script>

<template>
  <div class="login-page">
    <button class="theme-toggle login-float-toggle" aria-label="切换主题" title="切换主题" @click="theme.toggle()">
      <BaseIcon name="sun" class-name="icon-sun" />
      <BaseIcon name="moon" class-name="icon-moon" />
    </button>

    <div class="login-box">
      <div class="login-brand">
        <span class="logo-mark"><BaseIcon name="pin" /></span>
      </div>
      <div class="login-title">欢迎来到 CityHub</div>
      <div class="login-subtitle">本地生活，尽在掌握</div>

      <div>
        <div class="form-group">
          <label>手机号</label>
          <input :value="phone" type="tel" class="form-control" placeholder="请输入手机号" maxlength="11" @input="onPhoneInput">
        </div>

        <div class="form-group">
          <label>验证码</label>
          <div class="code-row">
            <input v-model="code" type="text" class="form-control" placeholder="请输入验证码" maxlength="6">
            <button class="btn btn-outline btn-code" :disabled="countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <button class="btn btn-primary btn-block btn-lg" style="margin-top:8px;" @click="doLogin">登录</button>
      </div>

      <div style="text-align:center;margin-top:24px;font-size:13px;color:var(--text-light);">
        登录即表示同意 <a href="#" style="color:var(--primary)">服务条款</a> 和 <a href="#" style="color:var(--primary)">隐私政策</a>
      </div>

      <div style="text-align:center;margin-top:16px;">
        <router-link to="/" style="font-size:13px;color:var(--text-light);"><BaseIcon name="arrow-left" /> 返回首页</router-link>
      </div>
    </div>
  </div>
</template>
