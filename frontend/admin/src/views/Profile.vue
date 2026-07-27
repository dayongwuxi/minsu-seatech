<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateProfile, updatePassword } from '@/api/auth'
import { uploadFile } from '@/api/upload'
import { useAdminStore } from '@/store/admin'

const adminStore = useAdminStore()

const infoFormRef = ref()
const pwdFormRef = ref()
const infoLoading = ref(false)
const pwdLoading = ref(false)

// ProfileUpdateRequest：{ name, phone, email, avatar }
const infoForm = reactive({ avatar: '', name: '', phone: '', email: '' })
// PasswordUpdateRequest：{ oldPassword, newPassword }
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const infoRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

// 对接 POST /api/admin/upload，返回 { url }
async function handleAvatarUpload(options) {
  try {
    const data = await uploadFile(options.file)
    infoForm.avatar = data.url
    ElMessage.success('头像上传成功，保存信息后生效')
  } catch (e) { /* 拦截器已提示 */ }
}

async function saveInfo() {
  await infoFormRef.value.validate()
  infoLoading.value = true
  try {
    await updateProfile({ name: infoForm.name, phone: infoForm.phone, email: infoForm.email, avatar: infoForm.avatar })
    ElMessage.success('个人信息已保存')
    adminStore.fetchProfile()
  } catch (e) { /* 拦截器已提示 */ } finally {
    infoLoading.value = false
  }
}

async function savePassword() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) { /* 拦截器已提示 */ } finally {
    pwdLoading.value = false
  }
}

onMounted(async () => {
  const profile = adminStore.profile || (await adminStore.fetchProfile())
  if (profile) {
    infoForm.avatar = profile.avatar || ''
    infoForm.name = profile.name || ''
    infoForm.phone = profile.phone || ''
    infoForm.email = profile.email || ''
  }
})
</script>

<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :md="12" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>个人信息</span></div></template>
          <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="90px">
            <el-form-item label="头像">
              <el-upload :show-file-list="false" :http-request="handleAvatarUpload" accept="image/*">
                <el-avatar :size="80" :src="infoForm.avatar">
                  <el-icon :size="30"><UserFilled /></el-icon>
                </el-avatar>
                <div style="font-size: 12px; color: #8a94a6">点击更换头像</div>
              </el-upload>
            </el-form-item>
            <el-form-item label="姓名" prop="name">
              <el-input v-model="infoForm.name" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="infoLoading" @click="saveInfo">保存信息</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :md="12" :xs="24" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header><div class="card-header"><span>密码修改</span></div></template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
