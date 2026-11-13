<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="面试进行中"
    icon="🎙️"
    description="逐题作答，AI 面试官实时评分并给出反馈，支持文字与语音两种作答方式。"
    :features="['对话式面试', '逐题评分与点评', '语音作答 / 朗读', '面试进度与用时统计']"
  />
</template>
