import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface User {
  id: string
  phone: string
  nickname: string
  avatarUrl: string
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function persistTokens(access: string, refresh?: string) {
    accessToken.value = access
    localStorage.setItem('access_token', access)
    if (refresh) {
      refreshToken.value = refresh
      localStorage.setItem('refresh_token', refresh)
    }
  }

  function clearTokens() {
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }

  async function login(phone: string, code: string) {
    const res = await api.post('/auth/verify', { phone, code })
    const { access_token, refresh_token } = res.data
    persistTokens(access_token, refresh_token)
    await fetchProfile()
  }

  function logout() {
    clearTokens()
    user.value = null
  }

  async function fetchProfile() {
    try {
      const res = await api.get('/users/me')
      user.value = res.data
    } catch {
      // token invalid — clear silently
      clearTokens()
    }
  }

  // Hydrate profile on init if token exists
  if (accessToken.value) {
    fetchProfile()
  }

  return {
    accessToken,
    refreshToken,
    user,
    isAuthenticated,
    login,
    logout,
    fetchProfile,
  }
})
