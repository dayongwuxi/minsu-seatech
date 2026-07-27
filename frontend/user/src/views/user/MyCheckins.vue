<template>
  <div class="card-block">
    <h2 class="section-title">{{ t('user.checkins.title') }}</h2>
    <!-- CheckinUserVO -->
    <el-table :data="checkins" v-loading="loading" stripe>
      <el-table-column prop="checkinNo" :label="t('user.checkins.colCheckinNo')" min-width="150" />
      <el-table-column prop="orderNo" :label="t('user.checkins.colOrderNo')" min-width="170" />
      <el-table-column prop="roomName" :label="t('user.checkins.colRoom')" min-width="120" />
      <el-table-column :label="t('user.checkins.colStay')" min-width="200">
        <template #default="{ row }">
          {{ t('common.dateRange', { from: row.checkinDate, to: row.checkoutDate }) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isReviewed !== 1" type="success" link @click="openReview(row)">
            {{ t('user.checkins.goReview') }}
          </el-button>
          <el-tag v-else type="info">{{ t('user.checkins.reviewed') }}</el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('user.checkins.empty')" :image-size="80" />
      </template>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.current"
        @current-change="fetchCheckins"
      />
    </div>

    <el-dialog v-model="reviewVisible" :title="t('user.checkins.dialogTitle')" width="480px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="auto">
        <el-form-item :label="t('user.checkins.room')">
          <span>{{ current?.roomName }}</span>
        </el-form-item>
        <el-form-item :label="t('user.checkins.rating')" prop="rating">
          <el-rate v-model="reviewForm.rating" allow-half />
        </el-form-item>
        <el-form-item :label="t('user.checkins.content')" prop="content">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            :placeholder="t('user.checkins.contentPh')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmitReview">
          {{ t('user.checkins.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMyCheckins } from '@/api/user'
import { createReview } from '@/api/review'

const { t } = useI18n()

const checkins = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })

const reviewVisible = ref(false)
const submitting = ref(false)
const reviewFormRef = ref()
const current = ref(null)
// 与后端 ReviewCreateRequest 一致：{ bookingId, rating(0.5~5.0), content }
const reviewForm = reactive({ rating: 5, content: '' })

const reviewRules = computed(() => ({
  rating: [
    {
      validator: (rule, value, callback) => {
        if (!value || value < 0.5) callback(new Error(t('user.checkins.vRating')))
        else callback()
      },
      trigger: 'change'
    }
  ],
  content: [{ required: true, message: t('user.checkins.vContent'), trigger: 'blur' }]
}))

async function fetchCheckins() {
  loading.value = true
  try {
    const data = await getMyCheckins({ current: query.current, size: query.size })
    checkins.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (e) {
    checkins.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openReview(row) {
  current.value = row
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function handleSubmitReview() {
  await reviewFormRef.value.validate()
  submitting.value = true
  try {
    await createReview({
      bookingId: current.value?.bookingId,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    ElMessage.success(t('user.checkins.success'))
    reviewVisible.value = false
    fetchCheckins()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(fetchCheckins)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
