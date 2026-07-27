<template>
  <div class="page-container auth-page">
    <el-card class="auth-card">
      <h2 class="auth-title">{{ t('auth.registerTitle') }}</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto" @submit.prevent>
        <el-form-item :label="t('auth.username')" prop="username">
          <el-input v-model="form.username" :placeholder="t('auth.usernamePh')" clearable />
        </el-form-item>
        <el-form-item :label="t('auth.phone')" prop="phone">
          <!-- 国家区号可搜索下拉（国家名中/英/区号均可搜）+ 号码纯数字不限位数 -->
          <div class="phone-row">
            <el-select
              v-model="form.countryIso"
              filterable
              :filter-method="filterCountry"
              class="country-select"
              data-test="country-select"
              :placeholder="t('auth.countryCodePh')"
              @visible-change="resetCountryFilter"
            >
              <el-option
                v-for="c in countryOptions"
                :key="c.iso"
                :value="c.iso"
                :label="`${flagEmoji(c.iso)} ${c.dial}`"
              >
                <span>{{ flagEmoji(c.iso) }} {{ c.en }} {{ c.zh }} {{ c.dial }}</span>
              </el-option>
            </el-select>
            <el-input v-model="form.phone" :placeholder="t('auth.phonePh')" clearable class="phone-input" />
          </div>
        </el-form-item>
        <el-form-item :label="t('auth.password')" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="t('auth.passwordPh')" show-password />
        </el-form-item>
        <el-form-item :label="t('auth.confirmPassword')" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" :placeholder="t('auth.confirmPasswordPh')" show-password />
        </el-form-item>
        <el-form-item :label="t('auth.captcha')" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" :placeholder="t('auth.captchaPh')" />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              class="captcha-img"
              :alt="t('auth.captcha')"
              :title="t('auth.captchaRefresh')"
              @click="refreshCaptcha"
            />
            <el-button v-else text type="primary" @click="refreshCaptcha">{{ t('auth.getCaptcha') }}</el-button>
          </div>
        </el-form-item>
        <el-form-item prop="agree">
          <el-checkbox v-model="form.agree">
            {{ t('auth.agreePrefix') }}
            <el-link type="success" :underline="false">{{ t('auth.userAgreement') }}</el-link>
            {{ t('auth.andConj') }}
            <el-link type="success" :underline="false">{{ t('auth.privacyPolicy') }}</el-link>
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="success" class="submit-btn" :loading="submitting" @click="handleRegister">
            {{ t('auth.registerBtn') }}
          </el-button>
        </el-form-item>
        <div class="auth-extra">
          {{ t('auth.hasAccount') }}<router-link to="/login" class="link">{{ t('auth.goLogin') }}</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { register, getCaptcha } from '@/api/auth'
import { COUNTRY_CODES, filterCountries, flagEmoji, dialOf } from '@/utils/countryCodes'

const router = useRouter()
const { t } = useI18n()
const formRef = ref()
const submitting = ref(false)
const captchaImg = ref('')
const captchaKey = ref('')

const form = reactive({
  username: '',
  // 默认日本 +81（站点主要服务日本住宿）
  countryIso: 'JP',
  phone: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  agree: false
})

const countryOptions = ref(COUNTRY_CODES)

function filterCountry(query) {
  countryOptions.value = filterCountries(query)
}

function resetCountryFilter(visible) {
  if (visible) countryOptions.value = COUNTRY_CODES
}

const rules = computed(() => ({
  username: [
    { required: true, message: t('auth.vUsernameRequired'), trigger: 'blur' },
    { min: 3, max: 20, message: t('auth.vUsernameLen'), trigger: 'blur' }
  ],
  phone: [
    { required: true, message: t('auth.vPhoneRequired'), trigger: 'blur' },
    // 不限位数，仅要求纯数字（支持日本 080 等各国制式）
    { pattern: /^\d+$/, message: t('auth.vPhoneFormat'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('auth.vPasswordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('auth.vPasswordLen'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('auth.vConfirmRequired'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error(t('auth.vConfirmMismatch')))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  captchaCode: [{ required: true, message: t('auth.vCaptchaRequired'), trigger: 'blur' }],
  agree: [
    {
      validator: (rule, value, callback) => {
        if (!value) callback(new Error(t('auth.vAgreeRequired')))
        else callback()
      },
      trigger: 'change'
    }
  ]
}))

async function refreshCaptcha() {
  try {
    // CaptchaVO: { key, imageBase64 }
    const data = await getCaptcha()
    captchaKey.value = data?.key || ''
    const img = data?.imageBase64 || ''
    captchaImg.value = img && !img.startsWith('data:') ? `data:image/png;base64,${img}` : img
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function handleRegister() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await register({
      username: form.username,
      phone: form.phone,
      phoneCountry: dialOf(form.countryIso),
      password: form.password,
      confirmPassword: form.confirmPassword,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode
    })
    ElMessage.success(t('auth.registerSuccess'))
    router.push('/login')
  } catch (e) {
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.auth-card {
  width: 500px;
  max-width: 100%;
}

.auth-title {
  text-align: center;
  margin: 8px 0 24px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  height: 32px;
  width: 100px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.submit-btn {
  width: 100%;
}

.auth-extra {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.link {
  color: #12945b;
  margin-left: 4px;
}

.phone-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.country-select {
  width: 132px;
  flex-shrink: 0;
}

.phone-input {
  flex: 1;
}
</style>
