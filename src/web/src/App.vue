<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useUserStore} from './stores/user'
import {getUnreadCount} from './api/message'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const unreadCount = ref(0)
const loadingUnread = ref(false)

const feedScenes = [
  { key: 'timeline', label: '最新视频', path: '/timeline', icon: 'home' },
  { key: 'recommend', label: '推荐流', path: '/recommend', icon: 'auto_awesome' },
  { key: 'following', label: '关注流', path: '/following', icon: 'subscriptions' },
  { key: 'hot', label: '热门榜单', path: '/hotfeed', icon: 'local_fire_department' }
]

const DEFAULT_AVATAR = 'data:image/svg+xml;charset=UTF-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect width="40" height="40" fill="%232a2d35"/><circle cx="20" cy="15" r="7" fill="%238b8fa3"/><path d="M8 35c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%238b8fa3"/></svg>'

const currentUser = computed(() => userStore.user || {
  id: 0,
  account: '',
  nickname: '',
  avatarUrl: '',
  bio: '',
  role: '',
  status: 0
})

const isAuthenticated = computed(() => !!userStore.token)

async function loadUnreadCount() {
  if (!isAuthenticated.value) {
    unreadCount.value = 0
    return
  }
  try {
    const data = await getUnreadCount()
    unreadCount.value = data.unreadCount || 0
  } catch {
    unreadCount.value = 0
  }
}

function navigate(path) {
  router.push(path)
}

function handleLogout() {
  userStore.logout()
  router.push('/auth')
}

onMounted(() => {
  if (isAuthenticated.value) {
    loadUnreadCount()
  }
})
</script>

<template>
  <div class="app-shell">
    <header class="top-nav">
      <div class="top-left">
        <button class="wordmark" @click="navigate(isAuthenticated ? '/recommend' : '/timeline')">
          PigFeed
        </button>
      </div>
      <div class="top-center">
        <label class="search-box">
<!--           <span class="material-symbols-outlined">search</span> -->
<!--           <input placeholder="搜索" /> -->
        </label>
      </div>
      <div class="top-actions">
        <button class="upload-button" @click="navigate(isAuthenticated ? '/upload' : '/auth')">
          <span class="material-symbols-outlined">upload</span>
          发布
        </button>
        <button
          class="icon-button badge-button"
          aria-label="通知"
          @click="navigate(isAuthenticated ? '/messages' : '/auth')"
        >
          <span class="material-symbols-outlined">notifications</span>
          <span v-if="isAuthenticated && unreadCount > 0" class="nav-badge floating">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
        <button
          class="avatar-button"
          :class="{ guest: !isAuthenticated }"
          @click="navigate(isAuthenticated ? '/profile' : '/auth')"
          :aria-label="isAuthenticated ? '个人资料' : '登录'"
        >
          <template v-if="isAuthenticated">
            <img :src="currentUser.avatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR" />
          </template>
          <template v-else>
            <span class="material-symbols-outlined">person</span>
            <span>登录</span>
          </template>
        </button>
        <button
          v-if="isAuthenticated"
          class="icon-button"
          @click="handleLogout"
          aria-label="退出登录"
        >
          <span class="material-symbols-outlined">logout</span>
        </button>
      </div>
    </header>

    <div class="app-body">
      <aside class="sidebar">
        <button
          v-for="scene in feedScenes"
          :key="scene.key"
          class="sidebar-link"
          :class="{ active: route.path === scene.path }"
          @click="navigate(scene.path)"
        >
          <span class="material-symbols-outlined filled">{{ scene.icon }}</span>
          <span>{{ scene.label }}</span>
        </button>
        <button
          class="sidebar-link"
          :class="{ active: route.path === '/messages' }"
          @click="navigate(isAuthenticated ? '/messages' : '/auth')"
        >
          <span class="material-symbols-outlined filled">notifications</span>
          <span>消息</span>
          <span v-if="isAuthenticated && unreadCount > 0" class="nav-badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
      </aside>
      <router-view />
    </div>
  </div>
</template>
