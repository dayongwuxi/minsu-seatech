import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import i18n, { setLang } from '@/i18n'

vi.mock('@stripe/stripe-js', () => ({
  loadStripe: vi.fn(() => Promise.resolve(null))
}))

vi.mock('@/api/payment', () => ({
  getPaymentConfig: vi.fn(),
  createStripeIntent: vi.fn(),
  createPayment: vi.fn(),
  createLinkTrustPayment: vi.fn(),
  getPaymentResult: vi.fn()
}))

vi.mock('@/api/paymethod', () => ({
  listPayMethods: vi.fn(() => Promise.resolve([]))
}))

vi.mock('@/api/booking', () => ({
  getBookingDetail: vi.fn(() =>
    Promise.resolve({
      booking: {
        orderNo: 'DD20300101TEST',
        checkinDate: '2030-01-01',
        checkoutDate: '2030-01-03',
        guestName: '张三',
        nights: 2,
        discountAmount: '0.00',
        totalAmount: '15000',
        payStatus: 0
      },
      room: { roomName: '湖景双床房' }
    })
  )
}))

import { getPaymentConfig, createLinkTrustPayment, createStripeIntent } from '@/api/payment'
import Payment from '@/views/Payment.vue'

const Stub = { template: '<div />' }
const ORDER_NO = 'DD20300101TEST'

let router

async function mountPayment() {
  router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pay/:orderNo', component: Stub },
      { path: '/:pathMatch(.*)*', component: Stub }
    ]
  })
  router.push(`/pay/${ORDER_NO}`)
  await router.isReady()
  const wrapper = mount(Payment, {
    global: { plugins: [i18n, ElementPlus, createPinia(), router] },
    attachTo: document.body
  })
  await flushPromises()
  return wrapper
}

let wrapper

describe('Payment LinkTrust 渠道分支', () => {
  beforeEach(() => {
    setLang('zh')
    getPaymentConfig.mockReset()
    createLinkTrustPayment.mockReset()
    createStripeIntent.mockReset()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('linktrustEnabled=true 时渲染跳转支付选项，隐藏模拟支付', async () => {
    getPaymentConfig.mockResolvedValue({
      stripeEnabled: false,
      linktrustEnabled: true,
      linktrustMethods: [1, 2, 7],
      currency: 'jpy'
    })
    wrapper = await mountPayment()
    await flushPromises()

    expect(wrapper.find('[data-test="linktrust-pay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="mock-pay"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="stripe-pay"]').exists()).toBe(false)
    // 三个方式：微信/支付宝/银联
    expect(wrapper.find('[data-test="linktrust-pay"]').text()).toContain('银联')
  })

  it('确认支付：GET 链接新标签页跳转收银台（表单 POST 会触发浏览器不安全提交拦截），本页转结果页', async () => {
    getPaymentConfig.mockResolvedValue({
      stripeEnabled: false,
      linktrustEnabled: true,
      linktrustMethods: [1, 2, 7],
      currency: 'jpy'
    })
    createLinkTrustPayment.mockResolvedValue({
      payUrl: 'http://pay.linktrust-pay.com/webpay/payform',
      merId: 'M810000022',
      merOrderId: 'SR20260724001',
      amount: '15000',
      payNo: 'SR20260724001'
    })
    // 点击时同步 window.open 保留用户手势（防弹窗拦截），拿到接口结果后再定位收银台 URL
    const fakeWin = { location: { replace: vi.fn() }, close: vi.fn() }
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(fakeWin)

    wrapper = await mountPayment()
    await flushPromises()
    const push = vi.spyOn(router, 'push')

    await wrapper.find('[data-test="linktrust-confirm"]').trigger('click')
    await flushPromises()

    // 默认选中第一个方式（微信=1）
    expect(createLinkTrustPayment).toHaveBeenCalledWith(ORDER_NO, 1)
    expect(openSpy).toHaveBeenCalled()
    const target = fakeWin.location.replace.mock.calls[0][0]
    expect(target).toContain('http://pay.linktrust-pay.com/webpay/payform?')
    expect(target).toContain('merId=M810000022')
    expect(target).toContain('merOrderId=SR20260724001')
    expect(target).toContain('amount=15000')
    expect(push).toHaveBeenCalledWith(`/pay-result/${ORDER_NO}`)
    openSpy.mockRestore()
  })

  it('接口失败时关闭预开标签页，不跳结果页', async () => {
    getPaymentConfig.mockResolvedValue({
      stripeEnabled: false,
      linktrustEnabled: true,
      linktrustMethods: [1],
      currency: 'jpy'
    })
    createLinkTrustPayment.mockRejectedValue(new Error('boom'))
    const fakeWin = { location: { replace: vi.fn() }, close: vi.fn() }
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(fakeWin)

    wrapper = await mountPayment()
    await flushPromises()
    const push = vi.spyOn(router, 'push')

    await wrapper.find('[data-test="linktrust-confirm"]').trigger('click')
    await flushPromises()

    expect(fakeWin.close).toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
    openSpy.mockRestore()
  })

  it('Stripe 与 LinkTrust 同时启用时两个渠道并列展示', async () => {
    getPaymentConfig.mockResolvedValue({
      stripeEnabled: true,
      publishableKey: 'pk_test_xyz',
      methods: ['card'],
      linktrustEnabled: true,
      linktrustMethods: [1, 2],
      currency: 'jpy'
    })
    createStripeIntent.mockResolvedValue({ clientSecret: 'cs_1', paymentIntentId: 'pi_1' })
    wrapper = await mountPayment()
    await flushPromises()

    expect(wrapper.find('[data-test="linktrust-pay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stripe-pay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="mock-pay"]').exists()).toBe(false)
  })

  it('两渠道均未启用时保留模拟支付', async () => {
    getPaymentConfig.mockResolvedValue({ stripeEnabled: false, linktrustEnabled: false })
    wrapper = await mountPayment()
    await flushPromises()

    expect(wrapper.find('[data-test="mock-pay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="linktrust-pay"]').exists()).toBe(false)
  })
})
