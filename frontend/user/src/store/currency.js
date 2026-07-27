import { defineStore } from 'pinia'
import { getExchangeRates } from '@/api/currency'

const STORAGE_KEY = 'user_currency'

/**
 * 展示货币仅影响显示，创建订单与扣款一律基准币（= 站点结算货币 PAYMENT_CURRENCY，契约第 9 节）。
 * base 来自 GET /api/exchange-rates；用户无本地选择时默认展示基准币。
 */
export const useCurrencyStore = defineStore('currency', {
  state: () => ({
    currency: (typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY)) || 'CNY',
    base: 'CNY',
    rates: [],
    loaded: false,
    loading: false
  }),
  actions: {
    async loadRates() {
      if (this.loaded || this.loading) return
      this.loading = true
      try {
        const data = await getExchangeRates()
        this.rates = data?.rates || []
        this.base = data?.base || 'CNY'
        if (typeof localStorage === 'undefined' || !localStorage.getItem(STORAGE_KEY)) {
          this.currency = this.base
        }
        this.loaded = true
      } catch (e) {
        // 汇率获取失败保持空列表，展示回退为基准币原样
      } finally {
        this.loading = false
      }
    },
    setCurrency(code) {
      this.currency = code
      localStorage.setItem(STORAGE_KEY, code)
    }
  }
})
