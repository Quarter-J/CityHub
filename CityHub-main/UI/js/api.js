const API_BASE = 'http://localhost:8081'
const TOKEN_KEY = 'cityhub_token'

const request = (config) => {
  const { url, method = 'GET', data = null, params = null } = config
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }

  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    options.headers['authorization'] = token
  }

  if (data) {
    options.body = JSON.stringify(data)
  }

  let queryString = ''
  if (params) {
    const parts = Object.entries(params)
      .filter(([_, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    if (parts.length) {
      queryString = '?' + parts.join('&')
    }
  }

  const fullUrl = `${API_BASE}${url}${queryString}`

  return fetch(fullUrl, options)
    .then((res) => res.json())
    .then((res) => {
      if (res.success) return res
      throw new Error(res.errorMsg || '请求失败')
    })
}

/* ========== 用户模块 ========== */
const userAPI = {
  sendCode: (phone) =>
    request({ url: '/user/code', method: 'POST', params: { phone } }),

  login: (loginForm) =>
    request({ url: '/user/login', method: 'POST', data: loginForm }),

  logout: () =>
    request({ url: '/user/logout', method: 'POST' }),

  me: () =>
    request({ url: '/user/me' }),

  info: (id) =>
    request({ url: `/user/info/${id}` }),

  userById: (id) =>
    request({ url: `/user/${id}` }),
}

/* ========== 商铺模块 ========== */
const shopAPI = {
  detail: (id) =>
    request({ url: `/shop/${id}` }),

  save: (shop) =>
    request({ url: '/shop', method: 'POST', data: shop }),

  update: (shop) =>
    request({ url: '/shop', method: 'PUT', data: shop }),

  byType: (typeId, current = 1, x, y) =>
    request({ url: '/shop/of/type', params: { typeId, current, x, y } }),

  byName: (name, current = 1) =>
    request({ url: '/shop/of/name', params: { name, current } }),
}

/* ========== 商铺分类模块 ========== */
const shopTypeAPI = {
  list: () =>
    request({ url: '/shop-type/list' }),
}

/* ========== 优惠券模块 ========== */
const voucherAPI = {
  add: (voucher) =>
    request({ url: '/voucher', method: 'POST', data: voucher }),

  addSeckill: (voucher) =>
    request({ url: '/voucher/seckill', method: 'POST', data: voucher }),

  listByShop: (shopId) =>
    request({ url: `/voucher/list/${shopId}` }),
}

/* ========== 秒杀模块 ========== */
const seckillAPI = {
  order: (voucherId) =>
    request({ url: `/voucher-order/seckill/${voucherId}`, method: 'POST' }),
}

/* ========== 博客模块 ========== */
const blogAPI = {
  save: (blog) =>
    request({ url: '/blog', method: 'POST', data: blog }),

  like: (id) =>
    request({ url: `/blog/like/${id}`, method: 'PUT' }),

  my: (current = 1) =>
    request({ url: '/blog/of/me', params: { current } }),

  hot: (current = 1) =>
    request({ url: '/blog/hot', params: { current } }),

  detail: (id) =>
    request({ url: `/blog/${id}` }),

  likes: (id) =>
    request({ url: `/blog/likes/${id}` }),

  byUser: (id, current = 1) =>
    request({ url: '/blog/of/user', params: { id, current } }),

  followFeed: (lastId, offset = 0) =>
    request({ url: '/blog/of/follow', params: { lastId, offset } }),
}

/* ========== 关注模块 ========== */
const followAPI = {
  follow: (id, isFollow) =>
    request({ url: `/follow/${id}/${isFollow}`, method: 'PUT' }),

  isFollow: (id) =>
    request({ url: `/follow/or/not/${id}` }),

  common: (id) =>
    request({ url: `/follow/common/${id}` }),
}

/* ========== 上传模块 ========== */
const uploadAPI = {
  blogImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = localStorage.getItem(TOKEN_KEY)
    return fetch(`${API_BASE}/upload/blog`, {
      method: 'POST',
      headers: token ? { authorization: token } : {},
      body: formData,
    }).then((r) => r.json())
  },

  deleteBlogImage: (name) =>
    request({ url: '/upload/blog/delete', params: { name } }),
}

/* ========== 工具函数 ========== */

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now - d
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function showToast(message, type = 'info') {
  let container = document.querySelector('.toast-container')
  if (!container) {
    container = document.createElement('div')
    container.className = 'toast-container'
    document.body.appendChild(container)
  }

  const icons = { success: '✓', error: '✕', info: 'ℹ' }
  const toast = document.createElement('div')
  toast.className = `toast toast-${type}`
  toast.innerHTML = `<span>${icons[type] || ''}</span><span>${message}</span>`
  container.appendChild(toast)

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transition = 'opacity 0.3s'
    setTimeout(() => toast.remove(), 300)
  }, 2500)
}

function getUser() {
  const raw = localStorage.getItem('cityhub_user')
  return raw ? JSON.parse(raw) : null
}

function setUser(user) {
  localStorage.setItem('cityhub_user', JSON.stringify(user))
}

function clearUser() {
  localStorage.removeItem('cityhub_user')
  localStorage.removeItem(TOKEN_KEY)
}

function isLoggedIn() {
  return !!localStorage.getItem(TOKEN_KEY)
}

function requireAuth() {
  if (!isLoggedIn()) {
    showToast('请先登录', 'error')
    setTimeout(() => { window.location.href = 'login.html' }, 800)
    return false
  }
  return true
}

function renderHeader() {
  const header = document.querySelector('.app-header')
  if (!header) return
  const user = getUser()
  const userHtml = user
    ? `<div class="header-actions">
         <a href="user.html">${user.nickName || user.icon || '我的'}</a>
         <a href="blog.html">笔记</a>
         <a href="javascript:void(0)" onclick="handleLogout()">退出</a>
       </div>`
    : `<div class="header-actions">
         <a href="blog.html">笔记</a>
         <a href="login.html" class="btn-login">登录</a>
       </div>`

  const searchHtml = window.location.pathname.includes('index') || window.location.pathname.endsWith('/UI/') || window.location.pathname.endsWith('/UI')
    ? `<div class="search-box">
         <input type="text" id="searchInput" placeholder="搜索商家..." onkeydown="if(event.key==='Enter') searchShop()">
         <span class="search-icon" onclick="searchShop()">🔍</span>
       </div>`
    : ''

  header.innerHTML = `
    <div class="header-inner">
      <a href="index.html" class="logo">City<span>Hub</span></a>
      ${searchHtml}
      ${userHtml}
    </div>
  `
}

function handleLogout() {
  userAPI.logout().finally(() => {
    clearUser()
    showToast('已退出登录', 'success')
    setTimeout(() => { window.location.href = 'index.html' }, 500)
  })
}

function searchShop() {
  const q = document.getElementById('searchInput')?.value?.trim()
  if (q) {
    window.location.href = `index.html?search=${encodeURIComponent(q)}`
  }
}

document.addEventListener('DOMContentLoaded', () => {
  renderHeader()
})
