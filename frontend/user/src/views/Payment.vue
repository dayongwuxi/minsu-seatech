<template>
  <div class="page-container" v-loading="loading">
    <div class="card-block">
      <h2 class="section-title">{{ t('pay.title') }}</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('pay.orderNo')">{{ orderNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('pay.roomName')">{{ detail?.room?.roomName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('room.checkinDate')">{{ booking?.checkinDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('room.checkoutDate')">{{ booking?.checkoutDate || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('pay.guest')">{{ booking?.guestName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('pay.nightsLabel')">
          {{ booking?.nights != null ? t('pay.nightsValue', { n: booking.nights }) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('pay.discount')">
          {{ booking ? format(booking.discountAmount ?? 0) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('pay.amount')">
          <span class="price">{{ booking ? format(booking.totalAmount) : '-' }}</span>
          <span v-if="isForeign" class="settle-notice">{{ t('currency.settleNotice', { cur: base }) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 已存卡（个人中心-支付管理）单选列表 -->
    <div v-if="savedMethods.length" class="card-block" data-test="saved-cards">
      <h3>{{ t('pay.savedCards') }}</h3>
      <el-radio-group v-model="selectedMethodId" class="saved-group">
        <el-radio v-for="m in savedMethods" :key="m.id" :value="m.id" border class="saved-radio">
          <span class="pm-brand">{{ brandLabel(m.brand) }}</span>
          **** {{ m.last4 }}
          <span class="pm-exp">{{ expText(m) }}</span>
          <el-tag v-if="m.isDefault === 1" size="small" type="success">{{ t('user.payMethods.defaultTag') }}</el-tag>
        </el-radio>
        <el-radio value="new" border class="saved-radio">{{ t('pay.useNew') }}</el-radio>
      </el-radio-group>
    </div>

    <!-- LinkTrust 跳转支付（微信/支付宝/银联；表单 POST 收银台） -->
    <div v-if="config && config.linktrustEnabled" class="card-block" data-test="linktrust-pay">
      <h3>{{ t('pay.chooseMethod') }}</h3>
      <p class="linktrust-hint">{{ t('pay.linktrustHint') }}</p>
      <el-radio-group v-model="linktrustMethod" class="pay-group">
        <el-radio v-for="code in linktrustOptions" :key="code" :value="code" border class="pay-radio">
          {{ dictText(PAY_METHOD, code, t) }}
        </el-radio>
      </el-radio-group>
      <div class="pay-actions">
        <el-button
          type="success"
          size="large"
          data-test="linktrust-confirm"
          :loading="paying"
          @click="handleLinkTrustPay"
        >
          {{ t('pay.confirmPay') }}
        </el-button>
        <el-button v-if="!config.stripeEnabled" size="large" @click="$router.push('/user/bookings')">
          {{ t('pay.payLater') }}
        </el-button>
      </div>
    </div>

    <!-- Stripe Payment Element（stripeEnabled=true） -->
    <div v-if="config && config.stripeEnabled" class="card-block" data-test="stripe-pay">
      <h3>{{ t('pay.chooseMethod') }}</h3>
      <div v-show="useNewMethod" class="stripe-box" v-loading="stripeLoading" :element-loading-text="t('pay.stripeLoading')">
        <div id="stripe-payment-element"></div>
      </div>
      <el-alert
        v-if="stripeError"
        type="error"
        :title="stripeError"
        :closable="false"
        show-icon
        class="stripe-alert"
      />
      <div class="pay-actions">
        <el-button
          type="success"
          size="large"
          data-test="stripe-confirm"
          :loading="paying"
          :disabled="useNewMethod && (stripeLoading || !stripeMounted)"
          @click="handleStripePay"
        >
          {{ t('pay.confirmPay') }}
        </el-button>
        <el-button size="large" @click="$router.push('/user/bookings')">{{ t('pay.payLater') }}</el-button>
      </div>
    </div>

    <!-- 模拟支付（无任何真实渠道时保留原 UI） -->
    <div v-if="config && !config.stripeEnabled && !config.linktrustEnabled" class="card-block" data-test="mock-pay">
      <h3>{{ t('pay.chooseMethod') }}</h3>
      <!-- payMethod: 1微信 2支付宝 3银行卡 4其他 -->
      <el-radio-group v-if="useNewMethod" v-model="payMethod" class="pay-group">
        <el-radio v-for="opt in payMethodOptions" :key="opt.value" :value="opt.value" border class="pay-radio">
          {{ opt.label }}
        </el-radio>
      </el-radio-group>
      <div class="pay-actions">
        <el-button type="success" size="large" :loading="paying" @click="handleMockPay">{{ t('pay.confirmPay') }}</el-button>
        <el-button size="large" @click="$router.push('/user/bookings')">{{ t('pay.payLater') }}</el-button>
      </div>
    </div>

    <!-- 支付配置加载中 -->
    <div v-if="!config" class="card-block" v-loading="true" :element-loading-text="t('pay.stripeLoading')">
      <div class="config-loading-placeholder"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { loadStripe } from '@stripe/stripe-js'
import { getBookingDetail } from '@/api/booking'
import { getPaymentConfig, createStripeIntent, createPayment, createLinkTrustPayment } from '@/api/payment'
import { listPayMethods } from '@/api/paymethod'
import { PAY_METHOD, dictOptions, dictText } from '@/utils/dict'
import { useCurrency } from '@/composables/useCurrency'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { format, isForeign, base } = useCurrency()

const orderNo = route.params.orderNo
// BookingDetailVO: { booking, member, room, payment, refund }
const detail = ref(null)
const loading = ref(false)
const paying = ref(false)

// GET /api/payments/config → { stripeEnabled, publishableKey, methods[], currency }
const config = ref(null)
const stripeLoading = ref(false)
const stripeMounted = ref(false)
const stripeError = ref('')
let stripe = null
let elements = null

// 已存卡：'new' 表示使用新支付方式
const savedMethods = ref([])
const selectedMethodId = ref('new')

const payMethod = ref(1)
const payMethodOptions = computed(() => dictOptions(PAY_METHOD, t))

// LinkTrust 跳转支付：方式编码 1微信 2支付宝 7银联（以 config.linktrustMethods 为准）
const linktrustMethod = ref(null)
const linktrustOptions = computed(() => {
  const list = config.value?.linktrustMethods
  return Array.isArray(list) && list.length ? list : [1, 2, 7]
})

const booking = computed(() => detail.value?.booking || null)
const useNewMethod = computed(() => selectedMethodId.value === 'new')

const BRAND_LABELS = { visa: 'VISA', mastercard: 'Mastercard', jcb: 'JCB', unionpay: 'UnionPay' }

function brandLabel(brand) {
  return BRAND_LABELS[String(brand || '').toLowerCase()] || String(brand || 'CARD').toUpperCase()
}

function expText(m) {
  if (m.expMonth == null || m.expYear == null) return ''
  return `${String(m.expMonth).padStart(2, '0')}/${String(m.expYear).slice(-2)}`
}

async function fetchOrder() {
  loading.value = true
  try {
    detail.value = await getBookingDetail(orderNo)
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function fetchSavedMethods() {
  try {
    savedMethods.value = (await listPayMethods()) || []
    const def = savedMethods.value.find((m) => m.isDefault === 1)
    // 默认卡预选
    if (def) selectedMethodId.value = def.id
  } catch (e) {
    savedMethods.value = []
  }
}

async function setupStripe() {
  stripeLoading.value = true
  stripeError.value = ''
  try {
    // POST /api/payments/stripe/intent {orderNo} → { clientSecret, ... }
    const intent = await createStripeIntent(orderNo)
    stripe = await loadStripe(config.value.publishableKey)
    if (!stripe || !intent?.clientSecret) {
      throw new Error(t('pay.stripeInitFailed'))
    }
    // Payment Element：支付方式由 Stripe 按 intent 的 payment_method_types 自动渲染
    elements = stripe.elements({ clientSecret: intent.clientSecret })
    const paymentElement = elements.create('payment', { layout: 'tabs' })
    await nextTick()
    paymentElement.mount('#stripe-payment-element')
    stripeMounted.value = true
  } catch (e) {
    stripeError.value = e?.message || t('pay.stripeInitFailed')
  } finally {
    stripeLoading.value = false
  }
}

async function handleStripePay() {
  paying.value = true
  stripeError.value = ''
  try {
    if (!useNewMethod.value) {
      // 已存卡：后端用 customer+payment_method 服务端确认；
      // 需要 3DS 等客户操作时后端返回 requiresAction，用 handleNextAction 完成后再进结果页
      const intent = await createStripeIntent(orderNo, selectedMethodId.value)
      if (intent?.requiresAction && intent?.clientSecret) {
        if (!stripe) stripe = await loadStripe(config.value.publishableKey)
        const next = await stripe.handleNextAction({ clientSecret: intent.clientSecret })
        if (next?.error) {
          stripeError.value = next.error.message || t('http.requestFailed')
          return
        }
      }
      router.push(`/pay-result/${orderNo}`)
      return
    }
    if (!stripe || !elements) return
    // 成功场景由 Stripe 重定向到 return_url，无需前端跳转
    const result = await stripe.confirmPayment({
      elements,
      confirmParams: {
        return_url: `${window.location.origin}/pay-result/${orderNo}`
      }
    })
    if (result?.error) {
      stripeError.value = result.error.message || t('http.requestFailed')
    }
  } catch (e) {
    stripeError.value = e?.message || t('http.requestFailed')
  } finally {
    paying.value = false
  }
}

/**
 * GET 链接跳转收银台（官方 sample 同款）。
 * 不用表单 POST：HTTPS 页面向 HTTP 收银台提交表单会触发浏览器「不安全提交」拦截弹窗，
 * 普通链接跳转则不会（渠道服务器仅开 HTTP，443 证书链不完整）。
 */
function linktrustUrl({ payUrl, merId, merOrderId, amount }) {
  const query = new URLSearchParams({ merId, merOrderId, amount: String(amount) })
  return `${payUrl}?${query.toString()}`
}

async function handleLinkTrustPay() {
  if (!linktrustMethod.value) {
    ElMessage.warning(t('pay.selectMethodWarn'))
    return
  }
  paying.value = true
  // 点击时同步开新标签页保留用户手势，防止 await 后 window.open 被弹窗拦截
  const win = window.open('about:blank', '_blank')
  try {
    const data = await createLinkTrustPayment(orderNo, linktrustMethod.value)
    const url = linktrustUrl(data)
    if (win) {
      win.location.replace(url)
      // 收银台在新标签页，本页转结果页轮询；IPN 未达时由后端反查兜底
      router.push(`/pay-result/${orderNo}`)
    } else {
      // 弹窗被拦截：当前页直接跳转收银台，支付后从「我的预定」返回查看结果
      window.location.assign(url)
    }
  } catch (e) {
    if (win) win.close()
    /* 拦截器已提示 */
  } finally {
    paying.value = false
  }
}

async function handleMockPay() {
  if (useNewMethod.value && !payMethod.value) {
    ElMessage.warning(t('pay.selectMethodWarn'))
    return
  }
  paying.value = true
  try {
    await createPayment({
      orderNo,
      // 已存卡视为银行卡通道（payMethod=3）并附 paymentMethodId
      payMethod: useNewMethod.value ? payMethod.value : 3,
      paymentMethodId: useNewMethod.value ? undefined : selectedMethodId.value
    })
    router.push(`/pay-result/${orderNo}`)
  } catch (e) {
    router.push(`/pay-result/${orderNo}`)
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  fetchOrder()
  fetchSavedMethods()
  try {
    config.value = await getPaymentConfig()
  } catch (e) {
    // 配置获取失败降级为模拟支付
    config.value = { stripeEnabled: false }
  }
  if (config.value?.linktrustEnabled) {
    linktrustMethod.value = linktrustOptions.value[0] ?? null
  }
  if (config.value?.stripeEnabled) {
    await setupStripe()
  }
})
</script>

<style scoped>
.pay-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin: 16px 0 24px;
}

.pay-radio {
  min-width: 160px;
  height: 52px;
  margin-right: 0;
}

.saved-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-top: 12px;
}

.saved-radio {
  height: 52px;
  margin-right: 0;
}

.pm-brand {
  font-weight: 700;
  color: #12945b;
  margin-right: 6px;
}

.pm-exp {
  color: #999;
  font-size: 12px;
  margin: 0 6px;
}

.pay-actions {
  display: flex;
  gap: 12px;
}

.stripe-box {
  min-height: 120px;
  margin: 16px 0;
}

.linktrust-hint {
  color: #999;
  font-size: 13px;
  margin: 8px 0 0;
}

.stripe-alert {
  margin-bottom: 16px;
}

.settle-notice {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}

.config-loading-placeholder {
  height: 140px;
}
</style>
