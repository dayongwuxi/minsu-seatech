import request from '@/utils/request'

// 用户注册
export function register(data) {
  return request.post('/auth/register', data)
}

// 用户登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 获取图形验证码（base64）
export function getCaptcha() {
  return request.get('/captcha')
}
