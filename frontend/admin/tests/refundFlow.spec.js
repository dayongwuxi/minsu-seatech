import { describe, it, expect } from 'vitest'
import { approveTip, canComplete, completeTip } from '@/utils/refundFlow'
import { PAY_CHANNEL } from '@/utils/dict'

describe('PAY_CHANNEL 字典', () => {
  it('包含 LinkTrust 渠道(3)', () => {
    expect(PAY_CHANNEL[3].label).toBe('LinkTrust')
  })
})

describe('approveTip 同意退款弹窗提示按渠道区分', () => {
  it('Stripe 单提示自动渠道退款', () => {
    expect(approveTip(2)).toContain('Stripe')
    expect(approveTip(2)).toContain('退款中')
  })

  it('LinkTrust 单提示不支持自动退款，需去渠道后台手工操作后回站内确认', () => {
    const tip = approveTip(3)
    expect(tip).toContain('LinkTrust')
    expect(tip).toContain('不支持自动退款')
    expect(tip).toContain('确认已退款')
  })

  it('Mock/未知渠道提示直接标记已退款', () => {
    expect(approveTip(1)).toContain('模拟')
    expect(approveTip(null)).toContain('模拟')
  })
})

describe('canComplete 确认已退款按钮可见性', () => {
  it('仅 LinkTrust 渠道(3)且退款中(1)可确认', () => {
    expect(canComplete({ channel: 3, status: 1 })).toBe(true)
  })

  it('Stripe 退款中由 webhook 确认，不显示按钮', () => {
    expect(canComplete({ channel: 2, status: 1 })).toBe(false)
  })

  it('非退款中状态不显示', () => {
    expect(canComplete({ channel: 3, status: 0 })).toBe(false)
    expect(canComplete({ channel: 3, status: 3 })).toBe(false)
    expect(canComplete({})).toBe(false)
  })
})

describe('completeTip', () => {
  it('确认弹窗提示先在渠道后台完成退款', () => {
    expect(completeTip()).toContain('LinkTrust 商户后台')
  })
})
