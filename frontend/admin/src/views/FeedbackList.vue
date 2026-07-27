<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listFeedbacks, replyFeedback, updateFeedbackStatus, deleteFeedback } from '@/api/feedback'
import { FEEDBACK_TYPE, FEEDBACK_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const typeOptions = dictOptions(FEEDBACK_TYPE)
const statusOptions = dictOptions(FEEDBACK_STATUS)

// 后端查询参数：status/type/keyword（keyword 匹配标题/内容/会员）
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listFeedbacks,
  { type: null, status: null, keyword: '' }
)

const replyDialog = ref(false)
const replySaving = ref(false)
const replyForm = reactive({ id: null, content: '', replyContent: '' })

function openReply(row) {
  replyForm.id = row.id
  replyForm.content = row.content
  replyForm.replyContent = row.replyContent || ''
  replyDialog.value = true
}

async function saveReply() {
  if (!replyForm.replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replySaving.value = true
  try {
    // 后端回复后自动置为已处理
    await replyFeedback(replyForm.id, replyForm.replyContent)
    ElMessage.success('回复成功，已自动标记为已处理')
    replyDialog.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    replySaving.value = false
  }
}

async function doResolve(row) {
  await ElMessageBox.confirm('确定将该条反馈标记为已处理吗？', '提示', { type: 'warning' })
  await updateFeedbackStatus(row.id, 2)
  ElMessage.success('已标记为已处理')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该条投诉反馈吗？', '删除确认', { type: 'warning' })
  await deleteFeedback(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="反馈类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="标题/内容/会员" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header><div class="card-header"><span>投诉反馈列表</span></div></template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="feedbackNo" label="反馈编号" width="150" />
        <el-table-column prop="type" label="反馈类型" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(FEEDBACK_TYPE, row.type)" effect="plain">{{ dictLabel(FEEDBACK_TYPE, row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memberName" label="会员" min-width="100">
          <template #default="{ row }">{{ row.memberName || row.memberUsername }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="replyContent" label="处理回复" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="处理状态" width="95">
          <template #default="{ row }">
            <el-tag :type="dictType(FEEDBACK_STATUS, row.status)">{{ dictLabel(FEEDBACK_STATUS, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="165" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">回复</el-button>
            <el-button v-if="row.status !== 2" link type="success" @click="doResolve(row)">标记已处理</el-button>
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

    <el-dialog v-model="replyDialog" title="回复投诉反馈" width="520px">
      <p style="margin-bottom: 12px; color: #606266">反馈内容：{{ replyForm.content }}</p>
      <el-input v-model="replyForm.replyContent" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyDialog = false">取消</el-button>
        <el-button type="primary" :loading="replySaving" @click="saveReply">确定回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>
