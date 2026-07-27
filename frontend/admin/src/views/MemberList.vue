<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listMembers, createMember, updateMember, deleteMember, updateMemberStatus } from '@/api/member'
import { listMemberTypes } from '@/api/memberType'
import { MEMBER_STATUS, dictLabel, dictType } from '@/utils/dict'
import { fullPhone } from '@/utils/phone'

// 后端查询参数：name / phone / typeId / status(0禁用 1正常)
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listMembers,
  { name: '', phone: '', typeId: null, status: null }
)

const memberTypes = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
// MemberSaveRequest：username/phone/password/name/email/avatar/memberTypeId/status
const form = reactive({ id: null, username: '', name: '', phone: '', email: '', memberTypeId: null, status: 1 })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    // 手机号国际化：不限位数仅要求纯数字（与注册口径一致）
    { pattern: /^\d+$/, message: '手机号必须为数字', trigger: 'blur' }
  ]
}

function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      name: row.name,
      phone: row.phone,
      email: row.email,
      memberTypeId: row.memberTypeId,
      status: row.status
    })
  } else {
    Object.assign(form, { id: null, username: '', name: '', phone: '', email: '', memberTypeId: null, status: 1 })
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateMember(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      // 密码为空则后端使用默认密码 123456
      await createMember({ ...form })
      ElMessage.success('新增成功（初始密码 123456）')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${target === 1 ? '启用' : '禁用'}会员「${row.username || row.name}」吗？`, '提示', { type: 'warning' })
  await updateMemberStatus(row.id, target)
  ElMessage.success('操作成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除会员「${row.username || row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteMember(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  try {
    const data = await listMemberTypes({ current: 1, size: 100 })
    memberTypes.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ }
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="姓名">
          <el-input v-model="query.name" placeholder="请输入姓名" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable style="width: 160px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="会员类型">
          <el-select v-model="query.typeId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="t in memberTypes" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>会员列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openDialog()">新增会员</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="memberNo" label="会员编号" width="150" />
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="name" label="姓名" min-width="90" />
        <el-table-column prop="phone" label="手机号" min-width="150">
          <template #default="{ row }">{{ fullPhone(row) }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="memberTypeName" label="会员类型" min-width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dictType(MEMBER_STATUS, row.status)">{{ dictLabel(MEMBER_STATUS, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间" min-width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">修改</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改会员' : '新增会员'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="会员类型">
          <el-select v-model="form.memberTypeId" placeholder="请选择会员类型" style="width: 100%">
            <el-option v-for="t in memberTypes" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
