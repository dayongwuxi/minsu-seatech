import { describe, it, expect } from 'vitest'
import {
  validateImageFile,
  fileUrl,
  indexByUid,
  collectImages,
  isServerUrl,
  hasPendingUpload,
  moveToFront,
  shiftByUid
} from '@/utils/roomImages'

// 构造 el-upload 风格的文件项
const item = (uid, url) => ({ uid, url })
const uploaded = (uid, url) => ({ uid, response: { url } })

describe('validateImageFile', () => {
  it('接受 JPG 且在大小上限内', () => {
    expect(validateImageFile({ type: 'image/jpeg', size: 5 * 1024 * 1024 })).toEqual({ ok: true, message: '' })
  })

  it('拒绝非图片格式', () => {
    const r = validateImageFile({ type: 'application/pdf', size: 1000 })
    expect(r.ok).toBe(false)
    expect(r.message).toContain('仅支持')
  })

  it('拒绝超过大小上限的图片', () => {
    const r = validateImageFile({ type: 'image/png', size: 21 * 1024 * 1024 })
    expect(r.ok).toBe(false)
    expect(r.message).toContain('20MB')
  })

  it('空文件/缺失也视为非法', () => {
    expect(validateImageFile(null).ok).toBe(false)
  })
})

describe('fileUrl', () => {
  it('新上传取 response.url，已存在取 url', () => {
    expect(fileUrl(uploaded('a', '/files/new.jpg'))).toBe('/files/new.jpg')
    expect(fileUrl(item('b', '/files/old.jpg'))).toBe('/files/old.jpg')
    expect(fileUrl({})).toBe('')
  })
})

describe('collectImages', () => {
  it('按当前顺序抽取 URL，第一张为封面', () => {
    const list = [item(1, '/files/a.jpg'), uploaded(2, '/files/b.jpg'), item(3, '/files/c.jpg')]
    expect(collectImages(list)).toEqual({
      images: ['/files/a.jpg', '/files/b.jpg', '/files/c.jpg'],
      coverImage: '/files/a.jpg'
    })
  })

  it('空列表时封面为空串', () => {
    expect(collectImages([])).toEqual({ images: [], coverImage: '' })
  })

  it('绝不收集 blob: 临时预览地址（根因回归）', () => {
    const list = [
      { uid: 1, url: 'blob:https://seabnb.axionintell.com/055946e9' }, // 上传未完成的预览
      uploaded(2, '/files/good.jpg'),
      { uid: 3, url: '' }
    ]
    expect(collectImages(list)).toEqual({
      images: ['/files/good.jpg'],
      coverImage: '/files/good.jpg'
    })
  })
})

describe('isServerUrl', () => {
  it('区分服务器地址与 blob/空值', () => {
    expect(isServerUrl('/files/a.jpg')).toBe(true)
    expect(isServerUrl('blob:https://x/1')).toBe(false)
    expect(isServerUrl('')).toBe(false)
    expect(isServerUrl(undefined)).toBe(false)
  })
})

describe('hasPendingUpload', () => {
  it('仍在上传或只有 blob 地址时为 true', () => {
    expect(hasPendingUpload([{ uid: 1, status: 'uploading', url: 'blob:x' }])).toBe(true)
    expect(hasPendingUpload([{ uid: 1, url: 'blob:https://x/1' }])).toBe(true)
  })

  it('全部拿到服务器地址时为 false', () => {
    expect(hasPendingUpload([
      { uid: 1, status: 'success', url: '/files/a.jpg' },
      uploaded(2, '/files/b.jpg')
    ])).toBe(false)
  })
})

describe('moveToFront（设为封面）', () => {
  it('把指定项移到第一位', () => {
    const list = [item(1, 'a'), item(2, 'b'), item(3, 'c')]
    expect(moveToFront(list, 3)).toBe(true)
    expect(list.map((f) => f.uid)).toEqual([3, 1, 2])
  })

  it('已在首位或不存在时不改动', () => {
    const list = [item(1, 'a'), item(2, 'b')]
    expect(moveToFront(list, 1)).toBe(false)
    expect(moveToFront(list, 99)).toBe(false)
    expect(list.map((f) => f.uid)).toEqual([1, 2])
  })
})

describe('shiftByUid（前移/后移）', () => {
  it('后移与前移相邻交换', () => {
    const list = [item(1, 'a'), item(2, 'b'), item(3, 'c')]
    expect(shiftByUid(list, 1, 1)).toBe(true) // a 后移
    expect(list.map((f) => f.uid)).toEqual([2, 1, 3])
    expect(shiftByUid(list, 3, -1)).toBe(true) // c 前移
    expect(list.map((f) => f.uid)).toEqual([2, 3, 1])
  })

  it('越界不改动', () => {
    const list = [item(1, 'a'), item(2, 'b')]
    expect(shiftByUid(list, 1, -1)).toBe(false) // 首项前移
    expect(shiftByUid(list, 2, 1)).toBe(false) // 末项后移
    expect(list.map((f) => f.uid)).toEqual([1, 2])
  })
})

describe('indexByUid', () => {
  it('定位 uid 下标，找不到返回 -1', () => {
    const list = [item(1, 'a'), item(2, 'b')]
    expect(indexByUid(list, 2)).toBe(1)
    expect(indexByUid(list, 9)).toBe(-1)
  })
})
