import request from '../utils/request'

export function getMessages(params) {
  const req = {
    cursor: '',
    limit: 50,
    ...params
  }
  return request.get('/messages/list', { params: req })
}

export function markMessagesRead(messageIds) {
  return request.patch('/messages/markread', { messageIds })
}

export function getUnreadCount() {
  return request.get('/messages-stats/countunread')
}
