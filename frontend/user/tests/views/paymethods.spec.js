import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import i18n, { setLang } from '@/i18n'

vi.mock('@/api/paymethod', () => ({
  listPayMethods: vi.fn(),
  createSetupIntent: vi.fn(),
  savePayMethod: vi.fn(),
  setDefaultPayMethod: vi.fn(),
  removePayMethod: vi.fn()
}))

vi.mock('@/api/payment', () => ({
  getPaymentConfig: vi.fn(() => Promise.resolve({ stripeEnabled: false })),
  createStripeIntent: vi.fn(),
  createPayment: vi.fn(),
  getPaymentResult: vi.fn()
}))

vi.mock('@stripe/stripe-js', () => ({
  loadStripe: vi.fn()
}))

import { listPayMethods, createSetupIntent, savePayMethod, setDefaultPayMethod } from '@/api/paymethod'
import PayMethods from '@/views/user/PayMethods.vue'

const CARDS = [
  { id: 1, brand: 'visa', last4: '4242', expMonth: 12, expYear: 2030, holderName: 'ZHANG SAN', isDefault: 1, channel: 2 },
  { id: 2, brand: 'mastercard', last4: '4444', expMonth: 6, expYear: 2028, holderName: 'LI SI', isDefault: 0, channel: 1 }
]

const Stub = { template: '<div />' }

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/user/pay-methods', component: Stub },
      { path: '/:pathMatch(.*)*', component: Stub }
    ]
  })
  router.push('/user/pay-methods')
  await router.isReady()
  const wrapper = mount(PayMethods, {
    global: { plugins: [i18n, ElementPlus, createPinia(), router] },
    attachTo: document.body
  })
  await flushPromises()
  return wrapper
}

let wrapper

describe('PayMethods', () => {
  beforeEach(() => {
    setLang('zh')
    listPayMethods.mockReset()
    createSetupIntent.mockReset()
    savePayMethod.mockReset()
    setDefaultPayMethod.mockReset()
    listPayMethods.mockResolvedValue([...CARDS])
    savePayMethod.mockResolvedValue(null)
    setDefaultPayMethod.mockResolvedValue(null)
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('renders the saved card list with brand, masked last4, expiry and default tag', async () => {
    wrapper = await mountView()
    const text = wrapper.text()
    expect(text).toContain('4242')
    expect(text).toContain('4444')
    expect(text.toUpperCase()).toContain('VISA')
    expect(text).toContain('****')
    expect(text).toContain('12/30')
    // 默认标签只出现在默认卡上
    expect(text).toContain('默认')
    const items = wrapper.findAll('[data-test="pm-item"]')
    expect(items).toHaveLength(2)
  })

  it('set-default button calls the API with the card id', async () => {
    wrapper = await mountView()
    // 只有非默认卡显示「设为默认」
    const buttons = wrapper.findAll('[data-test="pm-set-default"]')
    expect(buttons).toHaveLength(1)
    await buttons[0].trigger('click')
    await flushPromises()
    expect(setDefaultPayMethod).toHaveBeenCalledWith(2)
  })

  it('mock mode: submits only sanitized fields, never the full card number or CVC', async () => {
    createSetupIntent.mockResolvedValue({ mock: true })
    wrapper = await mountView()

    await wrapper.find('[data-test="pm-add"]').trigger('click')
    await flushPromises()
    // mock:true → 展示本地表单并标注模拟模式
    expect(wrapper.find('[data-test="pm-mock-form"]').exists()).toBe(true)

    await wrapper.find('[data-test="pm-card-number"] input').setValue('4242 4242 4242 4242')
    await wrapper.find('[data-test="pm-holder"] input').setValue('ZHANG SAN')
    await wrapper.find('[data-test="pm-expiry"] input').setValue('12/30')
    await wrapper.find('[data-test="pm-cvc"] input').setValue('123')
    await wrapper.find('[data-test="pm-submit"]').trigger('click')
    await flushPromises()

    expect(savePayMethod).toHaveBeenCalledTimes(1)
    const payload = savePayMethod.mock.calls[0][0]
    expect(payload).toEqual({
      brand: 'visa',
      last4: '4242',
      expMonth: 12,
      expYear: 2030,
      holderName: 'ZHANG SAN'
    })
    expect(payload.cardNumber).toBeUndefined()
    expect(payload.cvc).toBeUndefined()
    expect(JSON.stringify(payload)).not.toContain('4242424242424242')
  })

  it('mock mode: rejects a card number failing the Luhn check', async () => {
    createSetupIntent.mockResolvedValue({ mock: true })
    wrapper = await mountView()

    await wrapper.find('[data-test="pm-add"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-test="pm-card-number"] input').setValue('4242 4242 4242 4243')
    await wrapper.find('[data-test="pm-holder"] input').setValue('ZHANG SAN')
    await wrapper.find('[data-test="pm-expiry"] input').setValue('12/30')
    await wrapper.find('[data-test="pm-cvc"] input').setValue('123')
    await wrapper.find('[data-test="pm-submit"]').trigger('click')
    await flushPromises()

    expect(savePayMethod).not.toHaveBeenCalled()
  })
})
