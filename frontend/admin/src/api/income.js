import request from '@/utils/request'

// IncomeQuery：current/size/dateStart/dateEnd/roomName/payMethod(数字)/payStatus(数字)
// 返回 IncomeVO：payNo/orderNo/roomName/memberName/amount/payMethod/payStatus/payTime/transactionNo
export const listIncomes = (params) => request.get('/incomes', { params })
// 返回 IncomeStatsVO：todayIncome/todayDiff/monthIncome/monthDiff/totalIncome/refundTotal/actualIncome（diff 为增减额）
export const getIncomeStats = () => request.get('/incomes/stats')
// range: 7d/30d/12m，返回 [{ date, value }]
export const getIncomeTrend = (params) => request.get('/incomes/trend', { params })
// 返回 [{ payMethod, count, amount }]
export const getPayMethodRatio = () => request.get('/incomes/pay-method-ratio')
