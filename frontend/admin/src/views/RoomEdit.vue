<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoom, createRoom, updateRoom, generateRoomI18n, saveRoomI18n } from '@/api/room'
import { listRoomTypes } from '@/api/roomType'
import { uploadFile } from '@/api/upload'
import { I18N_LANGS, toI18nMap, hasAnyI18n, langName } from '@/utils/roomI18n'
import {
  ALLOWED_IMAGE_TYPES,
  MAX_IMAGE_SIZE_MB,
  validateImageFile,
  fileUrl as pickFileUrl,
  indexByUid,
  collectImages,
  hasPendingUpload,
  moveToFront,
  shiftByUid
} from '@/utils/roomImages'

const route = useRoute()
const router = useRouter()
const roomId = computed(() => route.params.id)
const isEdit = computed(() => !!roomId.value)

const facilityOptions = [
  '无线WiFi', '空调', '电视', '独立卫浴', '热水器', '吹风机',
  '洗漱用品', '电水壶', '迷你冰箱', '阳台', '衣柜'
]

const formRef = ref()
const saving = ref(false)
const roomTypes = ref([])
const fileList = ref([])
// 复选组与后端 facilities（逗号分隔字符串）互转
const facilityChecked = ref([])

// RoomSaveRequest 字段
const form = reactive({
  roomNo: '',
  roomName: '',
  roomTypeId: null,
  price: 0,
  originPrice: null,
  maxGuests: 2,
  area: null,
  bedInfo: '',
  coverImage: '',
  description: '',
  bookingNote: '',
  isRecommend: 0,
  roomStatus: 0,
  shelfStatus: 1,
  images: []
})

