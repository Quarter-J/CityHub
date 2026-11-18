<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { blogAPI } from '@/api/blog'
import { followAPI } from '@/api/follow'
import type { Blog, FollowUser } from '@/types'
import { requireAuth } from '@/utils/auth'
import { useAuthStore } from '@/stores/auth'
import BlogCard from '@/components/blog/BlogCard.vue'
import UserGrid from '@/components/user/UserGrid.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'

const route = useRoute()
const auth = useAuthStore()

type FollowTab = 'feed' | 'common' | 'following'
const currentTab = ref<FollowTab>('feed')

// 关注推送（滚动分页）
const feedBlogs = ref<Blog[]>([])
const feedLastId = ref<number | null>(null)
const feedOffset = ref(0)
const hasMoreFeed = ref(true)
const feedLoading = ref(false)
const feedError = ref('')

// 共同关注
const commonUsers = ref<FollowUser[]>([])
const commonLoading = ref(false)
const commonError = ref('')

// 我的关注
const followingUsers = ref<FollowUser[]>([])
const followingLoading = ref(false)
const followingError = ref('')

const commonUserId = computed(() => {
  const v = route.query.id
  return typeof v === 'string' ? v : null
})

function switchTab(tab: FollowTab) {
  currentTab.value = tab
  if (tab === 'feed') loadFeed(true)
  else if (tab === 'common') loadCommon()
  else loadFollowing()
}

async function loadFeed(reset = false) {
  if (!requireAuth()) return
  if (reset) {
    feedBlogs.value = []
    feedLastId.value = null
    feedOffset.value = 0
    hasMoreFeed.value = true
  }
  if (!hasMoreFeed.value) return

  feedLoading.value = true
  feedError.value = ''
  try {
    const lastId = feedLastId.value != null ? feedLastId.value : Date.now()
    const res = await blogAPI.followFeed(lastId, feedOffset.value)
    const data = res.data
    const blogs = data.list || []
    const minTime = data.minTime ?? null

    if (!blogs.length) {
      hasMoreFeed.value = false
      return
    }

    feedBlogs.value = feedBlogs.value.concat(blogs)

    if (minTime != null) {
      if (feedLastId.value != null && minTime === feedLastId.value) {
        feedOffset.value = feedOffset.value + (data.offset || 1)
      } else {
        feedOffset.value = data.offset || 1
      }
      feedLastId.value = minTime
    } else {
      hasMoreFeed.value = false
    }
  } catch (err) {
    feedError.value = (err as Error).message || '加载失败'
  } finally {
    feedLoading.value = false
  }
}

async function loadCommon() {
  commonError.value = ''
  const id = commonUserId.value || (auth.user ? String(auth.user.id) : null)
  if (!id) {
    commonError.value = '请指定要查看的用户'
    return
  }

  commonLoading.value = true
  try {
    const res = await followAPI.common(id)
    commonUsers.value = res.data || []
  } catch (err) {
    commonError.value = (err as Error).message || '加载失败'
    commonUsers.value = []
  } finally {
    commonLoading.value = false
  }
}

async function loadFollowing() {
  followingError.value = ''
  followingLoading.value = true
  try {
    const res = await followAPI.ofMe()
    followingUsers.value = res.data || []
  } catch (err) {
    followingError.value = (err as Error).message || '加载失败'
    followingUsers.value = []
  } finally {
    followingLoading.value = false
  }
}

onMounted(() => loadFeed(true))
</script>

<template>
  <div class="page-container">
    <div class="tabs">
      <span class="tab-item" :class="{ active: currentTab === 'feed' }" @click="switchTab('feed')">关注推送</span>
      <span class="tab-item" :class="{ active: currentTab === 'common' }" @click="switchTab('common')">共同关注</span>
      <span class="tab-item" :class="{ active: currentTab === 'following' }" @click="switchTab('following')">我的关注</span>
    </div>

    <template v-if="currentTab === 'feed'">
      <div v-if="feedError" class="empty-state"><p>{{ feedError }}</p></div>
      <div v-else-if="feedLoading && !feedBlogs.length" class="loading">加载中</div>
      <div v-else-if="!feedBlogs.length" class="empty-state">
        <div class="empty-icon"><BaseIcon name="file" /></div>
        <p>暂无关注推送<br><small>关注更多用户获取推送</small></p>
      </div>
      <div v-else>
        <div class="blog-list">
          <BlogCard v-for="blog in feedBlogs" :key="blog.id" :blog="blog" header-to-user title-to-detail show-images />
        </div>
        <div v-if="hasMoreFeed" style="text-align:center;padding:16px;">
          <button class="btn btn-outline" :disabled="feedLoading" @click="loadFeed(false)">加载更多</button>
        </div>
        <div v-else style="text-align:center;padding:16px;color:var(--text-light);font-size:13px;">没有更多推送了</div>
      </div>
    </template>

    <template v-else-if="currentTab === 'common'">
      <div v-if="commonError" class="empty-state"><p>{{ commonError }}</p></div>
      <div v-else-if="commonLoading && !commonUsers.length" class="loading">加载中</div>
      <div v-else-if="!commonUsers.length" class="empty-state">
        <div class="empty-icon"><BaseIcon name="users" /></div>
        <p>暂无共同关注</p>
      </div>
      <UserGrid v-else :users="commonUsers" />
    </template>

    <template v-else>
      <div v-if="followingError" class="empty-state"><p>{{ followingError }}</p></div>
      <div v-else-if="followingLoading && !followingUsers.length" class="loading">加载中</div>
      <div v-else-if="!followingUsers.length" class="empty-state">
        <div class="empty-icon"><BaseIcon name="users" /></div>
        <p>还没有关注任何人</p>
      </div>
      <UserGrid v-else :users="followingUsers" />
    </template>
  </div>
</template>
