// 周边设施表单的纯逻辑（抽出便于测试）

export const FACILITY_TYPES = ['地铁站', '餐饮美食', '便利店', '停车场', '景点']

/** 行数据 → 编辑表单；无 row 则空表单。务必带上 imageUrl（否则修改看不到/存不了图片） */
export function toFacilityForm(row) {
  if (!row) {
    return { id: null, name: '', type: '', distance: null, address: '', imageUrl: '', status: 1 }
  }
  return {
    id: row.id ?? null,
    name: row.name || '',
    type: row.type || '',
    distance: row.distance ?? null,
    address: row.address || '',
    imageUrl: row.imageUrl || '',
    status: row.status ?? 1
  }
}
