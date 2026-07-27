import request from '@/utils/request'

// POST /api/admin/auth/login，请求体 { username, password, captchaKey, captchaCode }
// 返回 AdminLoginVO { token, admin }
export const login = (data) => request.post('/auth/login', data)

// GET /api/admin/captcha，返回 CaptchaVO { key, imageBase64 }
export const getCaptcha = () => request.get('/captcha')

// 个人中心（AdminProfileController），返回/接收 AdminVO 字段
export const getProfile = () => request.get('/profile')
export const updateProfile = (data) => request.put('/profile', data)
export const updatePassword = (data) => request.put('/profile/password', data)
