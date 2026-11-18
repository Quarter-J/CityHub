<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Shop } from '@/types'
import BaseImg from '@/components/base/BaseImg.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import { PLACEHOLDER_IMG } from '@/utils/placeholder'

const props = defineProps<{ shop: Shop }>()
const router = useRouter()

const cover = computed(() => (props.shop.images ? props.shop.images.split(',')[0] : ''))
const score = computed(() => (props.shop.score ? (props.shop.score / 10).toFixed(1) : '5.0'))

function goDetail() {
  router.push(`/shop/${props.shop.id}`)
}
</script>

<template>
  <div class="shop-card" @click="goDetail">
    <BaseImg class="shop-img" :src="cover" :alt="shop.name" :placeholder="PLACEHOLDER_IMG" />
    <div class="shop-info">
      <div class="shop-name">{{ shop.name }}</div>
      <div class="shop-meta">
        <span class="shop-score"><BaseIcon name="star" /> {{ score }}</span>
        <span>|</span>
        <span>{{ shop.area || '未知区域' }}</span>
      </div>
      <div class="shop-meta" style="margin-top:6px;">
        <span><BaseIcon name="pin" /> {{ shop.address || '地址待补充' }}</span>
      </div>
    </div>
  </div>
</template>
