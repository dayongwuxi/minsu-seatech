<template>
  <div class="page-container" v-loading="loading">
    <div class="card-block" v-if="notice">
      <el-page-header :content="t('notice.detailTitle')" @back="$router.back()" class="page-header" />
      <h2 class="notice-title">{{ notice.title }}</h2>
      <div class="notice-meta">
        <span>{{ t('notice.publishTime') }}: {{ notice.publishTime || '-' }}</span>
        <span v-if="notice.creatorName">{{ t('notice.publisher') }}: {{ notice.creatorName }}</span>
      </div>
      <el-divider />
      <div class="notice-content" v-html="notice.content"></div>
    </div>
    <el-empty v-else-if="!loading" :description="t('notice.notFound')" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getNoticeDetail } from '@/api/notice'

const route = useRoute()
const { t } = useI18n()
const notice = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    notice.value = await getNoticeDetail(route.params.id)
  } catch (e) {
    notice.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.notice-title {
  text-align: center;
  margin: 8px 0 12px;
}

.notice-meta {
  text-align: center;
  color: #999;
  font-size: 13px;
  display: flex;
  justify-content: center;
  gap: 24px;
}

.notice-content {
  line-height: 1.9;
  color: #444;
  min-height: 200px;
}
</style>
