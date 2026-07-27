import request from '@/utils/request'

// POST /api/admin/upload（multipart form: file），返回 UploadVO { url }，url 为 /files/xxx
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