const rules = {
  roomName: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
  roomTypeId: [{ required: true, message: '请选择房间类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

// 图片放大预览
const viewerVisible = ref(false)
const viewerUrl = ref('')

// AI 多语言文案：base 中文在 form.description/bookingNote，其余语种存 i18nMap
const i18nLangs = I18N_LANGS
const i18nMap = ref({}) // { en: {description, bookingNote}, ja: {...}, ... }
const aiGenerating = ref(false)

// 语言版本对话框
const i18nDialogVisible = ref(false)
const dialogLang = ref('en')
const dialogEdit = reactive({ description: '', bookingNote: '' })
const savingLang = ref(false)

const hasI18n = computed(() => hasAnyI18n(i18nMap.value))

// 一键生成多语言：把当前中文说明/须知翻译为全部目标语种并落库
async function generateI18n() {
  if (!isEdit.value) {
    ElMessage.warning('请先保存房间，再生成多语言版本')
    return
  }
  if (!form.description?.trim() && !form.bookingNote?.trim()) {
    ElMessage.warning('请先填写中文房间说明或预订须知')
    return
  }
  if (hasI18n.value) {
    try {
      await ElMessageBox.confirm('将用当前中文内容重新翻译并覆盖所有语言版本，是否继续？', '一键生成多语言', {
        confirmButtonText: '覆盖生成',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  aiGenerating.value = true
  const hint = ElMessage({ message: '正在翻译为 8 种语言，请稍候（约 10~20 秒）…', type: 'info', duration: 0 })
  try {
    const list = await generateRoomI18n(roomId.value, {
      description: form.description,
      bookingNote: form.bookingNote
    })
    i18nMap.value = toI18nMap(list)
    ElMessage.success('多语言文案已生成并保存')
  } catch (e) {
    /* 拦截器已提示（含未配置 OLLAMA_API_KEY） */
  } finally {
    hint.close()
    aiGenerating.value = false
  }
}

function openLangDialog() {
  const first = i18nLangs.find((l) => i18nMap.value[l.code]) || i18nLangs[0]
  dialogLang.value = first.code
  loadDialogLang(first.code)
  i18nDialogVisible.value = true
}

function loadDialogLang(code) {
  const v = i18nMap.value[code] || { description: '', bookingNote: '' }
  dialogEdit.description = v.description || ''
  dialogEdit.bookingNote = v.bookingNote || ''
}

async function saveDialogLang() {
  savingLang.value = true
  try {
    const vo = await saveRoomI18n(roomId.value, dialogLang.value, {
      description: dialogEdit.description,
      bookingNote: dialogEdit.bookingNote
    })
    i18nMap.value = { ...i18nMap.value, [vo.lang]: { description: vo.description, bookingNote: vo.bookingNote } }
    ElMessage.success(`已保存 ${langName(vo.lang)} 版本`)
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    savingLang.value = false
  }
}

// 上传前的格式/大小校验（避免无谓请求后端）
function beforeUpload(file) {
  const { ok, message } = validateImageFile(file, {
    allowedTypes: ALLOWED_IMAGE_TYPES,
    maxSizeMB: MAX_IMAGE_SIZE_MB
  })
  if (!ok) {
    ElMessage.error(message)
  }
  return ok
}

// 对接 POST /api/admin/upload（返回 { url }），最多 8 张
async function handleImageUpload(options) {
  try {
    const data = await uploadFile(options.file)
    options.onSuccess(data)
  } catch (e) {
    options.onError(e)
  }
}

// 上传成功：用服务器返回地址替换 el-upload 的本地 blob 预览地址，避免把临时地址写库
function handleUploadSuccess(response, uploadFile) {
  if (response?.url) {
    uploadFile.url = response.url
  }
  syncImages()
}

// 上传失败：请求拦截器已弹出后端提示，这里移除占位的失败缩略图
function handleUploadError(err, file) {
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
  syncImages()
}

function fileUrl(file) {
  return pickFileUrl(file)
}

function picIndex(file) {
  return indexByUid(fileList.value, file.uid)
}

function previewImage(file) {
  viewerUrl.value = pickFileUrl(file)
  viewerVisible.value = true
}

// 将某张图片移到第一位作为封面
function setCover(file) {
  if (moveToFront(fileList.value, file.uid)) {
    syncImages()
  }
}

// 前移/后移一位（dir=-1 前移，dir=1 后移）
function moveImage(file, dir) {
  if (shiftByUid(fileList.value, file.uid, dir)) {
    syncImages()
  }
}

function removeImage(file) {
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
  syncImages()
}

function syncImages() {
  // 保持 fileList 的当前顺序作为图片顺序，第一张为封面
  const { images, coverImage } = collectImages(fileList.value)
  form.images = images
  form.coverImage = coverImage
}

async function save() {
  await formRef.value.validate()
  if (hasPendingUpload(fileList.value)) {
    ElMessage.warning('图片尚未上传完成，请稍候再保存')
    return
  }
  syncImages()
  saving.value = true
  try {
    const payload = { ...form, facilities: facilityChecked.value.join(',') }
    if (isEdit.value) {
      await updateRoom(roomId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createRoom(payload)
      ElMessage.success('新增成功')
    }
    router.push('/rooms')
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const data = await listRoomTypes({ current: 1, size: 100 })
    roomTypes.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ }
  if (isEdit.value) {
    try {
      // RoomDetailVO { room, typeName, images: [{ imageUrl, sort }], avgRating }
      const detail = await getRoom(roomId.value)
      const room = detail?.room
      if (room) {
        Object.keys(form).forEach((k) => {
          if (room[k] !== undefined && room[k] !== null) form[k] = room[k]
        })
        facilityChecked.value = room.facilities ? room.facilities.split(',').filter(Boolean) : []
        form.images = (detail.images || []).map((img) => img.imageUrl)
        // 已存在图片手动赋 uid（负数避开组件运行时生成的正数 uid），供排序/删除按 uid 定位
        fileList.value = form.images.map((url, i) => ({ uid: -(i + 1), name: `图片${i + 1}`, url, status: 'success' }))
        i18nMap.value = toI18nMap(detail.i18n)
      }
    } catch (e) { /* 拦截器已提示 */ }
  }
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '修改房间信息' : '新增房间信息' }}</span>
          <el-button :icon="'Back'" @click="router.push('/rooms')">返回列表</el-button>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 820px">
        <el-row :gutter="16">
          <el-col :md="12" :xs="24">
            <el-form-item label="房间编号">
              <el-input v-model="form.roomNo" placeholder="留空自动生成（RM+日期+随机）" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="房间名称" prop="roomName">
              <el-input v-model="form.roomName" placeholder="如：海景大床房" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="房间类型" prop="roomTypeId">
              <el-select v-model="form.roomTypeId" placeholder="请选择房间类型" style="width: 100%">
                <el-option v-for="t in roomTypes" :key="t.id" :label="t.typeName" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="价格（元/晚）" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="原价（元/晚）">
              <el-input-number v-model="form.originPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="可住人数">
              <el-input-number v-model="form.maxGuests" :min="1" :max="10" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="面积（㎡）">
              <el-input-number v-model="form.area" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="床型信息">
              <el-input v-model="form.bedInfo" placeholder="如：1.8米大床×1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="房间设施">
          <el-checkbox-group v-model="facilityChecked">
            <el-checkbox v-for="f in facilityOptions" :key="f" :value="f">{{ f }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="房间图片">
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :http-request="handleImageUpload"
            :limit="8"
            accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
            multiple
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
          >
            <el-icon><Plus /></el-icon>
            <template #file="{ file }">
              <div class="pic-item">
                <img class="pic-thumb" :src="fileUrl(file)" alt="房间图片" />
                <span v-if="picIndex(file) === 0" class="cover-badge">封面</span>
                <span class="pic-mask">
                  <el-icon title="放大预览" @click="previewImage(file)"><ZoomIn /></el-icon>
                  <el-icon v-if="picIndex(file) !== 0" title="设为封面" @click="setCover(file)"><StarFilled /></el-icon>
                  <el-icon v-if="picIndex(file) !== 0" title="前移" @click="moveImage(file, -1)"><ArrowLeft /></el-icon>
                  <el-icon v-if="picIndex(file) !== fileList.length - 1" title="后移" @click="moveImage(file, 1)"><ArrowRight /></el-icon>
                  <el-icon title="删除" @click="removeImage(file)"><Delete /></el-icon>
                </span>
              </div>
            </template>
            <template #tip>
              <div class="upload-tip">
                支持 JPG / PNG / GIF / WEBP / BMP 格式，单张不超过 20MB，超过 15MB 将自动压缩后保存，最多 8 张。<br />
                第一张为封面图，鼠标悬停图片可放大预览、调整顺序或点 ★ 设为封面。
              </div>
            </template>
          </el-upload>
          <el-image-viewer
            v-if="viewerVisible"
            :url-list="[viewerUrl]"
            @close="viewerVisible = false"
          />
        </el-form-item>

        <el-form-item label="房间说明">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入中文房间说明（500字以内）" />
        </el-form-item>

        <el-form-item label="预订须知">
          <el-input v-model="form.bookingNote" type="textarea" :rows="2" placeholder="请输入中文预订须知（选填）" />
        </el-form-item>

        <el-form-item label="AI 多语言">
          <div class="ai-copy-bar">
            <el-button type="success" :icon="'MagicStick'" :loading="aiGenerating" @click="generateI18n">
              一键生成多语言房间说明 / 预订须知
            </el-button>
            <el-button v-if="hasI18n" :icon="'Document'" @click="openLangDialog">
              AI 文案 · 语言版本
            </el-button>
            <span class="ai-copy-tip">
              以上方中文为源，用 gemma4:31b 翻译为站点其余 8 种语言并落库<span v-if="!isEdit">（请先保存房间）</span>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="首页推荐">
          <el-radio-group v-model="form.isRecommend">
            <el-radio :value="1">推荐</el-radio>
            <el-radio :value="0">不推荐</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="房间状态">
          <el-radio-group v-model="form.roomStatus">
            <el-radio :value="0">可预订</el-radio>
            <el-radio :value="1">维修中</el-radio>
            <el-radio :value="2">已满房</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="上架状态">
          <el-radio-group v-model="form.shelfStatus">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保 存</el-button>
          <el-button @click="router.push('/rooms')">取 消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- AI 文案 · 多语言版本：查看/编辑/保存各语言 -->
    <el-dialog v-model="i18nDialogVisible" title="AI 文案 · 语言版本" width="640px">
      <el-select v-model="dialogLang" style="width: 180px; margin-bottom: 14px" @change="loadDialogLang">
        <el-option
          v-for="l in i18nLangs"
          :key="l.code"
          :label="l.name + (i18nMap[l.code] ? '' : '（暂无）')"
          :value="l.code"
        />
      </el-select>
      <el-form label-width="90px">
        <el-form-item label="房间说明">
          <el-input v-model="dialogEdit.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="预订须知">
          <el-input v-model="dialogEdit.bookingNote" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="i18nDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="savingLang" @click="saveDialogLang">保存此语言版本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.upload-tip {
  font-size: 12px;
  color: #8a94a6;
  line-height: 1.6;
  margin-top: 4px;
}

.ai-copy-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.ai-copy-tip {
  font-size: 12px;
  color: #8a94a6;
}

.pic-item {
  position: relative;
  width: 100%;
  height: 100%;
}

.pic-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 1px 6px;
  font-size: 12px;
  color: #fff;
  background: var(--el-color-primary);
  border-bottom-right-radius: 6px;
}

.pic-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 18px;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.pic-item:hover .pic-mask {
  opacity: 1;
}

.pic-mask .el-icon {
  cursor: pointer;
}

.pic-mask .el-icon:hover {
  transform: scale(1.15);
}
</style>
