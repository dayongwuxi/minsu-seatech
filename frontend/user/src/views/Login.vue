<template>
  <div class="page-container auth-page">
    <el-card class="auth-card">
      <h2 class="auth-title">{{ t('auth.loginTitle') }}</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto" @submit.prevent>
        <el-form-item :label="t('auth.account')" prop="account">
          <el-input v-model="form.account" :placeholder="t('auth.accountPh')" clearable @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item :label="t('auth.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('auth.loginPwdPh')"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item :label="t('auth.captcha')" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" :placeholder="t('auth.captchaPh')" @keyup.enter="handleLogin" />
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
        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="form.remember">{{ t('auth.remember') }}</el-checkbox>
            <el-link type="info" :underline="false" @click="forgotPassword">{{ t('auth.forgot') }}</el-link>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="success" class="submit-btn" :loading="submitting" @click="handleLogin">
            {{ t('auth.loginBtn') }}
          </el-button>
        </el-form-item>
        <div class="auth-extra">
          {{ t('auth.noAccount') }}<router-link to="/register" class="link">{{ t('auth.goRegister') }}</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const formRef = ref()
const submitting = ref(false)
const captchaImg = ref('')
const captchaKey = ref('')

const form = reactive({
  account: localStorage.getItem('user_remember_account') || '',
  password: '',
  captchaCode: '',
  remember: !!localStorage.getItem('user_remember_account')
})

const rules = computed(() => ({
  account: [{ required: true, message: t('auth.vAccountRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('auth.vPasswordRequired'), trigger: 'blur' }],
  captchaCode: [{ required: true, message: t('auth.vCaptchaRequired'), trigger: 'blur' }]
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

async function handleLogin() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await userStore.login({
      account: form.account,
      password: form.password,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode
    })
    if (form.remember) {
      localStorage.setItem('user_remember_account', form.account)
    } else {
      localStorage.removeItem('user_remember_account')
    }
    ElMessage.success(t('auth.loginSuccess'))
    router.push(route.query.redirect || '/')
  } catch (e) {
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

function forgotPassword() {
  ElMessage.info(t('auth.forgotTip'))
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
  width: 440px;
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

.login-options {
  display: flex;
  justify-content: space-between;
  width: 100%;
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
</style>
