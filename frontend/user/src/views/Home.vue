<template>
  <div>
    <!-- 轮播图 + 日期搜索 -->
    <div class="banner-wrap" v-loading="bannerLoading">
      <!-- 自定义轮播：每张居中停留5秒 → 向左淡出，下一张同时淡入停留中央 -->
      <div class="banner-stage">
        <transition name="banner-slide">
          <div
            v-if="currentBanner"
            :key="currentBanner.id"
            class="banner-item"
            :style="{ backgroundImage: `url(${currentBanner.imageUrl})` }"
          >
            <div class="banner-text">
              <h1>{{ currentBanner.title || t('home.bannerTitle') }}</h1>
              <p>{{ t('home.bannerSub') }}</p>
            </div>
          </div>
          <div v-else class="banner-item banner-placeholder">
            <div class="banner-text">
              <h1>{{ t('home.bannerTitle') }}</h1>
              <p>{{ t('home.bannerSub') }}</p>
            </div>
          </div>
        </transition>
        <div v-if="banners.length > 1" class="banner-dots">
          <span
            v-for="(item, i) in banners"
            :key="item.id"
            :class="['banner-dot', { active: i === bannerIndex }]"
            @click="switchBanner(i)"
          />
        </div>
      </div>

      <div class="search-bar">
        <el-date-picker
          v-model="searchForm.checkIn"
          type="date"
          :placeholder="t('home.checkinPh')"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledCheckIn"
          size="large"
        />
        <el-date-picker
          v-model="searchForm.checkOut"
          type="date"
          :placeholder="t('home.checkoutPh')"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledCheckOut"
          size="large"
        />
        <el-button type="success" size="large" class="search-btn" @click="handleSearch">
          {{ t('home.search') }}
        </el-button>
      </div>
    </div>

    <div class="page-container">
      <!-- 推荐房间 -->
      <div class="card-block" v-loading="roomLoading">
        <h2 class="section-title">
          {{ t('home.recommend') }}
          <router-link to="/rooms" class="more-link">{{ t('common.viewMore') }}</router-link>
        </h2>
        <el-empty v-if="!roomLoading && !recommendRooms.length" :description="t('home.noRecommend')" />
        <el-row v-else :gutter="20">
          <el-col :span="8" v-for="room in recommendRooms" :key="room.id">
            <el-card shadow="hover" body-style="padding: 0" class="room-card" @click="goDetail(room.id)">
              <div class="room-img" :style="{ backgroundImage: `url(${room.coverImage})` }">
                <span class="room-count" v-if="room.maxGuests">{{ t('home.guestsUpTo', { n: room.maxGuests }) }}</span>
              </div>
              <div class="room-info">
                <div class="room-name">{{ room.roomName }}</div>
                <div>
                  <span class="price">{{ format(room.price) }}<small>{{ t('common.perNight') }}</small></span>
                  <span class="price-original" v-if="room.originPrice">{{ format(room.originPrice) }}{{ t('common.perNight') }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 最新公告 -->
      <div class="card-block" v-loading="noticeLoading">
        <h2 class="section-title">
          {{ t('home.latestNotices') }}
          <router-link to="/notices" class="more-link">{{ t('common.viewMore') }}</router-link>
        </h2>
        <el-empty v-if="!noticeLoading && !notices.length" :description="t('home.noNotice')" />
        <ul v-else class="notice-list">
          <li v-for="n in notices" :key="n.id" @click="$router.push(`/notices/${n.id}`)">
            <span class="notice-title">{{ n.title }}</span>
            <span class="notice-date">{{ n.publishTime || '' }}</span>
          </li>
        </ul>
      </div>

      <!-- 周边设施推荐 -->
      <div id="facilities" class="card-block" v-loading="facilityLoading">
        <h2 class="section-title">{{ t('home.facilitiesTitle') }}</h2>
        <el-empty v-if="!facilityLoading && !facilities.length" :description="t('home.noFacility')" />
        <div v-else class="facility-carousel">
          <div class="facility-track" :style="{ transform: `translateX(-${facilitySlide * 100}%)` }">
            <div class="facility-slide" v-for="(group, gi) in facilitySlides" :key="gi">
              <el-row :gutter="20">
                <el-col :span="6" v-for="f in group" :key="f.id">
                  <el-card shadow="hover" body-style="padding: 0" class="facility-card">
                    <div class="facility-img" :style="{ backgroundImage: `url(${f.imageUrl})` }"></div>
                    <div class="facility-info">
                      <div class="facility-name">
                        {{ f.name }}
                        <el-tag v-if="f.type" size="small" type="success" effect="plain">{{ f.type }}</el-tag>
                      </div>
                      <div class="facility-desc">
                        {{ f.distance != null ? t('home.distance', { n: f.distance }) : '' }} {{ f.address || '' }}
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </div>
          <div class="facility-dots" v-if="facilitySlides.length > 1">
            <span
              v-for="(g, i) in facilitySlides"
              :key="i"
              :class="['facility-dot', { active: i === facilitySlide }]"
              @click="goFacilitySlide(i)"
            ></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getBanners, getRecommendRooms, getFacilities } from '@/api/home'
