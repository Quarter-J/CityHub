<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shopAPI } from '@/api/shop'
import { voucherAPI } from '@/api/voucher'
import { blogAPI } from '@/api/blog'
import type { Shop, Voucher, Blog } from '@/types'
import BaseImg from '@/components/base/BaseImg.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import VoucherCard from '@/components/voucher/VoucherCard.vue'
import BlogCard from '@/components/blog/BlogCard.vue'
import StateView from '@/components/base/StateView.vue'
import { PLACEHOLDER_IMG } from '@/utils/placeholder'

const route = useRoute()
const shopId = route.params.id as string

const shop = ref<Shop | null>(null)
const loading = ref(true)
const error = ref('')
const vouchers = ref<Voucher[]>([])
const blogs = ref<Blog[]>([])

const bannerSrc = ref('')
const score = ref('5.0')

async function loadShop() {
  if (!shopId) {
    error.value = '商铺ID不存在'
    loading.value = false
    return
  }
  try {
    const res = await shopAPI.detail(shopId)
    shop.value = res.data
    if (!res.data) {
      error.value = '商铺不存在'
    } else {
      bannerSrc.value = res.data.images ? res.data.images.split(',')[0] : ''
      score.value = res.data.score ? (res.data.score / 10).toFixed(1) : '5.0'
    }
  } catch (err) {
    error.value = (err as Error).message
  } finally {
    loading.value = false
  }
}

async function loadVouchers() {
  if (!shopId) return
  try {
    const res = await voucherAPI.listByShop(shopId)
    vouchers.value = res.data || []
  } catch {
    vouchers.value = []
  }
}

async function loadShopBlogs() {
  try {
    const res = await blogAPI.ofShop(shopId, 1)
    blogs.value = res.data || []
  } catch {
    blogs.value = []
  }
}

onMounted(() => {
  loadShop()
  loadVouchers()
  loadShopBlogs()
})
</script>

<template>
  <div class="page-container">
    <StateView :loading="loading" :error="error">
      <template v-if="shop">
        <div class="shop-detail-header">
          <BaseImg class="shop-banner" :src="bannerSrc" :alt="shop.name" :placeholder="PLACEHOLDER_IMG" />
        </div>
        <div class="shop-detail-info">
          <div class="shop-name">{{ shop.name }}</div>
          <div style="display:flex;align-items:center;gap:14px;margin-bottom:8px;">
            <span class="shop-score-badge"><BaseIcon name="star" /> {{ score }}</span>
            <span style="color:var(--text-light);font-size:13px;">已售 {{ shop.sold || 0 }}</span>
          </div>
          <div class="shop-address"><BaseIcon name="pin" /> {{ shop.address || '地址不详' }}</div>
          <div v-if="shop.area" class="shop-address"><BaseIcon name="pin" /> {{ shop.area }}</div>
          <div class="shop-tags">
            <span class="tag tag-normal">{{ shop.typeName || '综合' }}</span>
            <span v-if="shop.averagePrice" class="tag tag-normal">人均 ¥{{ shop.averagePrice }}</span>
          </div>
        </div>

        <div style="margin-top:28px;">
          <div class="section-title"><h2>优惠券</h2></div>
          <div v-if="vouchers.length" class="voucher-list">
            <VoucherCard v-for="v in vouchers" :key="v.id" :voucher="v" variant="shop" />
          </div>
          <div v-else class="empty-state"><p>暂无优惠券</p></div>
        </div>

        <div style="margin-top:28px;">
          <div class="section-title"><h2>相关笔记</h2></div>
          <div v-if="blogs.length" class="blog-list">
            <BlogCard v-for="b in blogs" :key="b.id" :blog="b" card-to-detail />
          </div>
          <div v-else class="empty-state"><p>暂无相关笔记</p></div>
        </div>
      </template>
    </StateView>
  </div>
</template>
