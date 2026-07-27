// 房间说明/须知多语言的纯逻辑：目标语种(base 中文除外) + 列表↔映射转换

export const I18N_LANGS = [
  { code: 'en', name: 'English' },
  { code: 'ja', name: '日本語' },
  { code: 'ko', name: '한국어' },
  { code: 'fr', name: 'Français' },
  { code: 'de', name: 'Deutsch' },
  { code: 'es', name: 'Español' },
  { code: 'pt', name: 'Português' },
  { code: 'ru', name: 'Русский' }
]

/** 后端 [{lang, description, bookingNote}] → { lang: {description, bookingNote} } */
export function toI18nMap(list) {
  const map = {}
  ;(list || []).forEach((it) => {
    if (it && it.lang) {
      map[it.lang] = { description: it.description || '', bookingNote: it.bookingNote || '' }
    }
  })
  return map
}

/** 是否已有任意语言的译文 */
export function hasAnyI18n(map) {
  return Object.values(map || {}).some(
    (v) => v && ((v.description && v.description.trim()) || (v.bookingNote && v.bookingNote.trim()))
  )
}

/** 语言码 → 展示名 */
export function langName(code) {
  return I18N_LANGS.find((l) => l.code === code)?.name || code
}
