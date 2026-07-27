import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api/admin',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非标准包装（如文件流）直接返回
    if (res === null || typeof res !== 'object' || res.code === undefined) {
      return res
    }
    // 后端约定 code=0 为成功
    if (res.code === 0) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('admin_token')
      router.push('/login')
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('admin_token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
