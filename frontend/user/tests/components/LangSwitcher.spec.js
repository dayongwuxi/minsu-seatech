import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import i18n, { SUPPORTED_LANGS, setLang } from '@/i18n'
import LangSwitcher from '@/components/LangSwitcher.vue'

let wrapper

function mountSwitcher() {
  return mount(LangSwitcher, {
    global: { plugins: [i18n, ElementPlus] },
    attachTo: document.body
  })
}

describe('LangSwitcher', () => {
  beforeEach(() => {
    localStorage.clear()
    setLang('zh')
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('renders a globe trigger', () => {
    wrapper = mountSwitcher()
    const trigger = wrapper.find('[data-test="lang-trigger"]')
    expect(trigger.exists()).toBe(true)
    expect(trigger.find('svg').exists()).toBe(true)
  })

  it('dropdown lists all 9 languages with their native names', async () => {
    wrapper = mountSwitcher()
    const dropdown = wrapper.findComponent({ name: 'ElDropdown' })
    expect(dropdown.exists()).toBe(true)
    dropdown.vm.handleOpen()
    await nextTick()
    await new Promise((r) => setTimeout(r, 20))
    await nextTick()
    const bodyText = document.body.textContent
    for (const { name } of SUPPORTED_LANGS) {
      expect(bodyText, `dropdown should contain native name ${name}`).toContain(name)
    }
    const items = document.querySelectorAll('.el-dropdown-menu__item')
    expect(items.length).toBe(9)
  })

  it('selecting a language switches i18n locale and persists it', async () => {
    wrapper = mountSwitcher()
    const dropdown = wrapper.findComponent({ name: 'ElDropdown' })
    dropdown.vm.$emit('command', 'ja')
    await nextTick()
    expect(i18n.global.locale.value).toBe('ja')
    expect(localStorage.getItem('user_lang')).toBe('ja')
    expect(document.documentElement.lang).toBe('ja')

    dropdown.vm.$emit('command', 'pt')
    await nextTick()
    expect(i18n.global.locale.value).toBe('pt')
    expect(localStorage.getItem('user_lang')).toBe('pt')
  })
})
