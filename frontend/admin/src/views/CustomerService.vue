<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { listSessions, listMessages, sendMessage, listQuickReplies } from '@/api/cs'
import { CS_SESSION_STATUS, dictLabel, dictType } from '@/utils/dict'

const sessions = ref([])
const quickReplies = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const msgListRef = ref()
let pollTimer = null

async function loadSessions() {
  try {
    // 分页返回 { records }，CsSessionVO：memberName/memberAvatar/lastMessage/lastTime/unreadAdmin/status
    const data = await listSessions({ current: 1, size: 50 })
    sessions.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadMessages() {
  if (!currentSession.value) return
  try {
    // 拉取消息（后端同时清零客服未读）；CsMessage.senderType: 1会员 2客服
    const data = await listMessages(currentSession.value.id)
    messages.value = Array.isArray(data) ? data : []
    await nextTick()
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  } catch (e) { /* 拦截器已提示 */ }
}

async function selectSession(session) {
  currentSession.value = session
  session.unreadAdmin = 0
  await loadMessages()
}

async function send() {
  const content = inputText.value.trim()
  if (!content || !currentSession.value) return
  sending.value = true
  try {
    await sendMessage(currentSession.value.id, { content, contentType: 1 })
    inputText.value = ''
    await loadMessages()
  } catch (e) { /* 拦截器已提示 */ } finally {
    sending.value = false
  }
}

// CsQuickReply：{ question, answer }
function useQuickReply(item) {
  inputText.value = item.answer || ''
}

onMounted(async () => {
  loadSessions()
  try {
    const data = await listQuickReplies({ current: 1, size: 50 })
    quickReplies.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ }
  // REST 轮询：每 5 秒刷新会话与当前消息
  pollTimer = setInterval(() => {
    loadSessions()
    loadMessages()
  }, 5000)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
})
</script>

<template>
  <div class="page-container cs-page">
    <el-card shadow="never" class="cs-card">
      <div class="cs-layout">
        <!-- 左栏：会话列表 -->
        <div class="cs-sessions">
          <div class="cs-col-title">会话列表</div>
          <el-scrollbar>
            <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
            <div
              v-for="s in sessions"
              :key="s.id"
              class="session-item"
              :class="{ active: currentSession && currentSession.id === s.id }"
              @click="selectSession(s)"
            >
              <el-badge :value="s.unreadAdmin || 0" :hidden="!s.unreadAdmin">
                <el-avatar :size="36" :src="s.memberAvatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
              </el-badge>
              <div class="session-info">
                <div class="session-name">{{ s.memberName }}</div>
                <div class="session-last">{{ s.lastMessage }}</div>
              </div>
              <div class="session-side">
                <div class="session-time">{{ s.lastTime }}</div>
                <el-tag size="small" :type="dictType(CS_SESSION_STATUS, s.status)">{{ dictLabel(CS_SESSION_STATUS, s.status) }}</el-tag>
              </div>
            </div>
          </el-scrollbar>
        </div>

        <!-- 中栏：聊天窗 -->
        <div class="cs-chat">
          <div class="cs-col-title">{{ currentSession ? `与 ${currentSession.memberName} 的对话` : '聊天窗口' }}</div>
          <div ref="msgListRef" class="msg-list">
            <el-empty v-if="!currentSession" description="请从左侧选择会话" :image-size="80" />
            <template v-else>
              <div v-for="m in messages" :key="m.id" class="msg-item" :class="m.senderType === 2 ? 'from-admin' : 'from-member'">
                <div class="msg-bubble">{{ m.content }}</div>
                <div class="msg-time">{{ m.createTime }}</div>
              </div>
            </template>
          </div>
          <div class="msg-input">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="请输入回复内容，Enter 发送"
              :disabled="!currentSession || currentSession.status === 2"
              @keydown.enter.exact.prevent="send"
            />
            <div class="msg-input-bar">
              <el-button type="primary" :loading="sending" :disabled="!currentSession || currentSession.status === 2" @click="send">发 送</el-button>
            </div>
          </div>
        </div>

        <!-- 右栏：快捷回复 + 用户信息 -->
        <div class="cs-side">
          <div class="cs-col-title">快捷回复</div>
          <el-scrollbar class="quick-list">
            <el-empty v-if="!quickReplies.length" description="暂无快捷回复" :image-size="50" />
            <div v-for="q in quickReplies" :key="q.id" class="quick-reply-item" @click="useQuickReply(q)">
              <div class="quick-q">{{ q.question }}</div>
              <div class="quick-a">{{ q.answer }}</div>
            </div>
          </el-scrollbar>
          <div class="cs-col-title" style="border-top: 1px solid #f0f2f7">用户信息</div>
          <div class="user-info">
            <el-empty v-if="!currentSession" description="未选择会话" :image-size="50" />
            <el-descriptions v-else :column="1" size="small">
              <el-descriptions-item label="会员昵称">{{ currentSession.memberName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源渠道">{{ currentSession.sourceChannel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="会话状态">{{ dictLabel(CS_SESSION_STATUS, currentSession.status) }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ currentSession.startTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.cs-card :deep(.el-card__body) {
  padding: 0;
}
.cs-layout {
  display: flex;
  height: calc(100vh - 130px);
  min-height: 480px;
}
.cs-col-title {
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid #f0f2f7;
  flex-shrink: 0;
}
.cs-sessions {
  width: 280px;
  border-right: 1px solid #f0f2f7;
  display: flex;
  flex-direction: column;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f7f8fb;
}
.session-item:hover,
.session-item.active {
  background: #eef3ff;
}
.session-info {
  flex: 1;
  overflow: hidden;
}
.session-name {
  font-size: 14px;
  font-weight: 600;
}
.session-last {
  font-size: 12px;
  color: #8a94a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.session-time {
  font-size: 11px;
  color: #b0b8c8;
}
.cs-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #f0f2f7;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fb;
}
.msg-item {
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
}
.msg-item.from-admin {
  align-items: flex-end;
}
.msg-item.from-member {
  align-items: flex-start;
}
.msg-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  background: #fff;
}
.from-admin .msg-bubble {
  background: #2f6bff;
  color: #fff;
}
.msg-time {
  font-size: 11px;
  color: #b0b8c8;
  margin-top: 4px;
}
.msg-input {
  border-top: 1px solid #f0f2f7;
  padding: 10px 12px;
}
.msg-input-bar {
  margin-top: 8px;
  text-align: right;
}
.cs-side {
  width: 280px;
  display: flex;
  flex-direction: column;
}
.quick-list {
  height: 45%;
  flex-shrink: 0;
}
.quick-reply-item {
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f7f8fb;
}
.quick-reply-item:hover {
  background: #eef3ff;
}
.quick-q {
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
}
.quick-a {
  font-size: 12px;
  color: #8a94a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-info {
  padding: 12px 16px;
}
</style>
