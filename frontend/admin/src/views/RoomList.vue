<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageList } from '@/composables/usePageList'
import { listRooms, deleteRoom, updateRoomShelf, updateRoomStatus } from '@/api/room'
import { listRoomTypes } from '@/api/roomType'
import { ROOM_STATUS, SHELF_STATUS, dictLabel, dictType, dictOptions } from '@/utils/dict'

const router = useRouter()
const roomTypes = ref([])
const roomStatusOptions = dictOptions(ROOM_STATUS)

// 后端查询参数：name/roomNo/typeId/roomStatus/minPrice/maxPrice
const { query, loading, records, total, load, search, reset, handlePageChange, handleSizeChange } = usePageList(
  listRooms,
  { name: '', roomNo: '', typeId: null, roomStatus: null, minPrice: null, maxPrice: null }
)

// shelfStatus：0已下架 1已上架
async function toggleShelf(row) {
  const target = row.shelfStatus === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${target === 1 ? '上架' : '下架'}房间「${row.roomName}」吗？`, '提示', { type: 'warning' })
  await updateRoomShelf(row.id, target)
  ElMessage.success('操作成功')
  load()
}

// roomStatus：0可预订 1维修中 2已满房
async function setMaintenance(row) {
  const target = row.roomStatus === 1 ? 0 : 1
  await ElMessageBox.confirm(
    target === 1 ? `确定将房间「${row.roomName}」设置为维修中吗？` : `确定将房间「${row.roomName}」恢复为可预订吗？`,
    '提示',
    { type: 'warning' }
  )
  await updateRoomStatus(row.id, target)
  ElMessage.success('操作成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除房间「${row.roomName}」吗？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await deleteRoom(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  try {
    const data = await listRoomTypes({ current: 1, size: 100 })
    roomTypes.value = data?.records || []
  } catch (e) { /* 拦截器已提示 */ }
})
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="房间名称">
          <el-input v-model="query.name" placeholder="请输入房间名称" clearable style="width: 160px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="房间编号">
          <el-input v-model="query.roomNo" placeholder="请输入房间编号" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="房间类型">
          <el-select v-model="query.typeId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="t in roomTypes" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间状态">
          <el-select v-model="query.roomStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in roomStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格区间">
          <el-input-number v-model="query.minPrice" :min="0" :controls="false" placeholder="最低价" style="width: 100px" />
          <span style="margin: 0 6px">-</span>
          <el-input-number v-model="query.maxPrice" :min="0" :controls="false" placeholder="最高价" style="width: 100px" />
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
          <span>房间信息列表</span>
          <el-button type="primary" :icon="'Plus'" @click="router.push('/rooms/edit')">新增房间</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="roomNo" label="房间编号" width="130" />
        <el-table-column label="缩略图" width="110">
          <template #default="{ row }">
            <el-image
              :src="row.coverImage"
              fit="cover"
              style="width: 80px; height: 56px; border-radius: 6px"
              :preview-src-list="row.coverImage ? [row.coverImage] : []"
              preview-teleported
            >
              <template #error>
                <div class="img-slot"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="roomName" label="房间名称" min-width="130" />
        <el-table-column prop="typeName" label="房间类型" min-width="100" />
        <el-table-column prop="price" label="价格（元/晚）" width="120">
          <template #default="{ row }"><span style="color: #f5483d; font-weight: 600">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="maxGuests" label="可住人数" width="90" />
        <el-table-column prop="roomStatus" label="房间状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(ROOM_STATUS, row.roomStatus)">{{ dictLabel(ROOM_STATUS, row.roomStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shelfStatus" label="上架状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(SHELF_STATUS, row.shelfStatus)">{{ dictLabel(SHELF_STATUS, row.shelfStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/rooms/edit/${row.id}`)">修改</el-button>
            <el-button link :type="row.shelfStatus === 1 ? 'warning' : 'success'" @click="toggleShelf(row)">
              {{ row.shelfStatus === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="warning" @click="setMaintenance(row)">
              {{ row.roomStatus === 1 ? '结束维修' : '设为维修中' }}
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
  </div>
</template>

<style scoped>
.img-slot {
  width: 80px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f7;
  color: #b0b8c8;
  border-radius: 6px;
}
</style>
