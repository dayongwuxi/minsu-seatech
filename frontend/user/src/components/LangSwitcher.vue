<template>
  <el-dropdown trigger="click" @command="onSelect">
    <span class="lang-trigger" data-test="lang-trigger" :title="currentName">
      <!-- 内联 SVG 地球图标（@element-plus/icons-vue 无 Globe） -->
      <svg
        class="globe-icon"
        viewBox="0 0 24 24"
        width="20"
        height="20"
        fill="none"
        stroke="currentColor"
        stroke-width="1.6"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="9" />
        <ellipse cx="12" cy="12" rx="4" ry="9" />
        <path d="M3.6 9h16.8M3.6 15h16.8" />
      </svg>
      <el-icon class="lang-arrow"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="lang in SUPPORTED_LANGS"
          :key="lang.code"
          :command="lang.code"
          :class="{ 'lang-active': lang.code === locale }"
        >
          {{ lang.name }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown } from '@element-plus/icons-vue'
import { SUPPORTED_LANGS, setLang } from '@/i18n'

const { locale } = useI18n()

const currentName = computed(
  () => SUPPORTED_LANGS.find((l) => l.code === locale.value)?.name || ''
)

function onSelect(code) {
  setLang(code)
}

defineExpose({ onSelect })
</script>

<style scoped>
.lang-trigger {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  color: #303133;
  outline: none;
}

.lang-trigger:hover {
  color: #12945b;
}

.lang-arrow {
  font-size: 12px;
}

.lang-active {
  color: #12945b;
  font-weight: 600;
}
</style>
