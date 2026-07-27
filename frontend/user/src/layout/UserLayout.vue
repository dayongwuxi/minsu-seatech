<template>
  <div class="user-layout">
    <header class="layout-header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <el-icon color="#12945b" :size="24"><HomeFilled /></el-icon>
          <span>{{ t('nav.brand') }}</span>
        </router-link>
        <el-menu mode="horizontal" :default-active="activeMenu" :ellipsis="false" router class="nav-menu">
          <el-menu-item index="/">{{ t('nav.home') }}</el-menu-item>
          <el-menu-item index="/rooms">{{ t('nav.rooms') }}</el-menu-item>
          <el-menu-item index="/#facilities" @click.prevent="goFacilities">{{ t('nav.facilities') }}</el-menu-item>
          <el-menu-item index="/notices">{{ t('nav.notices') }}</el-menu-item>
          <el-menu-item index="/chat">{{ t('nav.chat') }}</el-menu-item>
          <el-menu-item index="/user">{{ t('nav.userCenter') }}</el-menu-item>
        </el-menu>
        <div class="header-right">
          <LangSwitcher class="lang-switcher" />
          <CurrencySwitcher class="currency-switcher" />
          <template v-if="userStore.isLogin">
            <el-dropdown @command="handleCommand">
              <span class="user-entry">
                <el-icon><User /></el-icon>
                {{ userStore.nickname || t('common.user') }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">{{ t('nav.userCenter') }}</el-dropdown-item>
                  <el-dropdown-item command="bookings">{{ t('nav.myBookings') }}</el-dropdown-item>
                  <el-dropdown-item command="payMethods">{{ t('user.menu.payMethods') }}</el-dropdown-item>
                  <el-dropdown-item divided command="logout">{{ t('nav.logout') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="auth-link">{{ t('nav.login') }}</router-link>
            <span class="divider">/</span>
            <router-link to="/register" class="auth-link">{{ t('nav.register') }}</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="layout-main">
      <router-view />
    </main>

    <footer class="layout-footer">
      <div class="footer-inner">
        <div class="footer-col">
          <h4>{{ t('footer.slogan') }}</h4>
          <p>{{ t('footer.subSlogan') }}</p>
        </div>
        <div class="footer-col">
          <h4>{{ t('footer.quickLinks') }}</h4>
          <p><router-link to="/rooms">{{ t('nav.rooms') }}</router-link></p>
          <p><router-link to="/notices">{{ t('nav.notices') }}</router-link></p>
          <p><router-link to="/chat">{{ t('nav.chat') }}</router-link></p>
        </div>
        <div class="footer-col">
          <h4>{{ t('footer.contact') }}</h4>
          <p>{{ t('footer.phone') }}</p>
          <p>{{ t('footer.email') }}</p>
        </div>
      </div>
      <div class="copyright">© {{ year }} {{ t('footer.systemName') }}</div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import LangSwitcher from '@/components/LangSwitcher.vue'
import CurrencySwitcher from '@/components/CurrencySwitcher.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const year = new Date().getFullYear()

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/rooms')) return '/rooms'
  if (p.startsWith('/notices')) return '/notices'
  if (p.startsWith('/user')) return '/user'
  if (p.startsWith('/chat')) return '/chat'
  return '/'
})

function goFacilities() {
  router.push({ path: '/', hash: '#facilities' })
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'bookings') {
    router.push('/user/bookings')
  } else if (command === 'payMethods') {
    router.push('/user/pay-methods')
  } else if (command === 'logout') {
    await ElMessageBox.confirm(t('common.logoutConfirm'), t('common.tip'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel')
    })
    userStore.logout()
    ElMessage.success(t('common.loggedOut'))
    router.push('/login')
  }
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  width: 1100px;
  max-width: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 16px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #12945b;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.lang-switcher {
  margin-right: 2px;
}

.auth-link {
  color: #12945b;
  font-size: 14px;
}

.divider {
  color: #ccc;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  outline: none;
}

.layout-main {
  flex: 1;
}

.layout-footer {
  background: #263238;
  color: #cfd8dc;
  margin-top: 24px;
}

.footer-inner {
  width: 1100px;
  max-width: 100%;
  margin: 0 auto;
  display: flex;
  gap: 60px;
  padding: 32px 16px 16px;
}

.footer-col h4 {
  color: #fff;
  margin: 0 0 12px;
}

.footer-col p {
  margin: 6px 0;
  font-size: 13px;
}

.footer-col a {
  color: #cfd8dc;
}

.footer-col a:hover {
  color: #fff;
}

.copyright {
  text-align: center;
  font-size: 12px;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
</style>
