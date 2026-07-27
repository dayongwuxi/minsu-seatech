<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listRoomTypes, createRoomType, updateRoomType, deleteRoomType } from '@/api/roomType'
import { COMMON_STATUS, dictLabel, dictType } from '@/utils/dict'

// 后端查询参数：typeName / status(0停用 1启用)
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listRoomTypes,
  { typeName: '', status: null }
)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
// RoomType：typeNo/typeName/description/status
const form = reactive({ id: null, typeName: '', description: '', status: 1 })
const rules = { typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }] }

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, typeName: row.typeName, description: row.description, status: row.status })
  } else {
    Object.assign(form, { id: null, typeName: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateRoomType(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createRoomType({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除房间类型「${row.typeName}」吗？`, '删除确认', { type: 'warning' })
  await deleteRoomType(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="类型名称">
          <el-input v-model="query.typeName" placeholder="请输入类型名称" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
          <span>房间类型列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openDialog()">新增类型</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="typeNo" label="类型编号" width="150" />
        <el-table-column prop="typeName" label="类型名称" min-width="140" />
        <el-table-column prop="description" label="类型描述" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dictType(COMMON_STATUS, row.status)">{{ dictLabel(COMMON_STATUS, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">修改</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改房间类型' : '新增房间类型'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="如：大床房" />
        </el-form-item>
        <el-form-item label="类型描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入类型描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
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
