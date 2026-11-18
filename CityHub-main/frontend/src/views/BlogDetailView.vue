<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { blogAPI } from '@/api/blog'
import { commentAPI } from '@/api/comment'
import type { Blog, FollowUser, BlogComment } from '@/types'
import { requireAuth } from '@/utils/auth'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { formatTime } from '@/utils/format'
import { PLACEHOLDER_AVATAR } from '@/utils/placeholder'
import BaseImg from '@/components/base/BaseImg.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import StateView from '@/components/base/StateView.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const blogId = route.params.id as string

const blog = ref<Blog | null>(null)
const loading = ref(true)
const error = ref('')
const liked = ref(false)
const likeCount = ref(0)
const likes = ref<FollowUser[]>([])
const comments = ref<BlogComment[]>([])
const commentCount = ref('')
const commentInput = ref('')
const commentError = ref('')

const images = computed(() => (blog.value?.images ? blog.value.images.split(',') : []))

async function loadBlog() {
  if (!blogId) {
    error.value = '笔记ID不存在'
    loading.value = false
    return
  }
  try {
    const res = await blogAPI.detail(blogId)
    blog.value = res.data
    if (!res.data) {
      error.value = '笔记不存在'
    } else {
      liked.value = !!res.data.isLike
      likeCount.value = res.data.liked || 0
    }
  } catch (err) {
    error.value = (err as Error).message
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!requireAuth()) return
  try {
    await blogAPI.like(blogId)
    if (liked.value) {
      liked.value = false
      likeCount.value -= 1
    } else {
      liked.value = true
      likeCount.value += 1
    }
  } catch (err) {
    toast.show((err as Error).message || '操作失败', 'error')
  }
}

async function loadLikes() {
  try {
    const res = await blogAPI.likes(blogId)
    likes.value = res.data || []
  } catch {
    likes.value = []
  }
}

async function loadComments() {
  commentError.value = ''
  try {
    const res = await commentAPI.list(blogId, 1)
    comments.value = res.data || []
    commentCount.value = res.total ? `（共 ${res.total} 条）` : ''
  } catch {
    commentError.value = '评论加载失败'
    comments.value = []
  }
}

async function submitComment() {
  if (!requireAuth()) return
  const content = commentInput.value.trim()
  if (!content) {
    toast.show('请输入评论内容', 'error')
    return
  }
  try {
    await commentAPI.add({ blogId: parseInt(blogId, 10), content })
    commentInput.value = ''
    toast.show('评论成功', 'success')
    loadComments()
  } catch (err) {
    toast.show((err as Error).message || '评论失败', 'error')
  }
}

async function deleteComment(id: number) {
  if (!window.confirm('确定删除这条评论吗？')) return
  try {
    await commentAPI.remove(id)
    toast.show('已删除', 'success')
    loadComments()
  } catch (err) {
    toast.show((err as Error).message || '删除失败', 'error')
  }
}

function isMine(c: BlogComment) {
  return auth.user && String(auth.user.id) === String(c.userId)
}

function goUser(id: number) {
  router.push(`/user/${id}`)
}

function goShop(id: number) {
  router.push(`/shop/${id}`)
}

onMounted(() => {
  loadBlog()
  loadLikes()
  loadComments()
})
</script>

<template>
  <div class="page-container">
    <div class="card">
      <div class="card-body">
        <StateView :loading="loading" :error="error">
          <template v-if="blog">
            <div class="blog-header" style="cursor:pointer;" @click="goUser(blog.userId)">
              <BaseImg class="blog-avatar" :src="blog.icon" alt="" :placeholder="PLACEHOLDER_AVATAR" />
              <div>
                <div class="blog-user">{{ blog.name || '匿名用户' }}</div>
                <div class="blog-time">{{ formatTime(blog.createTime) }}</div>
              </div>
            </div>

            <h1 style="font-size:24px;font-weight:800;margin:18px 0;letter-spacing:-0.4px;line-height:1.4;">{{ blog.title }}</h1>
            <div style="font-size:15px;color:var(--text-secondary);line-height:1.9;word-break:break-word;">{{ blog.content }}</div>

            <div v-if="images.length" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:8px;margin:16px 0;">
              <BaseImg
                v-for="(img, i) in images"
                :key="i"
                :src="img"
                alt=""
                hide-on-error
                style="width:100%;height:220px;object-fit:cover;border-radius:12px;"
              />
            </div>

            <div
              v-if="blog.shopId"
              style="margin-top:16px;padding:14px;background:var(--bg-subtle);border-radius:12px;display:flex;align-items:center;gap:8px;"
            >
              <span style="font-size:13px;color:var(--text-secondary);"><BaseIcon name="pin" /> 关联商家</span>
              <a href="#" style="color:var(--primary);font-size:13px;font-weight:600;" @click.prevent="goShop(blog.shopId)">
                查看商家 <BaseIcon name="arrow-right" />
              </a>
            </div>

            <div class="blog-actions" style="margin-top:20px;">
              <span class="blog-action" :class="{ liked }" style="font-size:15px;cursor:pointer;" @click="toggleLike">
                <BaseIcon name="heart" /> {{ likeCount }}
              </span>
            </div>
          </template>
        </StateView>
      </div>
    </div>

    <div class="card" style="margin-top:16px;">
      <div class="card-body">
        <div class="section-title" style="margin-bottom:12px;">
          <h2>点赞用户</h2>
        </div>
        <div style="display:flex;gap:16px;flex-wrap:wrap;">
          <span v-if="!likes.length" style="color:var(--text-light);font-size:14px;">暂无点赞</span>
          <div v-for="u in likes" :key="u.id" style="text-align:center;cursor:pointer;" @click="goUser(u.id)">
            <BaseImg
              :src="u.icon"
              alt=""
              :placeholder="PLACEHOLDER_AVATAR"
              style="width:44px;height:44px;border-radius:50%;object-fit:cover;border:2px solid var(--border);"
            />
            <div style="font-size:12px;color:var(--text-light);margin-top:4px;max-width:56px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              {{ u.nickName || u.name || '用户' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:16px;">
      <div class="card-body">
        <div class="section-title" style="margin-bottom:12px;">
          <h2>评论 <span style="font-size:13px;color:var(--text-light);font-weight:400;">{{ commentCount }}</span></h2>
        </div>
        <div style="display:flex;gap:10px;margin-bottom:20px;">
          <textarea v-model="commentInput" class="form-control" rows="2" placeholder="说说你的想法..." style="flex:1;resize:none;"></textarea>
          <button class="btn btn-primary" style="align-self:flex-end;" @click="submitComment">发表</button>
        </div>
        <div>
          <div v-if="commentError" class="empty-state"><p>{{ commentError }}</p></div>
          <div v-else-if="!comments.length" class="empty-state"><p>暂无评论，快来抢沙发~</p></div>
          <div v-else>
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <BaseImg class="comment-avatar" :src="c.icon" alt="" :placeholder="PLACEHOLDER_AVATAR" />
              <div class="comment-body">
                <div class="comment-head">
                  <span class="comment-name">{{ c.name || '匿名用户' }}</span>
                  <span class="comment-time">{{ formatTime(c.createTime) }}</span>
                </div>
                <div class="comment-text">{{ c.content }}</div>
                <a v-if="isMine(c)" class="comment-delete" @click.prevent="deleteComment(c.id)">删除</a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
