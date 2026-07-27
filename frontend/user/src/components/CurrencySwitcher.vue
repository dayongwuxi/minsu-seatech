<template>
  <el-dropdown trigger="click" @command="onSelect">
    <span class="currency-trigger" data-test="currency-trigger" :title="t('currency.label')">
      {{ store.currency }}
      <el-icon class="currency-arrow"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="c in options"
          :key="c.code"
          :command="c.code"
          :class="{ 'currency-active': c.code === store.currency }"
        >
          {{ c.symbol }} {{ c.code }} {{ c.name }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown } from '@element-plus/icons-vue'
import { useCurrencyStore } from '@/store/currency'

const { t } = useI18n()
const store = useCurrencyStore()

// 基准币（= 站点结算货币）固定在首位；其余来自 GET /api/exchange-rates
const options = computed(() => {
  const baseFromApi = store.rates.find((r) => r.code === store.base)
  const base = baseFromApi
    || { code: store.base, symbol: '¥', name: store.base === 'CNY' ? t('currency.cny') : store.base }
  return [base, ...store.rates.filter((r) => r.code !== store.base)]
})

function onSelect(code) {
  store.setCurrency(code)
}

onMounted(() => {
  store.loadRates()
})

defineExpose({ onSelect })
</script>

<style scoped>
.currency-trigger {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  outline: none;
}

.currency-trigger:hover {
  color: #12945b;
}

.currency-arrow {
  font-size: 12px;
}

.currency-active {
  color: #12945b;
  font-weight: 600;
}
</style>
