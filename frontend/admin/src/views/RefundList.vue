<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listRefunds, approveRefund, rejectRefund, completeRefund } from '@/api/refund'
import { REFUND_STATUS, PAY_CHANNEL, dictLabel, dictType, dictOptions } from '@/utils/dict'
import { approveTip, canComplete, completeTip } from '@/utils/refundFlow'

const statusOptions = dictOptions(REFUND_STATUS)

// 分页查询参数：orderNo/memberName/status
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listRefunds,
  { orderNo: '', memberName: '', status: null }
)

// 同意退款：Stripe 单调渠道退款(webhook 确认)；LinkTrust 单进入退款中等待线下操作；Mock 单直接已退款
async function doApprove(row) {
  await ElMessageBox.confirm(`${approveTip(row.channel)}\n\n确定同意退款 ¥${row.refundAmount} 吗？`, '同意退款', {
    type: 'warning',
    confirmButtonText: '同意退款',
    cancelButtonText: '取消'
  })
  await approveRefund(row.id)
  ElMessage.success(row.channel === 3 ? '已同意，请前往 LinkTrust 后台手工退款后回本页确认' : '已同意退款')
  load()
}

// LinkTrust 单：管理员在渠道后台手工退款完成后，站内确认置「已退款」
async function doComplete(row) {
  await ElMessageBox.confirm(`${completeTip()}\n\n订单编号：${row.orderNo}，退款金额 ¥${row.refundAmount}`, '确认已退款', {
    type: 'warning',
    confirmButtonText: '确认已退款',
    cancelButtonText: '取消'
  })
  await completeRefund(row.id)
  ElMessage.success('已确认退款完成')
  load()
}

const rejectDialog = ref(false)
const rejectSaving = ref(false)
const rejectForm = reactive({ id: null, orderNo: '', reason: '' })

function openReject(row) {
  rejectForm.id = row.id
  rejectForm.orderNo = row.orderNo
  rejectForm.reason = ''
  rejectDialog.value = true
}

async function doReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  rejectSaving.value = true
  try {
    await rejectRefund(rejectForm.id, rejectForm.reason)
    ElMessage.success('已拒绝该退款申请')
    rejectDialog.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    rejectSaving.value = false
  }
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="会员">
          <el-input v-model="query.memberName" placeholder="会员姓名/昵称" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
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
      <template #header><div class="card-header"><span>退款申请列表</span></div></template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="id" label="退款单号" width="90" />
        <el-table-column prop="orderNo" label="订单编号" min-width="160" />
        <el-table-column prop="memberName" label="会员" min-width="100">
          <template #default="{ row }">{{ row.memberName || row.memberUsername || '-' }}</template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="110">
          <template #default="{ row }">
            <span style="color: #f5483d; font-weight: 600">¥{{ row.refundAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="channel" label="渠道" width="90">
          <template #default="{ row }">
            <el-tag :type="dictType(PAY_CHANNEL, row.channel)" effect="plain">{{ dictLabel(PAY_CHANNEL, row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="dictType(REFUND_STATUS, row.status)">{{ dictLabel(REFUND_STATUS, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" min-width="165" />
        <el-table-column prop="handleTime" label="处理时间" min-width="165" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" @click="doApprove(row)">同意</el-button>
              <el-button link type="danger" @click="openReject(row)">拒绝</el-button>
            </template>
            <el-button v-else-if="canComplete(row)" link type="warning" @click="doComplete(row)">确认已退款</el-button>
            <span v-else style="color: #8a94a6; font-size: 13px">已处理</span>
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

    <el-dialog v-model="rejectDialog" title="拒绝退款" width="480px">
      <p style="margin-bottom: 12px; color: #606266">订单编号：{{ rejectForm.orderNo }}</p>
      <el-input v-model="rejectForm.reason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入拒绝原因（将反馈给用户）" />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="doReject">确定拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>
