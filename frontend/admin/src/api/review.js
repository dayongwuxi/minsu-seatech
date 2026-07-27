import request from '@/utils/request'

// ReviewQuery：current/size/roomName/memberName/rating/auditStatus
// 返回 ReviewAdminVO：rating/content/auditStatus/replyContent/replyTime/roomName/memberName
export const listReviews = (params) => request.get('/reviews', { params })
// POST /reviews/{id}/reply，请求体 { replyContent }
export const replyReview = (id, replyContent) => request.post(`/reviews/${id}/reply`, { replyContent })
// PUT /reviews/{id}/audit，auditStatus: 1通过 2不通过
export const auditReview = (id, auditStatus) => request.put(`/reviews/${id}/audit`, { auditStatus })
export const deleteReview = (id) => request.delete(`/reviews/${id}`)
