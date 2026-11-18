<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { blogAPI } from '@/api/blog'
import { uploadAPI } from '@/api/upload'
import type { Blog } from '@/types'
import { requireAuth } from '@/utils/auth'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import BlogCard from '@/components/blog/BlogCard.vue'
import BasePagination from '@/components/base/BasePagination.vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import StateView from '@/components/base/StateView.vue'

const auth = useAuthStore()
const toast = useToastStore()

const currentPage = ref(1)
const totalPages = ref(1)
const blogs = ref<Blog[]>([])
const loading = ref(true)
const error = ref('')

const publishShow = ref(false)
const blogTitle = ref('')
const blogShopId = ref('')
const blogContent = ref('')
const selectedFiles = ref<File[]>([])
const imagePreviews = ref<string[]>([])
const fileInputEl = ref<HTMLInputElement | null>(null)

const PAGE_SIZE = 10

async function loadBlogs() {
  loading.value = true
  error.value = ''
  try {
    const res = await blogAPI.hot(currentPage.value)
    const list = res.data || []
    const total = res.total ?? list.length
    totalPages.value = Math.max(1, Math.ceil(total / PAGE_SIZE))
    blogs.value = list
  } catch (err) {
    error.value = (err as Error).message
    blogs.value = []
  } finally {
    loading.value = false
  }
}

function goPage(page: number) {
  currentPage.value = page
  loadBlogs()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openPublish() {
  if (!requireAuth()) return
  publishShow.value = true
}

function closePublish() {
  publishShow.value = false
  blogTitle.value = ''
  blogShopId.value = ''
  blogContent.value = ''
  if (fileInputEl.value) fileInputEl.value.value = ''
  selectedFiles.value = []
  imagePreviews.value = []
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files ? Array.from(input.files) : []
  selectedFiles.value = files
  imagePreviews.value = []
  files.forEach((file) => {
    const reader = new FileReader()
    reader.onload = (ev) => {
      imagePreviews.value.push((ev.target?.result as string) || '')
    }
    reader.readAsDataURL(file)
  })
}

async function submitBlog() {
  const title = blogTitle.value.trim()
  const content = blogContent.value.trim()
  const shopId = blogShopId.value.trim()

  if (!title) {
    toast.show('请输入标题', 'error')
    return
  }
  if (!content) {
    toast.show('请输入内容', 'error')
    return
  }

  try {
    let imgNames: string[] = []
    if (selectedFiles.value.length) {
      imgNames = await Promise.all(
        selectedFiles.value.map((file) => uploadAPI.blogImage(file).then((r) => r.data || '')),
      )
    }

    const payload: { title: string; content: string; images: string; shopId?: number } = {
      title,
      content,
      images: imgNames.join(','),
    }
    if (shopId) payload.shopId = parseInt(shopId, 10)

    await blogAPI.save(payload)
    toast.show('发布成功', 'success')
    closePublish()
    currentPage.value = 1
    loadBlogs()
  } catch (err) {
    toast.show((err as Error).message || '发布失败', 'error')
  }
}

onMounted(loadBlogs)
</script>

<template>
  <div class="page-container">
    <div class="section-title">
      <h2>热门笔记</h2>
    </div>

    <StateView
      :loading="loading"
      :error="error"
      :empty="!loading && !blogs.length"
      empty-icon="file"
      empty-text="暂无笔记"
    >
      <div class="blog-list">
        <BlogCard
          v-for="blog in blogs"
          :key="blog.id"
          :blog="blog"
          header-to-user
          title-to-detail
          like-clickable
          show-images
          show-comments
        />
      </div>
    </StateView>

    <BasePagination :current="currentPage" :total-pages="totalPages" @change="goPage" />

    <button v-if="auth.isLoggedIn" class="fab" aria-label="发布笔记" @click="openPublish">
      <BaseIcon name="edit" />
    </button>

    <BaseModal v-model:show="publishShow" title="发布笔记">
      <div class="form-group">
        <label>标题</label>
        <input v-model="blogTitle" type="text" class="form-control" placeholder="输入笔记标题">
      </div>
      <div class="form-group">
        <label>关联商铺ID（可选）</label>
        <input v-model="blogShopId" type="text" class="form-control" placeholder="输入商铺ID">
      </div>
      <div class="form-group">
        <label>内容</label>
        <textarea v-model="blogContent" class="form-control" placeholder="分享你的探店体验..." rows="5"></textarea>
      </div>
      <div class="form-group">
        <label>上传图片</label>
        <input ref="fileInputEl" type="file" class="form-control" accept="image/*" multiple @change="onFileChange">
        <div v-if="imagePreviews.length" style="display:flex;gap:8px;margin-top:12px;flex-wrap:wrap;">
          <img
            v-for="(src, i) in imagePreviews"
            :key="i"
            :src="src"
            alt=""
            style="width:80px;height:80px;object-fit:cover;border-radius:8px;border:1px solid var(--border);"
          >
        </div>
      </div>
      <template #actions>
        <button class="btn btn-outline" @click="closePublish">取消</button>
        <button class="btn btn-primary" @click="submitBlog">发布</button>
      </template>
    </BaseModal>
  </div>
</template>
