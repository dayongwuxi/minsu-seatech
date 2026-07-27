import request from '@/utils/request'

// 促销管理（契约：docs/支付促销退款设计.md 第 2/5 节）
// 分页查询参数：current/size/name/type/status
// Promotion 字段：promoNo/name/type(1百分比 2满减 3长住 4早鸟)/discountRate(0.80=8折)/
//   discountAmount/thresholdAmount/minNights/advanceDays/roomId(NULL=全场)/couponCode(NULL=自动应用)/
//   startDate/endDate/usageLimit(NULL=不限)/usedCount/status(0停用 1启用)
export const listPromotions = (params) => request.get('/promotions', { params })
export const createPromotion = (data) => request.post('/promotions', data)
export const updatePromotion = (id, data) => request.put(`/promotions/${id}`, data)
export const deletePromotion = (id) => request.delete(`/promotions/${id}`)
// status: 0停用 1启用
export const updatePromotionStatus = (id, status) => request.put(`/promotions/${id}/status`, { status })
