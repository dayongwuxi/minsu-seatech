import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import i18n, { setLang } from '@/i18n'

vi.mock('@/api/payment', () => ({
  getPaymentConfig: vi.fn(),
  createStripeIntent: vi.fn(),
  createPayment: vi.fn(),
  createLinkTrustPayment: vi.fn(),
  getPaymentResult: vi.fn()
}))

import { getPaymentResult } from '@/api/payment'
import PayResult from '@/views/PayResult.vue'

const Stub = { template: '<div />' }
const ORDER_NO = 'DD20300101TEST'

async function mountResult() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pay-result/:orderNo', component: Stub },
      { path: '/:pathMatch(.*)*', component: Stub }
    ]
  })
  router.push(`/pay-result/${ORDER_NO}`)
  await router.isReady()
  const wrapper = mount(PayResult, {
    global: { plugins: [i18n, ElementPlus, createPinia(), router] },
    attachTo: document.body
  })
  await flushPromises()
  return wrapper
}

async function exhaustPolling() {
  // MAX_ATTEMPTS=10, POLL_INTERVAL=2000：轮询打满
  for (let i = 0; i < 12; i++) {
    await flushPromises()
    vi.advanceTimersByTime(2000)
  }
  await flushPromises()
}

let wrapper

describe('PayResult 外跳支付待确认态', () => {
  beforeEach(() => {
    setLang('zh')
    vi.useFakeTimers()
    getPaymentResult.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('轮询打满仍待支付：显示「我已完成支付，刷新」而非误报失败', async () => {
    getPaymentResult.mockResolvedValue({ orderNo: ORDER_NO, payStatus: 0 })
    wrapper = await mountResult()
    await exhaustPolling()

    expect(wrapper.find('[data-test="pending-final"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="recheck"]').exists()).toBe(true)
    // 不展示失败结果
    expect(wrapper.text()).not.toContain('支付未完成')
  })

  it('点击刷新按钮重新轮询，拿到成功后展示支付成功', async () => {
    getPaymentResult.mockResolvedValue({ orderNo: ORDER_NO, payStatus: 0 })
    wrapper = await mountResult()
    await exhaustPolling()

    getPaymentResult.mockResolvedValue({
      orderNo: ORDER_NO,
      payStatus: 1,
      amount: '15000',
      payMethod: 7
    })
    await wrapper.find('[data-test="recheck"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="pending-final"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('支付成功')
  })

  it('支付成功正常展示（原有行为不回归）', async () => {
    getPaymentResult.mockResolvedValue({
      orderNo: ORDER_NO,
      payStatus: 1,
      amount: '200.00',
      payMethod: 1
    })
    wrapper = await mountResult()
    await flushPromises()
    expect(wrapper.text()).toContain('支付成功')
    expect(wrapper.find('[data-test="pending-final"]').exists()).toBe(false)
  })
})
