<script setup lang="ts">
defineProps<{ show: boolean; title: string }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

function close() {
  emit('update:show', false)
}
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-title">{{ title }}</div>
      <slot />
      <div class="modal-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
