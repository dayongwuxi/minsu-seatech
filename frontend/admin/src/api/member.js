import request from '@/utils/request'

// 查询参数：current/size/name/phone/typeId/status；返回 MemberAdminVO（含 memberTypeName/registerTime）
export const listMembers = (params) => request.get('/members', { params })
export const getMember = (id) => request.get(`/members/${id}`)
// MemberSaveRequest：{ username, phone, password, name, email, avatar, memberTypeId, status }
export const createMember = (data) => request.post('/members', data)
export const updateMember = (id, data) => request.put(`/members/${id}`, data)
export const deleteMember = (id) => request.delete(`/members/${id}`)
// status: 0禁用 1正常
export const updateMemberStatus = (id, status) => request.put(`/members/${id}/status`, { status })
