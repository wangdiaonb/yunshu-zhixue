<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="题库中心"
    icon="📚"
    description="按学科与阶段浏览已上架题目，游客可浏览题目卡片，答案与解析需登录后查看。"
    :features="['学科 / 阶段 / 题目三级浏览', '题型与难度筛选', '关键词搜索', '查看解析与开始练习']"
  />
</template>
