<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { Blog } from '@/types'
import BaseImg from '@/components/base/BaseImg.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import { formatTime } from '@/utils/format'
import { blogAPI } from '@/api/blog'
import { requireAuth } from '@/utils/auth'
import { useToastStore } from '@/stores/toast'
import { PLACEHOLDER_AVATAR } from '@/utils/placeholder'

const props = withDefaults(
  defineProps<{
    blog: Blog
    cardToDetail?: boolean
    headerToUser?: boolean
    titleToDetail?: boolean
    likeClickable?: boolean
    showImages?: boolean
    showComments?: boolean
    showExtraTime?: boolean
  }>(),
  {
    cardToDetail: false,
    headerToUser: false,
    titleToDetail: false,
    likeClickable: false,
    showImages: false,
    showComments: false,
    showExtraTime: false,
  },
)

const router = useRouter()
const toast = useToastStore()

const images = computed(() => (props.blog.images ? props.blog.images.split(',') : []))
const liked = ref(!!props.blog.isLike)
const likeCount = ref(props.blog.liked || 0)

watch(
  () => [props.blog.isLike, props.blog.liked],
  () => {
    liked.value = !!props.blog.isLike
    likeCount.value = props.blog.liked || 0
  },
)

function goDetail() {
  router.push(`/blog/${props.blog.id}`)
}
function goUser() {
  router.push(`/user/${props.blog.userId}`)
}
function onCardClick() {
  if (props.cardToDetail) goDetail()
}
function onHeaderClick() {
  if (props.headerToUser) goUser()
}
function onTitleClick() {
  if (props.titleToDetail) goDetail()
}

async function onLikeClick() {
  if (!props.likeClickable) return
  if (!requireAuth()) return
  try {
    await blogAPI.like(props.blog.id)
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
</script>

<template>
  <div class="blog-card" :style="cardToDetail ? 'cursor:pointer' : ''" @click="onCardClick">
    <div class="blog-header" :style="headerToUser ? 'cursor:pointer' : ''" @click="onHeaderClick">
      <BaseImg class="blog-avatar" :src="blog.icon" alt="" :placeholder="PLACEHOLDER_AVATAR" />
      <div>
        <div class="blog-user">{{ blog.name || '匿名用户' }}</div>
        <div class="blog-time">{{ formatTime(blog.createTime) }}</div>
      </div>
    </div>
    <div class="blog-title" :style="titleToDetail ? 'cursor:pointer' : ''" @click="onTitleClick">
      {{ blog.title }}
    </div>
    <div class="blog-content">{{ blog.content }}</div>
    <div v-if="showImages && images.length" class="blog-images">
      <BaseImg v-for="(img, i) in images.slice(0, 3)" :key="i" :src="img" alt="" hide-on-error />
    </div>
    <div class="blog-actions">
      <span
        aria-label="点赞"
        class="blog-action"
        :class="{ liked }"
        :style="likeClickable ? 'cursor:pointer' : ''"
        @click="onLikeClick"
      >
        <BaseIcon name="heart" /> {{ likeCount }}
      </span>
      <span
        v-if="showComments"
        aria-label="评论"
        class="blog-action"
        style="cursor:pointer"
        @click="goDetail"
      >
        <BaseIcon name="message" /> {{ blog.comments || 0 }}
      </span>
      <span v-if="showExtraTime" class="blog-action" style="margin-left:auto">
        {{ formatTime(blog.createTime) }}
      </span>
    </div>
  </div>
</template>
