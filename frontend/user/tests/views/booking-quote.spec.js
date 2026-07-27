import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import i18n, { setLang } from '@/i18n'

vi.mock('@/api/room', () => ({
  getRoomDetail: vi.fn(() =>
    Promise.resolve({
      room: { id: 1, roomName: '湖景双床房', price: 100, maxGuests: 3, coverImage: '', bedInfo: '' },
      typeName: '双床房',
      images: [],
      avgRating: 0,
      reviewCount: 0
    })
  )
}))

vi.mock('@/api/booking', () => ({
  quoteBooking: vi.fn(),
  createBooking: vi.fn()
}))

import { quoteBooking } from '@/api/booking'
import BookingForm from '@/views/BookingForm.vue'

const QUOTE = {
  nights: 2,
  unitPrice: 100,
  roomFee: 200,
  promoDiscount: 20,
  promoName: '夏日特惠',
  memberDiscount: 9,
  totalAmount: 171,
  currency: 'CNY'
}

const Stub = { template: '<div />' }

async function mountForm() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/booking/:roomId', component: Stub },
      { path: '/:pathMatch(.*)*', component: Stub }
    ]
  })
  router.push('/booking/1?checkin=2030-01-01&checkout=2030-01-03')
  await router.isReady()
  const wrapper = mount(BookingForm, {
    global: { plugins: [i18n, ElementPlus, createPinia(), router] }
  })
  await flushPromises()
  return wrapper
}

let wrapper

describe('BookingForm price quote', () => {
  beforeEach(() => {
    setLang('zh')
    quoteBooking.mockReset()
    quoteBooking.mockResolvedValue({ ...QUOTE })
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    document.body.innerHTML = ''
  })

  it('fetches a quote on mount and renders the Airbnb-style breakdown', async () => {
    wrapper = await mountForm()
    await vi.waitFor(() => expect(quoteBooking).toHaveBeenCalled())
    await flushPromises()

    expect(quoteBooking).toHaveBeenCalledWith(
      expect.objectContaining({ roomId: 1, checkinDate: '2030-01-01', checkoutDate: '2030-01-03' })
    )

    const roomFee = wrapper.find('[data-test="quote-room-fee"]')
    expect(roomFee.exists()).toBe(true)
    expect(roomFee.text()).toContain('200')

    const promo = wrapper.find('[data-test="quote-promo"]')
    expect(promo.exists()).toBe(true)
    expect(promo.text()).toContain('夏日特惠')
    expect(promo.text()).toContain('20')
    expect(promo.text()).toContain('-')
    expect(promo.classes().join(' ') + ' ' + promo.html()).toContain('discount')

    const member = wrapper.find('[data-test="quote-member-discount"]')
    expect(member.exists()).toBe(true)
    expect(member.text()).toContain('9')

    const total = wrapper.find('[data-test="quote-total"]')
    expect(total.exists()).toBe(true)
    expect(total.text()).toContain('171')
  })

  it('applies a promo code and re-quotes with promoCode', async () => {
    wrapper = await mountForm()
    await vi.waitFor(() => expect(quoteBooking).toHaveBeenCalled())
    await flushPromises()

    await wrapper.find('[data-test="promo-input"] input').setValue('SUMMER')
    await wrapper.find('[data-test="promo-apply"]').trigger('click')
    await flushPromises()

    const lastCall = quoteBooking.mock.calls.at(-1)[0]
    expect(lastCall.promoCode).toBe('SUMMER')
    // 应用成功后出现「移除」按钮
    expect(wrapper.find('[data-test="promo-remove"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="promo-error"]').exists()).toBe(false)
  })

  it('shows backend message for an invalid promo code and does not keep it applied', async () => {
    wrapper = await mountForm()
    await vi.waitFor(() => expect(quoteBooking).toHaveBeenCalled())
    await flushPromises()

    quoteBooking.mockRejectedValueOnce(new Error('优惠码无效或已过期'))
    await wrapper.find('[data-test="promo-input"] input').setValue('BADCODE')
    await wrapper.find('[data-test="promo-apply"]').trigger('click')
    await flushPromises()

    const err = wrapper.find('[data-test="promo-error"]')
    expect(err.exists()).toBe(true)
    expect(err.text()).toContain('优惠码无效或已过期')
    expect(wrapper.find('[data-test="promo-remove"]').exists()).toBe(false)
  })

  it('removes an applied promo code and re-quotes without promoCode', async () => {
    wrapper = await mountForm()
    await vi.waitFor(() => expect(quoteBooking).toHaveBeenCalled())
    await flushPromises()

    await wrapper.find('[data-test="promo-input"] input').setValue('SUMMER')
    await wrapper.find('[data-test="promo-apply"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="promo-remove"]').exists()).toBe(true)

    await wrapper.find('[data-test="promo-remove"]').trigger('click')
    await flushPromises()

    const lastCall = quoteBooking.mock.calls.at(-1)[0]
    expect(lastCall.promoCode).toBeUndefined()
    expect(wrapper.find('[data-test="promo-remove"]').exists()).toBe(false)
  })
})
