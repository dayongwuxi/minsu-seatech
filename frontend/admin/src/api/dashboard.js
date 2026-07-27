import request from '@/utils/request'

// 返回 DashboardStatsVO（扁平结构，diff 为较昨日增减额）：
// todayBookings/bookingsDiff、todayCheckins/checkinsDiff、todayIncome/incomeDiff、
// pendingFeedbacks、roomTotal、availableRooms、memberTotal、bookingTotal
export const getStats = () => request.get('/dashboard/stats')
// range: 7d / 30d / 12m，返回 [{ date, value }]
export const getIncomeTrend = (params) => request.get('/dashboard/income-trend', { params })
export const getBookingTrend = (params) => request.get('/dashboard/booking-trend', { params })
// params: { days, limit }，返回 [{ roomId, roomName, coverImage, bookingCount }]
export const getHotRooms = (params) => request.get('/dashboard/hot-rooms', { params })
// params: { limit }，返回 BookingAdminVO 列表
export const getLatestBookings = (params) => request.get('/dashboard/latest-bookings', { params })
