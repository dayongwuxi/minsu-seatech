import request from '@/utils/request'

// 查询参数：current/size/typeName/status；实体字段 typeNo/typeName/discount/benefits/status
export const listMemberTypes = (params) => request.get('/member-types', { params })
export const createMemberType = (data) => request.post('/member-types', data)
export const updateMemberType = (id, data) => request.put(`/member-types/${id}`, data)
export const updateMemberTypeStatus = (id, status) => request.put(`/member-types/${id}/status`, { status })
export const deleteMemberType = (id) => request.delete(`/member-types/${id}`)
