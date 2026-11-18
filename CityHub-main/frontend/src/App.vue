<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import ToastHost from '@/components/base/ToastHost.vue'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const theme = useThemeStore()

onMounted(() => {
  theme.init()
})
</script>

<template>
  <AppHeader v-if="!route.meta.hideHeader" />
  <router-view />
  <ToastHost />
</template>
