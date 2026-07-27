/**
 * 银行卡本地校验工具（仅浏览器内使用；完整卡号与 CVC 永不出浏览器）
 */

/** Luhn 校验 */
export function luhnCheck(cardNumber) {
  const digits = String(cardNumber).replace(/\D/g, '')
  if (digits.length < 12 || digits.length > 19) return false
  let sum = 0
  let double = false
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = Number(digits[i])
    if (double) {
      d *= 2
      if (d > 9) d -= 9
    }
    sum += d
    double = !double
  }
  return sum % 10 === 0
}

/** 品牌识别：4→visa、5→mastercard、35→jcb、62→unionpay，其余 card */
export function detectBrand(cardNumber) {
  const digits = String(cardNumber).replace(/\D/g, '')
  if (/^62/.test(digits)) return 'unionpay'
  if (/^35/.test(digits)) return 'jcb'
  if (/^4/.test(digits)) return 'visa'
  if (/^5/.test(digits)) return 'mastercard'
  return 'card'
}

/** 取末四位 */
export function cardLast4(cardNumber) {
  const digits = String(cardNumber).replace(/\D/g, '')
  return digits.slice(-4)
}

/** 解析 MM/YY → { expMonth, expYear(4位) }，非法返回 null */
export function parseExpiry(expiry) {
  const m = /^(0[1-9]|1[0-2])\s*\/\s*(\d{2})$/.exec(String(expiry).trim())
  if (!m) return null
  return { expMonth: Number(m[1]), expYear: 2000 + Number(m[2]) }
}
