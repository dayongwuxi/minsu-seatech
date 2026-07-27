import axios from 'axios'
import { ElMessage } from 'element-plus'
import i18n from '@/i18n'

const TOKEN_KEY = 'user_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

function goLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('user_member')
  const redirect = encodeURIComponent(window.location.hash ? window.location.hash.slice(1) : window.location.pathname)
  window.location.href = `/login?redirect=${redirect}`
}

request.interceptors.response.use(
  (response) => {
    const t = i18n.global.t
    const res = response.data
    // 非标准结构（如文件流）直接返回
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (res.code !== 0) {
      if (res.code === 401) {
        ElMessage.error(t('http.sessionExpired'))
        goLogin()
      } else {
        // 业务错误消息来自后端，保持原样展示
        ElMessage.error(res.message || t('http.requestFailed'))
      }
      return Promise.reject(new Error(res.message || t('http.requestFailed')))
    }
    return res.data
  },
  (error) => {
    const t = i18n.global.t
    if (error.response && error.response.status === 401) {
      ElMessage.error(t('http.sessionExpired'))
      goLogin()
    } else {
      ElMessage.error(error.response?.data?.message || error.message || t('http.networkError'))
    }
    return Promise.reject(error)
  }
)

export default request
