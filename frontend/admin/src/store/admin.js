import { defineStore } from 'pinia'
import { login as loginApi, getProfile } from '@/api/auth'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    profile: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.profile?.name || state.profile?.username || 'admin'
  },
  actions: {
    // AdminLoginVO { token, admin }
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      localStorage.setItem('admin_token', data.token)
      this.profile = data.admin || null
      return data
    },
    async fetchProfile() {
      try {
        this.profile = await getProfile()
      } catch (e) {
        /* 拦截器已提示 */
      }
      return this.profile
    },
    // 后端无登出接口，客户端清除凭证即可
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('admin_token')
    }
  }
})
