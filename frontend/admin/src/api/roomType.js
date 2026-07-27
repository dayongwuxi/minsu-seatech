import request from '@/utils/request'

// 查询参数：current/size/typeName/status；实体字段 typeNo/typeName/description/status
export const listRoomTypes = (params) => request.get('/room-types', { params })
export const createRoomType = (data) => request.post('/room-types', data)
export const updateRoomType = (id, data) => request.put(`/room-types/${id}`, data)
export const updateRoomTypeStatus = (id, status) => request.put(`/room-types/${id}/status`, { status })
export const deleteRoomType = (id) => request.delete(`/room-types/${id}`)
