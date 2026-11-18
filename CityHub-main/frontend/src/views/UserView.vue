<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { userAPI } from '@/api/user'
import { blogAPI } from '@/api/blog'
import { followAPI } from '@/api/follow'
import { uploadAPI } from '@/api/upload'
import type { UserDTO, UserInfo, Blog, FollowUser } from '@/types'
import { requireAuth } from '@/utils/auth'
import { uploadsUrl } from '@/utils/url'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { PLACEHOLDER_AVATAR } from '@/utils/placeholder'
import BaseImg from '@/components/base/BaseImg.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import BaseModal from '@/components/base/BaseModal.vue'
import StateView from '@/components/base/StateView.vue'
import BlogCard from '@/components/blog/BlogCard.vue'
import UserGrid from '@/components/user/UserGrid.vue'

const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()

const targetUserId = computed<string | null>(() => {
  const p = route.params.id
  const v = Array.isArray(p) ? p[0] : p
  return v || null
})
const isMyPage = computed(
  () => !targetUserId.value || (auth.user && String(auth.user.id) === targetUserId.value),
)
const viewingUserId = computed<string | null>(
  () => targetUserId.value || (auth.user ? String(auth.user.id) : null),
)
const needLogin = computed(() => !viewingUserId.value)

type UserTab = 'blogs' | 'liked' | 'follow' | 'fans'
const currentTab = ref<UserTab>('blogs')

const tabs = computed(() => {
  const all: Array<{ key: UserTab; label: string; myOnly: boolean }> = [
    { key: 'blogs', label: '笔记', myOnly: false },
    { key: 'liked', label: '点赞', myOnly: true },
    { key: 'follow', label: '关注', myOnly: false },
    { key: 'fans', label: '粉丝', myOnly: false },
  ]
  return isMyPage.value ? all : all.filter((t) => !t.myOnly)
})

// 个人资料
const profileUser = ref<UserDTO | null>(null)
const profileInfo = ref<UserInfo | null>(null)
const profileLoading = ref(true)
const profileError = ref('')
const followed = ref(false)
const signed = ref(false)
const signInfo = ref('')
const blogCountStat = ref('0')

// 页签内容
const tabLoading = ref(false)
const tabError = ref('')
const tabBlogs = ref<Blog[]>([])
const likedBlogs = ref<Blog[]>([])
const followUsers = ref<FollowUser[]>([])
const fansUsers = ref<FollowUser[]>([])

// 编辑资料
const editShow = ref(false)
const editNickName = ref('')
const editCity = ref('')
const editIntroduce = ref('')
const avatarPreviewSrc = ref('')
const editIconFile = ref<File | null>(null)
const editIconInput = ref<HTMLInputElement | null>(null)

async function loadProfile() {
  const uid = viewingUserId.value
  profileLoading.value = true
  profileError.value = ''
  followed.value = false
  signed.value = false
  signInfo.value = ''
  blogCountStat.value = '0'

  if (!uid) {
    profileLoading.value = false
    return
  }

  try {
    const [userRes, infoRes] = await Promise.all([userAPI.userById(uid), userAPI.info(uid)])
    profileUser.value = userRes.data
    profileInfo.value = infoRes.data
  } catch {
    profileError.value = '用户信息加载失败'
  } finally {
    profileLoading.value = false
  }

  if (isMyPage.value) {
    loadSignInfo()
    loadSignStatus()
  } else if (auth.user) {
    try {
      const r = await followAPI.isFollow(uid)
      followed.value = !!r.data
    } catch {
      // 忽略关注状态查询失败
    }
  }
}

async function loadSignInfo() {
  try {
    const res = await userAPI.signCount()
    signInfo.value = `已连续签到 ${res.data || 0} 天`
  } catch {
    // 忽略
  }
}

async function loadSignStatus() {
  try {
    const res = await userAPI.signStatus()
    if (res.data) signed.value = true
  } catch {
    // 忽略
  }
}

async function doSign() {
  if (!requireAuth()) return
  try {
    await userAPI.sign()
    toast.show('签到成功！', 'success')
    loadSignInfo()
    signed.value = true
  } catch (err) {
    toast.show((err as Error).message || '签到失败', 'error')
  }
}

function openEdit() {
  const user = auth.user
  editNickName.value = user?.nickName || ''
  editCity.value = ''
  editIntroduce.value = ''
  editIconFile.value = null
  avatarPreviewSrc.value = user?.icon || PLACEHOLDER_AVATAR
  const uid = viewingUserId.value
  if (uid) {
    userAPI
      .info(uid)
      .then((res) => {
        const info = res.data || {}
        editCity.value = info.city || ''
        editIntroduce.value = info.introduce || ''
      })
      .catch(() => {})
  }
  editShow.value = true
}

