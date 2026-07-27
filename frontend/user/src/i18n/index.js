import { createI18n } from 'vue-i18n'
import zh from './locales/zh'
import ja from './locales/ja'
import en from './locales/en'
import de from './locales/de'
import fr from './locales/fr'
import es from './locales/es'
import pt from './locales/pt'
import ru from './locales/ru'
import ko from './locales/ko'

// 语言切换菜单展示各语言的本族名称
export const SUPPORTED_LANGS = [
  { code: 'zh', name: '中文' },
  { code: 'ja', name: '日本語' },
  { code: 'en', name: 'English' },
  { code: 'de', name: 'Deutsch' },
  { code: 'fr', name: 'Français' },
  { code: 'es', name: 'Español' },
  { code: 'pt', name: 'Português' },
  { code: 'ru', name: 'Русский' },
  { code: 'ko', name: '한국어' }
]

const CODES = SUPPORTED_LANGS.map((l) => l.code)
const STORAGE_KEY = 'user_lang'

/**
 * 默认语言解析：localStorage 优先 > navigator.language 前缀匹配（zh-CN→zh、pt-BR→pt）> fallback en
 */
export function resolveDefaultLang(stored, nav) {
  if (stored && CODES.includes(stored)) return stored
  if (nav) {
    const prefix = String(nav).toLowerCase().split('-')[0]
    if (CODES.includes(prefix)) return prefix
  }
  return 'en'
}

const initialLocale = resolveDefaultLang(
  typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null,
  typeof navigator !== 'undefined' ? navigator.language : null
)

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: initialLocale,
  fallbackLocale: 'en',
  messages: { zh, ja, en, de, fr, es, pt, ru, ko }
})

if (typeof document !== 'undefined') {
  document.documentElement.lang = initialLocale
}

/** 切换语言：更新 locale + localStorage 持久化 + <html lang> */
export function setLang(code) {
  if (!CODES.includes(code)) return
  i18n.global.locale.value = code
  localStorage.setItem(STORAGE_KEY, code)
  document.documentElement.lang = code
}

export default i18n
