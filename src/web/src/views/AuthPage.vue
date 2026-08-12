<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '../stores/user'
import {getCurrentUser, login, register} from '../api/account'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('login')
const form = ref({ account: '', password: '', nickname: '' })
const message = ref('')
const messageType = ref('error')
const submitting = ref(false)

async function handleSubmit() {
  message.value = ''
  submitting.value = true
  
  try {
    if (mode.value === 'register') {
      await register({
        account: form.value.account.trim(),
        password: form.value.password,
        nickname: form.value.nickname.trim()
      })
    }
    
    const tokenResponse = await login({
      account: form.value.account.trim(),
      password: form.value.password
    })
    
    userStore.setAuth(tokenResponse.accessToken, null)
    const profile = await getCurrentUser()
    userStore.setAuth(tokenResponse.accessToken, profile)
    router.push('/recommend')
  } catch (error) {
    messageType.value = 'error'
    message.value = error.message || '登录失败，请检查账号与密码'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-center">
      <div class="auth-card">
        <div class="brand-block">
          <span class="brand-mark">Pig</span>
          <div>
            <h1>登录 PigFeed</h1>
            <p>连接后端账号、Feed 和个人资料。</p>
          </div>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <div class="auth-mode-tabs">
            <button
              :class="{ active: mode === 'login' }"
              type="button"
              @click="mode = 'login'"
            >
              登录
            </button>
            <button
              :class="{ active: mode === 'register' }"
              type="button"
              @click="mode = 'register'"
            >
              注册
            </button>
          </div>
          <label>
            <span>账号</span>
            <input
              v-model="form.account"
              placeholder="请输入账号"
              autocomplete="username"
            />
          </label>
          <label v-if="mode === 'register'">
            <span>昵称</span>
            <input
              v-model="form.nickname"
              placeholder="输入昵称"
              autocomplete="nickname"
            />
          </label>
          <label>
            <span>密码</span>
            <input
              v-model="form.password"
              placeholder="输入密码"
              type="password"
              autocomplete="current-password"
            />
          </label>
          <p v-if="message" class="form-message">{{ message }}</p>
          <button class="primary-button" :disabled="submitting">
            <span class="material-symbols-outlined">login</span>
            {{ submitting ? '提交中' : mode === 'register' ? '注册并登录' : '登录' }}
          </button>
        </form>
      </div>
    </section>
  </main>
</template>
