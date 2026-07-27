import request from '@/utils/request'

// 在线客服（REST 轮询）
// 会话分页：current/size/status/keyword，返回 CsSessionVO：memberName/memberAvatar/lastMessage/lastTime/unreadAdmin/status
export const listSessions = (params) => request.get('/cs/sessions', { params })
export const getSession = (id) => request.get(`/cs/sessions/${id}`)
// 拉取消息（后端同时清零客服未读），可传 afterId 增量拉取；CsMessage.senderType: 1会员 2客服
export const listMessages = (sessionId, params) => request.get(`/cs/sessions/${sessionId}/messages`, { params })
// 发送消息：{ content, contentType(1文本 默认) }
export const sendMessage = (sessionId, data) => request.post(`/cs/sessions/${sessionId}/messages`, data)
export const closeSession = (sessionId) => request.put(`/cs/sessions/${sessionId}/close`)
// 快捷回复（分页），CsQuickReply：{ question, answer, sort }
export const listQuickReplies = (params) => request.get('/cs/quick-replies', { params })
export const createQuickReply = (data) => request.post('/cs/quick-replies', data)
export const updateQuickReply = (id, data) => request.put(`/cs/quick-replies/${id}`, data)
export const deleteQuickReply = (id) => request.delete(`/cs/quick-replies/${id}`)
