<template>
  <div class="page-container" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="15">
        <div class="card-block">
          <h2 class="section-title">{{ t('booking.title') }}</h2>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
            <el-form-item :label="t('room.checkinDate')" prop="checkinDate">
              <el-date-picker
                v-model="form.checkinDate"
                type="date"
                :placeholder="t('booking.checkinPh')"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledCheckin"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('room.checkoutDate')" prop="checkoutDate">
              <el-date-picker
                v-model="form.checkoutDate"
                type="date"
                :placeholder="t('booking.checkoutPh')"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledCheckout"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('booking.guestCount')" prop="guestCount">
              <el-input-number v-model="form.guestCount" :min="1" :max="room?.maxGuests || 10" />
              <span class="capacity-hint" v-if="room?.maxGuests">
                {{ t('booking.capacityHint', { n: room.maxGuests }) }}
              </span>
            </el-form-item>
            <el-form-item :label="t('booking.guestName')" prop="guestName">
              <el-input v-model="form.guestName" :placeholder="t('booking.guestNamePh')" clearable />
            </el-form-item>
            <el-form-item :label="t('booking.contactPhone')" prop="contactPhone">
              <el-input v-model="form.contactPhone" :placeholder="t('booking.contactPhonePh')" maxlength="20" clearable />
            </el-form-item>
            <el-form-item :label="t('booking.remark')" prop="userRemark">
              <el-input
                v-model="form.userRemark"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                :placeholder="t('booking.remarkPh')"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="success" size="large" :loading="submitting" @click="handleSubmit">
                {{ t('booking.submit') }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <el-col :span="9">
        <div class="card-block order-panel">
          <h3 class="panel-title">{{ t('booking.orderInfo') }}</h3>
          <template v-if="room">
            <div class="order-room">
              <div
                class="order-thumb"
                :style="{ backgroundImage: `url(${room.coverImage})` }"
              ></div>
              <div>
                <div class="order-room-name">{{ room.roomName }}</div>
                <div class="order-room-meta">
                  {{ room.bedInfo || '' }}
                  {{ room.maxGuests ? `· ${t('home.guestsUpTo', { n: room.maxGuests })}` : '' }}
                </div>
              </div>
            </div>
            <el-divider />
            <div class="order-line">
              <span>{{ t('booking.stayRange') }}</span>
              <span>{{ t('common.dateRange', { from: form.checkinDate || '-', to: form.checkoutDate || '-' }) }}</span>
            </div>

            <!-- Airbnb 式价格明细（来自 POST /api/bookings/quote） -->
            <div v-loading="quoteLoading">
              <template v-if="quote">
                <div class="order-line" data-test="quote-room-fee">
                  <span>
                    {{ t('booking.roomFee') }}
                    <small class="fee-calc">{{ t('booking.feeTimes', { price: format(quote.unitPrice), n: quote.nights }) }}</small>
                  </span>
                  <span>{{ format(quote.roomFee) }}</span>
                </div>
                <div
                  v-if="Number(quote.promoDiscount) > 0"
                  class="order-line discount"
                  data-test="quote-promo"
                >
                  <span>{{ quote.promoName || t('booking.promoDiscount') }}</span>
                  <span>-{{ format(quote.promoDiscount) }}</span>
                </div>
                <div
                  v-if="Number(quote.memberDiscount) > 0"
                  class="order-line discount"
                  data-test="quote-member-discount"
                >
                  <span>{{ t('booking.memberDiscount') }}</span>
                  <span>-{{ format(quote.memberDiscount) }}</span>
                </div>
                <el-divider />
                <div class="order-line total" data-test="quote-total">
                  <span>{{ t('booking.total') }}</span>
                  <span class="price">{{ format(quote.totalAmount) }}</span>
                </div>
                <div v-if="isForeign" class="settle-notice">{{ t('currency.settleNotice', { cur: base }) }}</div>
              </template>
              <div v-else-if="quoteError" class="quote-error">{{ t('booking.quoteFailed') }}</div>
            </div>

            <!-- 优惠码 -->
            <el-divider />
            <div class="promo-block">
              <div class="promo-row">
                <div class="promo-input-wrap" data-test="promo-input">
                  <el-input
                    v-model="promoInput"
                    :placeholder="t('booking.promoCodePh')"
                    :disabled="!!appliedPromo"
                    clearable
                    @keyup.enter="applyPromo"
                  />
                </div>
                <el-button
                  v-if="!appliedPromo"
                  data-test="promo-apply"
                  type="success"
                  plain
                  :disabled="!promoInput.trim() || quoteLoading"
                  @click="applyPromo"
                >
                  {{ t('booking.promoApply') }}
                </el-button>
                <el-button
                  v-else
                  data-test="promo-remove"
                  type="danger"
                  plain
                  :disabled="quoteLoading"
                  @click="removePromo"
                >
                  {{ t('booking.promoRemove') }}
                </el-button>
              </div>
              <div v-if="promoError" class="promo-error" data-test="promo-error">{{ promoError }}</div>
            </div>

            <div class="order-tip">{{ t('booking.discountTip') }}</div>
          </template>
          <el-empty v-else :description="t('booking.roomLoading')" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getRoomDetail } from '@/api/room'
