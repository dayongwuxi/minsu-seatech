/**
 * 后端 TINYINT 状态码 → i18n key / el-tag 颜色 统一映射
 * 文案通过 i18n 词条渲染：页面调用 dictText(dict, code, t) 传入 t 函数。
 * 状态码与 backend entity/dto 注释保持一致，勿私自增减。
 */

// booking.pay_status 0待支付 1已支付 2已退款（payment.pay_status 同义）
export const PAY_STATUS = {
  0: { key: 'dict.payStatus.waitPay', tag: 'warning' },
  1: { key: 'dict.payStatus.paid', tag: 'success' },
  2: { key: 'dict.payStatus.refunded', tag: 'info' }
}

// booking.booking_status 0待确认 1已确认 2已入住 3已完成 4已取消
export const BOOKING_STATUS = {
  0: { key: 'dict.bookingStatus.waitConfirm', tag: 'warning' },
  1: { key: 'dict.bookingStatus.confirmed', tag: 'primary' },
  2: { key: 'dict.bookingStatus.checkedIn', tag: 'success' },
  3: { key: 'dict.bookingStatus.finished', tag: 'success' },
  4: { key: 'dict.bookingStatus.cancelled', tag: 'info' }
}

// payment.pay_method 1微信 2支付宝 3银行卡 4其他 7银联
export const PAY_METHOD = {
  1: { key: 'dict.payMethod.wechat', tag: 'success' },
  2: { key: 'dict.payMethod.alipay', tag: 'primary' },
  3: { key: 'dict.payMethod.bankCard', tag: 'warning' },
  4: { key: 'dict.payMethod.other', tag: 'info' },
  7: { key: 'dict.payMethod.unionpay', tag: 'danger' }
}

// feedback.type 1住宿服务 2设施问题 3订单问题 4环境问题 5其他
export const FEEDBACK_TYPE = {
  1: { key: 'dict.feedbackType.service', tag: 'primary' },
  2: { key: 'dict.feedbackType.facility', tag: 'warning' },
  3: { key: 'dict.feedbackType.order', tag: 'danger' },
  4: { key: 'dict.feedbackType.environment', tag: 'success' },
  5: { key: 'dict.feedbackType.other', tag: 'info' }
}

// feedback.status 0待处理 1处理中 2已处理
export const FEEDBACK_STATUS = {
  0: { key: 'dict.feedbackStatus.pending', tag: 'info' },
  1: { key: 'dict.feedbackStatus.processing', tag: 'warning' },
  2: { key: 'dict.feedbackStatus.done', tag: 'success' }
}

// refund_record.status 0申请中 1已同意(退款中) 2已拒绝 3已退款
export const REFUND_STATUS = {
  0: { key: 'dict.refundStatus.applying', tag: 'warning' },
  1: { key: 'dict.refundStatus.processing', tag: 'warning' },
  2: { key: 'dict.refundStatus.rejected', tag: 'danger' },
  3: { key: 'dict.refundStatus.refunded', tag: 'info' }
}

// review.audit_status 0待审核 1已通过 2未通过
export const REVIEW_AUDIT_STATUS = {
  0: { key: 'dict.reviewAudit.pending', tag: 'info' },
  1: { key: 'dict.reviewAudit.approved', tag: 'success' },
  2: { key: 'dict.reviewAudit.rejected', tag: 'danger' }
}

/** 按当前 locale 翻译状态码文案；t 为 vue-i18n 的翻译函数 */
export function dictText(dict, code, t) {
  const entry = dict[code]
  if (!entry) {
    return code === null || code === undefined ? '-' : String(code)
  }
  return t(entry.key)
}

export function dictTag(dict, code) {
  return dict[code]?.tag ?? 'info'
}

/** 将字典对象转为 el-select/el-radio 可用的选项数组（label 已翻译） */
export function dictOptions(dict, t) {
  return Object.entries(dict).map(([value, entry]) => ({
    value: Number(value),
    label: t(entry.key)
  }))
}
