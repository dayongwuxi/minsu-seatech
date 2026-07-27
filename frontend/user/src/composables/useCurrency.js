import { computed } from 'vue'
import { useCurrencyStore } from '@/store/currency'

function fmtNum(n, digits) {
  return n.toLocaleString('en-US', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  })
}

// 零小数货币：金额无小数位（基准币为这些币种时按整数展示）
const ZERO_DECIMAL = new Set(['JPY', 'KRW', 'VND'])

/**
 * 全站价格换算与格式化（基准币 = 站点结算货币，来自汇率接口 base）：
 * - format(amountBase)：基准币按其符号展示（零小数货币整数，其余 2 位小数）；
 *   切换到其他币种时一律只显示整数位，换算出现非整数时向上取整(ceiling)，
 *   符号前置并加千分位（如 $58 / JP¥14,280）
 * - rates 未加载时按基准币回退展示
 */
export function useCurrency() {
  const store = useCurrencyStore()

  const currency = computed(() => store.currency)
  const base = computed(() => store.base)
  const isForeign = computed(() => store.currency !== store.base)

  function baseFormat(n) {
    const entry = store.rates.find((r) => r.code === store.base)
    const symbol = entry?.symbol || '¥'
    const digits = ZERO_DECIMAL.has(store.base) ? 0 : 2
    return `${symbol}${fmtNum(n, digits)}`
  }

  function format(amountBase) {
    const n = Number(amountBase)
    if (!isFinite(n)) return `¥${amountBase ?? '-'}`
    if (store.currency === store.base) return baseFormat(n)
    const entry = store.rates.find((r) => r.code === store.currency)
    if (!entry || !Number(entry.rate)) return baseFormat(n)
    // 非基准币种一律整数显示；换算出现非整数时向上取整(ceiling)。
    // 先减去极小 epsilon 抵消浮点误差（如 100×0.14=14.000000000000002），避免整数被误进位。
    const raw = n * Number(entry.rate)
    const converted = Math.max(0, Math.ceil(raw - 1e-9))
    return `${entry.symbol || entry.code}${fmtNum(converted, 0)}`
  }

  return { format, currency, base, isForeign }
}