import { getNotices } from '@/api/notice'
import { useCurrency } from '@/composables/useCurrency'
import { chunk, useAutoRotate } from '@/composables/useCarousel'

const router = useRouter()
const { t } = useI18n()
const { format } = useCurrency()

const banners = ref([])
const recommendRooms = ref([])
const notices = ref([])
const facilities = ref([])

// 周边设施轮播：每屏 4 张，每 3 秒自动移动一屏
const facilitySlides = computed(() => chunk(facilities.value, 4))
const { index: facilitySlide, start: startFacilityRotate } = useAutoRotate(
  () => facilitySlides.value.length,
  3000
)
function goFacilitySlide(i) {
  facilitySlide.value = i
  startFacilityRotate() // 手动切换后重置计时
}

const bannerLoading = ref(false)

// —— 轮播控制：停留 5 秒后切换，切换动效见 .banner-slide-* ——
const BANNER_STAY_MS = 5000
const bannerIndex = ref(0)
let bannerTimer = null

const currentBanner = computed(() => banners.value[bannerIndex.value] || null)

function switchBanner(i) {
  bannerIndex.value = i
  startBannerTimer()
}

function startBannerTimer() {
  if (bannerTimer) clearInterval(bannerTimer)
  if (banners.value.length < 2) return
  bannerTimer = setInterval(() => {
    bannerIndex.value = (bannerIndex.value + 1) % banners.value.length
  }, BANNER_STAY_MS)
}

onBeforeUnmount(() => {
  if (bannerTimer) clearInterval(bannerTimer)
})
const roomLoading = ref(false)
const noticeLoading = ref(false)
const facilityLoading = ref(false)

const searchForm = reactive({
  checkIn: '',
  checkOut: ''
})

function disabledCheckIn(date) {
  return date.getTime() < Date.now() - 24 * 3600 * 1000
}

function disabledCheckOut(date) {
  if (searchForm.checkIn) {
    return date.getTime() <= new Date(searchForm.checkIn).getTime()
  }
  return date.getTime() < Date.now()
}

function handleSearch() {
  if (searchForm.checkIn && searchForm.checkOut && searchForm.checkIn >= searchForm.checkOut) {
    ElMessage.warning(t('home.dateOrderError'))
    return
  }
  router.push({
    path: '/rooms',
    query: {
      checkin: searchForm.checkIn || undefined,
      checkout: searchForm.checkOut || undefined
    }
  })
}

function goDetail(id) {
  router.push(`/rooms/${id}`)
}

