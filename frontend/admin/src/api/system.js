import request from '@/utils/request'

// 轮播图管理：/api/admin/banners（分页），Banner：title/imageUrl/linkUrl/sort/status
export const listBanners = (params) => request.get('/banners', { params })
export const createBanner = (data) => request.post('/banners', data)
export const updateBanner = (id, data) => request.put(`/banners/${id}`, data)
export const updateBannerStatus = (id, status) => request.put(`/banners/${id}/status`, { status })
export const deleteBanner = (id) => request.delete(`/banners/${id}`)

// 菜单管理：GET /menus 返回全量列表（非分页），Menu：parentId/menuName/icon/path/sort/status
export const listMenus = () => request.get('/menus')
export const createMenu = (data) => request.post('/menus', data)
export const updateMenu = (id, data) => request.put(`/menus/${id}`, data)
export const deleteMenu = (id) => request.delete(`/menus/${id}`)

// 角色权限：GET /roles 返回全量列表，Role：roleName/roleCode/remark
export const listRoles = () => request.get('/roles')
export const createRole = (data) => request.post('/roles', data)
export const updateRole = (id, data) => request.put(`/roles/${id}`, data)
export const deleteRole = (id) => request.delete(`/roles/${id}`)
// 角色菜单回显/保存（全量重建）
export const getRoleMenus = (id) => request.get(`/roles/${id}/menus`)
export const saveRoleMenus = (id, menuIds) => request.put(`/roles/${id}/menus`, { menuIds })

// 管理员账号：GET /admins 分页（current/size/keyword/roleId/status），实体 Admin
export const listAdmins = (params) => request.get('/admins', { params })
export const createAdmin = (data) => request.post('/admins', data)
export const updateAdmin = (id, data) => request.put(`/admins/${id}`, data)
export const updateAdminStatus = (id, status) => request.put(`/admins/${id}/status`, { status })
export const deleteAdmin = (id) => request.delete(`/admins/${id}`)

// 系统参数：GET /configs 全量列表；PUT /configs 批量保存 [{ configName, configKey, configValue }]
export const listConfigs = () => request.get('/configs')
export const saveConfigs = (configs) => request.put('/configs', configs)

// 操作日志：GET /logs 分页（current/size/logType/keyword）；DELETE /logs/clear 清空
export const listLogs = (params) => request.get('/logs', { params })
export const clearLogs = () => request.delete('/logs/clear')
