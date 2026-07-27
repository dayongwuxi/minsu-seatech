<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listReviews, replyReview, auditReview, deleteReview } from '@/api/review'
import { REVIEW_AUDIT_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const auditStatusOptions = dictOptions(REVIEW_AUDIT_STATUS)

// ReviewQuery：roomName/memberName/rating/auditStatus(0待审核 1已通过 2未通过)
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listReviews,
  { memberName: '', roomName: '', auditStatus: null }
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
    await replyReview(replyForm.id, replyForm.replyContent)
    ElMessage.success('回复成功')
    replyDialog.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    replySaving.value = false
  }
}

// auditStatus：1通过 2不通过
async function doAudit(row, auditStatus) {
  await ElMessageBox.confirm(`确定将该评价审核为「${auditStatus === 1 ? '通过' : '不通过'}」吗？`, '审核确认', { type: 'warning' })
  await auditReview(row.id, auditStatus)
  ElMessage.success('审核完成')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该条评价吗？', '删除确认', { type: 'warning' })
  await deleteReview(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="会员姓名">
          <el-input v-model="query.memberName" placeholder="请输入会员姓名" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="房间名称">
          <el-input v-model="query.roomName" placeholder="请输入房间名称" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in auditStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="search">查询</el-button>
          <el-button :icon="'Refresh'" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header><div class="card-header"><span>评价记录列表</span></div></template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="reviewNo" label="评价编号" width="150" />
        <el-table-column prop="memberName" label="会员姓名" min-width="100">
          <template #default="{ row }">{{ row.memberName || row.memberUsername }}</template>
        </el-table-column>
        <el-table-column prop="roomName" label="房间名称" min-width="120" />
        <el-table-column prop="rating" label="评分" width="160">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.rating) || 0" disabled allow-half />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="replyContent" label="商家回复" min-width="160" show-overflow-tooltip />
        <el-table-column prop="auditStatus" label="审核状态" width="95">
          <template #default="{ row }">
            <el-tag :type="dictType(REVIEW_AUDIT_STATUS, row.auditStatus)">{{ dictLabel(REVIEW_AUDIT_STATUS, row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" min-width="165" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">回复</el-button>
            <el-button v-if="row.auditStatus === 0" link type="success" @click="doAudit(row, 1)">通过</el-button>
            <el-button v-if="row.auditStatus === 0" link type="warning" @click="doAudit(row, 2)">不通过</el-button>
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

    <el-dialog v-model="replyDialog" title="回复评价" width="520px">
      <p style="margin-bottom: 12px; color: #606266">评价内容：{{ replyForm.content }}</p>
      <el-input v-model="replyForm.replyContent" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyDialog = false">取消</el-button>
        <el-button type="primary" :loading="replySaving" @click="saveReply">确定回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>
