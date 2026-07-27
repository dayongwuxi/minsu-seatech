<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listFacilities, createFacility, updateFacility, deleteFacility } from '@/api/facility'
import { uploadFile } from '@/api/upload'
import { dictLabel, dictType } from '@/utils/dict'
import { FACILITY_TYPES, toFacilityForm } from '@/utils/facility'
import { validateImageFile } from '@/utils/roomImages'

const facilityTypes = FACILITY_TYPES

// NearbyFacility.status：0维护中 1启用
const FACILITY_STATUS = {
  0: { label: '维护中', type: 'warning' },
  1: { label: '启用', type: 'success' }
}

// 后端查询参数：name / type / status
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listFacilities,
  { name: '', type: '', status: null }
)

const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const formRef = ref()
// NearbyFacility：facilityNo/name/type/distance(米)/address/imageUrl/status
const form = reactive(toFacilityForm())
const rules = {
  name: [{ required: true, message: '请输入设施名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择设施类型', trigger: 'change' }]
}

function openDialog(row) {
  Object.assign(form, toFacilityForm(row))
  dialogVisible.value = true
}

// 单张设施图片上传（复用房间图片校验，落库到 imageUrl）
function beforeUpload(file) {
  const { ok, message } = validateImageFile(file)
  if (!ok) ElMessage.error(message)
  return ok
}

async function handleUpload(options) {
  uploading.value = true
  try {
    const data = await uploadFile(options.file)
    form.imageUrl = data.url
    options.onSuccess(data)
  } catch (e) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateFacility(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createFacility({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除周边设施「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteFacility(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="设施名称">
          <el-input v-model="query.name" placeholder="请输入设施名称" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="设施类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="t in facilityTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="维护中" :value="0" />
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
          <span>周边设施列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openDialog()">新增设施</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="facilityNo" label="设施编号" width="140" />
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              :preview-teleported="true"
              fit="cover"
              class="facility-cell-img"
            />
            <span v-else class="facility-no-img">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="设施名称" min-width="140" />
        <el-table-column prop="type" label="设施类型" width="110">
          <template #default="{ row }"><el-tag>{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="distance" label="距离（米）" width="110" />
        <el-table-column prop="address" label="详细地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dictType(FACILITY_STATUS, row.status)">{{ dictLabel(FACILITY_STATUS, row.status) }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改周边设施' : '新增周边设施'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="设施名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设施名称" />
        </el-form-item>
        <el-form-item label="设施类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择设施类型" style="width: 100%">
            <el-option v-for="t in facilityTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="距离（米）">
          <el-input-number v-model="form.distance" :min="0" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="设施图片">
          <div class="facility-upload">
            <el-image
              v-if="form.imageUrl"
              :src="form.imageUrl"
              :preview-src-list="[form.imageUrl]"
              :preview-teleported="true"
              fit="cover"
              class="facility-thumb"
            />
            <el-upload
              :show-file-list="false"
              :http-request="handleUpload"
              :before-upload="beforeUpload"
              accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
            >
              <el-button :icon="'Upload'" :loading="uploading">{{ form.imageUrl ? '更换图片' : '上传图片' }}</el-button>
            </el-upload>
            <el-button v-if="form.imageUrl" link type="danger" @click="form.imageUrl = ''">移除</el-button>
          </div>
          <div class="facility-upload-tip">单张图片，≤20MB，点击缩略图可放大；将展示在首页周边设施轮播</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">维护中</el-radio>
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
.facility-cell-img {
  width: 56px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
}

.facility-no-img {
  color: #c0c4cc;
}

.facility-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.facility-thumb {
  width: 88px;
  height: 64px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  cursor: pointer;
}

.facility-upload-tip {
  font-size: 12px;
  color: #8a94a6;
  margin-top: 6px;
}
</style>
