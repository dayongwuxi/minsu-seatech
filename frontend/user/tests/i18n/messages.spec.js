import { describe, it, expect } from 'vitest'
import zh from '@/i18n/locales/zh'
import ja from '@/i18n/locales/ja'
import en from '@/i18n/locales/en'
import de from '@/i18n/locales/de'
import fr from '@/i18n/locales/fr'
import es from '@/i18n/locales/es'
import pt from '@/i18n/locales/pt'
import ru from '@/i18n/locales/ru'
import ko from '@/i18n/locales/ko'

const locales = { zh, ja, en, de, fr, es, pt, ru, ko }

function flatten(obj, prefix = '', out = {}) {
  for (const [k, v] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${k}` : k
    if (v !== null && typeof v === 'object') {
      flatten(v, path, out)
    } else {
      out[path] = v
    }
  }
  return out
}

const flatZh = flatten(zh)
const zhKeys = Object.keys(flatZh).sort()

describe('locale message files', () => {
  it('all 9 locales exist and are plain objects', () => {
    for (const [code, messages] of Object.entries(locales)) {
      expect(messages, `locale ${code} should be an object`).toBeTypeOf('object')
      expect(Object.keys(messages).length, `locale ${code} should not be empty`).toBeGreaterThan(0)
    }
  })

  it.each(Object.keys(locales))('locale "%s" has exactly the same key set as zh', (code) => {
    const keys = Object.keys(flatten(locales[code])).sort()
    const missing = zhKeys.filter((k) => !keys.includes(k))
    const extra = keys.filter((k) => !zhKeys.includes(k))
    expect(missing, `locale ${code} is missing keys: ${missing.join(', ')}`).toEqual([])
    expect(extra, `locale ${code} has extra keys: ${extra.join(', ')}`).toEqual([])
  })

  it.each(Object.keys(locales))('locale "%s" has non-empty string leaves only', (code) => {
    const flat = flatten(locales[code])
    for (const [path, value] of Object.entries(flat)) {
      expect(typeof value, `${code}.${path} should be a string`).toBe('string')
      expect(value.trim().length, `${code}.${path} should not be empty`).toBeGreaterThan(0)
    }
  })

  // 品牌 wordmark 词条不翻译（Yadogo Trip），豁免 zh 的 CJK 检查
  const BRAND_KEYS = new Set(['nav.brand', 'footer.systemName'])

  it('zh has no untranslated pure-ASCII placeholders (every leaf contains CJK)', () => {
    for (const [path, value] of Object.entries(flatZh)) {
      if (BRAND_KEYS.has(path)) continue
      expect(
        /[一-鿿]/.test(value),
        `zh.${path} = "${value}" contains no Chinese characters (looks like an English placeholder)`
      ).toBe(true)
    }
  })

  it('brand wordmark is "Yadogo Trip" and identical across all 9 locales', () => {
    for (const [code, messages] of Object.entries(locales)) {
      const flat = flatten(messages)
      expect(flat['nav.brand'], `${code}.nav.brand`).toBe('Yadogo Trip')
      expect(flat['footer.systemName'], `${code}.footer.systemName`).toBe('Yadogo Trip')
      expect(flat['footer.slogan'], `${code}.footer.slogan`).toContain('Yadogo Trip')
    }
  })

  it('en has no residual Chinese characters', () => {
    const flatEn = flatten(en)
    for (const [path, value] of Object.entries(flatEn)) {
      expect(
        /[一-鿿]/.test(value),
        `en.${path} = "${value}" still contains Chinese characters`
      ).toBe(false)
    }
  })
})