import { quoteBooking, createBooking } from '@/api/booking'
import { useCurrency } from '@/composables/useCurrency'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { format, isForeign, base } = useCurrency()

const roomId = route.params.roomId
const room = ref(null)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

// 与后端 BookingCreateRequest 字段一致（promoCode 为契约新增）
const form = reactive({
  checkinDate: route.query.checkin || '',
  checkoutDate: route.query.checkout || '',
  guestCount: 1,
  guestName: '',
  contactPhone: '',
  userRemark: ''
})

// 报价与优惠码状态
const quote = ref(null)
const quoteLoading = ref(false)
const quoteError = ref(false)
const promoInput = ref('')
const appliedPromo = ref('')
const promoError = ref('')
let quoteTimer = null

const rules = computed(() => ({
  checkinDate: [{ required: true, message: t('booking.vCheckinRequired'), trigger: 'change' }],
  checkoutDate: [
    { required: true, message: t('booking.vCheckoutRequired'), trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value && form.checkinDate && value <= form.checkinDate) callback(new Error(t('booking.vCheckoutAfter')))
        else callback()
      },
      trigger: 'change'
    }
  ],
  guestCount: [{ required: true, message: t('booking.vGuestCountRequired'), trigger: 'change' }],
  guestName: [{ required: true, message: t('booking.vGuestNameRequired'), trigger: 'blur' }],
  contactPhone: [
    { required: true, message: t('booking.contactPhonePh'), trigger: 'blur' },
    // 不限位数，仅要求纯数字（支持日本 080 等各国制式）
    { pattern: /^\d+$/, message: t('auth.vPhoneFormat'), trigger: 'blur' }
  ]
}))

const nights = computed(() => {
  if (!form.checkinDate || !form.checkoutDate) return 0
  const diff = new Date(form.checkoutDate).getTime() - new Date(form.checkinDate).getTime()
  return diff > 0 ? Math.round(diff / 86400000) : 0
})

function canQuote() {
  return form.checkinDate && form.checkoutDate && form.checkinDate < form.checkoutDate
}

/**
 * 报价：POST /api/bookings/quote
 * codeOverride: undefined=用当前已应用的码；''=强制不带码；其他=试用该码
 */
async function fetchQuote(codeOverride) {
  if (!canQuote()) {
    quote.value = null
    return null
  }
  const code = codeOverride !== undefined ? codeOverride : appliedPromo.value
  quoteLoading.value = true
  quoteError.value = false
  try {
    const data = await quoteBooking({
      roomId: Number(roomId),
      checkinDate: form.checkinDate,
      checkoutDate: form.checkoutDate,
      promoCode: code || undefined
    })
    quote.value = data
    return data
  } finally {
    quoteLoading.value = false
  }
}

