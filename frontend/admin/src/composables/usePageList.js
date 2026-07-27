import { reactive, ref } from 'vue'

/**
 * 列表页通用组合式函数：搜索区 + 表格 + 分页
 * 后端分页参数为 current/size，返回 { total, pages, current, size, records }
 * @param {Function} fetcher 接口函数，接收查询参数
 * @param {Object} defaultQuery 默认查询条件
 */
export function usePageList(fetcher, defaultQuery = {}) {
  const query = reactive({ current: 1, size: 10, ...defaultQuery })
  const loading = ref(false)
  const records = ref([])
  const total = ref(0)

  async function load() {
    loading.value = true
    try {
      const data = await fetcher({ ...query })
      if (Array.isArray(data)) {
        records.value = data
        total.value = data.length
      } else if (data) {
        records.value = data.records || []
        total.value = Number(data.total) || 0
      }
    } catch (e) {
      /* 错误已由请求拦截器统一提示 */
    } finally {
      loading.value = false
    }
  }

  function search() {
    query.current = 1
    load()
  }

  function reset() {
    Object.keys(defaultQuery).forEach((k) => {
      query[k] = defaultQuery[k]
    })
    query.current = 1
    load()
  }

  function handlePageChange(current) {
    query.current = current
    load()
  }

  function handleSizeChange(size) {
    query.size = size
    query.current = 1
    load()
  }

  return { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange }
}
