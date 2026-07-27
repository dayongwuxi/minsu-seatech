<template>
  <div class="card-block">
    <h2 class="section-title">
      {{ t('user.payMethods.title') }}
      <el-button type="success" size="small" data-test="pm-add" @click="openAdd">
        {{ t('user.payMethods.add') }}
      </el-button>
    </h2>

    <div v-loading="loading">
      <el-empty v-if="!loading && !methods.length" :description="t('user.payMethods.empty')" />
      <div v-for="m in methods" :key="m.id" class="pm-item" data-test="pm-item">
        <span class="pm-brand">{{ brandLabel(m.brand) }}</span>
        <span class="pm-number">**** {{ m.last4 }}</span>
        <span class="pm-exp">{{ expText(m) }}</span>
        <span class="pm-holder">{{ m.holderName || '' }}</span>
        <el-tag v-if="m.isDefault === 1" type="success" size="small">{{ t('user.payMethods.defaultTag') }}</el-tag>
        <span class="pm-actions">
          <el-button
            v-if="m.isDefault !== 1"
            type="primary"
            link
            data-test="pm-set-default"
            @click="handleSetDefault(m)"
          >
            {{ t('user.payMethods.setDefault') }}
          </el-button>
          <el-button type="danger" link data-test="pm-remove" @click="handleRemove(m)">
            {{ t('user.payMethods.remove') }}
          </el-button>
        </span>
      </div>
    </div>

    <!-- 添加银行卡 -->
    <el-dialog v-model="addVisible" :title="t('user.payMethods.add')" width="520px" @closed="resetAdd">
      <div v-loading="setupLoading" :element-loading-text="t('pay.stripeLoading')">
        <!-- Stripe 通道：SetupIntent + Payment Element(mode=setup) -->
        <div v-if="mode === 'stripe'" data-test="pm-stripe-form">
          <div id="pm-setup-element" class="stripe-box"></div>
        </div>

        <!-- Mock 通道：本地表单，仅提取 brand/last4/有效期，完整卡号与 CVC 不上传 -->
        <div v-else-if="mode === 'mock'" data-test="pm-mock-form">
          <el-alert type="info" :title="t('user.payMethods.mockNotice')" :closable="false" show-icon class="mock-alert" />
          <el-form ref="cardFormRef" :model="cardForm" :rules="cardRules" label-width="auto">
            <el-form-item :label="t('user.payMethods.cardNumber')" prop="cardNumber">
              <div class="pm-input" data-test="pm-card-number">
                <el-input v-model="cardForm.cardNumber" :placeholder="t('user.payMethods.cardNumberPh')" maxlength="23" />
              </div>
            </el-form-item>
            <el-form-item :label="t('user.payMethods.holderName')" prop="holderName">
              <div class="pm-input" data-test="pm-holder">
                <el-input v-model="cardForm.holderName" :placeholder="t('user.payMethods.holderNamePh')" />
              </div>
            </el-form-item>
            <el-form-item :label="t('user.payMethods.expiry')" prop="expiry">
              <div class="pm-input" data-test="pm-expiry">
                <el-input v-model="cardForm.expiry" :placeholder="t('user.payMethods.expiryPh')" maxlength="5" />
              </div>
            </el-form-item>
            <el-form-item :label="t('user.payMethods.cvc')" prop="cvc">
              <div class="pm-input" data-test="pm-cvc">
                <el-input v-model="cardForm.cvc" :placeholder="t('user.payMethods.cvcPh')" maxlength="4" show-password />
              </div>
            </el-form-item>
          </el-form>
        </div>

        <el-alert
          v-if="setupError"
          type="error"
          :title="setupError"
          :closable="false"
          show-icon
          class="mock-alert"
        />
      </div>
      <template #footer>
        <el-button @click="addVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="success"
          data-test="pm-submit"
          :loading="submitting"
          :disabled="setupLoading || !mode"
          @click="handleSubmit"
        >
          {{ t('common.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { loadStripe } from '@stripe/stripe-js'
import {
  listPayMethods,
  createSetupIntent,
  savePayMethod,
  setDefaultPayMethod,
  removePayMethod
} from '@/api/paymethod'
import { getPaymentConfig } from '@/api/payment'
import { luhnCheck, detectBrand, cardLast4, parseExpiry } from '@/utils/card'

const { t } = useI18n()

const methods = ref([])
const loading = ref(false)

const addVisible = ref(false)
const setupLoading = ref(false)
const submitting = ref(false)
const setupError = ref('')
// 'stripe' | 'mock' | ''（setup-intent 决定）
const mode = ref('')
let stripe = null
let elements = null

const cardFormRef = ref()
const cardForm = reactive({ cardNumber: '', holderName: '', expiry: '', cvc: '' })

const cardRules = computed(() => ({
  cardNumber: [
    { required: true, message: t('user.payMethods.cardNumberPh'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!luhnCheck(value)) callback(new Error(t('user.payMethods.vCardNumber')))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  holderName: [{ required: true, message: t('user.payMethods.vHolder'), trigger: 'blur' }],
  expiry: [
    { required: true, message: t('user.payMethods.vExpiry'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!parseExpiry(value)) callback(new Error(t('user.payMethods.vExpiry')))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  cvc: [
    { required: true, message: t('user.payMethods.vCvc'), trigger: 'blur' },
    { pattern: /^\d{3,4}$/, message: t('user.payMethods.vCvc'), trigger: 'blur' }
  ]
}))

const BRAND_LABELS = {
  visa: 'VISA',
  mastercard: 'Mastercard',
  jcb: 'JCB',
  unionpay: 'UnionPay'
}

function brandLabel(brand) {
  return BRAND_LABELS[String(brand || '').toLowerCase()] || String(brand || 'CARD').toUpperCase()
}

function expText(m) {
  if (m.expMonth == null || m.expYear == null) return ''
  const mm = String(m.expMonth).padStart(2, '0')
  const yy = String(m.expYear).slice(-2)
  return `${mm}/${yy}`
}

async function fetchMethods() {
  loading.value = true
  try {
    methods.value = (await listPayMethods()) || []
  } catch (e) {
    methods.value = []
  } finally {
    loading.value = false
  }
}

async function openAdd() {
  addVisible.value = true
  setupLoading.value = true
  setupError.value = ''
  mode.value = ''
  try {
    // Stripe → {clientSecret}；Mock → {mock:true}
    const res = await createSetupIntent()
    if (res?.mock) {
      mode.value = 'mock'
    } else if (res?.clientSecret) {
      mode.value = 'stripe'
      const config = await getPaymentConfig()
      stripe = await loadStripe(config?.publishableKey)
      if (!stripe) throw new Error(t('pay.stripeInitFailed'))
      elements = stripe.elements({ clientSecret: res.clientSecret })
      const el = elements.create('payment', { layout: 'tabs' })
      await nextTick()
      el.mount('#pm-setup-element')
    } else {
      throw new Error(t('pay.stripeInitFailed'))
    }
  } catch (e) {
    setupError.value = e?.message || t('pay.stripeInitFailed')
  } finally {
    setupLoading.value = false
  }
}

function resetAdd() {
  mode.value = ''
  setupError.value = ''
  stripe = null
  elements = null
  cardForm.cardNumber = ''
  cardForm.holderName = ''
  cardForm.expiry = ''
  cardForm.cvc = ''
}

async function handleSubmit() {
  if (mode.value === 'mock') {
    await submitMock()
  } else if (mode.value === 'stripe') {
    await submitStripe()
  }
}

async function submitMock() {
  // el-form 校验（界面提示）
  try {
    await cardFormRef.value?.validate()
  } catch (e) {
    return
  }
  // 硬校验兜底：任何一项不合法都不提交（安全属性不依赖 UI 校验框架）
  const exp = parseExpiry(cardForm.expiry)
  if (!luhnCheck(cardForm.cardNumber)) {
    setupError.value = t('user.payMethods.vCardNumber')
    return
  }
  if (!cardForm.holderName.trim()) {
    setupError.value = t('user.payMethods.vHolder')
    return
  }
  if (!exp) {
    setupError.value = t('user.payMethods.vExpiry')
    return
  }
  if (!/^\d{3,4}$/.test(cardForm.cvc)) {
    setupError.value = t('user.payMethods.vCvc')
    return
  }
  setupError.value = ''
  submitting.value = true
  try {
    // 提交体只含元数据：完整卡号与 CVC 永不上传
    await savePayMethod({
      brand: detectBrand(cardForm.cardNumber),
      last4: cardLast4(cardForm.cardNumber),
      expMonth: exp.expMonth,
      expYear: exp.expYear,
      holderName: cardForm.holderName.trim()
    })
    ElMessage.success(t('user.payMethods.addSuccess'))
    addVisible.value = false
    fetchMethods()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

async function submitStripe() {
  if (!stripe || !elements) return
  submitting.value = true
  setupError.value = ''
  try {
    const result = await stripe.confirmSetup({
      elements,
      confirmParams: { return_url: `${window.location.origin}/user/pay-methods` },
      redirect: 'if_required'
    })
    if (result?.error) {
      setupError.value = result.error.message || t('http.requestFailed')
      return
    }
    const pmId = result?.setupIntent?.payment_method
    if (pmId) {
      await savePayMethod({ stripePaymentMethodId: pmId })
      ElMessage.success(t('user.payMethods.addSuccess'))
      addVisible.value = false
      fetchMethods()
    }
  } catch (e) {
    setupError.value = e?.message || t('http.requestFailed')
  } finally {
    submitting.value = false
  }
}

async function handleSetDefault(m) {
  try {
    await setDefaultPayMethod(m.id)
    ElMessage.success(t('user.payMethods.setDefaultSuccess'))
    fetchMethods()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function handleRemove(m) {
  await ElMessageBox.confirm(t('user.payMethods.removeConfirm'), t('common.tip'), {
    type: 'warning',
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel')
  })
  await removePayMethod(m.id)
  ElMessage.success(t('user.payMethods.removeSuccess'))
  fetchMethods()
}

onMounted(fetchMethods)
</script>

<style scoped>
.pm-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 6px;
  border-bottom: 1px dashed #ebeef5;
}

.pm-brand {
  display: inline-block;
  min-width: 84px;
  padding: 3px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
  color: #12945b;
}

.pm-number {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  letter-spacing: 1px;
}

.pm-exp,
.pm-holder {
  color: #999;
  font-size: 13px;
}

.pm-actions {
  margin-left: auto;
}

.stripe-box {
  min-height: 120px;
}

.mock-alert {
  margin-bottom: 14px;
}

.pm-input {
  width: 100%;
}
</style>
