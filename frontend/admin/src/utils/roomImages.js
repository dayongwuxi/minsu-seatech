// 房间图片列表的纯逻辑：格式/大小校验、URL 提取、排序与封面。
// 抽出为纯函数以便单元测试（不依赖 Vue 组件与 Element Plus）。

export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
export const MAX_IMAGE_SIZE_MB = 20

/** 上传前校验：返回 { ok, message } */
export function validateImageFile(file, { allowedTypes = ALLOWED_IMAGE_TYPES, maxSizeMB = MAX_IMAGE_SIZE_MB } = {}) {
  if (!file || !allowedTypes.includes(file.type)) {
    return { ok: false, message: '仅支持 JPG / PNG / GIF / WEBP / BMP 格式的图片' }
  }
  if (file.size / 1024 / 1024 > maxSizeMB) {
    return { ok: false, message: `图片过大，单张请控制在 ${maxSizeMB}MB 以内` }
  }
  return { ok: true, message: '' }
}

/** el-upload 文件项的可访问 URL：新上传取 response.url，已存在取 url */
export function fileUrl(file) {
  return file?.response?.url || file?.url || ''
}

export function indexByUid(list, uid) {
  return list.findIndex((f) => f.uid === uid)
}

/** 判断是否为服务器返回的可持久化地址（排除 el-upload 的 blob: 本地预览地址） */
export function isServerUrl(url) {
  return typeof url === 'string' && url.length > 0 && !url.startsWith('blob:')
}

/**
 * 按当前顺序抽取图片 URL 列表与封面（第一张）。
 * 只保留服务器地址：绝不把 blob: 临时预览地址写入，避免刷新后链接失效。
 */
export function collectImages(list) {
  const images = list.map((f) => f.response?.url || f.url).filter(isServerUrl)
  return { images, coverImage: images[0] || '' }
}

/** 是否仍有图片在上传中或未拿到服务器地址（用于阻止过早保存） */
export function hasPendingUpload(list) {
  return list.some((f) => {
    if (f.status && f.status !== 'success') return true
    return !isServerUrl(f.response?.url || f.url)
  })
}

/** 就地把 uid 对应项移到最前（设为封面）；返回是否发生变化 */
export function moveToFront(list, uid) {
  const i = indexByUid(list, uid)
  if (i <= 0) return false
  const [item] = list.splice(i, 1)
  list.unshift(item)
  return true
}

/** 就地交换 uid 项与相邻项（dir=-1 前移，dir=1 后移）；返回是否发生变化 */
export function shiftByUid(list, uid, dir) {
  const i = indexByUid(list, uid)
  const j = i + dir
  if (i < 0 || j < 0 || j >= list.length) return false
  ;[list[i], list[j]] = [list[j], list[i]]
  return true
}
