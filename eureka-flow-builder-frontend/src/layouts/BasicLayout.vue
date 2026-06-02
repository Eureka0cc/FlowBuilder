<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />

    <!-- 内容区域 - 通栏全屏流式布局 -->
    <a-layout-content class="content">
      <div class="content-wrapper">
        <RouterView />
      </div>
    </a-layout-content>

    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.content {
  flex: 1;
  background: #fff;
  /* 移除内边距，让内容区域真正通栏 */
  padding: 0;
}

.content-wrapper {
  width: 100%;
  min-height: calc(100vh - 64px - 72px);
  background: #fff;
  /* 移除之前的卡片样式 */
  margin: 0;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  /* 为固定导航栏留出空间 */
  padding-top: 64px;
}

/* 响应式设计 - 简化响应式处理 */
@media (max-width: 768px) {
  .content-wrapper {
    min-height: calc(100vh - 64px - 100px);
  }
}
</style>
