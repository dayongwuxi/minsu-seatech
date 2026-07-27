<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getStats, getIncomeTrend, getBookingTrend, getHotRooms, getLatestBookings } from '@/api/dashboard'
import { BOOKING_STATUS, dictLabel, dictType } from '@/utils/dict'

const router = useRouter()

// DashboardStatsVO 扁平字段映射，diff 为较昨日增减额（无百分比）
const statCards = reactive([
  { valueKey: 'todayBookings', diffKey: 'bookingsDiff', title: '今日预约数', value: 0, diff: null, prefix: '', color: '#2f6bff', icon: 'Calendar' },
  { valueKey: 'todayCheckins', diffKey: 'checkinsDiff', title: '今日入住数', value: 0, diff: null, prefix: '', color: '#22b573', icon: 'Key' },
  { valueKey: 'todayIncome', diffKey: 'incomeDiff', title: '今日收入', value: 0, diff: null, prefix: '¥', color: '#ff8f1f', icon: 'Wallet' },
  { valueKey: 'pendingFeedbacks', diffKey: null, title: '待处理投诉', value: 0, diff: null, prefix: '', color: '#f5483d', icon: 'Warning' },
  { valueKey: 'roomTotal', diffKey: null, title: '房间总数', value: 0, diff: null, prefix: '', color: '#7a5cff', icon: 'House' },
  { valueKey: 'availableRooms', diffKey: null, title: '可预约房间数', value: 0, diff: null, prefix: '', color: '#00a2e8', icon: 'DocumentChecked' },
  { valueKey: 'memberTotal', diffKey: null, title: '会员总数', value: 0, diff: null, prefix: '', color: '#3b82f6', icon: 'UserFilled' },
  { valueKey: 'bookingTotal', diffKey: null, title: '订单总数', value: 0, diff: null, prefix: '', color: '#f7b500', icon: 'Tickets' }
])

const quickActions = [
  { title: '查看预约', desc: '查看今日预约详情', icon: 'Calendar', color: '#2f6bff', path: '/bookings' },
  { title: '查看入住', desc: '查看今日入住详情', icon: 'Key', color: '#22b573', path: '/checkin-records' },
  { title: '查看收入', desc: '查看今日收入明细', icon: 'Coin', color: '#ff8f1f', path: '/incomes' },
  { title: '查看投诉', desc: '查看投诉处理情况', icon: 'Warning', color: '#f5483d', path: '/feedbacks' }
]

const incomeRange = ref('7d')
const bookingRange = ref('7d')
const hotRooms = ref([])
const latestBookings = ref([])

const incomeChartRef = ref()
const bookingChartRef = ref()
let incomeChart = null
let bookingChart = null

function lineOption(points, name, color) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', boundaryGap: false, data: points.map((p) => p.date) },
    yAxis: { type: 'value' },
    series: [
      {
        name,
        type: 'line',
        smooth: true,
        data: points.map((p) => Number(p.value)),
        itemStyle: { color },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: color + '55' },
            { offset: 1, color: color + '05' }
          ])
        }
      }
    ]
  }
}

