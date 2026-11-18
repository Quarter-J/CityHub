<script setup lang="ts">
import { computed } from 'vue'
import { icon } from '@/utils/icons'

const props = defineProps<{ name: string; className?: string }>()

const svg = computed(() => icon(props.name, props.className))
</script>

<template>
  <span class="base-icon" v-html="svg" />
</template>

<style scoped>
.base-icon {
  display: contents;
}
</style>
