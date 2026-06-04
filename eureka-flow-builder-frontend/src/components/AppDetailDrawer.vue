<template>
  <a-drawer
    :open="visible"
    title="应用详情"
    placement="right"
    :width="360"
    :closable="true"
    @close="handleClose"
  >
    <div v-if="app" class="app-detail-content">
      <!-- 应用基础信息 -->
      <div class="detail-section">
        <h3 class="section-title">应用信息</h3>
        <div class="detail-item">
          <span class="detail-label">应用名称：</span>
          <span class="detail-value">{{ app.appName || '未命名应用' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">应用类型：</span>
          <a-tag color="blue">{{ getCodeGenTypeText(app.codeGenType) }}</a-tag>
        </div>
        <div class="detail-item" v-if="app.deployKey">
          <span class="detail-label">部署状态：</span>
          <a-tag color="green">已部署</a-tag>
        </div>
      </div>

      <!-- 创建者信息 -->
      <div class="detail-section">
        <h3 class="section-title">创建者</h3>
        <div class="creator-info">
          <UserAvatar
            :user-avatar="app.user?.userAvatar"
            :user-id="app.user?.id"
            size="large"
          />
          <div class="creator-meta">
            <div class="creator-name">{{ app.user?.userName || '匿名用户' }}</div>
            <div class="create-time">创建于 {{ formatCreateTime(app.createTime) }}</div>
          </div>
        </div>
      </div>

      <!-- 操作栏（仅本人或管理员可见） -->
      <div v-if="canManage" class="detail-section">
        <h3 class="section-title">操作</h3>
        <div class="action-buttons">
          <a-button type="primary" block @click="handleEdit">
            <edit-outlined /> 修改应用
          </a-button>
          <a-button type="primary" danger block @click="handleDelete">
            <delete-outlined /> 删除应用
          </a-button>
        </div>
      </div>
    </div>

    <a-skeleton v-else active :paragraph="{ rows: 6 }" />
  </a-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
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
  userId?: string
  user?: User
}

interface Props {
  /** 是否可见 */
  visible: boolean
  /** 应用数据 */
  app?: AppVO
  /** 当前登录用户ID */
  currentUserId?: string
  /** 当前登录用户角色 */
  currentUserRole?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'edit', app: AppVO): void
  (e: 'delete', app: AppVO): void
}>()

// 是否可以管理应用（本人或管理员）
const canManage = computed(() => {
  if (!props.app?.userId || !props.currentUserId) return false
  const isAdmin = props.currentUserRole === 'admin'
  const isOwner = props.app.userId === props.currentUserId
  return isAdmin || isOwner
})

// 处理关闭
const handleClose = () => {
  emit('update:visible', false)
}

// 处理编辑
const handleEdit = () => {
  if (props.app) {
    emit('edit', props.app)
  }
}

// 处理删除
const handleDelete = () => {
  if (props.app) {
    emit('delete', props.app)
  }
}

// 格式化创建时间
const formatCreateTime = (time?: string): string => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

</script>

<style scoped>
.app-detail-content {
  padding: 8px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.detail-label {
  color: #666;
  margin-right: 8px;
  min-width: 80px;
}

.detail-value {
  color: #333;
  font-weight: 500;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.creator-meta {
  flex: 1;
}

.creator-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.create-time {
  font-size: 12px;
  color: #999;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
