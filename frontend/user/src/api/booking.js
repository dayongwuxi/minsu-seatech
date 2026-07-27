import request from '@/utils/request'

// 价格报价（Airbnb 式明细）：{roomId,checkinDate,checkoutDate,promoCode?}
// → {nights,unitPrice,roomFee,promoDiscount,promoName,memberDiscount,totalAmount,currency}
export function quoteBooking(data) {
  return request.post('/bookings/quote', data)
}

// 提交预订（字段：roomId/checkinDate/checkoutDate/guestCount/guestName/contactPhone/userRemark/promoCode?），返回 Booking 实体
export function createBooking(data) {
  return request.post('/bookings', data)
}

// 我的预约分页（current/size）；status tab: waitPay/paid/cancelled/finished，不传查全部
export function getMyBookings(params) {
  return request.get('/user/bookings', { params })
}

// 订单详情（BookingDetailVO: { booking, member, room, payment, refund }）
export function getBookingDetail(orderNo) {
  return request.get(`/bookings/${orderNo}`)
}

// 取消订单（可选 reason）
export function cancelBooking(orderNo, reason) {
  return request.post(`/bookings/${orderNo}/cancel`, reason ? { reason } : {})
}

// 已完成订单申请退款（可选 reason）
export function refundBooking(orderNo, reason) {
  return request.post(`/bookings/${orderNo}/refund`, reason ? { reason } : {})
}
