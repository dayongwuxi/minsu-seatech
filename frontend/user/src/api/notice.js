import request from '@/utils/request'

// 公告分页列表
export function getNotices(params) {
  return request.get('/notices', { params })
}

// 公告详情
export function getNoticeDetail(id) {
  return request.get(`/notices/${id}`)
}
