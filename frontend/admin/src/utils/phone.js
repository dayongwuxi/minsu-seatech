/** 带国家区号的完整手机号展示：+81 08012345678；存量用户无区号仅显号码 */
export function fullPhone(row) {
  const phone = row?.phone
  if (!phone) return '-'
  return row?.phoneCountry ? `${row.phoneCountry} ${phone}` : phone
}
