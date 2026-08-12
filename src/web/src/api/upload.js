import request from '../utils/request'

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/uploads', formData, {
    headers: { 'Content-Type': undefined }
  })
}
