import { describe, it, expect, beforeEach } from 'vitest'
import i18n, { setLang } from '@/i18n'
import {
  PAY_STATUS,
  BOOKING_STATUS,
  PAY_METHOD,
  FEEDBACK_TYPE,
  FEEDBACK_STATUS,
  REVIEW_AUDIT_STATUS,
  dictText,
  dictTag,
  dictOptions
} from '@/utils/dict'

const t = (...args) => i18n.global.t(...args)

describe('dict i18n', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('payStatus 0 follows the active locale', () => {
    setLang('zh')
    expect(dictText(PAY_STATUS, 0, t)).toBe('待支付')
    setLang('en')
    expect(dictText(PAY_STATUS, 0, t)).toBe('Pending Payment')
    setLang('ja')
    expect(dictText(PAY_STATUS, 0, t)).toBe('支払い待ち')
  })

  it('bookingStatus values translate across locales', () => {
    setLang('zh')
    expect(dictText(BOOKING_STATUS, 4, t)).toBe('已取消')
    setLang('en')
    expect(dictText(BOOKING_STATUS, 4, t)).toBe('Cancelled')
    setLang('ko')
    expect(dictText(BOOKING_STATUS, 2, t)).toBe(t('dict.bookingStatus.checkedIn'))
  })

  it('every dict entry resolves to a translation in every locale (no raw keys leak)', () => {
    const dicts = { PAY_STATUS, BOOKING_STATUS, PAY_METHOD, FEEDBACK_TYPE, FEEDBACK_STATUS, REVIEW_AUDIT_STATUS }
    for (const code of ['zh', 'ja', 'en', 'de', 'fr', 'es', 'pt', 'ru', 'ko']) {
      setLang(code)
      for (const [name, dict] of Object.entries(dicts)) {
        for (const k of Object.keys(dict)) {
          const text = dictText(dict, Number(k), t)
          expect(text.length, `${code} ${name}[${k}] empty`).toBeGreaterThan(0)
          expect(text.startsWith('dict.'), `${code} ${name}[${k}] leaked raw key: ${text}`).toBe(false)
        }
      }
    }
  })

  it('unknown codes degrade gracefully', () => {
    setLang('en')
    expect(dictText(PAY_STATUS, 99, t)).toBe('99')
    expect(dictText(PAY_STATUS, null, t)).toBe('-')
    expect(dictText(PAY_STATUS, undefined, t)).toBe('-')
    expect(dictTag(PAY_STATUS, 99)).toBe('info')
  })

  it('dictOptions returns numeric values with translated labels', () => {
    setLang('en')
    const opts = dictOptions(PAY_METHOD, t)
    // 1微信 2支付宝 3银行卡 4其他 7银联(LinkTrust)
    expect(opts).toHaveLength(5)
    expect(opts.map((o) => o.value)).toEqual([1, 2, 3, 4, 7])
    for (const o of opts) {
      expect(typeof o.label).toBe('string')
      expect(/[一-鿿]/.test(o.label)).toBe(false)
    }
  })
})
