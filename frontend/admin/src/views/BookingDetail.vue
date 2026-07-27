<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBooking, confirmBooking, cancelBooking, checkinBooking, refundBooking, updateBookingRemark } from '@/api/booking'
import { BOOKING_STATUS, BOOKING_PAY_STATUS, PAY_METHOD, PAYMENT_STATUS, REFUND_STATUS, PAY_CHANNEL, dictLabel, dictType } from '@/utils/dict'
import { fullPhone } from '@/utils/phone'

const route = useRoute()
const router = useRouter()
const bookingId = route.params.id

const loading = ref(false)
// BookingDetailVO { booking, member, room, payment, refund }
const detail = ref({})
const remark = ref('')
const remarkSaving = ref(false)

const booking = computed(() => detail.value.booking || {})
const member = computed(() => detail.value.member || {})
const room = computed(() => detail.value.room || {})
const payment = computed(() => detail.value.payment)
const refund = computed(() => detail.value.refund)

const canConfirm = computed(() => booking.value.bookingStatus === 0)
const canCancel = computed(() => booking.value.bookingStatus === 0 || booking.value.bookingStatus === 1)
const canCheckin = computed(
  () => booking.value.payStatus === 1 && (booking.value.bookingStatus === 0 || booking.value.bookingStatus === 1)
)
const canRefund = computed(() => booking.value.payStatus === 1)

async function load() {
  loading.value = true
  try {
    detail.value = (await getBooking(bookingId)) || {}
    remark.value = booking.value.adminRemark || ''
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

async function doConfirm() {
  await ElMessageBox.confirm('确定确认该预约吗？', '确认预约', { type: 'warning' })
  await confirmBooking(bookingId)
  ElMessage.success('预约已确认')
  load()
}

async function doCancel() {
  await ElMessageBox.confirm('确定取消该预约吗？', '取消预约', { type: 'warning' })
  await cancelBooking(bookingId)
  ElMessage.success('预约已取消')
  load()
}

async function doCheckin() {
  await ElMessageBox.confirm('确定为该订单办理入住吗？', '办理入住', { type: 'warning' })
  await checkinBooking(bookingId)
  ElMessage.success('入住办理成功')
  load()
}

async function doRefund() {
  await ElMessageBox.confirm('确定对该订单进行退款处理吗？', '退款处理', { type: 'warning' })
  await refundBooking(bookingId)
  ElMessage.success('退款处理成功')
  load()
}

async function saveRemark() {
  remarkSaving.value = true
  try {
    await updateBookingRemark(bookingId, remark.value)
    ElMessage.success('备注已保存')
  } catch (e) { /* 拦截器已提示 */ } finally {
    remarkSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <el-card shadow="never" style="margin-bottom: 16px">
      <div class="summary-bar">
        <div class="summary-left">
          <span class="order-no">订单号：{{ booking.orderNo || '-' }}</span>
          <el-tag :type="dictType(BOOKING_STATUS, booking.bookingStatus)" size="large">
            {{ dictLabel(BOOKING_STATUS, booking.bookingStatus) }}
          </el-tag>
          <el-tag :type="dictType(BOOKING_PAY_STATUS, booking.payStatus)" size="large">
            {{ dictLabel(BOOKING_PAY_STATUS, booking.payStatus) }}
          </el-tag>
        </div>
        <div class="summary-actions">
          <el-button v-if="canConfirm" type="success" @click="doConfirm">确认预约</el-button>
          <el-button v-if="canCancel" type="warning" @click="doCancel">取消预约</el-button>
          <el-button v-if="canCheckin" type="primary" @click="doCheckin">办理入住</el-button>
          <el-button v-if="canRefund" type="danger" @click="doRefund">退款处理</el-button>
          <el-button :icon="'Back'" @click="router.push('/bookings')">返回列表</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :md="8" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>用户信息</span></div></template>
          <el-descriptions :column="1">
            <el-descriptions-item label="会员用户名">{{ member.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="会员姓名">{{ member.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ fullPhone(member) }}</el-descriptions-item>
            <el-descriptions-item label="入住人">{{ booking.guestName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ booking.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入住人数">{{ booking.guestCount ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :md="8" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>房间信息</span></div></template>
          <el-descriptions :column="1">
            <el-descriptions-item label="房间名称">{{ room.roomName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="房间编号">{{ room.roomNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入住日期">{{ booking.checkinDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="离店日期">{{ booking.checkoutDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入住晚数">{{ booking.nights ? booking.nights + ' 晚' : '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :md="8" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header><div class="card-header"><span>金额明细</span></div></template>
          <el-descriptions :column="1">
            <el-descriptions-item label="房费单价">¥{{ booking.unitPrice ?? '-' }}/晚</el-descriptions-item>
            <el-descriptions-item label="优惠金额">¥{{ booking.discountAmount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单总额">
              <span style="color: #f5483d; font-weight: 700">¥{{ booking.totalAmount ?? '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card shadow="never">
          <template #header><div class="card-header"><span>支付信息</span></div></template>
          <el-descriptions v-if="payment" :column="1">
            <el-descriptions-item label="支付流水号">{{ payment.payNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ dictLabel(PAY_METHOD, payment.payMethod) }}</el-descriptions-item>
            <el-descriptions-item label="支付状态">{{ dictLabel(PAYMENT_STATUS, payment.payStatus) }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ payment.payTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="第三方单号">{{ payment.transactionNo || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无支付记录" :image-size="50" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :md="12" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>状态记录</span></div></template>
          <el-descriptions :column="1">
            <el-descriptions-item label="下单时间">{{ booking.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ booking.updateTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退款申请">
              <template v-if="refund">
                <el-tag :type="dictType(REFUND_STATUS, refund.status)" size="small">{{ dictLabel(REFUND_STATUS, refund.status) }}</el-tag>
                <el-tag :type="dictType(PAY_CHANNEL, refund.channel)" size="small" effect="plain" style="margin-left: 6px">
                  {{ dictLabel(PAY_CHANNEL, refund.channel) }}
                </el-tag>
              </template>
              <span v-else>无</span>
            </el-descriptions-item>
            <el-descriptions-item label="退款金额">{{ refund ? '¥' + refund.refundAmount : '-' }}</el-descriptions-item>
            <el-descriptions-item label="退款原因">{{ refund?.reason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退款处理时间">{{ refund?.handleTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :md="12" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>备注</span></div></template>
          <p style="color: #8a94a6; font-size: 13px; margin-bottom: 8px">用户备注：{{ booking.userRemark || '无' }}</p>
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="管理员备注（可编辑）" />
          <div style="margin-top: 10px; text-align: right">
            <el-button type="primary" size="small" :loading="remarkSaving" @click="saveRemark">保存备注</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.summary-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.order-no {
  font-size: 16px;
  font-weight: 700;
}
</style>
