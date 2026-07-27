<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { useAdminStore } from '@/store/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const formRef = ref()
const loading = ref(false)
const captchaImage = ref('')
const form = reactive({
  username: localStorage.getItem('admin_remember_username') || '',
  password: '',
  captchaCode: '',
  captchaKey: '',
  remember: !!localStorage.getItem('admin_remember_username')
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    // CaptchaVO { key, imageBase64 }
    const data = await getCaptcha()
    form.captchaKey = data.key || ''
    const img = data.imageBase64 || ''
    captchaImage.value = img.startsWith('data:') ? img : `data:image/png;base64,${img}`
  } catch (e) {
    /* 验证码加载失败可点击重试 */
  }
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await adminStore.login({
      username: form.username,
      password: form.password,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    if (form.remember) {
      localStorage.setItem('admin_remember_username', form.username)
    } else {
      localStorage.removeItem('admin_remember_username')
    }
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } catch (e) {
    form.captchaCode = ''
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="login-title">
        <el-icon :size="28" color="#2f6bff"><HomeFilled /></el-icon>
        <h2>Yadogo Trip 管理后台</h2>
        <p>Yadogo Trip 民宿在线预约 · 管理员登录</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员账号" :prefix-icon="'User'" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入登录密码" :prefix-icon="'Lock'" show-password />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" :prefix-icon="'Picture'" />
            <div class="captcha-img" title="点击刷新验证码" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else>点击获取</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.remember">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #16244a 0%, #2f6bff 100%);
}
.login-card {
  width: 400px;
  border-radius: 12px;
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
}
.login-title h2 {
  margin: 8px 0 4px;
  color: #16244a;
}
.login-title p {
  color: #8a94a6;
  font-size: 13px;
}
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.captcha-img {
  width: 120px;
  height: 40px;
  flex-shrink: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #8a94a6;
  overflow: hidden;
}
.captcha-img img {
  width: 100%;
  height: 100%;
}
.login-btn {
  width: 100%;
}
</style>
