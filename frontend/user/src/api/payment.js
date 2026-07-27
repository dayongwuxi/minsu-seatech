import request from '@/utils/request'

// 支付配置（免登录）：{ stripeEnabled, publishableKey, methods[], currency }
export function getPaymentConfig() {
  return request.get('/payments/config')
}

// 创建 Stripe PaymentIntent：{orderNo, paymentMethodId?} → { clientSecret, paymentIntentId, amount, currency }
// 带 paymentMethodId 时后端用 customer+payment_method 直接确认（已存卡支付）
export function createStripeIntent(orderNo, paymentMethodId) {
  return request.post('/payments/stripe/intent', {
    orderNo,
    paymentMethodId: paymentMethodId || undefined
  })
}

// 发起 LinkTrust 跳转支付：{orderNo, payMethod: 1微信 2支付宝 7银联}
// → { payUrl, merId, merOrderId, amount, payNo }，前端据此隐藏表单 POST 跳转收银台
export function createLinkTrustPayment(orderNo, payMethod) {
  return request.post('/payments/linktrust/create', { orderNo, payMethod })
}

// 模拟支付（stripeEnabled=false 时）：{ orderNo, payMethod: 1微信 2支付宝 3银行卡 4其他 }，返回 PaymentResultVO
export function createPayment(data) {
  return request.post('/payments', data)
}

// 查询支付结果，返回 PaymentResultVO
export function getPaymentResult(orderNo) {
  return request.get('/payments/result', { params: { orderNo } })
}
