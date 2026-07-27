<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listBanners, createBanner, updateBanner, deleteBanner,
  listMenus, createMenu, updateMenu, deleteMenu,
  listRoles, createRole, updateRole, deleteRole, getRoleMenus, saveRoleMenus,
  listAdmins, createAdmin, updateAdmin, deleteAdmin,
  listConfigs, saveConfigs,
  listLogs, clearLogs
} from '@/api/system'
import { uploadFile } from '@/api/upload'
import { COMMON_STATUS, MEMBER_STATUS, LOG_TYPE, dictLabel, dictType, dictOptions } from '@/utils/dict'

const activeTab = ref('banner')
const logTypeOptions = dictOptions(LOG_TYPE)

/* ---------- 通用小型 CRUD 状态工厂 ---------- */
function useCrud({ list, create, update, remove, emptyForm, label, paged = true }) {
  const state = reactive({
    loading: false,
    records: [],
    dialogVisible: false,
    saving: false,
    form: { ...emptyForm }
  })

  async function load() {
    state.loading = true
    try {
      const data = paged ? await list({ current: 1, size: 50 }) : await list()
      state.records = Array.isArray(data) ? data : data?.records || []
    } catch (e) { /* 拦截器已提示 */ } finally {
      state.loading = false
    }
  }

  function openDialog(row) {
    state.form = row ? { ...emptyForm, ...row } : { ...emptyForm }
    state.dialogVisible = true
  }

  async function save() {
    state.saving = true
    try {
      if (state.form.id) {
        await update(state.form.id, { ...state.form })
        ElMessage.success('修改成功')
      } else {
        await create({ ...state.form })
        ElMessage.success('新增成功')
      }
      state.dialogVisible = false
      load()
    } catch (e) { /* 拦截器已提示 */ } finally {
      state.saving = false
    }
  }

  async function del(row) {
    await ElMessageBox.confirm(`确定删除该${label}吗？`, '删除确认', { type: 'warning' })
    await remove(row.id)
    ElMessage.success('删除成功')
    load()
  }

  return { state, load, openDialog, save, del }
}

// Banner：title/imageUrl/linkUrl/sort/status
const banner = useCrud({
  list: listBanners, create: createBanner, update: updateBanner, remove: deleteBanner,
  emptyForm: { id: null, imageUrl: '', title: '', linkUrl: '', sort: 1, status: 1 }, label: '轮播图'
})
// Menu：parentId/menuName/icon/path/sort/status（GET 为全量列表）
const menu = useCrud({
  list: listMenus, create: createMenu, update: updateMenu, remove: deleteMenu,
  emptyForm: { id: null, parentId: 0, menuName: '', icon: '', path: '', sort: 1, status: 1 }, label: '菜单', paged: false
})
// Role：roleName/roleCode/remark（GET 为全量列表）
const role = useCrud({
  list: listRoles, create: createRole, update: updateRole, remove: deleteRole,
  emptyForm: { id: null, roleName: '', roleCode: '', remark: '' }, label: '角色', paged: false
})
// Admin：username/name/phone/email/roleId/password/status
const admin = useCrud({
  list: listAdmins, create: createAdmin, update: updateAdmin, remove: deleteAdmin,
  emptyForm: { id: null, username: '', name: '', phone: '', email: '', roleId: null, password: '', status: 1 }, label: '管理员账号'
})

async function handleBannerUpload(options) {
  try {
    const data = await uploadFile(options.file)
    banner.state.form.imageUrl = data.url
  } catch (e) { /* 拦截器已提示 */ }
}

/* ---------- 角色菜单权限 ---------- */
const roleMenuDialog = ref(false)
const roleMenuSaving = ref(false)
const roleMenuTarget = ref(null)
const roleMenuChecked = ref([])

async function openRoleMenus(row) {
  roleMenuTarget.value = row
  try {
    roleMenuChecked.value = (await getRoleMenus(row.id)) || []
  } catch (e) {
    roleMenuChecked.value = []
  }
  roleMenuDialog.value = true
}

async function doSaveRoleMenus() {
  roleMenuSaving.value = true
  try {
    await saveRoleMenus(roleMenuTarget.value.id, roleMenuChecked.value)
    ElMessage.success('角色菜单已保存')
    roleMenuDialog.value = false
  } catch (e) { /* 拦截器已提示 */ } finally {
    roleMenuSaving.value = false
  }
}

