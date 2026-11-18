import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/shop/:id', name: 'shop-detail', component: () => import('@/views/ShopDetailView.vue') },
    { path: '/voucher', name: 'voucher', component: () => import('@/views/VoucherView.vue') },
    { path: '/seckill/:id', name: 'seckill', component: () => import('@/views/SeckillView.vue') },
    { path: '/blog', name: 'blog', component: () => import('@/views/BlogView.vue') },
    { path: '/blog/:id', name: 'blog-detail', component: () => import('@/views/BlogDetailView.vue') },
    {
      path: '/follow',
      name: 'follow',
      component: () => import('@/views/FollowView.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/user/:id?', name: 'user', component: () => import('@/views/UserView.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { hideHeader: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
})

export default router
