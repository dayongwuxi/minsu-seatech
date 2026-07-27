import { describe, it, expect } from 'vitest'
import { COUNTRY_CODES, filterCountries, flagEmoji, dialOf } from '@/utils/countryCodes'

describe('国家区号数据', () => {
  it('覆盖主要国家且 iso 唯一', () => {
    const isos = COUNTRY_CODES.map((c) => c.iso)
    expect(new Set(isos).size).toBe(isos.length)
    expect(dialOf('JP')).toBe('+81')
    expect(dialOf('CN')).toBe('+86')
    expect(dialOf('US')).toBe('+1')
    expect(dialOf('KR')).toBe('+82')
    expect(COUNTRY_CODES.length).toBeGreaterThan(200)
  })

  it('每条记录含 iso/en/zh/dial 且区号形如 +数字', () => {
    for (const c of COUNTRY_CODES) {
      expect(c.iso).toMatch(/^[A-Z]{2}$/)
      expect(c.en.length).toBeGreaterThan(0)
      expect(c.zh.length).toBeGreaterThan(0)
      expect(c.dial).toMatch(/^\+\d{1,4}$/)
    }
  })
})

describe('filterCountries 国家名搜索', () => {
  it('按英文名搜索（不区分大小写）', () => {
    const r = filterCountries('japan')
    expect(r[0].iso).toBe('JP')
  })

  it('按中文名搜索', () => {
    expect(filterCountries('日本')[0].iso).toBe('JP')
    expect(filterCountries('美国')[0].iso).toBe('US')
  })

  it('按区号搜索（带不带 + 均可）', () => {
    expect(filterCountries('+81').some((c) => c.iso === 'JP')).toBe(true)
    expect(filterCountries('81').some((c) => c.iso === 'JP')).toBe(true)
  })

  it('空查询返回全量', () => {
    expect(filterCountries('')).toHaveLength(COUNTRY_CODES.length)
    expect(filterCountries('  ')).toHaveLength(COUNTRY_CODES.length)
  })

  it('无匹配返回空数组', () => {
    expect(filterCountries('zzzzzz')).toEqual([])
  })
})

describe('flagEmoji', () => {
  it('ISO 转国旗 emoji', () => {
    expect(flagEmoji('JP')).toBe('🇯🇵')
    expect(flagEmoji('CN')).toBe('🇨🇳')
  })
})
