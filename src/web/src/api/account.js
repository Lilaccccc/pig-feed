import request from '../utils/request'

export function register(data) {
  return request.post('/users', data)
}

export function login(data) {
  return request.post('/sessions', data)
}

export function logout() {
  return request.delete('/sessions/current')
}

export function getCurrentUser() {
  return request.get('/users/me')
}

export function updateProfile(data) {
  return request.patch('/users/update/me', data)
}

export function getUserProfile(userId) {
  return request.get(`/users/${userId}`)
}
