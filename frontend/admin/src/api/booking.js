import request from '@/utils/request'

// BookingQuery：current/size/orderNo/memberName/roomName/checkinStart/checkinEnd/payStatus/bookingStatus
// 返回 BookingAdminVO（含 memberName/memberPhone/roomName/roomNo/totalAmount/payStatus/bookingStatus）
export const listBookings = (params) => request.get('/bookings', { params })
// 返回 BookingDetailVO { booking, member, room, payment, refund }
export const getBooking = (id) => request.get(`/bookings/${id}`)
// 仅待确认(bookingStatus=0)可确认
export const confirmBooking = (id) => request.put(`/bookings/${id}/confirm`)
// 待确认/已确认可取消，无请求体
export const cancelBooking = (id) => request.put(`/bookings/${id}/cancel`)
// POST 办理入住：需已支付
export const checkinBooking = (id) => request.post(`/bookings/${id}/checkin`)
// POST 同意退款：需已支付
export const refundBooking = (id) => request.post(`/bookings/${id}/refund`)
export const updateBookingRemark = (id, remark) => request.put(`/bookings/${id}/remark`, { remark })
