import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '@/layout/UserLayout.vue'
import i18n from '@/i18n'

const routes = [
  {
    path: '/',
    component: UserLayout,
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue'), meta: { titleKey: 'title.home' } },
      { path: 'register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { titleKey: 'title.register' } },
      { path: 'login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { titleKey: 'title.login' } },
      { path: 'rooms', name: 'RoomList', component: () => import('@/views/RoomList.vue'), meta: { titleKey: 'title.roomList' } },
      { path: 'rooms/:id', name: 'RoomDetail', component: () => import('@/views/RoomDetail.vue'), meta: { titleKey: 'title.roomDetail' } },
      { path: 'booking/:roomId', name: 'BookingForm', component: () => import('@/views/BookingForm.vue'), meta: { titleKey: 'title.booking', requiresAuth: true } },
      { path: 'pay/:orderNo', name: 'Payment', component: () => import('@/views/Payment.vue'), meta: { titleKey: 'title.pay', requiresAuth: true } },
      { path: 'pay-result/:orderNo', name: 'PayResult', component: () => import('@/views/PayResult.vue'), meta: { titleKey: 'title.payResult', requiresAuth: true } },
      { path: 'notices', name: 'NoticeList', component: () => import('@/views/NoticeList.vue'), meta: { titleKey: 'title.notices' } },
      { path: 'notices/:id', name: 'NoticeDetail', component: () => import('@/views/NoticeDetail.vue'), meta: { titleKey: 'title.noticeDetail' } },
      { path: 'chat', name: 'Chat', component: () => import('@/views/Chat.vue'), meta: { titleKey: 'title.chat', requiresAuth: true } },
      {
        path: 'user',
        component: () => import('@/views/UserCenter.vue'),
        meta: { titleKey: 'title.userCenter', requiresAuth: true },
        redirect: '/user/profile',
        children: [
          { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue'), meta: { titleKey: 'title.userCenter', requiresAuth: true } },
          { path: 'pay-methods', name: 'PayMethods', component: () => import('@/views/user/PayMethods.vue'), meta: { titleKey: 'title.payMethods', requiresAuth: true } },
          { path: 'bookings', name: 'MyBookings', component: () => import('@/views/user/MyBookings.vue'), meta: { titleKey: 'title.myBookings', requiresAuth: true } },
          { path: 'checkins', name: 'MyCheckins', component: () => import('@/views/user/MyCheckins.vue'), meta: { titleKey: 'title.myCheckins', requiresAuth: true } },
          { path: 'reviews', name: 'MyReviews', component: () => import('@/views/user/MyReviews.vue'), meta: { titleKey: 'title.myReviews', requiresAuth: true } },
          { path: 'feedbacks', name: 'MyFeedbacks', component: () => import('@/views/user/MyFeedbacks.vue'), meta: { titleKey: 'title.myFeedbacks', requiresAuth: true } }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const t = i18n.global.t
  if (to.meta.titleKey) {
    document.title = `${t(to.meta.titleKey)} - ${t('footer.systemName')}`
  }
  if (to.meta.requiresAuth && !localStorage.getItem('user_token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
