import request from '@/utils/request'

// 提交评价：{ bookingId, rating(0.5~5.0 半星), content }
export function createReview(data) {
  return request.post('/reviews', data)
}

// 我的评价分页（current/size，MyReviewVO）
export function getMyReviews(params) {
  return request.get('/user/reviews', { params })
}