function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  editIconFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    avatarPreviewSrc.value = (ev.target?.result as string) || ''
  }
  reader.readAsDataURL(file)
}

async function saveEdit() {
  const nickName = editNickName.value.trim()
  const city = editCity.value.trim()
  const introduce = editIntroduce.value.trim()
  const file = editIconFile.value

  try {
    let icon = ''
    if (file) {
      const r = await uploadAPI.avatar(file)
      icon = r.data ? uploadsUrl(r.data) : ''
    }

    const userPromise: Promise<unknown> = nickName || icon
      ? userAPI.update({ nickName, icon })
      : Promise.resolve()
    const infoPromise = userAPI.updateInfo({ city, introduce })
    await Promise.all([userPromise, infoPromise])

    toast.show('保存成功', 'success')
    editShow.value = false

    const u = auth.user
    if (u) {
      const updated = { ...u }
      if (nickName) updated.nickName = nickName
      if (icon) updated.icon = icon
      auth.setUser(updated)
    }
    loadProfile()
  } catch (err) {
    toast.show((err as Error).message || '保存失败', 'error')
  }
}

async function toggleFollow() {
  if (!requireAuth()) return
  const uid = viewingUserId.value
  if (!uid) return
  const isFollowed = followed.value
  try {
    await followAPI.follow(uid, !isFollowed)
    if (isFollowed) {
      followed.value = false
      toast.show('已取消关注', 'info')
    } else {
      followed.value = true
      toast.show('关注成功', 'success')
    }
    loadProfile()
  } catch (err) {
    toast.show((err as Error).message || '操作失败', 'error')
  }
}

function switchTab(tab: UserTab) {
  currentTab.value = tab
  loadTabContent()
}

async function loadTabContent() {
  const uid = viewingUserId.value
  tabLoading.value = true
  tabError.value = ''

  if (!uid) {
    tabError.value = '请先登录'
    tabLoading.value = false
    return
  }

  if (currentTab.value === 'liked' && !isMyPage.value) {
    tabError.value = '对方点赞的笔记仅自己可见'
    tabLoading.value = false
    return
  }

  try {
    if (currentTab.value === 'blogs') {
      const res = await blogAPI.byUser(uid, 1)
      const blogs = res.data || []
      blogCountStat.value = String(res.total ?? blogs.length)
      tabBlogs.value = blogs
      likedBlogs.value = []
      followUsers.value = []
      fansUsers.value = []
    } else if (currentTab.value === 'liked') {
      const res = await blogAPI.ofLiked(1)
      likedBlogs.value = res.data || []
      tabBlogs.value = []
      followUsers.value = []
      fansUsers.value = []
    } else if (currentTab.value === 'follow') {
      const res = isMyPage.value ? await followAPI.ofMe() : await followAPI.fans(uid)
      followUsers.value = res.data || []
      tabBlogs.value = []
      likedBlogs.value = []
      fansUsers.value = []
    } else {
      const res = await followAPI.fans(uid)
      fansUsers.value = res.data || []
      tabBlogs.value = []
      likedBlogs.value = []
      followUsers.value = []
    }
  } catch (err) {
    tabError.value = (err as Error).message || '加载失败'
  } finally {
    tabLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    currentTab.value = 'blogs'
    loadProfile()
    loadTabContent()
  },
)

onMounted(() => {
  loadProfile()
  loadTabContent()
})
</script>

