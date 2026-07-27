<template>
  <div class="page-container">
    <!-- 支付确认中：Stripe 回跳后 webhook 可能尚未到达，轮询确认 -->
    <div v-if="polling" class="card-block result-block confirming-block">
      <el-icon class="confirming-icon is-loading" :size="42"><Loading /></el-icon>
      <p class="confirming-text">{{ t('pay.confirming') }}</p>
      <p class="confirming-order">{{ t('pay.orderNo') }}: {{ orderNo }}</p>
    </div>

    <!-- 外跳支付（LinkTrust 等）轮询打满仍待支付：不误报失败，等用户完成支付后手动刷新 -->
    <div v-else-if="pendingFinal" class="card-block result-block confirming-block" data-test="pending-final">
      <el-icon class="confirming-icon" :size="42"><Clock /></el-icon>
      <p class="confirming-text">{{ t('pay.stillPending') }}</p>
      <p class="confirming-order">{{ t('pay.orderNo') }}: {{ orderNo }}</p>
      <div class="result-actions">
        <el-button type="success" data-test="recheck" @click="recheck">{{ t('pay.recheck') }}</el-button>
        <el-button @click="$router.push('/user/bookings')">{{ t('pay.viewBookings') }}</el-button>
      </div>
    </div>

    <div v-else class="card-block result-block">
      <el-result
        :icon="success ? 'success' : 'error'"
        :title="success ? t('pay.resultSuccess') : t('pay.resultFail')"
        :sub-title="success ? t('pay.resultSuccessSub') : t('pay.resultFailSub')"
      >
        <template #extra>
          <el-descriptions :column="1" border class="result-desc">
            <el-descriptions-item :label="t('pay.orderNo')">{{ result?.orderNo || orderNo }}</el-descriptions-item>
            <el-descriptions-item :label="t('pay.roomName')">{{ result?.roomName || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="t('pay.stay')">
              {{ t('common.dateRange', { from: result?.checkinDate || '-', to: result?.checkoutDate || '-' }) }}
              {{ result?.nights != null ? t('pay.stayNights', { n: result.nights }) : '' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('pay.payAmount')">
              <span class="price">{{ result?.amount != null ? format(result.amount) : '-' }}</span>
              <span v-if="isForeign" class="settle-notice">{{ t('currency.settleNotice', { cur: base }) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('pay.payMethod')">{{ dictText(PAY_METHOD, result?.payMethod, t) }}</el-descriptions-item>
            <el-descriptions-item :label="t('pay.payTime')">{{ result?.payTime || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="t('pay.payStatus')">
              <el-tag :type="dictTag(PAY_STATUS, result?.payStatus ?? 0)">
                {{ dictText(PAY_STATUS, result?.payStatus ?? 0, t) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="result-actions">
            <el-button type="success" @click="$router.push('/user/bookings')">{{ t('pay.viewBookings') }}</el-button>
            <el-button @click="$router.push('/')">{{ t('pay.backHome') }}</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Clock, Loading } from '@element-plus/icons-vue'
import { getPaymentResult } from '@/api/payment'
import { PAY_STATUS, PAY_METHOD, dictText, dictTag } from '@/utils/dict'
import { useCurrency } from '@/composables/useCurrency'

const route = useRoute()
const { t } = useI18n()
const { format, isForeign, base } = useCurrency()
const orderNo = route.params.orderNo

// Stripe return_url 回跳参数（payment_intent / redirect_status），仅作参考，最终以后端轮询结果为准
const redirectStatus = route.query.redirect_status

// PaymentResultVO: { orderNo, payNo, amount, payMethod, payStatus, payTime, roomName, checkinDate, checkoutDate, nights }
const result = ref(null)
const polling = ref(true)
// 轮询打满仍待支付（外跳收银台可能仍在支付中）：展示手动刷新态而非失败
const pendingFinal = ref(false)

const MAX_ATTEMPTS = 10
const POLL_INTERVAL = 2000
let attempts = 0
let pollTimer = null

const success = computed(() => result.value?.payStatus === 1)

async function poll() {
  attempts += 1
  try {
    result.value = await getPaymentResult(orderNo)
  } catch (e) {
    /* 保留上次结果，继续轮询 */
  }
  const status = result.value?.payStatus
  // 非待支付态（已支付/已退款等）即终态；Stripe 回跳明确失败且已查过一次也提前结束
  const settled = status != null && status !== 0
  const failedRedirectChecked = redirectStatus === 'failed' && attempts >= 1 && result.value != null
  if (settled || failedRedirectChecked) {
    polling.value = false
    pendingFinal.value = false
    return
  }
  if (attempts >= MAX_ATTEMPTS) {
    polling.value = false
    pendingFinal.value = true
    return
  }
  pollTimer = setTimeout(poll, POLL_INTERVAL)
}

function recheck() {
  attempts = 0
  pendingFinal.value = false
  polling.value = true
  poll()
}

onMounted(poll)

onBeforeUnmount(() => {
  if (pollTimer) clearTimeout(pollTimer)
})
</script>

<style scoped>
.result-block {
  max-width: 720px;
  margin: 20px auto;
}

.confirming-block {
  text-align: center;
  padding: 60px 20px;
}

.confirming-icon {
  color: #12945b;
}

.confirming-text {
  font-size: 16px;
  margin: 16px 0 6px;
}

.confirming-order {
  color: #999;
  font-size: 13px;
}

.result-desc {
  width: 480px;
  max-width: 100%;
  margin: 0 auto 20px;
  text-align: left;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.settle-notice {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}
</style>
