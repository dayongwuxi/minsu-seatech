<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listBookings, confirmBooking, cancelBooking, checkinBooking, refundBooking } from '@/api/booking'
import { BOOKING_STATUS, BOOKING_PAY_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const router = useRouter()
const bookingStatusOptions = dictOptions(BOOKING_STATUS)
const payStatusOptions = dictOptions(BOOKING_PAY_STATUS)

// BookingQuery：orderNo/memberName/roomName/checkinStart/checkinEnd/payStatus/bookingStatus
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listBookings,
  { orderNo: '', memberName: '', roomName: '', checkinStart: '', checkinEnd: '', payStatus: null, bookingStatus: null }
)

// 日期范围控件 → checkinStart/checkinEnd
const dateRange = ref([])
watch(dateRange, (val) => {
  query.checkinStart = val?.[0] || ''
  query.checkinEnd = val?.[1] || ''
})

// 操作可见性（与后端校验一致）：
// 确认：bookingStatus=0；取消：0/1；办理入住：payStatus=1 且 bookingStatus 0/1；退款：payStatus=1
const canConfirm = (row) => row.bookingStatus === 0
const canCancel = (row) => row.bookingStatus === 0 || row.bookingStatus === 1
const canCheckin = (row) => row.payStatus === 1 && (row.bookingStatus === 0 || row.bookingStatus === 1)
const canRefund = (row) => row.payStatus === 1

async function doConfirm(row) {
  await ElMessageBox.confirm(`确定确认订单「${row.orderNo}」的预约吗？`, '确认预约', { type: 'warning' })
  await confirmBooking(row.id)
  ElMessage.success('预约已确认')
  load()
}

async function doCancel(row) {
  await ElMessageBox.confirm(`确定取消订单「${row.orderNo}」的预约吗？`, '取消预约', { type: 'warning' })
  await cancelBooking(row.id)
  ElMessage.success('预约已取消')
  load()
}

async function doCheckin(row) {
  await ElMessageBox.confirm(`确定为订单「${row.orderNo}」办理入住吗？`, '办理入住', { type: 'warning' })
  await checkinBooking(row.id)
  ElMessage.success('入住办理成功')
  load()
}

async function doRefund(row) {
  await ElMessageBox.confirm(`确定对订单「${row.orderNo}」进行退款处理吗？`, '退款处理', { type: 'warning' })
  await refundBooking(row.id)
  ElMessage.success('退款处理成功')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable style="width: 170px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="会员">
          <el-input v-model="query.memberName" placeholder="会员姓名/昵称" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="房间名称">
          <el-input v-model="query.roomName" placeholder="请输入房间名称" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="query.payStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in payStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约状态">
          <el-select v-model="query.bookingStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in bookingStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button :icon="'Refresh'" @click="dateRange = []; reset()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header><div class="card-header"><span>预定记录列表</span></div></template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="165" />
        <el-table-column prop="memberName" label="会员" min-width="100">
          <template #default="{ row }">{{ row.memberName || row.memberUsername }}</template>
        </el-table-column>
        <el-table-column prop="roomName" label="房间名称" min-width="120" />
        <el-table-column prop="checkinDate" label="入住日期" width="110" />
        <el-table-column prop="checkoutDate" label="离店日期" width="110" />
        <el-table-column prop="totalAmount" label="订单金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付状态" width="95">
          <template #default="{ row }">
            <el-tag :type="dictType(BOOKING_PAY_STATUS, row.payStatus)">{{ dictLabel(BOOKING_PAY_STATUS, row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bookingStatus" label="预约状态" width="95">
          <template #default="{ row }">
            <el-tag :type="dictType(BOOKING_STATUS, row.bookingStatus)">{{ dictLabel(BOOKING_STATUS, row.bookingStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="170" />
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/bookings/${row.id}`)">查看详情</el-button>
            <el-button v-if="canConfirm(row)" link type="success" @click="doConfirm(row)">确认预约</el-button>
            <el-button v-if="canCancel(row)" link type="warning" @click="doCancel(row)">取消预约</el-button>
            <el-button v-if="canCheckin(row)" link type="primary" @click="doCheckin(row)">办理入住</el-button>
            <el-button v-if="canRefund(row)" link type="danger" @click="doRefund(row)">退款处理</el-button>
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
