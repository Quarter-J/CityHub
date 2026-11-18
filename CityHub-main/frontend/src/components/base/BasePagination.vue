<script setup lang="ts">
const props = defineProps<{ current: number; totalPages: number }>()
const emit = defineEmits<{ change: [page: number] }>()

function go(page: number) {
  if (page < 1 || page > props.totalPages) return
  emit('change', page)
}
</script>

<template>
  <div v-if="totalPages > 1" class="pagination">
    <button class="btn btn-outline btn-sm" :disabled="current === 1" @click="go(current - 1)">上一页</button>
    <button
      v-for="i in totalPages"
      :key="i"
      class="btn btn-sm"
      :class="i === current ? 'btn-primary' : 'btn-outline'"
      @click="go(i)"
    >
      {{ i }}
    </button>
    <button class="btn btn-outline btn-sm" :disabled="current === totalPages" @click="go(current + 1)">下一页</button>
  </div>
</template>
