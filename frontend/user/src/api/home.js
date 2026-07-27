import request from '@/utils/request'

// 首页轮播图（List<Banner>: id/title/imageUrl/linkUrl/sort）
export function getBanners() {
  return request.get('/banners')
}

// 推荐房间（List<Room>，后端固定最多 6 条）
export function getRecommendRooms() {
  return request.get('/rooms/recommend')
}

// 周边设施（List<NearbyFacility>: name/type/distance/address/imageUrl，可按 type 过滤）
export function getFacilities(type) {
  return request.get('/facilities', { params: type ? { type } : {} })
}
