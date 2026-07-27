import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/currency', () => ({
  getExchangeRates: vi.fn()
}))

import { getExchangeRates } from '@/api/currency'
import { useCurrencyStore } from '@/store/currency'
import { useCurrency } from '@/composables/useCurrency'

const RATES = [
  { code: 'USD', name: '美元', symbol: '$', rate: 0.14 },
  { code: 'EUR', name: '欧元', symbol: '€', rate: 0.12 },
  { code: 'JPY', name: '日元', symbol: 'JP¥', rate: 14.28 }
]

describe('currency store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    getExchangeRates.mockReset()
    getExchangeRates.mockResolvedValue({ base: 'CNY', rates: RATES })
  })

  it('defaults to CNY when nothing is stored', () => {
    const store = useCurrencyStore()
    expect(store.currency).toBe('CNY')
  })

  it('restores the stored currency from localStorage', () => {
    localStorage.setItem('user_currency', 'EUR')
    setActivePinia(createPinia())
    const store = useCurrencyStore()
    expect(store.currency).toBe('EUR')
  })

  it('setCurrency updates state and persists to localStorage', () => {
    const store = useCurrencyStore()
    store.setCurrency('USD')
    expect(store.currency).toBe('USD')
    expect(localStorage.getItem('user_currency')).toBe('USD')
  })

  it('loadRates fetches once and caches', async () => {
    const store = useCurrencyStore()
    await store.loadRates()
    await store.loadRates()
    expect(getExchangeRates).toHaveBeenCalledTimes(1)
    expect(store.rates).toHaveLength(3)
  })
})

describe('useCurrency format', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('formats CNY amounts with the yuan sign', () => {
    const { format } = useCurrency()
    expect(format(100)).toBe('¥100.00')
  })

  it('non-CNY displays integer only: rate 0.14 → ¥100 = $14 (no decimals)', () => {
    const store = useCurrencyStore()
    store.rates = [...RATES]
    store.setCurrency('USD')
    const { format } = useCurrency()
    expect(format(100)).toBe('$14')
  })

  it('rounds up (ceiling) when conversion is not an integer: ¥388 × 0.14 = 54.32 → $55', () => {
    const store = useCurrencyStore()
    store.rates = [...RATES]
    store.setCurrency('USD')
    const { format } = useCurrency()
    expect(format(388)).toBe('$55')
    // 边界：刚好整数不加 1
    expect(format(100)).toBe('$14')
    // 极小非整数也向上取整：10 × 0.14 = 1.4 → 2
    expect(format(10)).toBe('$2')
  })

  it('JPY also integer + ceiling with grouping: ¥1000 → JP¥14,280；非整数向上取整', () => {
    const store = useCurrencyStore()
    store.rates = [...RATES]
    store.setCurrency('JPY')
    const { format } = useCurrency()
    expect(format(1000)).toBe('JP¥14,280')
    // 101 × 14.28 = 1442.28 → 1443
    expect(format(101)).toBe('JP¥1,443')
  })

  it('falls back to the raw CNY display when rates are not loaded', () => {
    const store = useCurrencyStore()
    store.setCurrency('USD')
    // rates 为空 → 原样 ¥ 展示
    const { format } = useCurrency()
    expect(format(100)).toBe('¥100.00')
  })

  it('CNY base keeps two decimals', () => {
    const { format } = useCurrency()
    expect(format(100)).toBe('¥100.00')
    expect(format(99.9)).toBe('¥99.90')
  })

  it('handles string amounts and invalid input gracefully', () => {
    const store = useCurrencyStore()
    store.rates = [...RATES]
    store.setCurrency('USD')
    const { format } = useCurrency()
    // '200.00' × 0.14 = 28 → $28（整数）
    expect(format('200.00')).toBe('$28')
    expect(format(undefined)).toContain('¥')
  })
})

describe('JPY 基准站点（PAYMENT_CURRENCY=jpy 时汇率接口 base=JPY）', () => {
  const JPY_RATES = [
    { code: 'CNY', name: '人民币', symbol: '¥', rate: 0.0414 },
    { code: 'USD', name: '美元', symbol: '$', rate: 0.0061 },
    { code: 'JPY', name: '日元', symbol: 'JP¥', rate: 1 }
  ]

  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    getExchangeRates.mockReset()
    getExchangeRates.mockResolvedValue({ base: 'JPY', rates: JPY_RATES })
  })

  it('无本地选择时默认展示货币跟随基准币', async () => {
    const store = useCurrencyStore()
    await store.loadRates()
    expect(store.base).toBe('JPY')
    expect(store.currency).toBe('JPY')
  })

  it('基准币为零小数货币时整数展示，符号取自汇率表', async () => {
    const store = useCurrencyStore()
    await store.loadRates()
    const { format, isForeign } = useCurrency()
    expect(format(16400)).toBe('JP¥16,400')
    expect(isForeign.value).toBe(false)
  })

  it('切到 CNY 按汇率换算并整数向上取整', async () => {
    const store = useCurrencyStore()
    await store.loadRates()
    store.setCurrency('CNY')
    const { format, isForeign } = useCurrency()
    // 16400×0.0414=678.96 → 679
    expect(format(16400)).toBe('¥679')
    expect(isForeign.value).toBe(true)
  })

  it('本地已存的展示货币选择不被基准覆盖', async () => {
    localStorage.setItem('user_currency', 'USD')
    setActivePinia(createPinia())
    const store = useCurrencyStore()
    await store.loadRates()
    expect(store.currency).toBe('USD')
  })
})
