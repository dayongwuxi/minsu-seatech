<template>
  <div class="card-block" v-loading="loading">
    <h2 class="section-title">{{ t('user.reviews.title') }}</h2>
    <el-empty v-if="!loading && !reviews.length" :description="t('user.reviews.empty')" />
    <div v-else>
      <!-- MyReviewVO: roomName/rating/content/auditStatus/replyContent/createTime -->
      <div class="review-item" v-for="r in reviews" :key="r.id">
        <div class="review-head">
          <span class="review-room">{{ r.roomName || t('user.reviews.roomFallback') }}</span>
          <el-rate :model-value="Number(r.rating) || 0" disabled allow-half size="small" />
          <el-tag size="small" :type="dictTag(REVIEW_AUDIT_STATUS, r.auditStatus)">
            {{ dictText(REVIEW_AUDIT_STATUS, r.auditStatus, t) }}
          </el-tag>
          <span class="review-date">{{ r.createTime || '' }}</span>
        </div>
        <div class="review-content">{{ r.content }}</div>
        <div class="review-reply" v-if="r.replyContent">{{ t('room.reply') }}{{ r.replyContent }}</div>
      </div>
      <div class="pager" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.size"
          v-model:current-page="query.current"
          @current-change="fetchReviews"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getMyReviews } from '@/api/review'
import { REVIEW_AUDIT_STATUS, dictText, dictTag } from '@/utils/dict'

const { t } = useI18n()

const reviews = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })

async function fetchReviews() {
  loading.value = true
  try {
    const data = await getMyReviews({ current: query.current, size: query.size })
    reviews.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (e) {
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchReviews)
</script>

<style scoped>
.review-item {
  padding: 14px 0;
  border-bottom: 1px dashed #ebeef5;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.review-room {
  font-weight: 600;
}

.review-date {
  color: #999;
  font-size: 13px;
}

.review-content {
  color: #555;
  line-height: 1.6;
}

.review-reply {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  color: #888;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
