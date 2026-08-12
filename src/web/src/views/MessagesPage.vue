<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { getMessages, markMessagesRead } from '../api/message'

const DEFAULT_AVATAR = 'data:image/svg+xml;charset=UTF-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect width="40" height="40" fill="%232a2d35"/><circle cx="20" cy="15" r="7" fill="%238b8fa3"/><path d="M8 35c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%238b8fa3"/></svg>'

const userStore = useUserStore()

const messages = ref([])
const nextCursor = ref('')
const hasMore = ref(false)
const state = ref('loading')
const error = ref('')
const busyId = ref(0)
const markingAll = ref(false)

const unreadCount = computed(() => 
  messages.value.filter(m => !m.isRead).length
)

const loadingInitial = computed(() => 
  state.value === 'loading' && messages.value.length === 0
)

async function loadMessages(reset = false) {
  if (reset) {
    state.value = 'loading'
    error.value = ''
  } else {
    state.value = 'loadingMore'
  }
  
  try {
    const cursor = reset ? '' : nextCursor.value
    const data = await getMessages({ limit: 20, cursor })
    const items = data.items || []
    
    if (reset) {
      messages.value = items
    } else {
      messages.value = [...messages.value, ...items]
    }
    
    nextCursor.value = data.nextCursor || ''
    hasMore.value = Boolean(data.hasMore && data.nextCursor)
    state.value = 'ready'
  } catch (err) {
    error.value = err.message || '消息加载失败'
    state.value = 'error'
  }
}

async function markMessageRead(message) {
  if (!message || message.isRead || busyId.value) return
  busyId.value = message.id
  
  try {
    await markMessagesRead([message.id])
    message.isRead = true
    message.readAt = new Date().toISOString()
  } catch (err) {
    error.value = err.message || '已读操作失败'
  } finally {
    busyId.value = 0
  }
}

async function markAllRead() {
  if (markingAll.value || unreadCount.value === 0) return
  markingAll.value = true
  
  try {
    await markMessagesRead([])
    for (const msg of messages.value) {
      msg.isRead = true
      msg.readAt = msg.readAt || new Date().toISOString()
    }
  } catch (err) {
    error.value = err.message || '全部已读失败'
  } finally {
    markingAll.value = false
  }
}

onMounted(() => {
  loadMessages(true)
})
</script>

<template>
  <main class="messages-page">
    <section class="messages-header">
      <div>
        <p class="eyebrow">Messages</p>
        <h1>消息中心</h1>
      </div>
      <div class="messages-actions">
        <span class="messages-count">{{ unreadCount > 0 ? `${unreadCount} 未读` : '已读完' }}</span>
        <button
          class="ghost-button compact"
          @click="loadMessages(true)"
          :disabled="loadingInitial || state === 'loadingMore'"
        >
          <span class="material-symbols-outlined">refresh</span>
          刷新
        </button>
        <button
          class="primary-button compact"
          @click="markAllRead"
          :disabled="markingAll || unreadCount === 0"
        >
          <span class="material-symbols-outlined">done_all</span>
          {{ markingAll ? '处理中' : '全部已读' }}
        </button>
      </div>
    </section>

    <section class="messages-list-wrap">
      <div v-if="loadingInitial" class="page-message">
        <span class="material-symbols-outlined">hourglass_top</span>
        <strong>正在加载消息</strong>
      </div>
      
      <div v-else-if="state === 'error' && messages.length === 0" class="page-message">
        <span class="material-symbols-outlined">sync_problem</span>
        <strong>{{ error || '消息加载失败' }}</strong>
        <button @click="loadMessages(true)">重试</button>
      </div>
      
      <div v-else-if="state === 'ready' && messages.length === 0" class="page-message">
        <span class="material-symbols-outlined">notifications</span>
        <strong>暂无消息</strong>
      </div>
      
      <p v-if="error && messages.length > 0" class="form-message">{{ error }}</p>
      
      <div v-else class="messages-list">
        <button
          v-for="message in messages"
          :key="message.id"
          class="message-item"
          :class="{ unread: !message.isRead, read: message.isRead }"
          @click="markMessageRead(message)"
          :disabled="busyId === message.id"
        >
          <span class="message-icon" :class="{ active: !message.isRead }">
            <span class="material-symbols-outlined">
              {{ message.messageType === 'like' ? 'favorite' :
                 message.messageType === 'comment' ? 'chat_bubble' :
                 message.messageType === 'follow' ? 'person_add' : 'notifications' }}
            </span>
          </span>
          <div class="message-copy">
            <div class="message-title-row">
              <strong>{{ message.title }}</strong>
              <small>{{ message.createdAt.replace('T', ' ') }}</small>
            </div>
            <div class="message-actor-row">
              <img :src="message.actorAvatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR" />
              <strong>{{ message.actorNickname }}</strong>
            </div>
          </div>
          <span class="message-state">
            {{ message.isRead ? '已读' : '未读' }}
          </span>
        </button>
        
        <button
          v-if="state === 'ready' && hasMore"
          class="ghost-button compact messages-more"
          @click="loadMessages(false)"
          :disabled="state === 'loadingMore'"
        >
          <span class="material-symbols-outlined">expand_more</span>
          {{ state === 'loadingMore' ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </section>
  </main>
</template>
