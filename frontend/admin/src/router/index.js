import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layout/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'member-types', name: 'MemberTypeList', component: () => import('@/views/MemberTypeList.vue'), meta: { title: '会员类型管理' } },
      { path: 'members', name: 'MemberList', component: () => import('@/views/MemberList.vue'), meta: { title: '会员管理' } },
      { path: 'room-types', name: 'RoomTypeList', component: () => import('@/views/RoomTypeList.vue'), meta: { title: '房间类型管理' } },
      { path: 'rooms', name: 'RoomList', component: () => import('@/views/RoomList.vue'), meta: { title: '房间信息管理' } },
      { path: 'rooms/edit/:id?', name: 'RoomEdit', component: () => import('@/views/RoomEdit.vue'), meta: { title: '房间信息编辑', activeMenu: '/rooms' } },
      { path: 'facilities', name: 'FacilityList', component: () => import('@/views/FacilityList.vue'), meta: { title: '周边设施管理' } },
      { path: 'bookings', name: 'BookingList', component: () => import('@/views/BookingList.vue'), meta: { title: '预定记录管理' } },
      { path: 'bookings/:id', name: 'BookingDetail', component: () => import('@/views/BookingDetail.vue'), meta: { title: '预定详情', activeMenu: '/bookings' } },
      { path: 'checkin-records', name: 'CheckinList', component: () => import('@/views/CheckinList.vue'), meta: { title: '入住记录管理' } },
      { path: 'incomes', name: 'IncomeList', component: () => import('@/views/IncomeList.vue'), meta: { title: '收入管理' } },
      { path: 'promotions', name: 'PromotionList', component: () => import('@/views/PromotionList.vue'), meta: { title: '促销管理' } },
      { path: 'refunds', name: 'RefundList', component: () => import('@/views/RefundList.vue'), meta: { title: '退款管理' } },
      { path: 'exchange-rates', name: 'ExchangeRateList', component: () => import('@/views/ExchangeRateList.vue'), meta: { title: '汇率管理' } },
      { path: 'reviews', name: 'ReviewList', component: () => import('@/views/ReviewList.vue'), meta: { title: '评价记录管理' } },
      { path: 'feedbacks', name: 'FeedbackList', component: () => import('@/views/FeedbackList.vue'), meta: { title: '投诉反馈管理' } },
      { path: 'notices', name: 'NoticeList', component: () => import('@/views/NoticeList.vue'), meta: { title: '公告管理' } },
      { path: 'customer-service', name: 'CustomerService', component: () => import('@/views/CustomerService.vue'), meta: { title: '在线客服管理' } },
      { path: 'system', name: 'System', component: () => import('@/views/System.vue'), meta: { title: '系统管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
  document.title = to.meta.title ? `${to.meta.title} - Yadogo Trip 管理后台` : 'Yadogo Trip 管理后台'
  return true
})

export default router
