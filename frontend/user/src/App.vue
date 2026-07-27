<template>
  <el-config-provider :locale="epLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import epZhCn from 'element-plus/es/locale/lang/zh-cn'
import epJa from 'element-plus/es/locale/lang/ja'
import epEn from 'element-plus/es/locale/lang/en'
import epDe from 'element-plus/es/locale/lang/de'
import epFr from 'element-plus/es/locale/lang/fr'
import epEs from 'element-plus/es/locale/lang/es'
import epPt from 'element-plus/es/locale/lang/pt'
import epRu from 'element-plus/es/locale/lang/ru'
import epKo from 'element-plus/es/locale/lang/ko'

// Element Plus 内置文案随 i18n locale 同步切换
const EP_LOCALES = {
  zh: epZhCn,
  ja: epJa,
  en: epEn,
  de: epDe,
  fr: epFr,
  es: epEs,
  pt: epPt,
  ru: epRu,
  ko: epKo
}

const { locale } = useI18n()
const epLocale = computed(() => EP_LOCALES[locale.value] || epEn)
</script>
