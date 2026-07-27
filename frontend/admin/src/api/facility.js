import request from '@/utils/request'

// 查询参数：current/size/name/type/status；实体 NearbyFacility：facilityNo/name/type/distance(米)/address/imageUrl/status
export const listFacilities = (params) => request.get('/facilities', { params })
export const createFacility = (data) => request.post('/facilities', data)
export const updateFacility = (id, data) => request.put(`/facilities/${id}`, data)
export const updateFacilityStatus = (id, status) => request.put(`/facilities/${id}/status`, { status })
export const deleteFacility = (id) => request.delete(`/facilities/${id}`)
