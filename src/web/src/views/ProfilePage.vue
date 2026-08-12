<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getCurrentUser, updateProfile, getUserProfile } from '../api/account'
import { getMyVideos, getUserVideos } from '../api/video'
import { getFollowingList, getFollowerList, followUser, unfollowUser } from '../api/relation'

const DEFAULT_AVATAR = 'data:image/svg+xml;charset=UTF-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect width="40" height="40" fill="%232a2d35"/><circle cx="20" cy="15" r="7" fill="%238b8fa3"/><path d="M8 35c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%238b8fa3"/></svg>'

const props = defineProps({
  userId: {
    type: [String, Number],
    default: null
  }
})

const router = useRouter()
const userStore = useUserStore()

const isOwnProfile = computed(() => {
  return props.userId === null || 
         String(props.userId) === String(userStore.user?.id)
})

const profile = ref(null)
const profileLoading = ref(true)
const profileError = ref('')

const videos = ref([])
const videosLoading = ref(false)

const activeTab = ref('following')
const relations = ref([])
const relationsLoading = ref(false)

const editing = ref(false)
const editForm = ref({ nickname: '', avatarUrl: '', bio: '' })
const saving = ref(false)

async function loadProfile() {
  profileLoading.value = true
  profileError.value = ''
  try {
    if (isOwnProfile.value) {
      profile.value = await getCurrentUser()
    } else {
      profile.value = await getUserProfile(props.userId)
    }
    loadVideos()
    if (isOwnProfile.value) loadRelations()
  } catch (error) {
    profileError.value = error.message || '加载失败'
  } finally {
    profileLoading.value = false
  }
}

async function loadVideos() {
  if (!profile.value) return
  videosLoading.value = true
  try {
    if (isOwnProfile.value) {
      const data = await getMyVideos({ limit: 20 })
      videos.value = data.items || []
    } else {
      const data = await getUserVideos(props.userId, { limit: 20 })
      videos.value = data.items || []
    }
  } catch {
    videos.value = []
  } finally {
    videosLoading.value = false
  }
}

async function loadRelations() {
  relationsLoading.value = true
  try {
    if (activeTab.value === 'following') {
      const data = await getFollowingList({ limit: 50 })
      relations.value = data.items || []
    } else {
      const data = await getFollowerList({ limit: 50 })
      relations.value = data.items || []
    }
  } catch {
    relations.value = []
  } finally {
    relationsLoading.value = false
  }
}

function openEditModal() {
  editForm.value = {
    nickname: profile.value.nickname || '',
    avatarUrl: profile.value.avatarUrl || '',
    bio: profile.value.bio || ''
  }
  editing.value = true
}

