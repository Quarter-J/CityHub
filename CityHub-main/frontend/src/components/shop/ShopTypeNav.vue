<script setup lang="ts">
import type { ShopType } from '@/types'
import BaseIcon from '@/components/base/BaseIcon.vue'
import { shopTypeIconName } from '@/utils/icons'

defineProps<{ types: ShopType[]; activeId: number | null }>()
const emit = defineEmits<{ select: [id: number | null] }>()

function select(id: number | null) {
  emit('select', id)
}
</script>

<template>
  <div class="shop-type-nav">
    <div class="shop-type-item" :class="{ active: activeId === null }" @click="select(null)">
      <span class="type-icon"><BaseIcon name="home" /></span>
      <span class="type-name">全部</span>
    </div>
    <div
      v-for="type in types"
      :key="type.id"
      class="shop-type-item"
      :class="{ active: activeId === type.id }"
      @click="select(type.id)"
    >
      <span class="type-icon"><BaseIcon :name="shopTypeIconName(type)" /></span>
      <span class="type-name">{{ type.name }}</span>
    </div>
  </div>
</template>
