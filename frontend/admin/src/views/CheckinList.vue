<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listCheckins, updateCheckin, deleteCheckin, checkout } from '@/api/checkin'
import { CHECKIN_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const checkinStatusOptions = dictOptions(CHECKIN_STATUS)

// CheckinQuery：guestName/roomName/dateStart/dateEnd/status
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listCheckins,
  { guestName: '', roomName: '', dateStart: '', dateEnd: '', status: null }
)

const dateRange = ref([])
watch(dateRange, (val) => {
  query.dateStart = val?.[0] || ''
  query.dateEnd = val?.[1] || ''
})

const dialogVisible = ref(false)
const saving = ref(false)
// CheckinRecord 可编辑字段：guestName/checkinTime/checkoutTime
const form = reactive({ id: null, guestName: '', checkinTime: '', checkoutTime: '' })

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    guestName: row.guestName,
    checkinTime: row.checkinTime,
    checkoutTime: row.checkoutTime
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await updateCheckin(form.id, { guestName: form.guestName, checkinTime: form.checkinTime, checkoutTime: form.checkoutTime })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function doCheckout(row) {
  await ElMessageBox.confirm(`确定为「${row.guestName}」办理退房吗？`, '办理退房', { type: 'warning' })
  await checkout(row.id)
  ElMessage.success('退房办理成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除入住记录「${row.checkinNo}」吗？`, '删除确认', { type: 'warning' })
  await deleteCheckin(row.id)
  ElMessage.success('删除成功')
  load()
}

function handleExport() {
  // 导出占位：后端暂未提供导出接口
  ElMessage.info('导出功能开发中')
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="入住人姓名">
          <el-input v-model="query.guestName" placeholder="请输入姓名" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="房间名称">
          <el-input v-model="query.roomName" placeholder="请输入房间名称" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in checkinStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button :icon="'Refresh'" @click="dateRange = []; reset()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>入住记录列表</span>
          <el-button :icon="'Download'" @click="handleExport">导出 Excel</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="checkinNo" label="入住编号" min-width="150" />
        <el-table-column prop="orderNo" label="订单号" min-width="150" />
        <el-table-column prop="guestName" label="入住人姓名" min-width="100" />
        <el-table-column prop="memberName" label="会员" min-width="100">
          <template #default="{ row }">{{ row.memberName || row.memberUsername }}</template>
        </el-table-column>
        <el-table-column prop="roomName" label="房间名称" min-width="120" />
        <el-table-column prop="checkinTime" label="入住时间" min-width="165" />
        <el-table-column prop="checkoutTime" label="退房时间" min-width="165" />
        <el-table-column prop="status" label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="dictType(CHECKIN_STATUS, row.status)">{{ dictLabel(CHECKIN_STATUS, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 2" link type="success" @click="doCheckout(row)">办理退房</el-button>
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
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

    <el-dialog v-model="dialogVisible" title="修改入住记录" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="入住人姓名">
          <el-input v-model="form.guestName" />
        </el-form-item>
        <el-form-item label="入住时间">
          <el-date-picker v-model="form.checkinTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退房时间">
          <el-date-picker v-model="form.checkoutTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
