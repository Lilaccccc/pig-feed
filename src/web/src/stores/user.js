import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('pigfeed.accessToken') || '',
    user: JSON.parse(localStorage.getItem('pigfeed.user') || 'null')
  }),
  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      if (token) {
        localStorage.setItem('pigfeed.accessToken', token)
      } else {
        localStorage.removeItem('pigfeed.accessToken')
      }
      localStorage.setItem('pigfeed.user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('pigfeed.accessToken')
      localStorage.removeItem('pigfeed.user')
    }
  }
})
