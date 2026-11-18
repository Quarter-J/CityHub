<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { voucherAPI } from '@/api/voucher'
import type { Voucher } from '@/types'
import VoucherCard from '@/components/voucher/VoucherCard.vue'
import BasePagination from '@/components/base/BasePagination.vue'
import StateView from '@/components/base/StateView.vue'

type FilterType = 'all' | 'normal' | 'seckill'

const tabs: Array<{ key: FilterType; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'normal', label: '普通券' },
  { key: 'seckill', label: '秒杀券' },
]

const filterType = ref<FilterType>('all')
const currentPage = ref(1)
const totalPages = ref(1)
const vouchers = ref<Voucher[]>([])
const loading = ref(true)
const error = ref('')

const PAGE_SIZE = 10

function switchTab(type: FilterType) {
  filterType.value = type
  currentPage.value = 1
  loadVouchers()
}

async function loadVouchers() {
  loading.value = true
  error.value = ''
  try {
    const typeParam = filterType.value === 'all' ? null : filterType.value === 'seckill' ? 1 : 0
    const res = await voucherAPI.all(typeParam, currentPage.value)
    const list = res.data || []
    const total = res.total ?? list.length
    vouchers.value = list
    totalPages.value = Math.max(1, Math.ceil(total / PAGE_SIZE))
  } catch (err) {
    error.value = (err as Error).message || '加载失败'
    vouchers.value = []
  } finally {
    loading.value = false
  }
}

function goPage(page: number) {
  currentPage.value = page
  loadVouchers()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadVouchers)
</script>

<template>
  <div class="page-container">
    <div class="section-title"><h2>全部优惠券</h2></div>

    <div class="tabs">
      <span
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: filterType === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </span>
    </div>

    <StateView
      :loading="loading"
      :error="error"
      :empty="!loading && !vouchers.length"
      empty-icon="ticket"
      empty-text="暂无优惠券"
    >
      <div class="voucher-list">
        <VoucherCard v-for="v in vouchers" :key="v.id" :voucher="v" />
      </div>
    </StateView>

    <BasePagination :current="currentPage" :total-pages="totalPages" @change="goPage" />
  </div>
</template>