function refreshQuote() {
  fetchQuote().catch((e) => {
    if (appliedPromo.value) {
      // 已应用的码在新日期下无效：提示并回退到无码报价
      promoError.value = e?.message || t('booking.quoteFailed')
      appliedPromo.value = ''
      fetchQuote('').catch(() => { quoteError.value = true })
    } else {
      quoteError.value = true
    }
  })
}

async function applyPromo() {
  const code = promoInput.value.trim()
  if (!code || quoteLoading.value) return
  promoError.value = ''
  try {
    await fetchQuote(code)
    appliedPromo.value = code
  } catch (e) {
    // 无效码：展示后端 message，恢复无码报价
    promoError.value = e?.message || t('booking.quoteFailed')
    fetchQuote('').catch(() => { quoteError.value = true })
  }
}

function removePromo() {
  appliedPromo.value = ''
  promoInput.value = ''
  promoError.value = ''
  fetchQuote('').catch(() => { quoteError.value = true })
}

// 日期变化 300ms 防抖重新报价
watch(
  () => [form.checkinDate, form.checkoutDate],
  () => {
    if (quoteTimer) clearTimeout(quoteTimer)
    quoteTimer = setTimeout(() => {
      promoError.value = ''
      refreshQuote()
    }, 300)
  }
)

function disabledCheckin(date) {
  return date.getTime() < Date.now() - 24 * 3600 * 1000
}

function disabledCheckout(date) {
  if (form.checkinDate) return date.getTime() <= new Date(form.checkinDate).getTime()
  return date.getTime() < Date.now()
}

async function handleSubmit() {
  await formRef.value.validate()
  if (nights.value <= 0) {
    ElMessage.warning(t('booking.dateInvalid'))
    return
  }
  submitting.value = true
  try {
    // 返回 Booking 实体（含 orderNo）；服务端按同一 PriceService 重算
    const booking = await createBooking({
      roomId: Number(roomId),
      checkinDate: form.checkinDate,
      checkoutDate: form.checkoutDate,
      guestCount: form.guestCount,
      guestName: form.guestName,
      contactPhone: form.contactPhone,
      userRemark: form.userRemark,
      promoCode: appliedPromo.value || undefined
    })
    ElMessage.success(t('booking.submitSuccess'))
    router.push(`/pay/${booking.orderNo}`)
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const detail = await getRoomDetail(roomId)
    room.value = detail?.room || null
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
  refreshQuote()
})

onBeforeUnmount(() => {
  if (quoteTimer) clearTimeout(quoteTimer)
})
</script>

<style scoped>
.panel-title {
  margin: 0 0 16px;
}

.order-room {
  display: flex;
  gap: 12px;
  align-items: center;
}

.order-thumb {
  width: 90px;
  height: 64px;
  border-radius: 6px;
  flex-shrink: 0;
  background-color: #eceff1;
  background-size: cover;
  background-position: center;
}

.order-room-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.order-room-meta {
  font-size: 13px;
  color: #999;
}

.order-line {
  display: flex;
  justify-content: space-between;
  margin: 10px 0;
  color: #555;
  font-size: 14px;
}

.order-line.discount {
  color: #12945b;
  font-weight: 500;
}

.fee-calc {
  color: #999;
  margin-left: 6px;
  font-weight: 400;
}

.order-line.total {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.order-line.total .price {
  font-size: 22px;
}

.quote-error {
  color: #f56c6c;
  font-size: 13px;
  margin: 8px 0;
}

.settle-notice {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.promo-block {
  margin: 4px 0 8px;
}

.promo-row {
  display: flex;
  gap: 8px;
}

.promo-input-wrap {
  flex: 1;
}

.promo-error {
  color: #f56c6c;
  font-size: 13px;
  margin-top: 6px;
}

.order-tip {
  font-size: 12px;
  color: #bbb;
  text-align: right;
  margin-top: 8px;
}

.capacity-hint {
  margin-left: 10px;
  color: #999;
  font-size: 13px;
}
</style>
