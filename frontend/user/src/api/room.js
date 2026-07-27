import request from '@/utils/request'

// 房型分类
export function getRoomTypes() {
  return request.get('/room-types')
}

// 房间分页列表（参数：current/size/typeId/checkin/checkout/minPrice/maxPrice/capacity）
export function getRooms(params) {
  return request.get('/rooms', { params })
}

// 房间详情（RoomDetailVO: { room, typeName, images, avgRating, reviewCount }）
export function getRoomDetail(id) {
  return request.get(`/rooms/${id}`)
}

// 房间评价分页（current/size，RoomReviewVO）
export function getRoomReviews(id, params) {
  return request.get(`/rooms/${id}/reviews`, { params })
}

// 收藏 / 取消收藏
export function addFavorite(roomId) {
  return request.post(`/favorites/${roomId}`)
}

export function removeFavorite(roomId) {
  return request.delete(`/favorites/${roomId}`)
}
