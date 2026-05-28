import axios from 'axios'
import { useAuthStore } from '@/store/auth'
import router from '@/router'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  // MyBatis-Plus pagination is 1-based; legacy callers send 0-based page.
  if (config.params && config.params.page !== undefined) {
    const p = Number(config.params.page)
    if (!Number.isNaN(p)) {
      config.params = { ...config.params, page: p + 1 }
    }
  }
  return config
})

// Normalize MyBatis-Plus IPage payload to legacy Spring Page shape so
// existing views keep working with `content/totalElements/number/size`.
function normalizePage(data) {
  if (data && typeof data === 'object' && Array.isArray(data.records) && 'total' in data) {
    const current = typeof data.current === 'number' ? data.current : 1
    return {
      ...data,
      content: data.records,
      totalElements: data.total,
      totalPages: data.pages,
      number: Math.max(0, current - 1),
      size: data.size,
      first: current <= 1,
      last: current >= (data.pages ?? 1)
    }
  }
  return data
}

api.interceptors.response.use(
  response => {
    response.data = normalizePage(response.data)
    return response
  },
  error => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default api
