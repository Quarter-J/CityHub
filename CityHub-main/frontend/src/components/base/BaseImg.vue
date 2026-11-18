<script setup lang="ts">
import { ref, watch } from 'vue'
import { PLACEHOLDER_IMG } from '@/utils/placeholder'

const props = withDefaults(
  defineProps<{
    src?: string
    alt?: string
    placeholder?: string
    hideOnError?: boolean
  }>(),
  { placeholder: PLACEHOLDER_IMG, hideOnError: false },
)

const currentSrc = ref(props.src || props.placeholder)
const hidden = ref(false)

watch(
  () => props.src,
  (v) => {
    currentSrc.value = v || props.placeholder
    hidden.value = false
  },
)

function onError() {
  if (props.hideOnError) {
    hidden.value = true
  } else {
    currentSrc.value = props.placeholder
  }
}
</script>

<template>
  <img v-if="!hidden" :src="currentSrc" :alt="alt" @error="onError" />
</template>
