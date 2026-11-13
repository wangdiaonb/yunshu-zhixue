<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="学习路线"
    icon="🗺️"
    description="按学科方向跟学 B 站精选课程，从入门到就业体系化进阶。"
    :features="['学科路线切换', '课程按序学习', 'B 站视频直达', '学习进度勾选']"
  />
</template>
