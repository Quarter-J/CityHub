<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { voucherAPI, seckillAPI } from '@/api/voucher'
import type { Voucher } from '@/types'
import { requireAuth } from '@/utils/auth'
import { useToastStore } from '@/stores/toast'
import { formatTimeRemaining } from '@/utils/format'
import BaseIcon from '@/components/base/BaseIcon.vue'
import StateView from '@/components/base/StateView.vue'

const route = useRoute()
const toast = useToastStore()

const voucherId = route.params.id as string

const voucher = ref<Voucher | null>(null)
const loading = ref(true)
const error = ref('')
const timerText = ref('--:--:--')
const isProcessing = ref(false)
const done = ref(false)
const now = ref(Date.now())
let timerInterval: number | null = null

const begin = computed(() =>
  voucher.value?.beginTime ? new Date(voucher.value.beginTime).getTime() : now.value - 10000,
)
const end = computed(() =>
  voucher.value?.endTime ? new Date(voucher.value.endTime).getTime() : now.value + 86400000,
)

type Status = 'before' | 'ongoing' | 'ended'
const status = computed<Status>(() => {
  if (now.value < begin.value) return 'before'
  if (now.value > end.value) return 'ended'
  return 'ongoing'
})

const btnText = computed(() => {
  if (status.value === 'before') return '等待开始'
  if (status.value === 'ended') return '已结束'
  return '立即抢购'
})

async function loadSeckill() {
  if (!voucherId) {
    error.value = '秒杀券ID不存在'
    loading.value = false
    return
  }
  try {
    const res = await voucherAPI.detail(voucherId)
    voucher.value = res.data
    if (!res.data) {
      error.value = '秒杀券不存在或已下架'
    } else {
      startCountdown()
    }
  } catch {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
}

function startCountdown() {
  if (timerInterval) window.clearInterval(timerInterval)
  const tick = () => {
    now.value = Date.now()
    if (status.value === 'before') {
      timerText.value = formatTimeRemaining(begin.value - now.value)
    } else if (status.value === 'ongoing') {
      timerText.value = formatTimeRemaining(end.value - now.value)
    } else {
      if (timerInterval) window.clearInterval(timerInterval)
      timerInterval = null
    }
  }
  tick()
  timerInterval = window.setInterval(tick, 1000)
}

async function doSeckill() {
  if (!requireAuth()) return
  if (isProcessing.value) return
  isProcessing.value = true
  try {
    await seckillAPI.order(voucherId)
    done.value = true
    toast.show('抢购成功，订单正在异步生成', 'success')
  } catch (err) {
    toast.show((err as Error).message || '抢购失败', 'error')
  } finally {
    isProcessing.value = false
  }
}

onMounted(loadSeckill)
onUnmounted(() => {
  if (timerInterval) window.clearInterval(timerInterval)
})
</script>

<template>
  <div class="page-container">
    <StateView :loading="loading" :error="error">
      <template v-if="voucher">
        <div v-if="status === 'before'" class="seckill-timer">
          <div class="timer-label">距离开始还有</div>
          <div class="timer-display">{{ timerText }}</div>
        </div>
        <div v-else-if="status === 'ongoing'" class="seckill-timer">
          <div class="timer-label"><BaseIcon name="zap" /> 秒杀进行中</div>
          <div class="timer-display" style="font-size:24px;letter-spacing:2px;">{{ timerText }}</div>
        </div>
        <div v-else class="seckill-timer" style="background:linear-gradient(135deg, #9aa0aa, #5f6570);box-shadow:none;">
          <div class="timer-label">秒杀已结束</div>
        </div>

        <div class="card">
          <div class="card-body" style="text-align:center;padding:40px 32px;">
            <div style="font-size:52px;font-weight:800;color:var(--primary);letter-spacing:-2px;line-height:1;">¥{{ voucher.payValue }}</div>
            <div style="font-size:20px;font-weight:700;margin:16px 0 6px;">{{ voucher.title }}</div>
            <div style="color:var(--text-light);margin-bottom:16px;">{{ voucher.subTitle || '' }}</div>

            <div style="display:flex;justify-content:center;gap:48px;margin:24px 0;">
              <div>
                <div style="font-size:26px;font-weight:700;">{{ voucher.stock ?? '--' }}</div>
                <div style="font-size:12px;color:var(--text-light);margin-top:4px;">剩余库存</div>
              </div>
              <div style="width:1px;background:var(--border);"></div>
              <div>
                <div style="font-size:26px;font-weight:700;color:var(--secondary);">¥{{ voucher.actualValue }}</div>
                <div style="font-size:12px;color:var(--text-light);margin-top:4px;">原价</div>
              </div>
            </div>

            <button
              class="btn btn-primary btn-lg"
              :disabled="status === 'ended' || isProcessing || done"
              :style="status === 'ended' ? 'min-width:200px;opacity:0.4;cursor:not-allowed;' : 'min-width:200px;'"
              @click="doSeckill"
            >
              {{ done ? '已抢购' : isProcessing ? '处理中...' : btnText }}
            </button>
          </div>
        </div>
      </template>
    </StateView>
  </div>
</template>
