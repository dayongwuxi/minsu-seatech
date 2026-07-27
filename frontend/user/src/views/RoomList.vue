<template>
  <div class="page-container">
    <!-- 顶部条件栏 -->
    <div class="card-block filter-bar">
      <div class="filter-item">
        <label>{{ t('room.type') }}</label>
        <el-select v-model="query.typeId" :placeholder="t('room.allTypes')" clearable style="width: 150px">
          <el-option v-for="tp in roomTypes" :key="tp.id" :label="tp.typeName" :value="tp.id" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>{{ t('room.checkinDate') }}</label>
        <el-date-picker v-model="query.checkin" type="date" :placeholder="t('room.selectDate')" value-format="YYYY-MM-DD" style="width: 160px" />
      </div>
      <div class="filter-item">
        <label>{{ t('room.checkoutDate') }}</label>
        <el-date-picker v-model="query.checkout" type="date" :placeholder="t('room.selectDate')" value-format="YYYY-MM-DD" style="width: 160px" />
      </div>
      <div class="filter-item">
        <label>{{ t('room.priceRange') }}</label>
        <el-select v-model="priceRange" :placeholder="t('room.unlimited')" clearable style="width: 160px">
          <el-option :label="t('room.priceUnder', { n: 100 })" value="0-100" />
          <el-option :label="t('room.priceBetween', { min: 100, max: 300 })" value="100-300" />
          <el-option :label="t('room.priceBetween', { min: 300, max: 600 })" value="300-600" />
          <el-option :label="t('room.priceBetween', { min: 600, max: 1100 })" value="600-1100" />
          <el-option :label="t('room.priceOver', { n: 1100 })" value="1100-" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>{{ t('room.capacity') }}</label>
        <el-select v-model="query.capacity" :placeholder="t('room.unlimited')" clearable style="width: 130px">
          <el-option v-for="n in 3" :key="n" :label="t('room.nGuests', { n })" :value="n" />
          <el-option :label="t('room.fourPlus')" :value="4" />
        </el-select>
      </div>
      <el-button type="success" @click="handleSearch">{{ t('common.query') }}</el-button>
      <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
    </div>

    <el-row :gutter="20">
      <!-- 左侧筛选 -->
      <el-col :span="5">
        <div class="card-block side-filter">
          <div class="side-title">{{ t('room.typeCategory') }}</div>
          <ul class="type-list">
            <li :class="{ active: !query.typeId }" @click="selectType(null)">{{ t('room.allTypes') }}</li>
            <li
              v-for="tp in roomTypes"
              :key="tp.id"
              :class="{ active: query.typeId === tp.id }"
              @click="selectType(tp.id)"
            >
              {{ tp.typeName }}
            </li>
          </ul>

          <div class="side-title">{{ t('room.priceRangeNight') }}</div>
          <el-slider
            v-model="sliderPrice"
            range
            :min="0"
            :max="2000"
            :step="50"
            @change="onSliderChange"
          />
          <div class="slider-label">¥{{ sliderPrice[0] }} - ¥{{ sliderPrice[1] }}</div>

          <div class="side-title">{{ t('room.capacity') }}</div>
          <el-radio-group v-model="query.capacity" class="capacity-group" @change="handleSearch">
            <el-radio :value="null">{{ t('room.unlimited') }}</el-radio>
            <el-radio :value="1">{{ t('room.nGuests', { n: 1 }) }}</el-radio>
            <el-radio :value="2">{{ t('room.nGuests', { n: 2 }) }}</el-radio>
            <el-radio :value="3">{{ t('room.nGuests', { n: 3 }) }}</el-radio>
            <el-radio :value="4">{{ t('room.fourPlus') }}</el-radio>
          </el-radio-group>
        </div>
      </el-col>

      <!-- 房间卡片列表 -->
      <el-col :span="19">
        <div v-loading="loading">
          <el-empty v-if="!loading && !rooms.length" :description="t('room.noRooms')" />
          <el-card v-for="room in rooms" :key="room.id" shadow="hover" class="room-row">
            <div class="room-row-inner">
              <div
                class="room-thumb"
                :style="{ backgroundImage: `url(${room.coverImage})` }"
                @click="goDetail(room.id)"
              ></div>
              <div class="room-main">
                <h3 class="room-name" @click="goDetail(room.id)">{{ room.roomName }}</h3>
                <div class="room-meta">
                  <span v-if="room.bedInfo"><el-icon><House /></el-icon> {{ room.bedInfo }}</span>
                  <span v-if="room.maxGuests"><el-icon><User /></el-icon> {{ t('home.guestsUpTo', { n: room.maxGuests }) }}</span>
                </div>
                <div class="room-meta" v-if="room.facilities">
                  <span><el-icon><Connection /></el-icon> {{ room.facilities.split(',').slice(0, 4).join(' · ') }}</span>
                </div>
              </div>
              <div class="room-side">
                <div class="room-price">
                  <span class="price">{{ format(room.price) }}</span><small>{{ t('common.perNight') }}</small>
                  <span class="price-original" v-if="room.originPrice">{{ format(room.originPrice) }}{{ t('common.perNight') }}</span>
                </div>
                <div class="room-actions">
                  <el-button @click="goDetail(room.id)">{{ t('common.viewDetail') }}</el-button>
                  <el-button type="success" @click="goBooking(room.id)">{{ t('common.bookNow') }}</el-button>
                </div>
              </div>
            </div>
          </el-card>

          <div class="pager" v-if="total > 0">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="total"
              :page-size="query.size"
              v-model:current-page="query.current"
              @current-change="fetchRooms"
            />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getRoomTypes, getRooms } from '@/api/room'
