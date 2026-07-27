import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import i18n, { setLang } from '@/i18n'

const paymentElementMock = { mount: vi.fn(), on: vi.fn(), unmount: vi.fn() }
const elementsMock = { create: vi.fn(() => paymentElementMock) }
const stripeMock = {
  elements: vi.fn(() => elementsMock),
  confirmPayment: vi.fn(() => Promise.resolve({}))
}

vi.mock('@stripe/stripe-js', () => ({
  loadStripe: vi.fn(() => Promise.resolve(stripeMock))
}))

vi.mock('@/api/payment', () => ({
  getPaymentConfig: vi.fn(),
  createStripeIntent: vi.fn(),
  createPayment: vi.fn(),
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
        totalAmount: '200.00',
        payStatus: 0
      },
      room: { roomName: '湖景双床房' }
    })
  )
}))

import { loadStripe } from '@stripe/stripe-js'
import { getPaymentConfig, createStripeIntent } from '@/api/payment'
import Payment from '@/views/Payment.vue'

const Stub = { template: '<div />' }
const ORDER_NO = 'DD20300101TEST'

async function mountPayment() {
  const router = createRouter({
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

describe('Payment stripeEnabled branches', () => {
  beforeEach(() => {
    setLang('zh')
    getPaymentConfig.mockReset()
    createStripeIntent.mockReset()
    loadStripe.mockClear()
    stripeMock.elements.mockClear()
    stripeMock.confirmPayment.mockClear()
    elementsMock.create.mockClear()
    paymentElementMock.mount.mockClear()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('renders the mock payment UI when stripeEnabled=false', async () => {
    getPaymentConfig.mockResolvedValue({ stripeEnabled: false, publishableKey: null, methods: [], currency: 'cny' })
    wrapper = await mountPayment()
    await flushPromises()

    expect(getPaymentConfig).toHaveBeenCalled()
    expect(wrapper.find('[data-test="mock-pay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stripe-pay"]').exists()).toBe(false)
    expect(createStripeIntent).not.toHaveBeenCalled()
    expect(loadStripe).not.toHaveBeenCalled()
  })

  it('creates an intent and mounts the Payment Element when stripeEnabled=true', async () => {
    getPaymentConfig.mockResolvedValue({
      stripeEnabled: true,
      publishableKey: 'pk_test_xyz',
      methods: ['card', 'alipay', 'wechat_pay'],
      currency: 'cny'
    })
    createStripeIntent.mockResolvedValue({
      clientSecret: 'cs_test_123',
      paymentIntentId: 'pi_1',
      amount: 200,
      currency: 'cny'
    })

    wrapper = await mountPayment()
    await flushPromises()
    await vi.waitFor(() => expect(paymentElementMock.mount).toHaveBeenCalled())

    expect(createStripeIntent).toHaveBeenCalledWith(ORDER_NO)
    expect(loadStripe).toHaveBeenCalledWith('pk_test_xyz')
    expect(stripeMock.elements).toHaveBeenCalledWith(expect.objectContaining({ clientSecret: 'cs_test_123' }))
    expect(elementsMock.create).toHaveBeenCalledWith('payment', expect.anything())

    expect(wrapper.find('[data-test="stripe-pay"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="mock-pay"]').exists()).toBe(false)
  })

  it('confirmPayment is called with a return_url pointing to /pay-result/{orderNo}', async () => {
    getPaymentConfig.mockResolvedValue({
      stripeEnabled: true,
      publishableKey: 'pk_test_xyz',
      methods: ['card'],
      currency: 'cny'
    })
    createStripeIntent.mockResolvedValue({ clientSecret: 'cs_test_123', paymentIntentId: 'pi_1', amount: 200, currency: 'cny' })

    wrapper = await mountPayment()
    await flushPromises()
    await vi.waitFor(() => expect(paymentElementMock.mount).toHaveBeenCalled())

    await wrapper.find('[data-test="stripe-confirm"]').trigger('click')
    await flushPromises()

    expect(stripeMock.confirmPayment).toHaveBeenCalledTimes(1)
    const arg = stripeMock.confirmPayment.mock.calls[0][0]
    expect(arg.confirmParams.return_url).toContain(`/pay-result/${ORDER_NO}`)
    expect(arg.elements).toBe(elementsMock)
  })
})
