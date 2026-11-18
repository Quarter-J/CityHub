<script setup lang="ts">
import { useToastStore } from '@/stores/toast'
import BaseIcon from './BaseIcon.vue'

const toast = useToastStore()

function iconOf(type: string): string {
  if (type === 'success') return 'check'
  if (type === 'error') return 'alert'
  return 'info'
}
</script>

<template>
  <TransitionGroup name="toast" tag="div" class="toast-container">
    <div v-for="t in toast.toasts" :key="t.id" class="toast" :class="`toast-${t.type}`">
      <BaseIcon :name="iconOf(t.type)" />
      <span>{{ t.message }}</span>
    </div>
  </TransitionGroup>
</template>

<style>
.toast-leave-active {
  transition: opacity 0.3s ease;
}
.toast-leave-to {
  opacity: 0;
}
</style>
