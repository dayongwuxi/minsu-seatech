<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listPromotions, createPromotion, updatePromotion, deletePromotion, updatePromotionStatus } from '@/api/promotion'
import { listRooms } from '@/api/room'
import { PROMOTION_TYPE, COMMON_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const typeOptions = dictOptions(PROMOTION_TYPE)
const rooms = ref([])

// 分页查询参数：name/type/status
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listPromotions,
  { name: '', type: null, status: null }
)

/* 折扣率换算：数据库存 0.80，界面用「8 折」表示 */
function rateToZhe(rate) {
  if (rate === null || rate === undefined) return null
  return Number((Number(rate) * 10).toFixed(2))
}
function zheToRate(zhe) {
  if (zhe === null || zhe === undefined) return null
  return Number((Number(zhe) / 10).toFixed(3))
}

// 优惠内容列：按 type 组合展示（8折 / 满1000减100 / 连住7晚9折 / 提前30天95折）
function benefitText(row) {
  switch (row.type) {
    case 1:
      return `${rateToZhe(row.discountRate)}折`
    case 2:
      return `满${row.thresholdAmount}减${row.discountAmount}`
    case 3:
      return `连住${row.minNights}晚${rateToZhe(row.discountRate)}折`
    case 4:
      return `提前${row.advanceDays}天${rateToZhe(row.discountRate)}折`
    default:
      return '-'
  }
}

function roomName(roomId) {
  if (!roomId) return '全场'
  const room = rooms.value.find((r) => r.id === roomId)
  return room ? room.roomName : `房间#${roomId}`
}

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  type: 1,
  discountZhe: 8, // 界面输入「折」，提交换算为 discountRate
  thresholdAmount: null,
  discountAmount: null,
  minNights: 2,
  advanceDays: 7,
  roomId: null,
  couponCode: '',
  dateRange: [],
  usageLimit: null,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择促销类型', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      discountZhe: rateToZhe(row.discountRate) ?? 8,
      thresholdAmount: row.thresholdAmount,
      discountAmount: row.discountAmount,
      minNights: row.minNights ?? 2,
      advanceDays: row.advanceDays ?? 7,
      roomId: row.roomId ?? null,
      couponCode: row.couponCode || '',
      dateRange: row.startDate && row.endDate ? [row.startDate, row.endDate] : [],
      usageLimit: row.usageLimit,
      status: row.status
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      type: 1,
      discountZhe: 8,
      thresholdAmount: null,
      discountAmount: null,
      minNights: 2,
      advanceDays: 7,
      roomId: null,
      couponCode: '',
      dateRange: [],
      usageLimit: null,
      status: 1
    })
  }
  dialogVisible.value = true
}

function buildPayload() {
  const useRate = form.type === 1 || form.type === 3 || form.type === 4
  return {
    name: form.name,
    type: form.type,
    discountRate: useRate ? zheToRate(form.discountZhe) : null,
    thresholdAmount: form.type === 2 ? form.thresholdAmount : null,
    discountAmount: form.type === 2 ? form.discountAmount : null,
    minNights: form.type === 3 ? form.minNights : null,
    advanceDays: form.type === 4 ? form.advanceDays : null,
    roomId: form.roomId || null,
    couponCode: form.couponCode.trim() || null,
    startDate: form.dateRange[0],
    endDate: form.dateRange[1],
    usageLimit: form.usageLimit || null,
    status: form.status
  }
}

