<template>
  <div class="page-container">
    <div class="card-block chat-wrap">
      <h2 class="section-title">{{ t('chat.title') }}</h2>
      <div class="chat-window" ref="chatWindowRef" v-loading="loading">
        <el-empty v-if="!loading && !messages.length" :description="t('chat.empty')" :image-size="80" />
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="chat-msg"
          :class="{ mine: isMine(msg) }"
        >
          <div class="chat-avatar">
            <el-avatar :size="36">{{ isMine(msg) ? t('chat.me') : t('chat.agent') }}</el-avatar>
          </div>
          <div class="chat-bubble-wrap">
            <div class="chat-meta">{{ msg.createTime || '' }}</div>
            <div class="chat-bubble">{{ msg.content }}</div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="500"
          :placeholder="t('chat.inputPh')"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="chat-actions">
          <span class="chat-hint">{{ t('chat.workTime') }}</span>
          <el-button type="success" :loading="sending" :disabled="!draft.trim() || !sessionId" @click="handleSend">
            {{ t('chat.send') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getChatSession, getChatMessages, sendChatMessage } from '@/api/chat'

const { t } = useI18n()

const sessionId = ref(null)
const messages = ref([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const chatWindowRef = ref()
let pollTimer = null

// CsMessage.senderType: 1会员 2客服
function isMine(msg) {
  return msg.senderType === 1
}

function lastId() {
  return messages.value.length ? messages.value[messages.value.length - 1].id : undefined
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatWindowRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function appendMessages(list) {
  if (!list || !list.length) return
  const known = new Set(messages.value.map((m) => m.id))
  const fresh = list.filter((m) => !known.has(m.id))
  if (fresh.length) {
    messages.value.push(...fresh)
    scrollToBottom()
  }
}

// 增量轮询：afterId 之后的新消息
async function pollMessages() {
  if (!sessionId.value) return
  try {
    const list = await getChatMessages(sessionId.value, lastId())
    appendMessages(list)
  } catch (e) {
    /* 静默失败，等待下次轮询 */
  }
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value || !sessionId.value) return
  sending.value = true
  try {
    const msg = await sendChatMessage({ sessionId: sessionId.value, content })
    if (msg) appendMessages([msg])
    draft.value = ''
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    // CsSession: { id, ... }
    const session = await getChatSession()
    sessionId.value = session?.id || null
    if (sessionId.value) {
      const list = await getChatMessages(sessionId.value)
      appendMessages(list)
    }
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
  pollTimer = setInterval(pollMessages, 5000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.chat-wrap {
  max-width: 820px;
  margin: 0 auto;
}

.chat-window {
  height: 420px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
  margin-bottom: 12px;
}

.chat-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.chat-msg.mine {
  flex-direction: row-reverse;
}

.chat-bubble-wrap {
  max-width: 70%;
}

.chat-meta {
  font-size: 12px;
  color: #bbb;
  margin-bottom: 4px;
}

.chat-msg.mine .chat-meta {
  text-align: right;
}

.chat-bubble {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-msg.mine .chat-bubble {
  background: #d8f3e6;
  border-color: #bfe8d4;
}

.chat-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.chat-hint {
  font-size: 13px;
  color: #999;
}
</style>
