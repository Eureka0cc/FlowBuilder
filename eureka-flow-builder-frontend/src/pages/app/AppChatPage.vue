<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="header-left">
        <a-dropdown v-if="appInfo">
          <a-button type="text" class="app-selector">
            {{ appInfo.appName || '未命名应用' }}
            <down-outlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="goToEditPage">编辑应用信息</a-menu-item>
              <a-menu-item @click="confirmDeleteApp" danger>删除应用</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-skeleton v-else active :paragraph="false" style="width: 120px" />
      </div>
      <div class="header-right">
        <a-button
          v-if="appInfo?.deployKey"
          type="primary"
          class="deploy-btn"
          :loading="deploying"
          @click="handleDeploy"
        >
          <cloud-upload-outlined />
          重新部署
        </a-button>
        <a-button
          v-else
          type="primary"
          class="deploy-btn"
          :loading="deploying"
          :disabled="!canDeploy"
          @click="handleDeploy"
        >
          <cloud-upload-outlined />
          部署
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="chat-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section" :class="{ 'with-preview': showPreview }">
        <!-- 消息区域 -->
        <div ref="messagesContainer" class="messages-container">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="msg.messageType === 'user' ? 'user-message' : 'ai-message'"
          >
            <div class="message-avatar">
              <a-avatar v-if="msg.messageType === 'user'" :src="loginUserStore.loginUser.userAvatar">
                <user-outlined />
              </a-avatar>
              <a-avatar v-else style="background: #1890ff">
                <robot-outlined />
              </a-avatar>
            </div>
            <div class="message-bubble">
              <div class="message-content" v-html="formatMessage(msg.message)"></div>
              <div v-if="msg.messageType === 'ai' && index === messages.length - 1 && generating" class="generating-indicator">
                <loading-outlined spin />
                生成中...
              </div>
            </div>
          </div>

          <!-- 代码生成进度展示 -->
          <div v-if="generating && codeProgress.length > 0" class="code-progress">
            <div class="progress-title">正在生成代码...</div>
            <div class="progress-files">
              <div
                v-for="(file, idx) in codeProgress"
                :key="idx"
                class="progress-file"
              >
                <file-text-outlined />
                <span class="file-name">{{ file.fileName }}</span>
                <span class="file-path">{{ file.filePath }}</span>
                <check-circle-outlined v-if="file.completed" class="file-status success" />
                <loading-outlined v-else class="file-status loading" spin />
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-section">
          <a-textarea
            v-model:value="inputMessage"
            :placeholder="'描述越详细，页面越具体，可以一步一步完善生成效果'"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            class="chat-input"
            @keydown.enter.prevent="handleSend"
          />
          <div class="input-actions">
            <div class="left-actions">
              <a-button type="text" class="action-btn">
                <paper-clip-outlined />
                上传
              </a-button>
              <a-button type="text" class="action-btn" @click="optimizePrompt">
                <thunderbolt-outlined />
                优化
              </a-button>
            </div>
            <a-button
              type="primary"
              shape="circle"
              class="send-btn"
              :loading="sending"
              :disabled="!inputMessage.trim()"
              @click="handleSend"
            >
              <arrow-up-outlined />
            </a-button>
          </div>
        </div>
      </div>

      <!-- 右侧预览区域 -->
      <div v-if="showPreview" class="preview-section">
        <div class="preview-header">
          <span class="preview-title">生成效果预览</span>
          <a-button type="text" size="small" @click="refreshPreview">
            <reload-outlined />
            刷新
          </a-button>
        </div>
        <div class="preview-frame-wrapper">
          <iframe
            v-if="previewUrl"
            :src="previewUrl"
            class="preview-frame"
            sandbox="allow-scripts allow-same-origin"
          ></iframe>
          <div v-else class="preview-placeholder">
            <loading-outlined v-if="generating" spin />
            <span v-else>预览加载中...</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 部署成功弹窗 -->
    <a-modal
      v-model:open="deployModalVisible"
      title="部署成功"
      :footer="null"
      @cancel="deployModalVisible = false"
    >
      <div class="deploy-success">
        <check-circle-outlined class="success-icon" />
        <p>您的应用已成功部署！</p>
        <a-input-group compact>
          <a-input
            v-model:value="deployedUrl"
            readonly
            style="width: calc(100% - 80px)"
          />
          <a-button type="primary" @click="copyUrl">复制</a-button>
        </a-input-group>
        <a-button type="link" @click="openDeployedUrl">访问应用</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  DownOutlined,
  CloudUploadOutlined,
  UserOutlined,
  RobotOutlined,
  PaperClipOutlined,
  ThunderboltOutlined,
  ArrowUpOutlined,
  LoadingOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deleteApp,
  deployApp,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用ID
