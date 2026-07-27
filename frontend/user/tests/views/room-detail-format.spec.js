import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import i18n, { setLang } from '@/i18n'

// 房间详情中会调用的接口全部 mock；只关注说明/须知的排版渲染
vi.mock('@/api/room', () => ({
  getRoomDetail: vi.fn(),
  getRoomReviews: vi.fn(() => Promise.resolve({ records: [], total: 0 })),
  addFavorite: vi.fn(),
  removeFavorite: vi.fn()
}))

vi.mock('@/api/promotion', () => ({
  getActivePromotions: vi.fn(() => Promise.resolve([]))
}))

import { getRoomDetail } from '@/api/room'
import RoomDetail from '@/views/RoomDetail.vue'

const Stub = { template: '<div />' }

// 含换行 + HTML 注入串：用于验证「按纯文本渲染 + 保留换行」，而非 v-html
const DESCRIPTION = '安静整洁的房间。\n步行 5 分钟到海滩。\n<script>alert(1)</script>'
const BOOKING_NOTE = '入住时间 14:00\n退房时间 12:00'

async function mountDetail() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/room/:id', component: Stub },
      { path: '/:pathMatch(.*)*', component: Stub }
    ]
  })
  router.push('/room/1')
  await router.isReady()
  const wrapper = mount(RoomDetail, {
    global: { plugins: [i18n, ElementPlus, createPinia(), router] },
    attachTo: document.body
  })
  await flushPromises()
  return wrapper
}

let wrapper

describe('RoomDetail 说明/须知排版', () => {
  beforeEach(() => {
    setLang('zh')
    getRoomDetail.mockReset()
    getRoomDetail.mockResolvedValue({
      room: {
        roomName: '海景大床房',
        price: '388.00',
        description: DESCRIPTION,
        bookingNote: BOOKING_NOTE,
        facilities: ''
      },
      typeName: '大床房',
      images: [],
      avgRating: 0,
      reviewCount: 0
    })
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('房间说明以纯文本渲染并保留换行', async () => {
    wrapper = await mountDetail()
    const desc = wrapper.findAll('.detail-desc')[0]
    expect(desc.exists()).toBe(true)
    // 换行被保留在文本内容中
    expect(desc.text()).toContain('安静整洁的房间。')
    expect(desc.text()).toContain('步行 5 分钟到海滩。')
    expect(desc.element.textContent).toContain('\n')
  })

  it('房间说明不使用 v-html：注入的标签作为文本而非元素渲染', async () => {
    wrapper = await mountDetail()
    const desc = wrapper.findAll('.detail-desc')[0]
    // 注入的 <script> 应作为字面文本出现，且不会被解析为真实 DOM 元素
    expect(desc.text()).toContain('<script>alert(1)</script>')
    expect(desc.element.querySelector('script')).toBeNull()
  })

  it('预订须知同样保留换行文本', async () => {
    wrapper = await mountDetail()
    const notes = wrapper.findAll('.detail-desc')
    // 第二个 .detail-desc 为预订须知
    const note = notes[notes.length - 1]
    expect(note.text()).toContain('入住时间 14:00')
    expect(note.text()).toContain('退房时间 12:00')
    expect(note.element.textContent).toContain('\n')
  })
})

describe('RoomDetail 多语言说明按站点语言切换', () => {
  beforeEach(() => {
    setLang('zh')
    getRoomDetail.mockReset()
    getRoomDetail.mockResolvedValue({
      room: { roomName: '海景大床房', price: '388.00', description: '中文说明', bookingNote: '中文须知', facilities: '' },
      typeName: '大床房',
      images: [],
      i18n: [
        { lang: 'en', description: 'English description', bookingNote: 'English note' },
        { lang: 'ja', description: '日本語の説明', bookingNote: '日本語の案内' }
      ],
      avgRating: 0,
      reviewCount: 0
    })
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
    setLang('zh')
  })

  it('zh 显示中文，切到 en 即时显示英文译文', async () => {
    wrapper = await mountDetail()
    expect(wrapper.findAll('.detail-desc')[0].text()).toContain('中文说明')

    setLang('en')
    await flushPromises()
    expect(wrapper.findAll('.detail-desc')[0].text()).toContain('English description')
    expect(wrapper.findAll('.detail-desc')[1].text()).toContain('English note')
  })

  it('切到无译文的语言回退中文 base', async () => {
    wrapper = await mountDetail()
    setLang('fr') // 无 fr 译文
    await flushPromises()
    expect(wrapper.findAll('.detail-desc')[0].text()).toContain('中文说明')
  })
})
