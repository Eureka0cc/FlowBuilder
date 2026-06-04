<template>
  <a-card hoverable class="app-card" @click="handleCardClick">
    <!-- 封面区域 -->
    <div class="app-cover-wrapper">
      <img
        v-if="app.cover && app.id && !coverError"
        :src="app.cover"
        :alt="app.appName"
        class="app-cover"
        @error="handleCoverError"
      />
      <div v-else class="app-cover-default">
        <img src="/default_cover.svg" class="default-cover-svg" alt="默认封面" />
      </div>
    </div>

    <!-- 信息区域 -->
    <div class="app-info">
      <!-- 作者信息（仅展示模式） -->
      <div v-if="showAuthor && app.user" class="app-author">
        <UserAvatar
          :user-avatar="app.user.userAvatar"
          :user-id="app.user.id"
          size="small"
        />
        <span class="author-name">{{ app.user.userName || '匿名用户' }}</span>
        <a-tag v-if="app.priority === 99" color="gold">精选</a-tag>
      </div>

      <!-- 应用名称 -->
      <h3 class="app-name" :title="app.appName">{{ app.appName || '未命名应用' }}</h3>

      <!-- 元信息 -->
      <p v-if="showCreateTime" class="app-meta">创建于 {{ formatTime(app.createTime) }}</p>
      <p v-if="showType" class="app-type">{{ getCodeGenTypeText(app.codeGenType) }}</p>
    </div>

    <!-- 操作按钮 -->
    <div class="app-actions">
      <slot name="actions">
        <a-button type="primary" size="small" @click.stop="handleViewChat">
          查看对话
        </a-button>
        <a-button
          v-if="app.deployKey"
          type="default"
          size="small"
          @click.stop="handleViewWork"
        >
          查看作品
        </a-button>
      </slot>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import dayjs from 'dayjs'
import UserAvatar from './UserAvatar.vue'
import { getCodeGenTypeText } from '@/constants/codeGenType'

interface User {
  id?: string
  userAvatar?: string
  userName?: string
  userAccount?: string
}

interface AppVO {
  id?: string
  appName?: string
  cover?: string
  codeGenType?: string
  deployKey?: string
  priority?: number
  createTime?: string
  user?: User
  userId?: string
}

interface Props {
  /** 应用数据 */
  app: AppVO
  /** 是否显示作者信息 */
  showAuthor?: boolean
  /** 是否显示创建时间 */
  showCreateTime?: boolean
  /** 是否显示应用类型 */
  showType?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showAuthor: false,
  showCreateTime: false,
  showType: false,
})

const emit = defineEmits<{
  /** 点击卡片 */
  (e: 'click', app: AppVO): void
  /** 点击查看对话 */
  (e: 'view-chat', app: AppVO): void
  /** 点击查看作品 */
  (e: 'view-work', app: AppVO): void
}>()

// 封面错误状态
const coverError = ref(false)

// 处理封面加载错误
const handleCoverError = () => {
  coverError.value = true
}

// 处理卡片点击
const handleCardClick = () => {
  emit('click', props.app)
}

// 处理查看对话
const handleViewChat = () => {
  emit('view-chat', props.app)
}

// 处理查看作品
const handleViewWork = () => {
  emit('view-work', props.app)
}

// 格式化时间
const formatTime = (time?: string): string => {
  if (!time) return '-'
  const diff = dayjs().diff(dayjs(time), 'hour')
  if (diff < 1) {
    return '刚刚'
  } else if (diff < 24) {
    return `${diff}小时前`
  } else if (diff < 24 * 7) {
    return `${Math.floor(diff / 24)}天前`
  } else if (diff < 24 * 30) {
    return `${Math.floor(diff / 24 / 7)}周前`
  } else {
    return dayjs(time).format('YYYY-MM-DD')
  }
}

</script>

<style scoped>
.app-card {
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  height: 100%;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.app-cover-wrapper {
  height: 180px;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  animation: fadeIn 0.5s ease-in-out forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.app-cover-default {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.default-cover-svg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-info {
  padding-top: 16px;
}

.app-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.author-name {
  font-size: 13px;
  color: #666;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-name {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-meta {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.app-type {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.app-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.app-actions :deep(.ant-btn) {
  flex: 1;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .app-cover-wrapper {
    height: 160px;
  }
}
</style>
