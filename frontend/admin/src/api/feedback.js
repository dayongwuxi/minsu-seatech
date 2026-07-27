import request from '@/utils/request'

// 查询参数：current/size/status/type/keyword
// 返回 FeedbackAdminVO：feedbackNo/type/title/content/status/replyContent/memberName/memberPhone
export const listFeedbacks = (params) => request.get('/feedbacks', { params })
// POST /feedbacks/{id}/reply，请求体 { replyContent }，后端回复后自动置为已处理
export const replyFeedback = (id, replyContent) => request.post(`/feedbacks/${id}/reply`, { replyContent })
// PUT /feedbacks/{id}/status，status: 0待处理 1处理中 2已处理
export const updateFeedbackStatus = (id, status) => request.put(`/feedbacks/${id}/status`, { status })
export const deleteFeedback = (id) => request.delete(`/feedbacks/${id}`)
