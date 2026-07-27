<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { usePageList } from '@/composables/usePageList'
import { listIncomes, getIncomeStats, getIncomeTrend, getPayMethodRatio } from '@/api/income'
import { PAY_METHOD, PAYMENT_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const router = useRouter()
const payMethodOptions = dictOptions(PAY_METHOD)
const payStatusOptions = dictOptions(PAYMENT_STATUS)

// IncomeQuery：dateStart/dateEnd/roomName/payMethod/payStatus
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listIncomes,
  { dateStart: '', dateEnd: '', roomName: '', payMethod: null, payStatus: null }
)

const dateRange = ref([])
watch(dateRange, (val) => {
  query.dateStart = val?.[0] || ''
  query.dateEnd = val?.[1] || ''
})

// IncomeStatsVO：diff 为环比增减额（非百分比）
const statCards = reactive([
  { valueKey: 'todayIncome', diffKey: 'todayDiff', diffText: '较昨日', title: '今日收入', value: 0, diff: null, color: '#2f6bff', icon: 'Coin' },
  { valueKey: 'monthIncome', diffKey: 'monthDiff', diffText: '较上月', title: '本月收入', value: 0, diff: null, color: '#22b573', icon: 'Wallet' },
  { valueKey: 'totalIncome', diffKey: null, diffText: '', title: '累计收入', value: 0, diff: null, color: '#f7b500', icon: 'Money' },
  { valueKey: 'refundTotal', diffKey: null, diffText: '', title: '退款金额', value: 0, diff: null, color: '#f5483d', icon: 'RefreshLeft' },
  { valueKey: 'actualIncome', diffKey: null, diffText: '', title: '实际收入', value: 0, diff: null, color: '#00a2e8', icon: 'TrendCharts' }
])

const trendRange = ref('7d')
const trendChartRef = ref()
const ratioChartRef = ref()
let trendChart = null
let ratioChart = null

async function loadStats() {
  try {
    const data = await getIncomeStats()
    if (!data) return
    statCards.forEach((c) => {
      c.value = data[c.valueKey] ?? 0
      c.diff = c.diffKey ? data[c.diffKey] : null
    })
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadTrend() {
  try {
    // [{ date, value }]
    const points = (await getIncomeTrend({ range: trendRange.value })) || []
    trendChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 60, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', boundaryGap: false, data: points.map((p) => p.date) },
      yAxis: { type: 'value' },
      series: [
        {
          name: '收入（元）',
          type: 'line',
          smooth: true,
          data: points.map((p) => Number(p.value)),
          itemStyle: { color: '#2f6bff' },
          areaStyle: { color: 'rgba(47,107,255,0.15)' }
        }
      ]
    })
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadRatio() {
  try {
    // [{ payMethod, count, amount }]
    const items = (await getPayMethodRatio()) || []
    ratioChart?.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c}（{d}%）' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [
        {
          name: '支付方式占比',
          type: 'pie',
          radius: ['48%', '72%'],
          center: ['38%', '50%'],
          avoidLabelOverlap: true,
          label: { show: false },
          data: items.map((i) => ({ name: dictLabel(PAY_METHOD, i.payMethod), value: Number(i.amount) }))
        }
      ]
    })
  } catch (e) { /* 拦截器已提示 */ }
}

function handleExport() {
  // 导出占位：后端暂未提供导出接口
  ElMessage.info('导出功能开发中')
}

function handleResize() {
  trendChart?.resize()
  ratioChart?.resize()
}

onMounted(() => {
  trendChart = echarts.init(trendChartRef.value)
  ratioChart = echarts.init(ratioChartRef.value)
  window.addEventListener('resize', handleResize)
  load()
  loadStats()
  loadTrend()
  loadRatio()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  ratioChart?.dispose()
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="房间名称">
          <el-input v-model="query.roomName" placeholder="请输入房间名称" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="query.payMethod" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in payMethodOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="query.payStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in payStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button :icon="'Refresh'" @click="dateRange = []; reset()">重置</el-button>
          <el-button :icon="'Download'" @click="handleExport">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.valueKey" :md="{ span: 4, offset: 0 }" :xs="12" style="margin-bottom: 16px; flex: 1">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.color + '1a', color: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-value">¥ {{ card.value }}</div>
              <div class="stat-rate">
                <template v-if="card.diff !== null && card.diff !== undefined">
                  {{ card.diffText }}
                  <span :class="Number(card.diff) >= 0 ? 'up' : 'down'">
                    {{ Number(card.diff) >= 0 ? '+' : '' }}{{ card.diff }}
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
      <el-col :md="15" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>收入趋势图</span>
              <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
                <el-radio-button value="7d">近7天</el-radio-button>
                <el-radio-button value="30d">近30天</el-radio-button>
                <el-radio-button value="12m">近12月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="9" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>支付方式占比</span></div></template>
          <div ref="ratioChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="never">
      <template #header><div class="card-header"><span>收入明细列表</span></div></template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="payNo" label="收入编号" min-width="150" />
        <el-table-column prop="orderNo" label="订单编号" min-width="150" />
        <el-table-column prop="roomName" label="房间名称" min-width="120" />
        <el-table-column prop="memberName" label="会员姓名" min-width="100">
          <template #default="{ row }">{{ row.memberName || row.memberUsername }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="支付金额（元）" width="120" />
        <el-table-column prop="payMethod" label="支付方式" width="120">
          <template #default="{ row }">
            <el-tag :type="dictType(PAY_METHOD, row.payMethod)" effect="plain">{{ dictLabel(PAY_METHOD, row.payMethod) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(PAYMENT_STATUS, row.payStatus)">{{ dictLabel(PAYMENT_STATUS, row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" min-width="165" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/bookings/${row.bookingId}`)">查看订单</el-link>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-icon {
  width: 48px;
  height: 48px;
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
  font-size: 19px;
  font-weight: 700;
  margin: 2px 0;
}
.stat-rate {
  font-size: 12px;
  color: #8a94a6;
  min-height: 17px;
}
.stat-rate .up {
  color: #f5483d;
}
.stat-rate .down {
  color: #22b573;
}
.chart {
  height: 300px;
}
</style>
