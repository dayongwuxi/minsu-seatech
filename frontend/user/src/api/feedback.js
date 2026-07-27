import request from '@/utils/request'

// 提交投诉反馈：{ type: 1住宿服务 2设施问题 3订单问题 4环境问题 5其他, title, content, images? }
export function createFeedback(data) {
  return request.post('/feedbacks', data)
}

// 我的投诉反馈分页（current/size，Feedback 实体）
export function getMyFeedbacks(params) {
  return request.get('/feedbacks', { params })
}
