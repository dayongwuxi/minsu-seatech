import request from '@/utils/request'

// 个人资料（MemberVO，phone/email 已由后端脱敏）
export function getProfile() {
  return request.get('/user/profile')
}

// 修改资料：{ name, email, avatar }（后端仅更新这三项）
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

// 修改密码：{ oldPassword, newPassword }
export function updatePassword(data) {
  return request.put('/user/password', data)
}

// 我的入住记录分页（current/size，CheckinUserVO）
export function getMyCheckins(params) {
  return request.get('/user/checkins', { params })
}

// 我的收藏分页（current/size，FavoriteVO）
export function getMyFavorites(params) {
  return request.get('/user/favorites', { params })
}
