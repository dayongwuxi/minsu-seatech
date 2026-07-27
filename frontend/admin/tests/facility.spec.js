import { describe, it, expect } from 'vitest'
import { FACILITY_TYPES, toFacilityForm } from '@/utils/facility'

describe('FACILITY_TYPES', () => {
  it('包含 5 种设施类型', () => {
    expect(FACILITY_TYPES).toEqual(['地铁站', '餐饮美食', '便利店', '停车场', '景点'])
  })
})

describe('toFacilityForm', () => {
  it('编辑时带上 imageUrl 等全部字段（回归：修改无法看到/保存图片）', () => {
    const row = {
      id: 7, name: '地铁2号线', type: '地铁站', distance: 300,
      address: '海景路口', imageUrl: '/files/metro.jpg', status: 1
    }
    expect(toFacilityForm(row)).toEqual({
      id: 7, name: '地铁2号线', type: '地铁站', distance: 300,
      address: '海景路口', imageUrl: '/files/metro.jpg', status: 1
    })
  })

  it('新增时给出空表单，imageUrl 为空串', () => {
    expect(toFacilityForm()).toEqual({
      id: null, name: '', type: '', distance: null, address: '', imageUrl: '', status: 1
    })
  })

  it('缺字段安全兜底', () => {
    const f = toFacilityForm({ id: 1, name: 'x', type: '景点' })
    expect(f.imageUrl).toBe('')
    expect(f.status).toBe(1)
    expect(f.distance).toBeNull()
  })
})
