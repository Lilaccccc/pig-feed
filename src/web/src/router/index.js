import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from '../stores/user'

const routes = [
  {
    path: '/auth',
    name: 'Auth',
    component: () => import('../views/AuthPage.vue')
  },
  {
    path: '/',
    name: 'Feed',
    component: () => import('../views/FeedPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/recommend',
    name: 'Recommend',
    component: () => import('../views/FeedPage.vue'),
    props: { scene: 'recommend' },
    meta: { requiresAuth: true }
  },
  {
    path: '/timeline',
    name: 'Timeline',
    component: () => import('../views/FeedPage.vue'),
    props: { scene: 'timeline' }
  },
  {
    path: '/following',
    name: 'Following',
    component: () => import('../views/FeedPage.vue'),
    props: { scene: 'following' },
    meta: { requiresAuth: true }
  },
  {
    path: '/hotfeed',
    name: 'Hot',
    component: () => import('../views/FeedPage.vue'),
    props: { scene: 'hot' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/ProfilePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users/:userId',
    name: 'PublicProfile',
    component: () => import('../views/ProfilePage.vue'),
    props: (route) => ({ userId: route.params.userId })
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/MessagesPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('../views/UploadPage.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) {
    next('/auth')
  } else {
    next()
  }
})

export default router
