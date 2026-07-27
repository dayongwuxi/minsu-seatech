<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/store/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const collapsed = ref(false)

const menus = [
  { path: '/dashboard', title: '后台首页', icon: 'HomeFilled' },
  { path: '/profile', title: '个人中心', icon: 'User' },
  { path: '/member-types', title: '会员类型管理', icon: 'Postcard' },
  { path: '/members', title: '会员管理', icon: 'UserFilled' },
  { path: '/room-types', title: '房间类型管理', icon: 'Collection' },
  { path: '/rooms', title: '房间信息管理', icon: 'House' },
  { path: '/facilities', title: '周边设施管理', icon: 'MapLocation' },
  { path: '/bookings', title: '预定记录管理', icon: 'Tickets' },
  { path: '/checkin-records', title: '入住记录管理', icon: 'Key' },
  { path: '/incomes', title: '收入管理', icon: 'Coin' },
  { path: '/promotions', title: '促销管理', icon: 'Discount' },
  { path: '/refunds', title: '退款管理', icon: 'RefreshLeft' },
  { path: '/exchange-rates', title: '汇率管理', icon: 'Money' },
  { path: '/reviews', title: '评价记录管理', icon: 'ChatDotSquare' },
  { path: '/feedbacks', title: '投诉反馈管理', icon: 'Warning' },
  { path: '/notices', title: '公告管理', icon: 'Bell' },
  { path: '/customer-service', title: '在线客服管理', icon: 'Service' },
  { path: '/system', title: '系统管理', icon: 'Setting' }
]

const activeMenu = computed(() => route.meta.activeMenu || route.path)
const breadcrumb = computed(() => route.meta.title || '')

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await adminStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  if (!adminStore.profile) adminStore.fetchProfile()
})
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon :size="24"><HomeFilled /></el-icon>
        <span v-show="!collapsed" class="logo-text">Yadogo Trip 管理后台</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#0f1a33"
          text-color="#c0c8dc"
          active-text-color="#ffffff"
          router
        >
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">后台首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumb && route.path !== '/dashboard'">{{ breadcrumb }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="avatar-wrap">
              <el-avatar :size="32" :src="adminStore.profile?.avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="admin-name">管理员 {{ adminStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100%;
}
.sidebar {
  background: #0f1a33;
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
}
.sidebar :deep(.el-menu) {
  border-right: none;
}
.sidebar :deep(.el-menu-item.is-active) {
  background: #2f6bff;
}
.header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8ecf3;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  cursor: pointer;
}
.avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.admin-name {
  font-size: 14px;
  color: #333;
}
.main {
  padding: 16px;
  background: #f5f7fa;
  overflow: auto;
}
</style>
