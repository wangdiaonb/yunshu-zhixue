<script setup lang="ts">
import { onMounted, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const route = useRoute()
const siteStore = useSiteStore()
const userStore = useUserStore()

onMounted(() => {
  void siteStore.ensureLoaded()
  void userStore.bootstrap()
})

/** 浏览器标题与描述跟随管理端系统设置 */
watchEffect(() => {
  const pageTitle = route.meta.title
  document.title = pageTitle ? `${pageTitle} - ${siteStore.siteName}` : siteStore.siteName

  const description = document.querySelector('meta[name="description"]')
  if (description) {
    description.setAttribute('content', siteStore.siteDescription)
  }
})
</script>

<template>
  <router-view />
</template>
