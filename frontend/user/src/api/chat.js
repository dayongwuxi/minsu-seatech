import request from '@/utils/request'

// 获取或创建我的客服会话，返回 CsSession
export function getChatSession() {
  return request.get('/chat/session')
}

// 增量拉取消息：sessionId 必填，afterId 之后的新消息（List<CsMessage>）
export function getChatMessages(sessionId, afterId) {
  return request.get('/chat/messages', { params: { sessionId, afterId } })
}

// 发送消息：{ sessionId, content, contentType? }，返回 CsMessage
export function sendChatMessage(data) {
  return request.post('/chat/messages', data)
}
