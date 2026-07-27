import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import i18n, { setLang } from '@/i18n'
import UserLayout from '@/layout/UserLayout.vue'

const iconsPlugin = {
  install(app) {
    for (const [name, comp] of Object.entries(Icons)) {
      app.component(name, comp)
    }
  }
}

const Stub = { template: '<div />' }

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: Stub },
      { path: '/:pathMatch(.*)*', component: Stub }
    ]
  })
}

let wrapper

describe('UserLayout i18n', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  async function mountLayout() {
    const router = makeRouter()
    router.push('/')
    await router.isReady()
    return mount(UserLayout, {
      global: {
        plugins: [i18n, ElementPlus, createPinia(), router, iconsPlugin]
      }
    })
  }

  it('renders navigation copy in Chinese when locale is zh', async () => {
    setLang('zh')
    wrapper = await mountLayout()
    await nextTick()
    const text = wrapper.text()
    expect(text).toContain('首页')
    expect(text).toContain('房间信息')
    expect(text).toContain('公告信息')
    expect(text).toContain('在线客服')
    expect(text).toContain('个人中心')
  })

  it('renders navigation copy in English when locale is en', async () => {
    setLang('en')
    wrapper = await mountLayout()
    await nextTick()
    const text = wrapper.text()
    expect(text).toContain('Home')
    expect(text).toContain('Rooms')
    expect(text).toContain('Notices')
    expect(text).not.toContain('首页')
  })

  it('renders navigation copy in Japanese when locale is ja', async () => {
    setLang('ja')
    wrapper = await mountLayout()
    await nextTick()
    const text = wrapper.text()
    expect(text).toContain('ホーム')
    expect(text).toContain('客室情報')
    expect(text).not.toContain('首页')
    expect(text).not.toContain('Rooms')
  })

  it('reactively re-renders when locale changes at runtime', async () => {
    setLang('zh')
    wrapper = await mountLayout()
    await nextTick()
    expect(wrapper.text()).toContain('首页')
    setLang('en')
    await nextTick()
    expect(wrapper.text()).toContain('Home')
    expect(wrapper.text()).not.toContain('首页')
  })
})