import { useCurrency } from '@/composables/useCurrency'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { format } = useCurrency()

const roomTypes = ref([])
const rooms = ref([])
const total = ref(0)
const loading = ref(false)

const priceRange = ref('')
const sliderPrice = ref([0, 2000])

// 后端 GET /api/rooms 参数：current/size/typeId/checkin/checkout/minPrice/maxPrice/capacity
const query = reactive({
  current: 1,
  size: 5,
  typeId: route.query.typeId ? Number(route.query.typeId) : null,
  checkin: route.query.checkin || '',
  checkout: route.query.checkout || '',
  minPrice: null,
  maxPrice: null,
  capacity: null
})

function applyPriceRange() {
  if (priceRange.value) {
    const [min, max] = priceRange.value.split('-')
    query.minPrice = min !== '' ? Number(min) : null
    query.maxPrice = max !== '' ? Number(max) : null
  } else {
    query.minPrice = sliderPrice.value[0] > 0 ? sliderPrice.value[0] : null
    query.maxPrice = sliderPrice.value[1] < 2000 ? sliderPrice.value[1] : null
  }
}

async function fetchRooms() {
  loading.value = true
  try {
    const data = await getRooms({
      current: query.current,
      size: query.size,
      typeId: query.typeId || undefined,
      checkin: query.checkin || undefined,
      checkout: query.checkout || undefined,
      minPrice: query.minPrice ?? undefined,
      maxPrice: query.maxPrice ?? undefined,
      capacity: query.capacity || undefined
    })
    rooms.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (e) {
    rooms.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  applyPriceRange()
  query.current = 1
  fetchRooms()
}

function handleReset() {
  query.typeId = null
  query.checkin = ''
  query.checkout = ''
  query.capacity = null
  query.minPrice = null
  query.maxPrice = null
  priceRange.value = ''
  sliderPrice.value = [0, 2000]
  query.current = 1
  fetchRooms()
}

function selectType(id) {
  query.typeId = id
  handleSearch()
}

function onSliderChange() {
  priceRange.value = ''
  handleSearch()
}

function goDetail(id) {
  router.push(`/rooms/${id}`)
}

function goBooking(id) {
  router.push({
    path: `/booking/${id}`,
    query: {
      checkin: query.checkin || undefined,
      checkout: query.checkout || undefined
    }
  })
}

onMounted(() => {
  getRoomTypes()
    .then((data) => { roomTypes.value = data || [] })
    .catch(() => {})
  fetchRooms()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item label {
  font-size: 13px;
  color: #666;
}

.side-filter {
  padding: 16px;
}

.side-title {
  font-weight: 600;
  margin: 16px 0 10px;
}

.side-title:first-child {
  margin-top: 0;
}

.type-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.type-list li {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  font-size: 14px;
}

.type-list li:hover {
  background: #f0f9f4;
  color: #12945b;
}

.type-list li.active {
  background: #12945b;
  color: #fff;
}

.slider-label {
  font-size: 13px;
  color: #666;
  text-align: center;
}

.capacity-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.room-row {
  margin-bottom: 16px;
}

.room-row-inner {
  display: flex;
  gap: 20px;
}

.room-thumb {
  width: 220px;
  height: 150px;
  flex-shrink: 0;
  border-radius: 8px;
  background-color: #eceff1;
  background-size: cover;
  background-position: center;
  cursor: pointer;
}

.room-main {
  flex: 1;
}

.room-name {
  margin: 4px 0 12px;
  cursor: pointer;
}

.room-name:hover {
  color: #12945b;
}

.room-meta {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.room-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.room-side {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
}

.room-price .price {
  font-size: 24px;
}

.pager {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}
</style>
