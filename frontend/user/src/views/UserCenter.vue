<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="5">
        <div class="card-block side-menu">
          <div class="user-brief">
            <el-avatar :size="56" :icon="UserFilled" />
            <div class="user-name">{{ userStore.nickname || t('common.user') }}</div>
          </div>
          <el-menu :default-active="$route.path" class="center-menu" @select="onSelect">
            <el-menu-item index="/user/profile">
              <el-icon><User /></el-icon>{{ t('user.menu.profile') }}
            </el-menu-item>
            <el-menu-item index="/user/pay-methods">
              <el-icon><CreditCard /></el-icon>{{ t('user.menu.payMethods') }}
            </el-menu-item>
            <el-menu-item index="/user/bookings">
              <el-icon><Tickets /></el-icon>{{ t('user.menu.bookings') }}
            </el-menu-item>
            <el-menu-item index="/user/checkins">
              <el-icon><House /></el-icon>{{ t('user.menu.checkins') }}
            </el-menu-item>
            <el-menu-item index="/user/reviews">
              <el-icon><ChatDotSquare /></el-icon>{{ t('user.menu.reviews') }}
            </el-menu-item>
            <el-menu-item index="/user/feedbacks">
              <el-icon><Warning /></el-icon>{{ t('user.menu.feedbacks') }}
            </el-menu-item>
            <el-menu-item index="password">
              <el-icon><Lock /></el-icon>{{ t('user.menu.password') }}
            </el-menu-item>
            <el-menu-item index="logout">
              <el-icon><SwitchButton /></el-icon>{{ t('user.menu.logout') }}
            </el-menu-item>
          </el-menu>
        </div>
      </el-col>
      <el-col :span="19">
        <router-view />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

function onSelect(index) {
  if (index === 'password') {
    router.push({ path: '/user/profile', hash: '#password' })
  } else if (index === 'logout') {
    handleLogout()
  } else {
    router.push(index)
  }
}

async function handleLogout() {
  await ElMessageBox.confirm(t('common.logoutConfirm'), t('common.tip'), {
    type: 'warning',
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel')
  })
  userStore.logout()
  ElMessage.success(t('common.loggedOut'))
  router.push('/login')
}
</script>

<style scoped>
.side-menu {
  padding: 16px 0;
}

.user-brief {
  text-align: center;
  padding: 8px 0 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.user-name {
  margin-top: 8px;
  font-weight: 600;
}

.center-menu {
  border-right: none;
}
</style>
