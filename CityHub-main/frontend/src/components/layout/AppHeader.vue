<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useToastStore } from '@/stores/toast'
import BaseIcon from '@/components/base/BaseIcon.vue'
import BaseImg from '@/components/base/BaseImg.vue'
import { PLACEHOLDER_AVATAR } from '@/utils/placeholder'

const auth = useAuthStore()
const theme = useThemeStore()
const toast = useToastStore()
const route = useRoute()
const router = useRouter()

const searchKeyword = ref('')

const navItems = [
  { to: '/', label: '首页', names: ['home'] },
  { to: '/voucher', label: '优惠券', names: ['voucher', 'seckill'] },
  { to: '/blog', label: '笔记', names: ['blog', 'blog-detail'] },
  { to: '/follow', label: '关注', names: ['follow'] },
]

function isActive(names: string[]): boolean {
  return names.includes(route.name as string)
}

async function handleLogout() {
  await auth.logout()
  toast.show('已退出登录', 'success')
  router.push('/')
}

function searchShop() {
  const q = searchKeyword.value.trim()
  if (q) {
    router.push({ path: '/', query: { search: q } })
    searchKeyword.value = ''
  }
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/" class="logo">
        <span class="logo-mark"><BaseIcon name="pin" /></span>City<span>Hub</span>
      </router-link>
      <nav class="nav-links">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          :class="{ active: isActive(item.names) }"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <div v-if="route.name === 'home'" class="search-box">
        <input v-model="searchKeyword" type="text" placeholder="搜索商家..." @keydown.enter="searchShop">
        <button type="button" class="search-icon" aria-label="搜索商家" @click="searchShop">
          <BaseIcon name="search" />
        </button>
      </div>
      <div class="header-actions">
        <template v-if="auth.isLoggedIn">
          <router-link to="/user">{{ auth.user?.nickName || '我的' }}</router-link>
          <a href="#" @click.prevent="handleLogout">退出</a>
          <button class="theme-toggle" aria-label="切换主题" title="切换主题" @click="theme.toggle()">
            <BaseIcon name="sun" class-name="icon-sun" />
            <BaseIcon name="moon" class-name="icon-moon" />
          </button>
          <router-link to="/user">
            <BaseImg class="avatar-sm" :src="auth.user?.icon" alt="" :placeholder="PLACEHOLDER_AVATAR" />
          </router-link>
        </template>
        <template v-else>
          <router-link to="/login" class="btn-login">登录</router-link>
          <button class="theme-toggle" aria-label="切换主题" title="切换主题" @click="theme.toggle()">
            <BaseIcon name="sun" class-name="icon-sun" />
            <BaseIcon name="moon" class-name="icon-moon" />
          </button>
        </template>
      </div>
    </div>
  </header>
</template>
