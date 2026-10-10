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

    <!-- 底部版权信息（整屏工作区页面通过路由 meta.hideFooter 隐藏） -->
    <GlobalFooter v-if="showFooter" />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

// 对话页等工作区页面自行管理整屏高度，隐藏页脚避免出现多余滚动条
const showFooter = computed(() => !route.meta.hideFooter)
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
  /* 内容区撑满剩余高度，让页脚始终贴在页面底部 */
  display: flex;
  flex-direction: column;
}

.content-wrapper {
  flex: 1;
  width: 100%;
  background: #fff;
  /* 纵向 flex：页面根节点可用 flex: 1 撑满内容区（如登录页居中卡片） */
  display: flex;
  flex-direction: column;
  /* 移除之前的卡片样式 */
  margin: 0;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  /* 为固定导航栏留出空间 */
  padding-top: 64px;
}
</style>
