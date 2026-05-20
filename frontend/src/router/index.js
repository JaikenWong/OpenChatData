import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'tenants', name: 'Tenants', component: () => import('@/views/Tenants.vue') },
      { path: 'tables', name: 'Tables', component: () => import('@/views/Tables.vue') },
      { path: 'dictionary', name: 'Dictionary', component: () => import('@/views/Dictionary.vue') },
      { path: 'ask', name: 'Ask', component: () => import('@/views/Ask.vue') },
      { path: 'history', name: 'History', component: () => import('@/views/History.vue') },
      { path: 'audit', name: 'Audit', component: () => import('@/views/Audit.vue') },
      { path: 'rbac', name: 'RBAC', component: () => import('@/views/RBAC.vue') },
      { path: 'llm', name: 'LLMConfig', component: () => import('@/views/LLMConfig.vue') },
      { path: 'monitor', name: 'Monitor', component: () => import('@/views/Monitor.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.path !== '/login' && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
