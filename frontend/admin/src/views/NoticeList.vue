<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listNotices, createNotice, updateNotice, deleteNotice, publishNotice, offlineNotice } from '@/api/notice'
import { NOTICE_STATUS, dictLabel, dictType } from '@/utils/dict'

// 后端查询参数：title / publishStatus(0草稿或已下架 1已发布)
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listNotices,
  { title: '', publishStatus: null }
)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
// Notice：title/summary/content（publishStatus 由发布/下架操作维护）
const form = reactive({ id: null, title: '', summary: '', content: '' })
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告正文', trigger: 'blur' }]
}

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, title: row.title, summary: row.summary, content: row.content })
  } else {
    Object.assign(form, { id: null, title: '', summary: '', content: '' })
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateNotice(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createNotice({ ...form })
      ElMessage.success('新增成功（默认为草稿）')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function doPublish(row) {
  await ElMessageBox.confirm(`确定发布公告「${row.title}」吗？`, '发布确认', { type: 'warning' })
  await publishNotice(row.id)
  ElMessage.success('发布成功')
  load()
}

async function doOffline(row) {
  await ElMessageBox.confirm(`确定下架公告「${row.title}」吗？`, '下架确认', { type: 'warning' })
  await offlineNotice(row.id)
  ElMessage.success('已下架')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="公告标题">
          <el-input v-model="query.title" placeholder="请输入公告标题" clearable style="width: 200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="query.publishStatus" placeholder="全部" clearable style="width: 150px">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿/已下架" :value="0" />
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
          <span>公告列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openDialog()">新增公告</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="noticeNo" label="公告编号" width="150" />
        <el-table-column prop="title" label="公告标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="creatorName" label="创建人" width="100" />
        <el-table-column prop="publishStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(NOTICE_STATUS, row.publishStatus)">{{ dictLabel(NOTICE_STATUS, row.publishStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" min-width="165" />
        <el-table-column prop="createTime" label="创建时间" min-width="165" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">修改</el-button>
            <el-button v-if="row.publishStatus !== 1" link type="success" @click="doPublish(row)">发布</el-button>
            <el-button v-if="row.publishStatus === 1" link type="warning" @click="doOffline(row)">下架</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改公告' : '新增公告'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="请输入公告摘要" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