async function saveProfile() {
  saving.value = true
  try {
    const data = {}
    if (editForm.value.nickname !== profile.value.nickname) {
      data.nickname = editForm.value.nickname
    }
    if (editForm.value.avatarUrl !== profile.value.avatarUrl) {
      data.avatarUrl = editForm.value.avatarUrl
    }
    if (editForm.value.bio !== profile.value.bio) {
      data.bio = editForm.value.bio
    }
    
    if (Object.keys(data).length > 0) {
      const updated = await updateProfile(data)
      profile.value = updated
      userStore.setAuth(userStore.token, updated)
    }
    editing.value = false
  } catch (error) {
    alert(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleFollow(targetUserId, isFollowing) {
  try {
    if (isFollowing) {
      await unfollowUser(targetUserId)
    } else {
      await followUser(targetUserId)
    }
    loadRelations()
  } catch (error) {
    alert(error.message || '操作失败')
  }
}

watch(activeTab, () => {
  loadRelations()
})

watch(() => props.userId, () => {
  loadProfile()
})

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <main class="profile-page">
    <div v-if="profileLoading" class="page-message">
      <span class="material-symbols-outlined">hourglass_top</span>
      <strong>加载中...</strong>
    </div>
    
    <div v-else-if="profileError" class="page-message">
      <span class="material-symbols-outlined">error</span>
      <strong>{{ profileError }}</strong>
      <button @click="loadProfile">重试</button>
    </div>
    
    <template v-else-if="profile">
      <section class="profile-hero">
        <div 
          class="profile-summary"
          :class="{ 'public-profile-summary': !isOwnProfile }"
        >
          <img class="profile-avatar" :src="profile.avatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR" />
          <div>
            <p class="eyebrow">{{ isOwnProfile ? '我的主页' : '用户主页' }}</p>
            <h1>{{ profile.nickname }}</h1>
            <p>{{ profile.bio }}</p>
          </div>
          <div class="profile-stats">
            <button>
              <strong>{{ profile.followingCount || 0 }}</strong>
              关注
            </button>
            <button>
              <strong>{{ profile.followerCount || 0 }}</strong>
              粉丝
            </button>
            <button>
              <strong>{{ profile.workCount || 0 }}</strong>
              作品
            </button>
          </div>
          <button
            v-if="isOwnProfile"
            class="profile-edit-button"
            @click="openEditModal"
          >
            <span class="material-symbols-outlined">edit</span>
          </button>
          <button
            v-else
            class="relation-follow-button"
            @click="router.back()"
          >
<!--             <span class="material-symbols-outlined">arrow_back</span> -->
            <span style="font-size: 14px">返回</span>
          </button>
        </div>
      </section>

      <div class="profile-grid">
        <section class="profile-card">
          <header>
            <h2>{{ isOwnProfile ? '我的作品' : 'TA的作品' }}</h2>
          </header>
          <div v-if="videosLoading" class="page-message">
            <strong>加载中...</strong>
          </div>
          <div v-else-if="videos.length === 0" class="page-message">
            <strong>暂无作品</strong>
          </div>
          <div v-else class="work-list">
            <button
              v-for="video in videos"
              :key="video.id"
              class="work-item"
              @click="router.push('/')"
            >
              <div class="work-thumb">
                <img :src="video.coverUrl" alt="" />
                <span class="material-symbols-outlined">play_arrow</span>
              </div>
              <div class="work-meta">
                <h3>{{ video.title }}</h3>
                <p>{{ video.description || '暂无描述' }}</p>
                <small class="status-badge">{{ video.status === 2 ? '已发布' : '草稿' }}</small>
              </div>
            </button>
          </div>
        </section>

        <section v-if="isOwnProfile" class="profile-card">
          <header>
            <h2>关系</h2>
            <div class="relation-tabs">
              <button
                :class="{ active: activeTab === 'following' }"
                @click="activeTab = 'following'"
              >
                关注
              </button>
              <button
                :class="{ active: activeTab === 'followers' }"
                @click="activeTab = 'followers'"
              >
                粉丝
              </button>
            </div>
          </header>
          <div class="relation-list-wrap">
            <div v-if="relationsLoading" class="page-message">
              <strong>加载中...</strong>
            </div>
            <div v-else-if="relations.length === 0" class="page-message">
              <strong>{{ activeTab === 'following' ? '暂无关注' : '暂无粉丝' }}</strong>
            </div>
            <div v-else class="relation-list">
              <div v-for="item in relations" :key="item.userId" class="relation-item">
                <img :src="item.avatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR" />
                <div>
                  <strong>{{ item.nickname }}</strong>
                  <p>{{ item.bio || '暂无简介' }}</p>
                </div>
                <button
                  class="relation-follow-button"
                  :class="{ active: activeTab === 'followers' }"
                  @click="handleFollow(item.userId, false)"
                >
                  {{ activeTab === 'followers' ? '回粉' : '已关注' }}
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </template>

    <div v-if="editing" class="modal-backdrop" @click.self="editing = false">
      <div class="profile-modal">
        <header>
          <h2>编辑资料</h2>
          <button class="icon-button small" @click="editing = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>
        <form class="profile-form" @submit.prevent="saveProfile">
          <label>
            <span>昵称</span>
            <input v-model="editForm.nickname" placeholder="输入昵称" />
          </label>
          <label>
            <span>头像 URL</span>
            <input v-model="editForm.avatarUrl" placeholder="输入头像 URL" />
          </label>
          <label>
            <span>个人简介</span>
            <textarea v-model="editForm.bio" placeholder="输入个人简介" />
          </label>
          <button class="primary-button" :disabled="saving">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </form>
      </div>
    </div>
  </main>
</template>
