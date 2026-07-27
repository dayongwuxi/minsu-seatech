<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExchangeRates, updateExchangeRate, restoreAutoUpdate, refreshExchangeRates } from '@/api/exchangeRate'
import { RATE_SOURCE, dictLabel, dictType } from '@/utils/dict'

const loading = ref(false)
const refreshing = ref(false)
const records = ref([])

// CNY 为基准货币（rate=1），禁用全部操作
const isBase = (row) => row.currencyCode === 'CNY'

async function load() {
  loading.value = true
  try {
    const data = await listExchangeRates()
    // 兼容直接数组或 { rates: [...] } 包装
    records.value = Array.isArray(data) ? data : data?.rates || data?.records || []
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

// 立即从 open.er-api.com 抓取（后端仅更新 autoUpdate=1 的行）
async function doRefresh() {
  refreshing.value = true
  try {
    const data = await refreshExchangeRates()
    const updated = data?.updated ?? data?.count
    const time = data?.fetchedAt ?? data?.time ?? data?.updateTime
    if (updated !== undefined) {
      ElMessage.success(`汇率刷新成功：更新 ${updated} 条${time ? '，时间 ' + time : ''}`)
    } else {
      ElMessage.success('汇率刷新成功')
    }
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    refreshing.value = false
  }
}

/* 调整汇率弹窗 */
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ code: '', name: '', symbol: '', rate: null })

function openEdit(row) {
  form.code = row.currencyCode
  form.name = row.currencyName
  form.symbol = row.symbol
  form.rate = Number(row.rate)
  dialogVisible.value = true
}

async function saveRate() {
  if (!form.rate || form.rate <= 0) {
    ElMessage.warning('汇率必须大于 0')
    return
  }
  saving.value = true
  try {
    await updateExchangeRate(form.code, form.rate)
    ElMessage.success(`已更新 ${form.code} 汇率，该币种已转为手工锁定，不再自动刷新`)
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function doRestoreAuto(row) {
  await ElMessageBox.confirm(
    `确定恢复 ${row.currencyCode}（${row.currencyName}）的自动更新吗？下次刷新时将以 API 汇率覆盖当前手工值。`,
    '恢复自动更新',
    { type: 'warning' }
  )
  await restoreAutoUpdate(row.currencyCode)
  ElMessage.success('已恢复自动更新')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        基准货币为 <b>CNY（人民币）</b>：展示汇率仅影响用户端价格显示，创建订单与实际结算一律使用 CNY。
        汇率来源 open.er-api.com，每 6 小时自动刷新（仅更新未锁定币种）。
      </template>
    </el-alert>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>汇率列表（1 CNY 兑换量）</span>
          <el-button type="primary" :icon="'Refresh'" :loading="refreshing" @click="doRefresh">立即从 API 刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="currencyCode" label="币种代码" width="110">
          <template #default="{ row }">
            <b>{{ row.currencyCode }}</b>
            <el-tag v-if="isBase(row)" size="small" style="margin-left: 6px">基准</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currencyName" label="币种名称" min-width="110" />
        <el-table-column prop="symbol" label="符号" width="80" />
        <el-table-column label="汇率（1 CNY =）" min-width="160">
          <template #default="{ row }">
            <span style="font-weight: 600">{{ row.rate }}</span>
            <span style="color: #8a94a6; margin-left: 4px">{{ row.currencyCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(RATE_SOURCE, row.source)" effect="plain">{{ dictLabel(RATE_SOURCE, row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="autoUpdate" label="自动更新" width="100">
          <template #default="{ row }">
            <el-tag :type="row.autoUpdate === 1 ? 'success' : 'info'">
              {{ row.autoUpdate === 1 ? '启用中' : '已锁定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="165" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="!isBase(row)">
              <el-button link type="primary" @click="openEdit(row)">调整汇率</el-button>
              <el-button v-if="row.autoUpdate === 0" link type="success" @click="doRestoreAuto(row)">恢复自动</el-button>
            </template>
            <span v-else style="color: #8a94a6; font-size: 13px">基准货币</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="`调整汇率 - ${form.code}（${form.name}）`" width="440px">
      <el-form label-width="110px">
        <el-form-item label="汇率">
          <el-input-number v-model="form.rate" :min="0.000001" :precision="6" :step="0.01" :controls="false" style="width: 200px" />
          <span class="form-tip">1 CNY = {{ form.rate || '?' }} {{ form.code }}</span>
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>调整后该币种将转为手工锁定（source=手工），不再随 API 自动刷新，可随时「恢复自动」。</template>
      </el-alert>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: #8a94a6;
  font-size: 12px;
}
</style>