<template>
  <div class="page-container">
    <div class="card">
      <div class="user-profile">
        <StateView :loading="profileLoading" :error="profileError">
          <div v-if="needLogin" class="empty-state">
            <div class="empty-icon"><BaseIcon name="user" /></div>
            <p>请先登录</p>
            <router-link to="/login" class="btn btn-primary" style="margin-top:12px;">去登录</router-link>
          </div>
          <template v-else>
            <BaseImg class="user-avatar" :src="profileUser?.icon" alt="" :placeholder="PLACEHOLDER_AVATAR" />
            <div class="user-name">{{ profileUser ? profileUser.nickName || '用户' : '未知用户' }}</div>
            <div class="user-desc">{{ profileInfo?.introduce || '这个人很懒，什么都没留下~' }}</div>
            <div class="user-stats">
              <div class="stat-item" @click="switchTab('follow')">
                <div class="stat-num">{{ profileInfo?.followee || 0 }}</div>
                <div class="stat-label">关注</div>
              </div>
              <div class="stat-item" @click="switchTab('fans')">
                <div class="stat-num">{{ profileInfo?.fans || 0 }}</div>
                <div class="stat-label">粉丝</div>
              </div>
              <div class="stat-item" @click="switchTab('blogs')">
                <div class="stat-num">{{ blogCountStat }}</div>
                <div class="stat-label">笔记</div>
              </div>
            </div>
            <div v-if="isMyPage" style="margin-top:10px;font-size:13px;color:var(--text-light);">{{ signInfo }}</div>
            <div style="margin-top:16px;">
              <template v-if="isMyPage">
                <button class="btn btn-outline" @click="openEdit"><BaseIcon name="edit" /> 编辑资料</button>
                <button
                  class="btn"
                  :class="signed ? 'btn-outline' : 'btn-primary'"
                  style="margin-left:8px;"
                  :disabled="signed"
                  @click="doSign"
                >
                  <BaseIcon :name="signed ? 'check' : 'calendar'" /> {{ signed ? '已签到' : '签到' }}
                </button>
              </template>
              <template v-else-if="auth.isLoggedIn">
                <button class="btn" :class="followed ? 'btn-primary' : 'btn-outline'" @click="toggleFollow">
                  {{ followed ? '已关注' : '关注' }}
                </button>
                <router-link :to="{ path: '/follow', query: { id: viewingUserId } }" class="btn btn-outline" style="margin-left:8px;">
                  共同关注
                </router-link>
              </template>
            </div>
          </template>
        </StateView>
      </div>
    </div>

    <template v-if="!needLogin">
      <div class="tabs" style="margin-top:20px;">
        <span
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ active: currentTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </span>
      </div>

      <StateView :loading="tabLoading" :error="tabError">
        <template v-if="currentTab === 'blogs'">
          <div v-if="tabBlogs.length" class="blog-list">
            <BlogCard v-for="b in tabBlogs" :key="b.id" :blog="b" card-to-detail show-comments show-extra-time />
          </div>
          <div v-else class="empty-state"><div class="empty-icon"><BaseIcon name="file" /></div><p>暂无笔记</p></div>
        </template>
        <template v-else-if="currentTab === 'liked'">
          <div v-if="likedBlogs.length" class="blog-list">
            <BlogCard v-for="b in likedBlogs" :key="b.id" :blog="b" card-to-detail show-comments show-extra-time />
          </div>
          <div v-else class="empty-state"><div class="empty-icon"><BaseIcon name="heart" /></div><p>还没有点过赞的笔记</p></div>
        </template>
        <template v-else-if="currentTab === 'follow'">
          <UserGrid v-if="followUsers.length" :users="followUsers" />
          <div v-else class="empty-state">
            <div class="empty-icon"><BaseIcon name="users" /></div>
            <p>{{ isMyPage ? '还没有关注任何人' : 'TA 还没有关注任何人' }}</p>
          </div>
        </template>
        <template v-else>
          <UserGrid v-if="fansUsers.length" :users="fansUsers" />
          <div v-else class="empty-state">
            <div class="empty-icon"><BaseIcon name="users" /></div>
            <p>还没有粉丝</p>
          </div>
        </template>
      </StateView>
    </template>

    <BaseModal v-model:show="editShow" title="编辑资料">
      <div class="form-group">
        <label>昵称</label>
        <input v-model="editNickName" type="text" class="form-control" placeholder="输入昵称">
      </div>
      <div class="form-group">
        <label>头像</label>
        <div style="display:flex;align-items:center;gap:16px;">
          <BaseImg :src="avatarPreviewSrc" alt="" :placeholder="PLACEHOLDER_AVATAR" style="width:64px;height:64px;border-radius:50%;object-fit:cover;" />
          <input ref="editIconInput" type="file" class="form-control" accept="image/*" @change="onAvatarChange">
        </div>
      </div>
      <div class="form-group">
        <label>城市</label>
        <input v-model="editCity" type="text" class="form-control" placeholder="所在城市">
      </div>
      <div class="form-group">
        <label>个人简介</label>
        <textarea v-model="editIntroduce" class="form-control" rows="3" placeholder="介绍一下自己（不超过128字）"></textarea>
      </div>
      <template #actions>
        <button class="btn btn-outline" @click="editShow = false">取消</button>
        <button class="btn btn-primary" @click="saveEdit">保存</button>
      </template>
    </BaseModal>
  </div>
</template>
