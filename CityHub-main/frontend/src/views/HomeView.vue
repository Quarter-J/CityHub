<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shopAPI, shopTypeAPI } from '@/api/shop'
import { blogAPI } from '@/api/blog'
import type { Shop, ShopType, Blog } from '@/types'
import ShopTypeNav from '@/components/shop/ShopTypeNav.vue'
import ShopCard from '@/components/shop/ShopCard.vue'
import BlogCard from '@/components/blog/BlogCard.vue'
import BasePagination from '@/components/base/BasePagination.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import StateView from '@/components/base/StateView.vue'

const route = useRoute()

const types = ref<ShopType[]>([])
const currentType = ref<number | null>(null)
const currentPage = ref(1)
const totalPages = ref(1)
const searchKeyword = ref('')
const shops = ref<Shop[]>([])
const totalShops = ref(0)
const loading = ref(true)
const error = ref('')
const hotBlogs = ref<Blog[]>([])

const PAGE_SIZE = 6

const sectionTitle = computed(() => {
  if (searchKeyword.value) return `搜索: "${searchKeyword.value}"`
  if (currentType.value) return '筛选结果'
  return '推荐商家'
})

async function loadShopTypes() {
  try {
    const res = await shopTypeAPI.list()
    types.value = res.data || []
  } catch {
    // 忽略分类加载失败
  }
}

async function loadShops() {
  loading.value = true
  error.value = ''
  try {
    let res
    if (searchKeyword.value) {
      res = await shopAPI.byName(searchKeyword.value, currentPage.value)
    } else if (currentType.value) {
      res = await shopAPI.byType(currentType.value, currentPage.value)
    } else {
      res = await shopAPI.byType(0, currentPage.value)
    }
    const list = res.data || []
    const total = res.total ?? list.length
    shops.value = list
    totalShops.value = total
    totalPages.value = Math.max(1, Math.ceil(total / PAGE_SIZE))
  } catch (err) {
    error.value = (err as Error).message
    shops.value = []
  } finally {
    loading.value = false
  }
}

function filterByType(typeId: number | null) {
  currentType.value = typeId
  currentPage.value = 1
  searchKeyword.value = ''
  loadShops()
  loadShopTypes()
}

function goPage(page: number) {
  currentPage.value = page
  loadShops()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadHotBlogs() {
  try {
    const res = await blogAPI.hot(1)
    hotBlogs.value = (res.data || []).slice(0, 3)
  } catch {
    hotBlogs.value = []
  }
}

watch(
  () => route.query.search,
  (val) => {
    searchKeyword.value = typeof val === 'string' ? val : ''
    currentType.value = null
    currentPage.value = 1
    loadShops()
    loadShopTypes()
  },
)

onMounted(() => {
  const q = route.query.search
  searchKeyword.value = typeof q === 'string' ? q : ''
  loadShopTypes()
  loadShops()
  loadHotBlogs()
})
</script>

<template>
  <div class="page-container">
    <div class="card" style="margin-bottom:24px;">
      <div class="card-body">
        <ShopTypeNav :types="types" :active-id="currentType" @select="filterByType" />
      </div>
    </div>

    <div class="section-title">
      <h2>{{ sectionTitle }}</h2>
      <span class="more">共 {{ totalShops }} 家</span>
    </div>

    <StateView
      :loading="loading"
      :error="error"
      :empty="!loading && !shops.length"
      empty-icon="inbox"
      empty-text="暂无商家信息"
    >
      <div class="shop-grid">
        <ShopCard v-for="shop in shops" :key="shop.id" :shop="shop" />
      </div>
    </StateView>

    <BasePagination :current="currentPage" :total-pages="totalPages" @change="goPage" />

    <div style="margin-top:48px;">
      <div class="section-title">
        <h2>热门笔记</h2>
        <router-link to="/blog" class="more">查看更多 <BaseIcon name="arrow-right" /></router-link>
      </div>
      <div v-if="hotBlogs.length" class="blog-list">
        <BlogCard v-for="blog in hotBlogs" :key="blog.id" :blog="blog" card-to-detail />
      </div>
    </div>
  </div>
</template>
