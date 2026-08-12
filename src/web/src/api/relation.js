import request from '../utils/request'

function idemKey(action) {
  return `pigfeed-${action}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function followUser(targetUserId) {
  return request.put(`/users/me/follow/${targetUserId}`, null, {
    headers: { 'Idempotency-Key': idemKey('follow-' + targetUserId) }
  })
}

export function unfollowUser(targetUserId) {
  return request.delete(`/users/me/unfollow/${targetUserId}`, {
    headers: { 'Idempotency-Key': idemKey('unfollow-' + targetUserId) }
  })
}

export function getFollowingList(params) {
  const req = {
    cursor: '',
    limit: 20,
    ...params
  }
  return request.get('/users/me/following/list', { params: req })
}

export function getFollowerList(params) {
  const req = {
    cursor: '',
    limit: 20,
    ...params
  }
  return request.get('/users/me/followers/list', { params: req })
}