async function save() {
  await formRef.value.validate()
  if (form.type === 2 && (!form.thresholdAmount || !form.discountAmount)) {
    ElMessage.warning('请填写满减门槛与立减金额')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updatePromotion(form.id, buildPayload())
      ElMessage.success('修改成功')
    } else {
      await createPromotion(buildPayload())
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${target === 1 ? '启用' : '停用'}促销活动「${row.name}」吗？`, '提示', { type: 'warning' })
  await updatePromotionStatus(row.id, target)
  ElMessage.success('操作成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除促销活动「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deletePromotion(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  try {
    const data = await listRooms({ current: 1, size: 200 })
    rooms.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ }
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="活动名称">
          <el-input v-model="query.name" placeholder="请输入活动名称" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="促销类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
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
          <span>促销活动列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openDialog()">新增促销</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="promoNo" label="促销编号" width="150" />
        <el-table-column prop="name" label="活动名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="dictType(PROMOTION_TYPE, row.type)" effect="plain">{{ dictLabel(PROMOTION_TYPE, row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="150">
          <template #default="{ row }">
            <span style="color: #f5483d; font-weight: 600">{{ benefitText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="适用房间" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="!row.roomId" type="success" effect="plain" size="small">全场</el-tag>
            <span v-else>{{ roomName(row.roomId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="couponCode" label="优惠码" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.couponCode" size="small">{{ row.couponCode }}</el-tag>
            <span v-else style="color: #8a94a6">自动应用</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="已用/限量" width="100">
          <template #default="{ row }">{{ row.usedCount ?? 0 }} / {{ row.usageLimit ?? '不限' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dictType(COMMON_STATUS, row.status)">{{ dictLabel(COMMON_STATUS, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">修改</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改促销活动' : '新增促销活动'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="将在价格明细中向用户展示" maxlength="100" />
        </el-form-item>
        <el-form-item label="促销类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- type=1 百分比折扣：折扣率 -->
        <el-form-item v-if="form.type === 1" label="折扣">
          <el-input-number v-model="form.discountZhe" :min="0.1" :max="9.9" :step="0.5" :precision="1" />
          <span class="form-tip">折（如 8 表示 8 折，提交为 0.8）</span>
        </el-form-item>

        <!-- type=2 满减：门槛+立减 -->
        <template v-if="form.type === 2">
          <el-form-item label="满减门槛（元）">
            <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="100" style="width: 200px" />
          </el-form-item>
          <el-form-item label="立减金额（元）">
            <el-input-number v-model="form.discountAmount" :min="0" :precision="2" :step="10" style="width: 200px" />
          </el-form-item>
        </template>

        <!-- type=3 长住折扣：最少晚数+折扣率 -->
        <template v-if="form.type === 3">
          <el-form-item label="最少连住晚数">
            <el-input-number v-model="form.minNights" :min="2" :max="365" />
            <span class="form-tip">晚</span>
          </el-form-item>
          <el-form-item label="折扣">
            <el-input-number v-model="form.discountZhe" :min="0.1" :max="9.9" :step="0.5" :precision="1" />
            <span class="form-tip">折</span>
          </el-form-item>
        </template>

        <!-- type=4 早鸟折扣：提前天数+折扣率 -->
        <template v-if="form.type === 4">
          <el-form-item label="提前预订天数">
            <el-input-number v-model="form.advanceDays" :min="1" :max="365" />
            <span class="form-tip">天</span>
          </el-form-item>
          <el-form-item label="折扣">
            <el-input-number v-model="form.discountZhe" :min="0.1" :max="9.9" :step="0.5" :precision="1" />
            <span class="form-tip">折</span>
          </el-form-item>
        </template>

        <el-form-item label="适用房间">
          <el-select v-model="form.roomId" placeholder="不选 = 全场适用" clearable style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomName}（${r.roomNo}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠码">
          <el-input v-model="form.couponCode" placeholder="留空 = 自动应用；填写后需用户输入优惠码" maxlength="30" />
        </el-form-item>
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker v-model="form.dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="限量">
          <el-input-number v-model="form.usageLimit" :min="1" placeholder="不填 = 不限" style="width: 200px" />
          <span class="form-tip">次（留空不限）</span>
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

<style scoped>
.form-tip {
  margin-left: 8px;
  color: #8a94a6;
  font-size: 12px;
}
</style>
