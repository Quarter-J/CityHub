<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { FollowUser } from '@/types'
import BaseImg from '@/components/base/BaseImg.vue'
import { PLACEHOLDER_AVATAR } from '@/utils/placeholder'

defineProps<{ users: FollowUser[] }>()
const router = useRouter()

function goUser(id: number) {
  router.push(`/user/${id}`)
}
</script>

<template>
  <div class="user-grid">
    <div v-for="u in users" :key="u.id" class="user-cell" @click="goUser(u.id)">
      <BaseImg :src="u.icon" alt="" :placeholder="PLACEHOLDER_AVATAR" />
      <div class="user-cell-name">{{ u.nickName || '用户' }}</div>
    </div>
  </div>
</template>
