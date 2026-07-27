<template>
  <div class="page-container" v-loading="loading">
    <template v-if="room">
      <div class="card-block">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-carousel height="320px" indicator-position="outside">
              <el-carousel-item v-for="(img, i) in images" :key="i">
                <div class="detail-img" :style="{ backgroundImage: `url(${img})` }"></div>
              </el-carousel-item>
              <el-carousel-item v-if="!images.length">
                <div class="detail-img placeholder">{{ t('room.noImage') }}</div>
              </el-carousel-item>
            </el-carousel>
          </el-col>
          <el-col :span="12">
            <h2 class="detail-name">{{ room.roomName }}</h2>
            <div class="detail-price">
              <span class="price">{{ format(room.price) }}</span><small>{{ t('common.perNight') }}</small>
              <span class="price-original" v-if="room.originPrice">{{ format(room.originPrice) }}{{ t('common.perNight') }}</span>
              <span v-if="isForeign" class="settle-notice">{{ t('currency.settleNotice', { cur: base }) }}</span>
              <!-- 生效中的促销标签（GET /api/promotions/active?roomId=） -->
              <el-tag
                v-for="p in promotions"
                :key="p.id"
                type="danger"
                effect="light"
                class="promo-tag"
                data-test="promo-tag"
              >
                {{ p.name }}
              </el-tag>
            </div>
            <el-descriptions :column="2" class="detail-attrs">
              <el-descriptions-item :label="t('room.area')">
                {{ room.area ? t('room.areaValue', { n: room.area }) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('room.bedType')">{{ room.bedInfo || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('room.maxGuests')">
                {{ room.maxGuests ? t('room.maxGuestsValue', { n: room.maxGuests }) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('room.roomTypeLabel')">{{ detail?.typeName || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div class="facility-tags" v-if="facilityTags.length">
              <el-tag v-for="tag in facilityTags" :key="tag" type="success" effect="plain" class="facility-tag">
                {{ tag }}
              </el-tag>
            </div>
            <div class="detail-actions">
              <el-button
                :type="favorited ? 'warning' : 'default'"
                :loading="favLoading"
                @click="toggleFavorite"
              >
                <el-icon><StarFilled v-if="favorited" /><Star v-else /></el-icon>
                {{ favorited ? t('room.favorited') : t('room.favorite') }}
              </el-button>
              <el-button type="success" size="large" @click="goBooking">{{ t('common.bookNow') }}</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="card-block">
        <h2 class="section-title">{{ t('room.intro') }}</h2>
        <div class="detail-desc" v-if="displayedDescription">{{ displayedDescription }}</div>
        <el-empty v-else :description="t('room.noIntro')" :image-size="60" />
        <template v-if="displayedBookingNote">
          <el-divider />
          <h4>{{ t('room.bookingNote') }}</h4>
          <div class="detail-desc">{{ displayedBookingNote }}</div>
        </template>
      </div>

      <div class="card-block" v-loading="reviewLoading">
        <h2 class="section-title">
          {{ t('room.reviewsTitle', { n: detail?.reviewCount ?? 0 }) }}
          <span class="review-score" v-if="avgRating > 0">
            <el-rate :model-value="avgRating" disabled allow-half />
            <span class="score-text">{{ avgRating }} {{ t('room.points') }}</span>
          </span>
        </h2>
        <el-empty v-if="!reviewLoading && !reviews.length" :description="t('room.noReviews')" :image-size="60" />
        <div v-else>
          <div class="review-item" v-for="r in reviews" :key="r.id">
            <div class="review-head">
              <span class="review-user">{{ r.memberName || t('room.anonymous') }}</span>
              <el-rate :model-value="Number(r.rating) || 0" disabled allow-half size="small" />
              <span class="review-date">{{ r.createTime || '' }}</span>
            </div>
            <div class="review-content">{{ r.content }}</div>
            <div class="review-reply" v-if="r.replyContent">{{ t('room.reply') }}{{ r.replyContent }}</div>
          </div>
          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="reviewTotal"
              :page-size="reviewQuery.size"
              v-model:current-page="reviewQuery.current"
              @current-change="fetchReviews"
            />
          </div>
        </div>
      </div>
    </template>
    <el-empty v-else-if="!loading" :description="t('room.notFound')" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getRoomDetail, getRoomReviews, addFavorite, removeFavorite } from '@/api/room'
import { getActivePromotions } from '@/api/promotion'
import { useUserStore } from '@/store/user'
import { useCurrency } from '@/composables/useCurrency'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t, locale } = useI18n()
const { format, isForeign, base } = useCurrency()

const roomId = route.params.id
// RoomDetailVO: { room, typeName, images: [{imageUrl}], avgRating, reviewCount }
const detail = ref(null)
const loading = ref(false)
const favorited = ref(false)
const favLoading = ref(false)

const reviews = ref([])
const reviewTotal = ref(0)
const reviewLoading = ref(false)
const reviewQuery = reactive({ current: 1, size: 5 })

// 生效中的促销活动（展示活动名标签）
const promotions = ref([])

const room = computed(() => detail.value?.room || null)

// 多语言说明/须知：base 中文在 room，其余语种在 detail.i18n。全部随详情一次加载，
// 切换站点语言即时切换展示（zh 或无译文时回退中文）
const i18nMap = computed(() => {
  const map = {}
  ;(detail.value?.i18n || []).forEach((it) => {
    if (it?.lang) map[it.lang] = it
  })
  return map
})

const displayedDescription = computed(() => {
  if (locale.value === 'zh') return room.value?.description || ''
  return i18nMap.value[locale.value]?.description || room.value?.description || ''
})

const displayedBookingNote = computed(() => {
  if (locale.value === 'zh') return room.value?.bookingNote || ''
  return i18nMap.value[locale.value]?.bookingNote || room.value?.bookingNote || ''
})

const avgRating = computed(() => Number(detail.value?.avgRating ?? 0))

const images = computed(() => {
  const list = (detail.value?.images || []).map((i) => i.imageUrl).filter(Boolean)
  if (list.length) return list
  return room.value?.coverImage ? [room.value.coverImage] : []
})

const facilityTags = computed(() => {
  const f = room.value?.facilities
  return f ? f.split(',').map((s) => s.trim()).filter(Boolean) : []
})

async function fetchRoom() {
  loading.value = true
  try {
    detail.value = await getRoomDetail(roomId)
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function fetchReviews() {
  reviewLoading.value = true
  try {
    const data = await getRoomReviews(roomId, { current: reviewQuery.current, size: reviewQuery.size })
    reviews.value = data?.records || []
    reviewTotal.value = data?.total ?? 0
  } catch (e) {
    reviews.value = []
  } finally {
    reviewLoading.value = false
  }
}

async function toggleFavorite() {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favLoading.value = true
  try {
    if (favorited.value) {
      await removeFavorite(roomId)
      favorited.value = false
      ElMessage.success(t('room.unfavSuccess'))
    } else {
      await addFavorite(roomId)
      favorited.value = true
      ElMessage.success(t('room.favSuccess'))
    }
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    favLoading.value = false
  }
}

function goBooking() {
  router.push(`/booking/${roomId}`)
}

onMounted(() => {
  fetchRoom()
  fetchReviews()
  getActivePromotions(roomId)
    .then((data) => { promotions.value = data || [] })
    .catch(() => {})
})
</script>

<style scoped>
.detail-img {
  height: 100%;
  border-radius: 8px;
  background-color: #eceff1;
  background-size: cover;
  background-position: center;
}

.detail-img.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

.detail-name {
  margin: 0 0 12px;
}

.detail-price {
  margin-bottom: 16px;
}

.detail-price .price {
  font-size: 28px;
}

.promo-tag {
  margin-left: 10px;
  vertical-align: middle;
}

.settle-notice {
  margin-left: 10px;
  font-size: 12px;
  color: #999;
}

.detail-attrs {
  margin-bottom: 16px;
}

.facility-tags {
  margin-bottom: 20px;
}

.facility-tag {
  margin: 0 8px 8px 0;
}

.detail-actions {
  display: flex;
  gap: 12px;
}

.detail-desc {
  line-height: 1.8;
  color: #555;
  /* 保留管理端文本框中输入的换行与缩进 */
  white-space: pre-wrap;
  word-break: break-word;
}

.review-score {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 400;
}

.score-text {
  font-size: 14px;
  color: #f7ba2a;
}

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

.review-user {
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
