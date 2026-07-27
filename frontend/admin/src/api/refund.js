import request from '@/utils/request'

// 退款管理（契约：docs/支付促销退款设计.md 第 3/5 节、docs/LinkTrustPay接入设计.md 第 4 节）
// 分页查询参数：current/size/orderNo/memberName/status
// refund_record：refundAmount/reason/status(0申请中 1退款中 2已拒绝 3已退款)/
//   channel(1模拟 2Stripe 3LinkTrust)/stripeRefundId/applyTime/handleTime
export const listRefunds = (params) => request.get('/refunds', { params })
// 同意退款：Stripe 单发起真实退款(status→1退款中，webhook 后→3)；
// LinkTrust 单→1退款中(渠道无退款API，管理员线下退款后 complete)；Mock 单直接→3已退款
export const approveRefund = (id) => request.put(`/refunds/${id}/approve`)
// 确认线下退款完成（LinkTrust 单，status 1→3）
export const completeRefund = (id) => request.put(`/refunds/${id}/complete`)
// 拒绝退款：status→2已拒绝
export const rejectRefund = (id, reason) => request.put(`/refunds/${id}/reject`, { reason })
