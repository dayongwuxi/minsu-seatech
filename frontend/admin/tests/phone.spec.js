import { describe, it, expect } from 'vitest'
import { fullPhone } from '@/utils/phone'

describe('fullPhone 带国家区号的完整号码展示', () => {
  it('有区号时拼接展示', () => {
    expect(fullPhone({ phoneCountry: '+81', phone: '08012345678' })).toBe('+81 08012345678')
    expect(fullPhone({ phoneCountry: '+86', phone: '13800138000' })).toBe('+86 13800138000')
  })

  it('存量用户无区号时仅显示号码', () => {
    expect(fullPhone({ phoneCountry: null, phone: '13800138000' })).toBe('13800138000')
    expect(fullPhone({ phone: '13800138000' })).toBe('13800138000')
  })

  it('空值安全', () => {
    expect(fullPhone(null)).toBe('-')
    expect(fullPhone({})).toBe('-')
    expect(fullPhone({ phoneCountry: '+81' })).toBe('-')
  })
})
