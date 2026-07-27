import request from '@/utils/request'

// CheckinQuery：current/size/guestName/roomName/dateStart/dateEnd/status
// 返回 CheckinAdminVO（含 roomName/roomNo/orderNo/memberName）
export const listCheckins = (params) => request.get('/checkins', { params })
export const updateCheckin = (id, data) => request.put(`/checkins/${id}`, data)
export const deleteCheckin = (id) => request.delete(`/checkins/${id}`)
// POST 办理退房：入住记录置已退房并联动订单已完成
export const checkout = (id) => request.post(`/checkins/${id}/checkout`)
