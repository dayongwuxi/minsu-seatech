import { describe, it, expect, beforeEach } from 'vitest'
import i18n, { SUPPORTED_LANGS, resolveDefaultLang, setLang } from '@/i18n'

describe('SUPPORTED_LANGS', () => {
  it('contains exactly the 9 required languages with native display names', () => {
    const expected = {
      zh: '中文',
      ja: '日本語',
      en: 'English',
      de: 'Deutsch',
      fr: 'Français',
      es: 'Español',
      pt: 'Português',
      ru: 'Русский',
      ko: '한국어'
    }
    expect(SUPPORTED_LANGS).toHaveLength(9)
    for (const { code, name } of SUPPORTED_LANGS) {
      expect(expected[code], `unexpected language code ${code}`).toBe(name)
    }
  })
})

describe('resolveDefaultLang', () => {
  it('prefers a valid stored language over navigator.language', () => {
    expect(resolveDefaultLang('ja', 'en-US')).toBe('ja')
    expect(resolveDefaultLang('ru', 'zh-CN')).toBe('ru')
  })

  it('falls back to navigator.language prefix matching when nothing stored', () => {
    expect(resolveDefaultLang(null, 'zh-CN')).toBe('zh')
    expect(resolveDefaultLang(null, 'pt-BR')).toBe('pt')
    expect(resolveDefaultLang(null, 'ko-KR')).toBe('ko')
    expect(resolveDefaultLang('', 'de')).toBe('de')
  })

  it('ignores invalid stored values', () => {
    expect(resolveDefaultLang('xx', 'fr-FR')).toBe('fr')
  })

  it('falls back to en when nothing matches', () => {
    expect(resolveDefaultLang(null, 'it-IT')).toBe('en')
    expect(resolveDefaultLang(undefined, undefined)).toBe('en')
    expect(resolveDefaultLang('xx', 'xx-XX')).toBe('en')
  })
})

describe('setLang', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('updates i18n locale, localStorage and document lang', () => {
    setLang('ko')
    expect(i18n.global.locale.value).toBe('ko')
    expect(localStorage.getItem('user_lang')).toBe('ko')
    expect(document.documentElement.lang).toBe('ko')

    setLang('de')
    expect(i18n.global.locale.value).toBe('de')
    expect(localStorage.getItem('user_lang')).toBe('de')
    expect(document.documentElement.lang).toBe('de')
  })

  it('ignores unsupported language codes', () => {
    setLang('zh')
    setLang('klingon')
    expect(i18n.global.locale.value).toBe('zh')
    expect(localStorage.getItem('user_lang')).toBe('zh')
  })
})
