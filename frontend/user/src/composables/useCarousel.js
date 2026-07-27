import { ref, onUnmounted } from 'vue'

/** 把数组按 size 分组 */
export function chunk(list, size) {
  const arr = Array.isArray(list) ? list : []
  const out = []
  for (let i = 0; i < arr.length; i += size) {
    out.push(arr.slice(i, i + size))
  }
  return out
}

/** 下一格下标（环绕）；count 为 0/1 时恒为 0 */
export function nextIndex(cur, count) {
  if (!count || count < 1) return 0
  return (cur + 1) % count
}

/**
 * 自动轮播：每 intervalMs 前进一格并环绕；不足 2 项不启动。
 * getCount 用函数以便数据异步加载后仍取到最新数量。
 */
export function useAutoRotate(getCount, intervalMs = 3000) {
  const index = ref(0)
  let timer = null

  function next() {
    index.value = nextIndex(index.value, getCount())
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function start() {
    stop()
    if (getCount() > 1) {
      timer = setInterval(next, intervalMs)
    }
  }

  onUnmounted(stop)

  return { index, next, start, stop }
}
