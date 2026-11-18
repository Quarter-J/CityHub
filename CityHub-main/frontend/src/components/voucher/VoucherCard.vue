<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Voucher } from '@/types'
import BaseIcon from '@/components/base/BaseIcon.vue'

const props = withDefaults(defineProps<{ voucher: Voucher; variant?: 'shop' | 'list' }>(), {
  variant: 'list',
})

const router = useRouter()

const isSeckill = computed(() => props.voucher.type === 1)
const gradient = computed(() =>
  isSeckill.value
    ? 'linear-gradient(135deg, var(--primary), var(--secondary))'
    : 'linear-gradient(135deg, #1890ff, #69c0ff)',
)
const subtitle = computed(() =>
  props.voucher.subTitle || (props.variant === 'shop' ? '优惠券描述' : '超值优惠，限时抢购'),
)

function goSeckill() {
  router.push(`/seckill/${props.voucher.id}`)
}
</script>

<template>
  <div class="voucher-card">
    <div class="voucher-left" :style="{ background: gradient }">
      <div class="v-price">{{ voucher.payValue }}</div>
      <div class="v-unit">元</div>
    </div>
    <div class="voucher-right">
      <div class="voucher-title">{{ voucher.title }}</div>
      <div class="voucher-desc">{{ subtitle }}</div>
      <div
        v-if="variant === 'list' && voucher.shopName"
        style="font-size:12px;color:var(--text-light);margin:4px 0 8px;"
      >
        <BaseIcon name="store" /> {{ voucher.shopName }}
      </div>
      <div class="voucher-footer">
        <span class="tag" :class="isSeckill ? 'tag-seckill' : 'tag-normal'">
          <BaseIcon v-if="isSeckill" name="zap" /> {{ isSeckill ? ' 秒杀' : '普通券' }}
        </span>
        <button v-if="isSeckill" class="btn btn-primary btn-sm" @click="goSeckill">立即抢购</button>
        <span v-else-if="variant === 'shop'" class="voucher-stock">满{{ voucher.condition || 0 }}元可用</span>
        <span v-else class="voucher-stock">抵扣 ¥{{ voucher.actualValue || 0 }}</span>
      </div>
    </div>
  </div>
</template>
