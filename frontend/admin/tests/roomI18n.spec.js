import { describe, it, expect } from 'vitest'
import { I18N_LANGS, toI18nMap, hasAnyI18n, langName } from '@/utils/roomI18n'

describe('I18N_LANGS', () => {
  it('是 base 中文之外的 8 个目标语种', () => {
    expect(I18N_LANGS.map((l) => l.code).sort()).toEqual(
      ['de', 'en', 'es', 'fr', 'ja', 'ko', 'pt', 'ru']
    )
    expect(I18N_LANGS.map((l) => l.code)).not.toContain('zh')
  })
})

describe('toI18nMap', () => {
  it('列表转 {lang: {description, bookingNote}}', () => {
    const map = toI18nMap([
      { lang: 'en', description: 'Sea view', bookingNote: 'Check-in 14:00' },
      { lang: 'ja', description: '海の眺め', bookingNote: null }
    ])
    expect(map.en).toEqual({ description: 'Sea view', bookingNote: 'Check-in 14:00' })
    expect(map.ja).toEqual({ description: '海の眺め', bookingNote: '' })
  })

  it('空/无效输入返回空对象', () => {
    expect(toI18nMap(null)).toEqual({})
    expect(toI18nMap([{ description: 'x' }])).toEqual({}) // 无 lang 跳过
  })
})

describe('hasAnyI18n', () => {
  it('有非空译文为 true', () => {
    expect(hasAnyI18n({ en: { description: 'x', bookingNote: '' } })).toBe(true)
  })
  it('全空为 false', () => {
    expect(hasAnyI18n({})).toBe(false)
    expect(hasAnyI18n({ en: { description: '', bookingNote: '  ' } })).toBe(false)
  })
})

describe('langName', () => {
  it('映射语言码到展示名', () => {
    expect(langName('ja')).toBe('日本語')
    expect(langName('unknown')).toBe('unknown')
  })
})