const appId = computed(() => Number(route.params.id))

// 应用信息
const appInfo = ref<API.AppVO>()

// 消息列表
const messages = ref<API.ChatHistory[]>([])

// 输入消息
const inputMessage = ref('')

// 状态
const sending = ref(false)
const generating = ref(false)
const deploying = ref(false)

// 代码生成进度
const codeProgress = ref<Array<{ fileName: string; filePath: string; completed: boolean }>>([])

// 预览相关
const showPreview = ref(false)
const previewUrl = ref('')
const canDeploy = ref(false)

// 部署弹窗
const deployModalVisible = ref(false)
const deployedUrl = ref('')

// 消息容器ref
const messagesContainer = ref<HTMLElement>()

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 格式化消息内容（简单的markdown风格）
const formatMessage = (content?: string) => {
  if (!content) return ''
  // 转换代码块
  let formatted = content
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
  return formatted
}

// 加载应用信息
const loadAppInfo = async () => {
  try {
    const res = await getAppVoById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      // 设置预览URL
      if (appInfo.value.codeGenType) {
        previewUrl.value = `http://localhost:8123/api/static/${appInfo.value.codeGenType}_${appId.value}/`
      }
      // 如果已部署，显示预览
      if (appInfo.value.deployKey) {
        showPreview.value = true
        canDeploy.value = true
      }
    } else {
      message.error('获取应用信息失败')
    }
  } catch (error) {
    message.error('获取应用信息失败')
  }
}

// 加载聊天历史
const loadChatHistory = async () => {
  try {
    const res = await listAppChatHistory({
      appId: appId.value,
      pageSize: 100,
    })
    if (res.data.code === 0 && res.data.data) {
      messages.value = res.data.data.records || []
      // 如果有消息，显示预览区域
      if (messages.value.length > 0) {
        const hasAiMessage = messages.value.some(msg => msg.messageType === 'ai')
        if (hasAiMessage) {
          showPreview.value = true
          canDeploy.value = true
        }
      }
      scrollToBottom()
    }
  } catch (error) {
    message.error('加载聊天历史失败')
  }
}

// 处理SSE流式响应
const handleSSEResponse = async (response: Response) => {
  const reader = response.body?.getReader()
  if (!reader) return

  const decoder = new TextDecoder()
  let buffer = ''

  // 添加AI消息占位
  messages.value.push({
    messageType: 'ai',
    message: '',
    appId: appId.value,
    userId: 0,
  })
  const aiMessageIndex = messages.value.length - 1

  generating.value = true

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            generating.value = false
            canDeploy.value = true
            return
          }

          try {
            const parsed = JSON.parse(data)
            if (parsed.type === 'code_progress' && parsed.files) {
              // 更新代码生成进度
              codeProgress.value = parsed.files
            } else if (parsed.content) {
              // 更新AI消息内容
              messages.value[aiMessageIndex].message += parsed.content
              scrollToBottom()
            }
          } catch (e) {
            // 不是JSON，直接追加内容
            messages.value[aiMessageIndex].message += data
            scrollToBottom()
          }
        }
      }
    }
  } finally {
    generating.value = false
    reader.releaseLock()
    // 加载完成后显示预览
    showPreview.value = true
    canDeploy.value = true
    // 刷新聊天历史
    loadChatHistory()
  }
}

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim() || sending.value) return

  const messageText = inputMessage.value.trim()
  inputMessage.value = ''
  sending.value = true

  // 添加用户消息
  messages.value.push({
    messageType: 'user',
    message: messageText,
    appId: appId.value,
    userId: loginUserStore.loginUser.id || 0,
  })
  scrollToBottom()

  try {
    // 使用fetch API处理SSE
    const response = await fetch(
      `http://localhost:8123/api/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(messageText)}`,
      {
        method: 'GET',
        credentials: 'include',
      }
    )

    if (!response.ok) {
      throw new Error('请求失败')
    }

    await handleSSEResponse(response)
  } catch (error) {
    message.error('发送消息失败')
    sending.value = false
  } finally {
    sending.value = false
  }
}

