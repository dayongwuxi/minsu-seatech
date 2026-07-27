<template>
  <div class="card-block">
    <h2 class="section-title">
      {{ t('user.bookings.title') }}
      <span v-if="isForeign" class="settle-notice">{{ t('currency.settleNotice', { cur: base }) }}</span>
    </h2>
    <!-- 后端 status tab 取值：waitPay/paid/cancelled/finished，空为全部 -->
    <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
      <el-tab-pane :label="t('user.bookings.tabAll')" name="" />
      <el-tab-pane :label="t('dict.payStatus.waitPay')" name="waitPay" />
      <el-tab-pane :label="t('dict.payStatus.paid')" name="paid" />
      <el-tab-pane :label="t('dict.bookingStatus.cancelled')" name="cancelled" />
      <el-tab-pane :label="t('dict.bookingStatus.finished')" name="finished" />
    </el-tabs>

    <el-table :data="bookings" v-loading="loading" stripe>
      <el-table-column prop="orderNo" :label="t('user.bookings.colOrderNo')" min-width="170" />
      <el-table-column prop="roomName" :label="t('user.bookings.colRoom')" min-width="120" />
      <el-table-column :label="t('user.bookings.colStay')" min-width="200">
        <template #default="{ row }">
          {{ t('common.dateRange', { from: row.checkinDate, to: row.checkoutDate }) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('user.bookings.colAmount')" width="120">
        <template #default="{ row }">
          <span class="price">{{ format(row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('user.bookings.colPayStatus')" width="140">
        <template #default="{ row }">
          <el-tag :type="dictTag(PAY_STATUS, row.payStatus)">{{ dictText(PAY_STATUS, row.payStatus, t) }}</el-tag>
          <!-- 退款状态（后端返回 refundStatus 时展示：0申请中 1退款中 2已拒绝 3已退款；payStatus=2 已由上方标签覆盖已退款态） -->
          <el-tag
            v-if="row.refundStatus != null && row.payStatus !== 2"
            :type="dictTag(REFUND_STATUS, row.refundStatus)"
            class="refund-tag"
            size="small"
          >
            {{ dictText(REFUND_STATUS, row.refundStatus, t) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('user.bookings.colBookingStatus')" width="120">
        <template #default="{ row }">
          <el-tag :type="dictTag(BOOKING_STATUS, row.bookingStatus)">{{ dictText(BOOKING_STATUS, row.bookingStatus, t) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="240" fixed="right">
        <template #default="{ row }">
          <!-- 待支付且未取消：去支付/取消 -->
          <el-button
            v-if="row.payStatus === 0 && row.bookingStatus !== 4"
            type="success"
            link
            @click="$router.push(`/pay/${row.orderNo}`)"
          >{{ t('user.bookings.goPay') }}</el-button>
          <el-button
            v-if="row.bookingStatus === 0 || row.bookingStatus === 1"
            type="danger"
            link
            @click="handleCancel(row)"
          >{{ t('user.bookings.cancelBooking') }}</el-button>
          <!-- 已完成且已支付才可申请退款 -->
          <el-button
            v-if="row.bookingStatus === 3 && row.payStatus === 1"
            type="warning"
            link
            @click="openRefund(row)"
          >{{ t('user.bookings.refund') }}</el-button>
          <el-button type="primary" link @click="showDetail(row)">{{ t('common.viewDetail') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('user.bookings.empty')" :image-size="80" />
      </template>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.current"
        @current-change="fetchBookings"
      />
    </div>

    <!-- 申请退款：填写原因 -->
    <el-dialog v-model="refundVisible" :title="t('user.bookings.refundTitle')" width="480px">
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="auto">
        <el-form-item :label="t('user.bookings.colOrderNo')">
          <span>{{ refundTarget?.orderNo }}</span>
        </el-form-item>
        <el-form-item :label="t('user.bookings.refundReason')" prop="reason">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            :placeholder="t('user.bookings.refundReasonPh')"
            data-test="refund-reason"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" :loading="refundSubmitting" @click="submitRefund">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="t('user.bookings.dialogTitle')" width="560px">
      <div v-loading="detailLoading">
        <el-descriptions :column="1" border v-if="detail">
          <el-descriptions-item :label="t('user.bookings.colOrderNo')">{{ detail.booking?.orderNo }}</el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.colRoom')">{{ detail.room?.roomName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.colStay')">
            {{ t('common.dateRange', { from: detail.booking?.checkinDate, to: detail.booking?.checkoutDate }) }}
            {{ detail.booking?.nights != null ? t('user.bookings.nightsSuffix', { n: detail.booking.nights }) : '' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('pay.guest')">
            {{ detail.booking?.guestName || '-' }}
            {{ detail.booking?.guestCount != null ? t('user.bookings.guestSuffix', { n: detail.booking.guestCount }) : '' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('booking.contactPhone')">{{ detail.booking?.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.unitDiscountLabel')">
            {{ t('user.bookings.unitDiscountValue', { price: detail.booking?.unitPrice, discount: detail.booking?.discountAmount ?? '0.00' }) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.colAmount')">
            <span class="price">¥{{ detail.booking?.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.colPayStatus')">
            <el-tag :type="dictTag(PAY_STATUS, detail.booking?.payStatus)">
              {{ dictText(PAY_STATUS, detail.booking?.payStatus, t) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.colBookingStatus')">
            <el-tag :type="dictTag(BOOKING_STATUS, detail.booking?.bookingStatus)">
              {{ dictText(BOOKING_STATUS, detail.booking?.bookingStatus, t) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('pay.payMethod')" v-if="detail.payment">
            {{ dictText(PAY_METHOD, detail.payment.payMethod, t) }} ({{ detail.payment.payTime || '-' }})
          </el-descriptions-item>
          <el-descriptions-item :label="t('user.bookings.remark')">{{ detail.booking?.userRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyBookings, getBookingDetail, cancelBooking, refundBooking } from '@/api/booking'
import { PAY_STATUS, BOOKING_STATUS, PAY_METHOD, REFUND_STATUS, dictText, dictTag } from '@/utils/dict'
import { useCurrency } from '@/composables/useCurrency'

const { t } = useI18n()
const { format, isForeign, base } = useCurrency()

const bookings = ref([])
const total = ref(0)
const loading = ref(false)
const activeStatus = ref('')
const query = reactive({ current: 1, size: 10 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function fetchBookings() {
  loading.value = true
  try {
    const data = await getMyBookings({
      current: query.current,
      size: query.size,
      status: activeStatus.value || undefined
    })
    bookings.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (e) {
    bookings.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  query.current = 1
  fetchBookings()
}

async function handleCancel(row) {
  await ElMessageBox.confirm(
    t('user.bookings.cancelConfirm', { orderNo: row.orderNo }),
    t('user.bookings.cancelTitle'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') }
  )
  await cancelBooking(row.orderNo)
  ElMessage.success(t('user.bookings.cancelSuccess'))
  fetchBookings()
}

// 申请退款：弹窗填写原因（POST /api/bookings/{orderNo}/refund {reason}）
const refundVisible = ref(false)
const refundSubmitting = ref(false)
const refundFormRef = ref()
const refundTarget = ref(null)
const refundForm = reactive({ reason: '' })
const refundRules = computed(() => ({
  reason: [{ required: true, message: t('user.bookings.vRefundReason'), trigger: 'blur' }]
}))

function openRefund(row) {
  refundTarget.value = row
  refundForm.reason = ''
  refundVisible.value = true
}

async function submitRefund() {
  await refundFormRef.value.validate()
  refundSubmitting.value = true
  try {
    await refundBooking(refundTarget.value.orderNo, refundForm.reason)
    ElMessage.success(t('user.bookings.refundSuccess'))
    refundVisible.value = false
    fetchBookings()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    refundSubmitting.value = false
  }
}

async function showDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    // BookingDetailVO: { booking, member, room, payment, refund }
    detail.value = await getBookingDetail(row.orderNo)
  } catch (e) {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(fetchBookings)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.refund-tag {
  margin-left: 4px;
}

.settle-notice {
  font-size: 12px;
  font-weight: 400;
  color: #999;
}
</style>
