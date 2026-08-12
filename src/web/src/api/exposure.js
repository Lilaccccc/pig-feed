import request from '../utils/request'

export function reportViewEvent(data) {
  return request.post('/exposure/video/view/events', data)
}