// 优化提示词
const optimizePrompt = () => {
  if (!inputMessage.value.trim()) {
    message.warning('请先输入提示词')
    return
  }
  // 这里可以调用优化API
  message.info('提示词优化功能开发中...')
}

// 部署应用
const handleDeploy = async () => {
  if (deploying.value) return

  deploying.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
      // 刷新应用信息
      loadAppInfo()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

// 复制部署URL
const copyUrl = () => {
  navigator.clipboard.writeText(deployedUrl.value)
  message.success('已复制到剪贴板')
}

// 打开部署的应用
const openDeployedUrl = () => {
  window.open(deployedUrl.value, '_blank')
}

// 刷新预览
const refreshPreview = () => {
  if (previewUrl.value) {
    previewUrl.value = previewUrl.value + '?t=' + Date.now()
  }
}

// 跳转到编辑页面
const goToEditPage = () => {
  router.push(`/app/edit/${appId.value}`)
}

// 确认删除应用
const confirmDeleteApp = () => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后将无法恢复，是否确认删除此应用？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteApp({ id: appId.value })
        if (res.data.code === 0) {
          message.success('删除成功')
          router.push('/')
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

// 页面加载
onMounted(() => {
  if (!appId.value) {
    message.error('应用ID不能为空')
    router.push('/')
    return
  }
  loadAppInfo()
  loadChatHistory()
})

// 监听消息变化，自动滚动
watch(messages, scrollToBottom, { deep: true })
</script>

<style scoped>
#appChatPage {
  height: calc(100vh - 64px - 48px);
  display: flex;
  flex-direction: column;
  background: #fff;
  margin: -24px;
}

/* 顶部栏 */
.chat-header {
  height: 56px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
}

.app-selector {
  font-size: 16px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.deploy-btn {
  background: #333;
}

/* 核心内容区域 */
.chat-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-section.with-preview {
  width: 50%;
  border-right: 1px solid #f0f0f0;
}

/* 消息区域 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f5f5f5;
}

.user-message .message-bubble {
  background: #1890ff;
  color: #fff;
}

.message-content {
  line-height: 1.6;
  word-break: break-word;
}

.message-content :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-content :deep(code) {
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.user-message .message-content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.generating-indicator {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.7;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 代码生成进度 */
.code-progress {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  padding: 16px;
  margin: 8px 24px;
}

.progress-title {
  font-weight: 500;
  margin-bottom: 12px;
  color: #52c41a;
}

.progress-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-file {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.file-name {
  font-weight: 500;
}

.file-path {
  color: #999;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-status.success {
  color: #52c41a;
}

.file-status.loading {
  color: #1890ff;
}

/* 输入区域 */
.input-section {
  border-top: 1px solid #f0f0f0;
  padding: 16px 24px;
  background: #fff;
}

.chat-input {
  border: none;
  resize: none;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 12px 16px;
}

.chat-input:focus {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.left-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  color: #666;
}

.send-btn {
  background: #333;
}

/* 右侧预览区域 */
.preview-section {
  width: 50%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.preview-header {
  height: 48px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.preview-title {
  font-weight: 500;
}

.preview-frame-wrapper {
  flex: 1;
  padding: 16px;
  overflow: hidden;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 24px;
}

/* 部署成功弹窗 */
.deploy-success {
  text-align: center;
  padding: 24px;
}

.success-icon {
  font-size: 48px;
  color: #52c41a;
  margin-bottom: 16px;
}

/* 响应式 */
@media (max-width: 992px) {
  .chat-section.with-preview {
    width: 100%;
    border-right: none;
  }

  .preview-section {
    display: none;
  }
}

@media (max-width: 576px) {
  #appChatPage {
    margin: -16px;
    height: calc(100vh - 64px - 32px);
  }

  .messages-container {
    padding: 16px;
  }

  .message-bubble {
    max-width: 85%;
  }
}
</style>