/* ---------- 系统参数（SysConfig 批量保存） ---------- */
const configsLoading = ref(false)
const configsSaving = ref(false)
const configs = ref([])

async function loadConfigs() {
  configsLoading.value = true
  try {
    configs.value = (await listConfigs()) || []
  } catch (e) { /* 拦截器已提示 */ } finally {
    configsLoading.value = false
  }
}

async function doSaveConfigs() {
  configsSaving.value = true
  try {
    // PUT /configs，按 configKey 批量更新
    await saveConfigs(configs.value.map((c) => ({ configName: c.configName, configKey: c.configKey, configValue: c.configValue })))
    ElMessage.success('系统参数已保存')
    loadConfigs()
  } catch (e) { /* 拦截器已提示 */ } finally {
    configsSaving.value = false
  }
}

/* ---------- 操作日志（只读 + 清空） ---------- */
const logsLoading = ref(false)
const logs = ref([])
const logQuery = reactive({ logType: null, keyword: '' })

async function loadLogs() {
  logsLoading.value = true
  try {
    const data = await listLogs({ current: 1, size: 50, logType: logQuery.logType, keyword: logQuery.keyword })
    logs.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ } finally {
    logsLoading.value = false
  }
}

async function doClearLogs() {
  await ElMessageBox.confirm('确定清空全部操作日志吗？此操作不可恢复。', '清空确认', { type: 'warning' })
  await clearLogs()
  ElMessage.success('日志已清空')
  loadLogs()
}

