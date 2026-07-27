import { defineStore } from 'pinia'
import { login as apiLogin } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('user_token') || '',
    member: JSON.parse(localStorage.getItem('user_member') || 'null')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    // 空串兜底：展示处用 nickname || t('common.user')
    nickname: (state) => state.member?.name || state.member?.username || ''
  },
  actions: {
    async login(payload) {
      // LoginVO: { token, member }
      const data = await apiLogin(payload)
      this.token = data.token
      this.member = data.member || null
      localStorage.setItem('user_token', this.token)
      localStorage.setItem('user_member', JSON.stringify(this.member))
      return data
    },
    setMember(member) {
      this.member = member
      localStorage.setItem('user_member', JSON.stringify(member))
    },
    logout() {
      this.token = ''
      this.member = null
      localStorage.removeItem('user_token')
      localStorage.removeItem('user_member')
    }
  }
})
