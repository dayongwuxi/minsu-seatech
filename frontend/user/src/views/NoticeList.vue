<template>
  <div class="page-container">
    <div class="card-block" v-loading="loading">
      <h2 class="section-title">{{ t('notice.listTitle') }}</h2>
      <el-empty v-if="!loading && !notices.length" :description="t('notice.empty')" />
      <ul v-else class="notice-list">
        <li v-for="n in notices" :key="n.id" @click="$router.push(`/notices/${n.id}`)">
          <div class="notice-main">
            <span class="notice-title">{{ n.title }}</span>
            <p class="notice-summary" v-if="n.summary">{{ n.summary }}</p>
          </div>
          <span class="notice-date">{{ n.publishTime || '' }}</span>
        </li>
      </ul>
      <div class="pager" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.size"
          v-model:current-page="query.current"
          @current-change="fetchNotices"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getNotices } from '@/api/notice'

const { t } = useI18n()
const notices = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })

async function fetchNotices() {
  loading.value = true
  try {
    const data = await getNotices({ current: query.current, size: query.size })
    notices.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (e) {
    notices.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchNotices)
</script>

<style scoped>
.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notice-list li {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 16px 4px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
}

.notice-list li:hover .notice-title {
  color: #12945b;
}

.notice-title {
  font-weight: 600;
}

.notice-summary {
  margin: 6px 0 0;
  font-size: 13px;
  color: #999;
}

.notice-date {
  color: #999;
  font-size: 13px;
  white-space: nowrap;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
