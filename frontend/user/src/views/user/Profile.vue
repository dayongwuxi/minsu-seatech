<template>
  <div>
    <div class="card-block" v-loading="loading">
      <h2 class="section-title">
        {{ t('user.profile.title') }}
        <el-button type="success" plain size="small" @click="openEdit">{{ t('user.profile.edit') }}</el-button>
      </h2>
      <!-- MemberVO：phone/email 已由后端脱敏 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('user.profile.memberNo')">{{ profile?.memberNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.username')">{{ profile?.username || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.name')">{{ profile?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.phone')">{{ profile?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.email')">{{ profile?.email || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.memberType')">{{ profile?.memberTypeName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.registerTime')">{{ profile?.registerTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('user.profile.accountStatus')">
          <el-tag :type="profile?.status === 1 ? 'success' : 'danger'">
            {{ profile?.status === 1 ? t('user.profile.statusNormal') : t('user.profile.statusDisabled') }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="card-block" id="password">
      <h2 class="section-title">{{ t('user.profile.pwdTitle') }}</h2>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="auto" class="pwd-form">
        <el-form-item :label="t('user.profile.oldPassword')" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password :placeholder="t('user.profile.oldPasswordPh')" />
        </el-form-item>
        <el-form-item :label="t('user.profile.newPassword')" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password :placeholder="t('user.profile.newPasswordPh')" />
        </el-form-item>
        <el-form-item :label="t('user.profile.confirmNewPassword')" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password :placeholder="t('user.profile.confirmNewPasswordPh')" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="pwdSubmitting" @click="handleUpdatePassword">
            {{ t('user.profile.saveChanges') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 后端 ProfileUpdateRequest 仅支持 name/email/avatar -->
    <el-dialog v-model="editVisible" :title="t('user.profile.edit')" width="480px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="auto">
        <el-form-item :label="t('user.profile.name')" prop="name">
          <el-input v-model="editForm.name" :placeholder="t('user.profile.namePh')" />
        </el-form-item>
        <el-form-item :label="t('user.profile.email')" prop="email">
          <el-input v-model="editForm.email" :placeholder="t('user.profile.emailPh')" />
        </el-form-item>
        <el-form-item :label="t('user.profile.avatar')" prop="avatar">
          <el-input v-model="editForm.avatar" :placeholder="t('user.profile.avatarPh')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" :loading="editSubmitting" @click="handleUpdateProfile">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, updatePassword } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const { t } = useI18n()

const profile = ref(null)
const loading = ref(false)

const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref()
const editForm = reactive({ name: '', email: '', avatar: '' })

const pwdFormRef = ref()
const pwdSubmitting = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const editRules = computed(() => ({
  name: [{ required: true, message: t('user.profile.vNameRequired'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('user.profile.vEmailFormat'), trigger: 'blur' }]
}))

const pwdRules = computed(() => ({
  oldPassword: [{ required: true, message: t('user.profile.vOldPwdRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('user.profile.vNewPwdRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('user.profile.vPwdLen'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('user.profile.vConfirmRequired'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error(t('user.profile.vPwdMismatch')))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}))

async function fetchProfile() {
  loading.value = true
  try {
    profile.value = await getProfile()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function openEdit() {
  editForm.name = profile.value?.name || ''
  // 脱敏邮箱不回填，避免把 a***@x.com 存回去
  editForm.email = ''
  editForm.avatar = profile.value?.avatar || ''
  editVisible.value = true
}

async function handleUpdateProfile() {
  await editFormRef.value.validate()
  editSubmitting.value = true
  try {
    await updateProfile({ name: editForm.name, email: editForm.email, avatar: editForm.avatar })
    ElMessage.success(t('user.profile.updateSuccess'))
    editVisible.value = false
    await fetchProfile()
    if (profile.value) userStore.setMember(profile.value)
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    editSubmitting.value = false
  }
}

async function handleUpdatePassword() {
  await pwdFormRef.value.validate()
  pwdSubmitting.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success(t('user.profile.pwdSuccess'))
    pwdFormRef.value.resetFields()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    pwdSubmitting.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.pwd-form {
  max-width: 520px;
}
</style>
