import request from '../utils/request'

function idemKey(action) {
  return `pigfeed-${action}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function likeVideo(videoId) {
  return request.put(`/videos/like?videoId=${videoId}`, null, {
    headers: { 'Idempotency-Key': idemKey('like-' + videoId) }
  })
}

export function unlikeVideo(videoId) {
  return request.delete(`/videos/unlike?videoId=${videoId}`, {
    headers: { 'Idempotency-Key': idemKey('unlike-' + videoId) }
  })
}

export function favoriteVideo(videoId) {
  return request.put(`/videos/favorite?videoId=${videoId}`, null, {
    headers: { 'Idempotency-Key': idemKey('favorite-' + videoId) }
  })
}

export function unfavoriteVideo(videoId) {
  return request.delete(`/videos/unfavorite?videoId=${videoId}`, {
    headers: { 'Idempotency-Key': idemKey('unfavorite-' + videoId) }
  })
}

export function createComment(videoId, data) {
  return request.post(`/videos/createcomments?videoId=${videoId}`, data, {
    headers: { 'Idempotency-Key': idemKey('comment-' + videoId) }
  })
}

export function getComments(videoId, params) {
  return request.get(`/videos/listcomments?videoId=${videoId}`, { params })
}

export function deleteComment(commentId) {
  return request.delete(`/comments/delete?commentId=${commentId}`)
}
