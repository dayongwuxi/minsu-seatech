import request from '@/utils/request'

// 查询参数：current/size/title/publishStatus；实体 Notice：noticeNo/title/summary/coverImage/content/publishStatus/publishTime/creatorName
export const listNotices = (params) => request.get('/notices', { params })
export const getNotice = (id) => request.get(`/notices/${id}`)
export const createNotice = (data) => request.post('/notices', data)
export const updateNotice = (id, data) => request.put(`/notices/${id}`, data)
export const deleteNotice = (id) => request.delete(`/notices/${id}`)
// 发布：publishStatus 置 1；下架：置 0
export const publishNotice = (id) => request.put(`/notices/${id}/publish`)
export const offlineNotice = (id) => request.put(`/notices/${id}/offline`)
