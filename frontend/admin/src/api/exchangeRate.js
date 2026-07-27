import request from '@/utils/request'

// 汇率管理（契约：docs/支付促销退款设计.md 第 9 节，表 exchange_rate）
// 字段：currencyCode/currencyName/symbol/rate(1 CNY 兑换量, DECIMAL(16,8))/
//   autoUpdate(1随API自动刷新 0手工锁定)/source(1 API 2手工)/status/updateTime
export const listExchangeRates = () => request.get('/exchange-rates')
// 手工调整：source→2 且 autoUpdate→0（锁定，不再被自动刷新覆盖）
export const updateExchangeRate = (code, rate) => request.put(`/exchange-rates/${code}`, { rate })
// 恢复自动刷新：autoUpdate→1
export const restoreAutoUpdate = (code) => request.put(`/exchange-rates/${code}/auto`)
// 立即从 open.er-api.com 抓取（仅更新 autoUpdate=1 的行）
export const refreshExchangeRates = () => request.post('/exchange-rates/refresh')