onMounted(() => {
  bannerLoading.value = true
  roomLoading.value = true
  noticeLoading.value = true
  facilityLoading.value = true

  getBanners()
    .then((data) => {
      banners.value = data || []
      // 预加载全部轮播图，避免切换瞬间白屏
      banners.value.forEach((b) => { const img = new Image(); img.src = b.imageUrl })
      startBannerTimer()
    })
    .catch(() => {})
    .finally(() => { bannerLoading.value = false })

  getRecommendRooms()
    .then((data) => { recommendRooms.value = (data || []).slice(0, 3) })
    .catch(() => {})
    .finally(() => { roomLoading.value = false })

  getNotices({ current: 1, size: 5 })
    .then((data) => { notices.value = data?.records || [] })
    .catch(() => {})
    .finally(() => { noticeLoading.value = false })

  getFacilities()
    .then((data) => {
      facilities.value = data || []
      startFacilityRotate()
    })
    .catch(() => {})
    .finally(() => { facilityLoading.value = false })
})
</script>

<style scoped>
.banner-wrap {
  position: relative;
}

.banner-stage {
  position: relative;
  height: 420px;
  overflow: hidden;
  background-color: #37474f;
}

.banner-item {
  position: absolute;
  inset: 0;
  background-color: #37474f;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
}

/* 出场：向左滑动并淡出；入场：原地（中央）淡入，二者同时进行 */
.banner-slide-leave-active {
  transition: transform 0.8s ease-in, opacity 0.8s ease-in;
}
.banner-slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}
.banner-slide-enter-active {
  transition: opacity 0.8s ease-out;
}
.banner-slide-enter-from {
  opacity: 0;
}

.banner-dots {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 96px;
  display: flex;
  gap: 8px;
  z-index: 5;
}
.banner-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: background 0.3s;
}
.banner-dot.active {
  background: #fff;
}

.banner-placeholder {
  background: linear-gradient(135deg, #16694d, #12945b);
}

.banner-text {
  color: #fff;
  padding-left: 12%;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

.banner-text h1 {
  font-size: 40px;
  margin: 0 0 12px;
}

.banner-text p {
  font-size: 18px;
  letter-spacing: 4px;
}

.search-bar {
  position: absolute;
  left: 50%;
  bottom: 32px;
  transform: translateX(-50%);
  z-index: 10;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-btn {
  width: 140px;
}

.more-link {
  font-size: 14px;
  font-weight: 400;
  color: #12945b;
}

.room-card {
  cursor: pointer;
  margin-bottom: 12px;
}

.room-img {
  height: 180px;
  background-color: #eceff1;
  background-size: cover;
  background-position: center;
  position: relative;
}

.room-count {
  position: absolute;
  left: 8px;
  bottom: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.room-info {
  padding: 12px;
}

.room-name {
  font-weight: 600;
  margin-bottom: 8px;
}

.price small {
  font-weight: 400;
  color: #666;
}

.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notice-list li {
  display: flex;
  justify-content: space-between;
  padding: 10px 4px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
}

.notice-list li:hover .notice-title {
  color: #12945b;
}

.notice-date {
  color: #999;
  font-size: 13px;
}

.facility-carousel {
  overflow: hidden;
  position: relative;
}

.facility-track {
  display: flex;
  transition: transform 0.6s ease;
}

.facility-slide {
  flex: 0 0 100%;
  min-width: 100%;
}

.facility-dots {
  text-align: center;
  margin-top: 14px;
}

.facility-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin: 0 4px;
  border-radius: 50%;
  background: #dcdfe6;
  cursor: pointer;
  transition: all 0.3s;
}

.facility-dot.active {
  width: 20px;
  border-radius: 4px;
  background: var(--el-color-primary, #409eff);
}

.facility-card {
  margin-bottom: 12px;
}

.facility-img {
  height: 120px;
  background-color: #eceff1;
  background-size: cover;
  background-position: center;
}

.facility-info {
  padding: 10px 12px;
}

.facility-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.facility-desc {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