onMounted(() => {
  banner.load()
  menu.load()
  role.load()
  admin.load()
  loadConfigs()
  loadLogs()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header><div class="card-header"><span>系统管理</span></div></template>
      <el-tabs v-model="activeTab">
        <!-- 轮播图管理 -->
        <el-tab-pane label="轮播图管理" name="banner">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="'Plus'" @click="banner.openDialog()">新增轮播图</el-button>
          </div>
          <el-table v-loading="banner.state.loading" :data="banner.state.records" stripe>
            <el-table-column label="图片预览" width="130">
              <template #default="{ row }">
                <el-image :src="row.imageUrl" fit="cover" style="width: 90px; height: 50px; border-radius: 4px" />
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="linkUrl" label="跳转链接" min-width="150" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dictType(COMMON_STATUS, row.status)">{{ dictLabel(COMMON_STATUS, row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="banner.openDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="banner.del(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 菜单管理 -->
        <el-tab-pane label="菜单管理" name="menu">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="'Plus'" @click="menu.openDialog()">新增菜单</el-button>
          </div>
          <el-table v-loading="menu.state.loading" :data="menu.state.records" stripe>
            <el-table-column prop="menuName" label="菜单名称" min-width="130" />
            <el-table-column prop="icon" label="图标" width="110" />
            <el-table-column prop="path" label="路径" min-width="140" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dictType(COMMON_STATUS, row.status)">{{ dictLabel(COMMON_STATUS, row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="menu.openDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="menu.del(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 角色权限 -->
        <el-tab-pane label="角色权限" name="role">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="'Plus'" @click="role.openDialog()">新增角色</el-button>
          </div>
          <el-table v-loading="role.state.loading" :data="role.state.records" stripe>
            <el-table-column prop="roleName" label="角色名称" min-width="120" />
            <el-table-column prop="roleCode" label="角色编码" min-width="130" />
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" min-width="165" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="role.openDialog(row)">编辑</el-button>
                <el-button link type="warning" @click="openRoleMenus(row)">菜单权限</el-button>
                <el-button link type="danger" @click="role.del(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 管理员账号 -->
        <el-tab-pane label="管理员账号" name="admin">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="'Plus'" @click="admin.openDialog()">新增账号</el-button>
          </div>
          <el-table v-loading="admin.state.loading" :data="admin.state.records" stripe>
            <el-table-column label="头像" width="80">
              <template #default="{ row }">
                <el-avatar :size="32" :src="row.avatar"><el-icon><UserFilled /></el-icon></el-avatar>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="110" />
            <el-table-column prop="name" label="姓名" min-width="100" />
            <el-table-column prop="roleName" label="角色" min-width="120" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dictType(MEMBER_STATUS, row.status)">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLoginTime" label="最后登录" min-width="165" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="admin.openDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="admin.del(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 系统参数 -->
        <el-tab-pane label="系统参数" name="configs">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="'Check'" :loading="configsSaving" @click="doSaveConfigs">保 存</el-button>
          </div>
          <el-table v-loading="configsLoading" :data="configs" stripe>
            <el-table-column prop="configName" label="参数名称" min-width="140" />
            <el-table-column prop="configKey" label="参数键" min-width="150" />
            <el-table-column label="参数值" min-width="220">
              <template #default="{ row }">
                <el-input v-model="row.configValue" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" min-width="165" />
          </el-table>
        </el-tab-pane>

        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="logs">
          <div class="tab-toolbar">
            <el-select v-model="logQuery.logType" placeholder="日志类型" clearable style="width: 130px">
              <el-option v-for="o in logTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-input v-model="logQuery.keyword" placeholder="操作内容/操作人" clearable style="width: 180px" @keyup.enter="loadLogs" />
            <el-button type="primary" :icon="'Search'" @click="loadLogs">查询</el-button>
            <el-button type="danger" :icon="'Delete'" @click="doClearLogs">清空日志</el-button>
          </div>
          <el-table v-loading="logsLoading" :data="logs" stripe>
            <el-table-column prop="logType" label="日志类型" width="110">
              <template #default="{ row }">
                <el-tag :type="dictType(LOG_TYPE, row.logType)" effect="plain">{{ dictLabel(LOG_TYPE, row.logType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作用户" width="120" />
            <el-table-column prop="content" label="操作内容" min-width="240" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column prop="createTime" label="操作时间" min-width="165" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 轮播图弹窗 -->
    <el-dialog v-model="banner.state.dialogVisible" :title="banner.state.form.id ? '编辑轮播图' : '新增轮播图'" width="480px">
      <el-form :model="banner.state.form" label-width="90px">
        <el-form-item label="图片">
          <el-upload :show-file-list="false" :http-request="handleBannerUpload" accept="image/*">
            <el-image v-if="banner.state.form.imageUrl" :src="banner.state.form.imageUrl" fit="cover" style="width: 160px; height: 90px; border-radius: 4px" />
            <el-button v-else :icon="'Upload'">上传图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="banner.state.form.title" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="banner.state.form.linkUrl" placeholder="如：/rooms" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="banner.state.form.sort" :min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="banner.state.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banner.state.dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="banner.state.saving" @click="banner.save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 菜单弹窗 -->
    <el-dialog v-model="menu.state.dialogVisible" :title="menu.state.form.id ? '编辑菜单' : '新增菜单'" width="480px">
      <el-form :model="menu.state.form" label-width="90px">
        <el-form-item label="菜单名称">
          <el-input v-model="menu.state.form.menuName" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="menu.state.form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="menu.state.form.path" placeholder="如：/dashboard" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="menu.state.form.sort" :min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="menu.state.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menu.state.dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menu.state.saving" @click="menu.save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色弹窗 -->
    <el-dialog v-model="role.state.dialogVisible" :title="role.state.form.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="role.state.form" label-width="90px">
        <el-form-item label="角色名称">
          <el-input v-model="role.state.form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="role.state.form.roleCode" placeholder="如：oper_admin" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="role.state.form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="role.state.dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="role.state.saving" @click="role.save">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色菜单权限弹窗 -->
    <el-dialog v-model="roleMenuDialog" :title="`菜单权限 - ${roleMenuTarget?.roleName || ''}`" width="420px">
      <el-checkbox-group v-model="roleMenuChecked" class="role-menu-group">
        <el-checkbox v-for="m in menu.state.records" :key="m.id" :value="m.id">{{ m.menuName }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleMenuDialog = false">取消</el-button>
        <el-button type="primary" :loading="roleMenuSaving" @click="doSaveRoleMenus">保存</el-button>
      </template>
    </el-dialog>

    <!-- 管理员账号弹窗 -->
    <el-dialog v-model="admin.state.dialogVisible" :title="admin.state.form.id ? '编辑管理员账号' : '新增管理员账号'" width="480px">
      <el-form :model="admin.state.form" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="admin.state.form.username" :disabled="!!admin.state.form.id" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="admin.state.form.name" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="admin.state.form.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="admin.state.form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in role.state.records" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="admin.state.form.id ? '重置密码' : '初始密码'">
          <el-input v-model="admin.state.form.password" type="password" show-password placeholder="留空则不修改/默认 123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="admin.state.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admin.state.dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="admin.state.saving" @click="admin.save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tab-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
.role-menu-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}
</style>
