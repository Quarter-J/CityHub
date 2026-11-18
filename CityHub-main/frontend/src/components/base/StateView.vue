<script setup lang="ts">
import BaseIcon from './BaseIcon.vue'

defineProps<{
  loading?: boolean
  error?: string
  empty?: boolean
  emptyIcon?: string
  emptyText?: string
}>()
</script>

<template>
  <div v-if="loading" class="loading">加载中</div>
  <div v-else-if="error" class="empty-state">
    <div class="empty-icon"><BaseIcon name="alert" /></div>
    <p>{{ error }}</p>
  </div>
  <div v-else-if="empty" class="empty-state">
    <div v-if="emptyIcon" class="empty-icon"><BaseIcon :name="emptyIcon" /></div>
    <p>{{ emptyText || '暂无数据' }}</p>
  </div>
  <slot v-else />
</template>
