import request from '@/utils/request'

// 查询参数：current/size/name/roomNo/typeId/roomStatus/minPrice/maxPrice
// 返回 RoomAdminVO（Room 全字段 + typeName）
export const listRooms = (params) => request.get('/rooms', { params })
// 返回 RoomDetailVO { room, typeName, images: [{ imageUrl, sort }], avgRating }
export const getRoom = (id) => request.get(`/rooms/${id}`)
// RoomSaveRequest：roomNo/roomName/roomTypeId/price/originPrice/maxGuests/area/bedInfo/
//   facilities(逗号分隔字符串)/coverImage/description/bookingNote/isRecommend/roomStatus/shelfStatus/images(URL数组)
export const createRoom = (data) => request.post('/rooms', data)
export const updateRoom = (id, data) => request.put(`/rooms/${id}`, data)
export const deleteRoom = (id) => request.delete(`/rooms/${id}`)
// PUT /rooms/{id}/shelf-status，status: 0已下架 1已上架
export const updateRoomShelf = (id, status) => request.put(`/rooms/${id}/shelf-status`, { status })
// PUT /rooms/{id}/room-status，status: 0可预订 1维修中 2已满房
export const updateRoomStatus = (id, status) => request.put(`/rooms/${id}/room-status`, { status })

// 一键生成多语言：body { description, bookingNote }(中文源)，返回 [{ lang, description, bookingNote }]
// 8 语种并发翻译约需 10~20s，单独放宽超时到 120s（默认 15s 不够）
export const generateRoomI18n = (id, data) =>
  request.post(`/rooms/${id}/i18n/generate`, data, { timeout: 120000 })
// 保存单个语言版本：body { description, bookingNote }
export const saveRoomI18n = (id, lang, data) => request.put(`/rooms/${id}/i18n/${lang}`, data)
