/**
 * 后端 TINYINT 数字码字典统一维护（与数据库/实体注释一致）
 * 每项：{ label: 中文标签, type: el-tag 颜色 }
 */

// booking.pay_status：0待支付 1已支付 2已退款
export const BOOKING_PAY_STATUS = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已退款', type: 'danger' }
}

// booking.booking_status：0待确认 1已确认 2已入住 3已完成(已退房) 4已取消
export const BOOKING_STATUS = {
  0: { label: '待确认', type: 'warning' },
  1: { label: '已确认', type: 'primary' },
  2: { label: '已入住', type: 'success' },
  3: { label: '已完成', type: 'info' },
  4: { label: '已取消', type: 'info' }
}

// room.room_status：0可预订 1维修中 2已满房
export const ROOM_STATUS = {
  0: { label: '可预订', type: 'success' },
  1: { label: '维修中', type: 'danger' },
  2: { label: '已满房', type: 'warning' }
}

// room.shelf_status：0已下架 1已上架
export const SHELF_STATUS = {
  0: { label: '已下架', type: 'info' },
  1: { label: '已上架', type: 'success' }
}

// review.audit_status：0待审核 1已通过 2未通过
export const REVIEW_AUDIT_STATUS = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '未通过', type: 'danger' }
}

// feedback.type：1住宿服务 2设施问题 3订单问题 4环境问题 5其他
export const FEEDBACK_TYPE = {
  1: { label: '住宿服务', type: 'primary' },
  2: { label: '设施问题', type: 'warning' },
  3: { label: '订单问题', type: 'danger' },
  4: { label: '环境问题', type: 'success' },
  5: { label: '其他', type: 'info' }
}

// feedback.status：0待处理 1处理中 2已处理
export const FEEDBACK_STATUS = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '处理中', type: 'primary' },
  2: { label: '已处理', type: 'success' }
}

// member.status / admin.status：0禁用 1正常(启用)
export const MEMBER_STATUS = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '正常', type: 'success' }
}

// 通用启停：0停用 1启用（member_type / room_type / banner / menu / facility 等）
export const COMMON_STATUS = {
  0: { label: '停用', type: 'info' },
  1: { label: '启用', type: 'success' }
}

// payment.pay_method：1微信 2支付宝 3银行卡 4余额/其他 7银联
export const PAY_METHOD = {
  1: { label: '微信支付', type: 'success' },
  2: { label: '支付宝支付', type: 'primary' },
  3: { label: '银行卡支付', type: 'warning' },
  4: { label: '余额/其他', type: 'info' },
  7: { label: '银联支付', type: 'danger' }
}

// payment.pay_status：0待支付 1成功 2失败 3已退款
export const PAYMENT_STATUS = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '支付失败', type: 'danger' },
  3: { label: '已退款', type: 'danger' }
}

// notice.publish_status：0草稿/已下架 1已发布
export const NOTICE_STATUS = {
  0: { label: '草稿/已下架', type: 'info' },
  1: { label: '已发布', type: 'success' }
}

// checkin_record.status：0待入住 1已入住 2已退房
export const CHECKIN_STATUS = {
  0: { label: '待入住', type: 'warning' },
  1: { label: '已入住', type: 'success' },
  2: { label: '已退房', type: 'info' }
}

// operation_log.log_type：1登录 2新增 3修改 4删除 5其他
export const LOG_TYPE = {
  1: { label: '登录', type: 'primary' },
  2: { label: '新增', type: 'success' },
  3: { label: '修改', type: 'warning' },
  4: { label: '删除', type: 'danger' },
  5: { label: '其他', type: 'info' }
}

// cs_session.status：0排队中 1会话中 2已结束
export const CS_SESSION_STATUS = {
  0: { label: '排队中', type: 'warning' },
  1: { label: '会话中', type: 'success' },
  2: { label: '已结束', type: 'info' }
}

// refund_record.status：0申请中 1已同意(退款中) 2已拒绝 3已退款
export const REFUND_STATUS = {
  0: { label: '申请中', type: 'warning' },
  1: { label: '退款中', type: 'primary' },
  2: { label: '已拒绝', type: 'danger' },
  3: { label: '已退款', type: 'success' }
}

// payment/refund_record.channel：1模拟 2Stripe 3LinkTrust
export const PAY_CHANNEL = {
  1: { label: '模拟', type: 'info' },
  2: { label: 'Stripe', type: 'primary' },
  3: { label: 'LinkTrust', type: 'warning' }
}

// exchange_rate.source：1 API自动 2 手工
export const RATE_SOURCE = {
  1: { label: 'API自动', type: 'primary' },
  2: { label: '手工', type: 'warning' }
}

// promotion.type：1百分比折扣 2满减 3长住折扣 4早鸟折扣
export const PROMOTION_TYPE = {
  1: { label: '百分比折扣', type: 'primary' },
  2: { label: '满减', type: 'danger' },
  3: { label: '长住折扣', type: 'success' },
  4: { label: '早鸟折扣', type: 'warning' }
}

export function dictLabel(dict, value) {
  return dict[value]?.label ?? (value ?? '-')
}

export function dictType(dict, value) {
  return dict[value]?.type ?? 'info'
}

/** 转 el-select 选项数组 [{label, value(Number)}] */
export function dictOptions(dict) {
  return Object.entries(dict).map(([value, item]) => ({ label: item.label, value: Number(value) }))
}
