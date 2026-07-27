import request from '@/utils/request'

// 汇率（免登录）：{ base: 'CNY', rates: [{code,name,symbol,rate,updateTime}] }
export function getExchangeRates() {
  return request.get('/exchange-rates')
}
