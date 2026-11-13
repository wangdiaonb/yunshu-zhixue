<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="AI 模拟面试"
    icon="🤖"
    description="选择技术方向、面试难度与题目数量，开启一场接近真实的 AI 模拟面试。"
    :features="['八大技术方向', '难度与题量配置', '每日面试配额', '逐题作答与实时评分']"
  />
</template>