async function loadStats() {
  try {
    const data = await getStats()
    if (!data) return
    statCards.forEach((card) => {
      card.value = data[card.valueKey] ?? 0
      card.diff = card.diffKey ? data[card.diffKey] : null
    })
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadIncomeTrend() {
  try {
    const points = (await getIncomeTrend({ range: incomeRange.value })) || []
    incomeChart?.setOption(lineOption(points, '收入（元）', '#2f6bff'))
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadBookingTrend() {
  try {
    const points = (await getBookingTrend({ range: bookingRange.value })) || []
    bookingChart?.setOption(lineOption(points, '预约数（单）', '#22b573'))
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadHotRooms() {
  try {
    hotRooms.value = (await getHotRooms({ days: 7, limit: 3 })) || []
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadLatestBookings() {
  try {
    latestBookings.value = (await getLatestBookings({ limit: 5 })) || []
  } catch (e) { /* 拦截器已提示 */ }
}

function handleResize() {
  incomeChart?.resize()
  bookingChart?.resize()
}

function refreshAll() {
  loadStats()
  loadIncomeTrend()
  loadBookingTrend()
  loadHotRooms()
  loadLatestBookings()
}

onMounted(() => {
  incomeChart = echarts.init(incomeChartRef.value)
  bookingChart = echarts.init(bookingChartRef.value)
  incomeChart.setOption(lineOption([], '收入（元）', '#2f6bff'))
  bookingChart.setOption(lineOption([], '预约数（单）', '#22b573'))
  window.addEventListener('resize', handleResize)
  refreshAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  incomeChart?.dispose()
  bookingChart?.dispose()
})
</script>

<template>
  <div class="page-container dashboard">
    <div class="dashboard-head">
      <h3>后台首页 / 数据看板</h3>
      <el-button type="primary" :icon="'Refresh'" @click="refreshAll">数据刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.valueKey" :xs="12" :sm="12" :md="6" style="margin-bottom: 16px">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.color + '1a', color: card.color }">
              <el-icon :size="26"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-value">{{ card.prefix }}{{ card.value }}</div>
              <div class="stat-diff">
                <template v-if="card.diff !== null && card.diff !== undefined">
                  较昨日
                  <span :class="Number(card.diff) >= 0 ? 'up' : 'down'">
                    {{ Number(card.diff) >= 0 ? '+' : '' }}{{ card.diff }}
                    <el-icon v-if="Number(card.diff) >= 0"><Top /></el-icon>
                    <el-icon v-else><Bottom /></el-icon>
                  </span>
                </template>
                <template v-else>&nbsp;</template>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :md="9" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>收入趋势图</span>
              <el-radio-group v-model="incomeRange" size="small" @change="loadIncomeTrend">
                <el-radio-button value="7d">近7天</el-radio-button>
                <el-radio-button value="30d">近30天</el-radio-button>
                <el-radio-button value="12m">近12月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="incomeChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="9" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>预约趋势图</span>
              <el-radio-group v-model="bookingRange" size="small" @change="loadBookingTrend">
                <el-radio-button value="7d">近7天</el-radio-button>
                <el-radio-button value="30d">近30天</el-radio-button>
                <el-radio-button value="12m">近12月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="bookingChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="6" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header><div class="card-header"><span>快捷操作</span></div></template>
          <div class="quick-grid">
            <div v-for="a in quickActions" :key="a.title" class="quick-item" :style="{ background: a.color + '12' }" @click="router.push(a.path)">
              <el-icon :size="20" :color="a.color"><component :is="a.icon" /></el-icon>
              <div>
                <div class="quick-title" :style="{ color: a.color }">{{ a.title }}</div>
                <div class="quick-desc">{{ a.desc }}</div>
              </div>
            </div>
          </div>
        </el-card>
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>房间预订热门排行（近7天）</span>
              <el-link type="primary" @click="router.push('/rooms')">更多</el-link>
            </div>
          </template>
          <el-empty v-if="!hotRooms.length" description="暂无数据" :image-size="60" />
          <div v-for="(room, i) in hotRooms" :key="room.roomId || i" class="hot-item">
            <el-tag :type="i === 0 ? 'warning' : 'info'" size="small" round>{{ i + 1 }}</el-tag>
            <el-image :src="room.coverImage" fit="cover" class="hot-cover">
              <template #error><div class="hot-cover-slot"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <div class="hot-info">
              <div>{{ room.roomName }}</div>
              <div class="quick-desc">预订量：{{ room.bookingCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header><div class="card-header"><span>最新预订记录</span></div></template>
      <el-table :data="latestBookings" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="memberName" label="会员昵称" min-width="100" />
        <el-table-column prop="roomName" label="房间名称" min-width="120" />
        <el-table-column prop="checkinDate" label="入住日期" min-width="110" />
        <el-table-column prop="checkoutDate" label="离店日期" min-width="110" />
        <el-table-column prop="totalAmount" label="订单金额" min-width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="160" />
        <el-table-column prop="bookingStatus" label="订单状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(BOOKING_STATUS, row.bookingStatus)">{{ dictLabel(BOOKING_STATUS, row.bookingStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/bookings/${row.id}`)">查看详情</el-link>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-title {
  color: #8a94a6;
  font-size: 13px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin: 2px 0;
}
.stat-diff {
  font-size: 12px;
  color: #8a94a6;
  min-height: 17px;
}
.stat-diff .up {
  color: #f5483d;
}
.stat-diff .down {
  color: #22b573;
}
.chart {
  height: 300px;
}
.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.quick-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}
.quick-title {
  font-size: 13px;
  font-weight: 600;
}
.quick-desc {
  font-size: 12px;
  color: #8a94a6;
}
.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f7;
}
.hot-item:last-child {
  border-bottom: none;
}
.hot-cover {
  width: 56px;
  height: 40px;
  border-radius: 6px;
  flex-shrink: 0;
}
.hot-cover-slot {
  width: 56px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f7;
  color: #b0b8c8;
}
.hot-info {
  flex: 1;
  font-size: 14px;
}
</style>
