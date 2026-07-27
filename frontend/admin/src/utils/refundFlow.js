// 退款处理流程的渠道差异（设计见 docs/LinkTrustPay接入设计.md 第4节）：
// Mock(1) 同意即退款；Stripe(2) 同意后渠道自动退款、webhook 确认；
// LinkTrust(3) 无退款 API，同意后需去渠道后台手工退款，再回站内「确认已退款」。

export function approveTip(channel) {
  if (channel === 2) {
    return '该笔为 Stripe 支付订单，同意后将调用 Stripe 发起真实退款（先进入「退款中」，Stripe 确认后自动变为「已退款」）。'
  }
  if (channel === 3) {
    return '该笔为 LinkTrust 支付订单，该渠道不支持自动退款：同意后将进入「退款中」，请前往 LinkTrust 商户后台手工退款，完成后回到本页点击「确认已退款」。'
  }
  return '该笔为模拟支付订单，同意后将直接标记为「已退款」。'
}

/** 「确认已退款」按钮：仅 LinkTrust 渠道的退款中记录（Stripe 由 webhook 自动确认） */
export function canComplete(row) {
  return row?.channel === 3 && row?.status === 1
}

export function completeTip() {
  return '请确认已在 LinkTrust 商户后台完成该笔退款的手工操作。确认后订单将标记为「已退款」并联动订单状态。'
}
