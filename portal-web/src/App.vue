<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

onMounted(() => {
  void userStore.bootstrap()
})
</script>

<template>
  <router-view />
</template>
