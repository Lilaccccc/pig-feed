import request from '../utils/request'

export function getFeed(params) {
  const req = {
    scene: 'recommend',
    limit: 10,
    cursor: '',
    ...params
  }
  return request.get('/feeds/items', { params: req })
}

export function queryFeed(data) {
  return request.post('/feeds/queries', data)
}

export function refreshFeed(params) {
  return request.get('/feeds/refresh', { params })
}
