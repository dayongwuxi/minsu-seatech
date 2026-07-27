<template>
  <div class="card-block">
    <h2 class="section-title">
      {{ t('user.feedbacks.title') }}
      <el-button type="success" size="small" @click="openCreate">{{ t('user.feedbacks.add') }}</el-button>
    </h2>
    <!-- Feedback 实体：type 1住宿服务 2设施问题 3订单问题 4环境问题 5其他；status 0待处理 1处理中 2已处理 -->
    <el-table :data="feedbacks" v-loading="loading" stripe>
      <el-table-column prop="feedbackNo" :label="t('user.feedbacks.colNo')" min-width="140" />
      <el-table-column :label="t('user.feedbacks.colType')" width="130">
        <template #default="{ row }">
          <el-tag :type="dictTag(FEEDBACK_TYPE, row.type)">{{ dictText(FEEDBACK_TYPE, row.type, t) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" :label="t('user.feedbacks.colTitle')" min-width="130" />
      <el-table-column prop="content" :label="t('user.feedbacks.colContent')" min-width="170" show-overflow-tooltip />
      <el-table-column :label="t('user.feedbacks.colStatus')" width="110">
        <template #default="{ row }">
          <el-tag :type="dictTag(FEEDBACK_STATUS, row.status)">{{ dictText(FEEDBACK_STATUS, row.status, t) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('user.feedbacks.colReply')" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.replyContent || '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('user.feedbacks.colTime')" width="160">
        <template #default="{ row }">{{ row.createTime || '-' }}</template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('user.feedbacks.empty')" :image-size="80" />
      </template>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.current"
        @current-change="fetchFeedbacks"
      />
    </div>

    <el-dialog v-model="createVisible" :title="t('user.feedbacks.dialogTitle')" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="auto">
        <el-form-item :label="t('user.feedbacks.colType')" prop="type">
          <el-radio-group v-model="createForm.type">
            <el-radio v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('user.feedbacks.colTitle')" prop="title">
          <el-input v-model="createForm.title" maxlength="50" :placeholder="t('user.feedbacks.titlePh')" />
        </el-form-item>
        <el-form-item :label="t('user.feedbacks.colContent')" prop="content">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            :placeholder="t('user.feedbacks.contentPh')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" :loading="submitting" @click="handleCreate">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMyFeedbacks, createFeedback } from '@/api/feedback'
import { FEEDBACK_TYPE, FEEDBACK_STATUS, dictText, dictTag, dictOptions } from '@/utils/dict'

const { t } = useI18n()

const feedbacks = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })
const typeOptions = computed(() => dictOptions(FEEDBACK_TYPE, t))

const createVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref()
const createForm = reactive({ type: 1, title: '', content: '' })

const createRules = computed(() => ({
  type: [{ required: true, message: t('user.feedbacks.vType'), trigger: 'change' }],
  title: [{ required: true, message: t('user.feedbacks.vTitle'), trigger: 'blur' }],
  content: [{ required: true, message: t('user.feedbacks.vContent'), trigger: 'blur' }]
}))

async function fetchFeedbacks() {
  loading.value = true
  try {
    const data = await getMyFeedbacks({ current: query.current, size: query.size })
    feedbacks.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (e) {
    feedbacks.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.type = 1
  createForm.title = ''
  createForm.content = ''
  createVisible.value = true
}

async function handleCreate() {
  await createFormRef.value.validate()
  submitting.value = true
  try {
    await createFeedback({ ...createForm })
    ElMessage.success(t('user.feedbacks.success'))
    createVisible.value = false
    fetchFeedbacks()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(fetchFeedbacks)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
