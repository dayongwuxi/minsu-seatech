import request from '@/utils/request'

// 我的银行卡列表（元数据：brand/last4/expMonth/expYear/holderName/isDefault/channel）
export function listPayMethods() {
  return request.get('/user/payment-methods')
}

// 绑卡准备：Stripe 通道返回 { clientSecret }；Mock 通道返回 { mock: true }
export function createSetupIntent() {
  return request.post('/user/payment-methods/setup-intent')
}

// 保存银行卡：Stripe {stripePaymentMethodId}；Mock {brand,last4,expMonth,expYear,holderName}
// 注意：完整卡号与 CVC 永不上传
export function savePayMethod(data) {
  return request.post('/user/payment-methods', data)
}

// 设为默认（同会员互斥）
export function setDefaultPayMethod(id) {
  return request.put(`/user/payment-methods/${id}/default`)
}

// 删除（Stripe 通道后端同时 detach）
export function removePayMethod(id) {
  return request.delete(`/user/payment-methods/${id}`)
}
