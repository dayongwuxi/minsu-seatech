import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { chunk, nextIndex, useAutoRotate } from '@/composables/useCarousel'

describe('chunk', () => {
  it('按尺寸分组', () => {
    expect(chunk([1, 2, 3, 4, 5], 2)).toEqual([[1, 2], [3, 4], [5]])
    expect(chunk([1, 2, 3, 4], 4)).toEqual([[1, 2, 3, 4]])
  })
  it('空/无效输入返回空数组', () => {
    expect(chunk([], 4)).toEqual([])
    expect(chunk(null, 4)).toEqual([])
  })
})

describe('nextIndex', () => {
  it('自增并环绕', () => {
    expect(nextIndex(0, 3)).toBe(1)
    expect(nextIndex(2, 3)).toBe(0) // 环绕
  })
  it('count 为 0/1 时安全', () => {
    expect(nextIndex(0, 0)).toBe(0)
    expect(nextIndex(0, 1)).toBe(0)
  })
})

describe('useAutoRotate', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('每 interval 前进一格并环绕', () => {
    const { index, start, stop } = useAutoRotate(() => 3, 3000)
    start()
    expect(index.value).toBe(0)
    vi.advanceTimersByTime(3000)
    expect(index.value).toBe(1)
    vi.advanceTimersByTime(3000)
    expect(index.value).toBe(2)
    vi.advanceTimersByTime(3000)
    expect(index.value).toBe(0) // 环绕
    stop()
  })

  it('stop 后不再前进', () => {
    const { index, start, stop } = useAutoRotate(() => 3, 3000)
    start()
    vi.advanceTimersByTime(3000)
    expect(index.value).toBe(1)
    stop()
    vi.advanceTimersByTime(9000)
    expect(index.value).toBe(1) // 停止后不动
  })

  it('少于 2 项不启动定时器', () => {
    const { index, start } = useAutoRotate(() => 1, 3000)
    start()
    vi.advanceTimersByTime(9000)
    expect(index.value).toBe(0)
  })
})
