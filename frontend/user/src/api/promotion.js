import request from '@/utils/request'

// 房间生效中的促销活动（免登录），用于详情页促销标签
export function getActivePromotions(roomId) {
  return request.get('/promotions/active', { params: { roomId } })
}
