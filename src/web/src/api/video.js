import request from '../utils/request'

function idemKey(action) {
  return `pigfeed-${action}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function createVideo(data) {
  return request.post('/users/create/videos', data, {
    headers: { 'Idempotency-Key': idemKey('video-create') }
  })
}

export function getVideo(videoId) {
  return request.get(`/users/videos/get/${videoId}`)
}

export function deleteVideo(videoId) {
  return request.delete(`/users/videos/delete/${videoId}`)
}

export function getUserVideos(userId, params) {
  const req = {
    limit: 20,
    offset: 0,
    ...params
  }
  return request.get(`/users/${userId}/videos`, { params: req })
}

export function getMyVideos(params) {
  const req = {
    limit: 20,
    offset: 0,
    ...params
  }
  return request.get('/users/me/videos/page', { params: req })
}
