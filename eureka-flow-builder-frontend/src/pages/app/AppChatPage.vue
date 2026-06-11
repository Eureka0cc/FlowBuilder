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
        <a-button type="default" class="detail-btn" @click="showAppDetailDrawer = true">
          <info-circle-outlined />
          应用详情
        </a-button>
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
          <div v-if="hasMoreHistory" class="load-more-wrapper">
            <a-button type="link" :loading="loadingMoreHistory" @click="loadMoreHistory">
              加载更多
            </a-button>
          </div>
          <div
            v-for="(msg, index) in messages"
            :key="msg.id || index"
            class="message-item"
            :class="msg.messageType === 'user' ? 'user-message' : 'ai-message'"
          >
            <div class="message-avatar">
              <a-avatar
                v-if="msg.messageType === 'user'"
                :src="loginUserStore.loginUser.userAvatar"
              >
                <user-outlined />
              </a-avatar>
              <a-avatar v-else style="background: #1890ff">
                <robot-outlined />
              </a-avatar>
            </div>
            <div class="message-bubble">
              <div class="message-content" v-html="formatMessage(msg.message)"></div>
              <div
                v-if="msg.messageType === 'ai' && index === messages.length - 1 && generating"
                class="generating-indicator"
              >
                <loading-outlined spin />
                生成中...
              </div>
            </div>
          </div>

          <!-- 代码生成进度展示 -->
          <div v-if="generating && codeProgress.length > 0" class="code-progress">
            <div class="progress-title">正在生成代码...</div>
            <div class="progress-files">
              <div v-for="(file, idx) in codeProgress" :key="idx" class="progress-file">
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
          <a-alert
            v-if="selectedElement"
            type="info"
            show-icon
            closable
            class="selected-element-alert"
            message="已选中页面元素"
            @close="clearSelectedElement(getPreviewIframe())"
          >
            <template #description>
              <div class="selected-element-detail">
                <span class="element-tag">&lt;{{ selectedElement.tagName }}&gt;</span>
                <code class="element-selector">{{ selectedElement.selector }}</code>
                <span v-if="selectedElement.textContent" class="element-text">
                  {{ selectedElement.textContent }}
                </span>
              </div>
            </template>
          </a-alert>
          <a-tooltip
            v-if="!isAppOwner"
            title="无法在别人的作品下对话哦~"
            placement="top"
            color="#ff4d4f"
          >
            <div class="input-disabled-mask">
              <a-textarea
                v-model:value="inputMessage"
                :placeholder="'无法在别人的作品下对话哦~'"
                :auto-size="{ minRows: 2, maxRows: 4 }"
                class="chat-input disabled-input"
                disabled
              />
            </div>
          </a-tooltip>
          <a-textarea
            v-else
            :key="inputKey"
            v-model:value="inputMessage"
            :placeholder="inputPlaceholder"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            class="chat-input"
            :disabled="generating"
            @keydown.enter.prevent="handleSend"
          />
          <div class="input-actions">
            <a-tooltip
              v-if="showVisualEditorButton"
              :title="isEditMode ? '退出点选模式' : '点击后可在右侧预览中选择页面元素进行修改'"
            >
              <a-button
                :type="isEditMode ? 'primary' : 'default'"
                class="edit-mode-btn"
                :class="{ 'edit-mode-btn-labeled': isWideScreen }"
                @click="toggleEditMode(getPreviewIframe())"
              >
                <aim-outlined />
                <span v-if="isWideScreen" class="edit-mode-label">
                  {{ isEditMode ? '退出点选' : '点选修改' }}
                </span>
              </a-button>
            </a-tooltip>
            <a-tooltip
              v-else-if="showVisualEditorWaiting"
              title="等待预览加载完成后可使用可视化编辑"
            >
              <a-button class="edit-mode-btn edit-mode-btn-labeled" disabled>
                <aim-outlined />
                <span class="edit-mode-label">点选修改</span>
              </a-button>
            </a-tooltip>
            <a-tooltip
              v-else-if="showVisualEditorMobileHint"
              title="请在大屏设备（宽度大于 992px）使用可视化编辑"
            >
              <a-button shape="circle" class="edit-mode-btn" disabled>
                <aim-outlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="generating"
              type="primary"
              shape="circle"
              class="send-btn stop-btn"
              @click="cancelGeneration"
            >
              <stop-outlined />
            </a-button>
            <a-button
              v-else
              type="primary"
              shape="circle"
              class="send-btn"
              :loading="sending"
              :disabled="!inputMessage.trim() || !isAppOwner"
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
        <div class="preview-frame-wrapper" :class="{ 'edit-mode-active': isEditMode }">
          <div
            v-if="isEditMode && previewUrl && previewBuildReady && !generating"
            class="edit-mode-banner"
          >
            <aim-outlined />
            <span>点击预览中的页面元素进行选择，然后在左侧描述你想如何修改</span>
            <a-button type="link" size="small" class="edit-mode-exit-btn" @click="toggleEditMode(getPreviewIframe())">
              退出
            </a-button>
          </div>
          <!-- 生成中状态：显示 Q 弹加载动画 -->
          <div v-if="generating" class="generating-loader">
            <div class="loader-container">
              <div class="shark-wrapper">
                <img src="@/assets/shark.svg" class="shark-icon" alt="loading" />
              </div>
              <div class="loader-shadow"></div>
              <div class="loader-text">
                <span class="loading-text">小鲨鱼正在全力构建中，请稍候</span>
                <span class="loading-dots">...</span>
              </div>
            </div>
          </div>
          <!-- Vue 工程构建中 -->
          <div
            v-else-if="isVueProject && previewBuildStatus === 'building'"
            class="preview-build-status"
          >
            <div class="loader-container">
              <div class="shark-wrapper">
                <img src="@/assets/shark.svg" class="shark-icon" alt="building" />
              </div>
              <div class="loader-shadow"></div>
              <div class="loader-text">
                <span class="loading-text">{{ previewBuildMessage || '正在安装依赖并构建项目，请稍候' }}</span>
                <span class="loading-dots">...</span>
              </div>
            </div>
          </div>
          <!-- Vue 工程构建失败 -->
          <div
            v-else-if="isVueProject && previewBuildStatus === 'failed'"
            class="preview-build-error"
          >
            <a-result status="error" title="预览构建失败">
              <template #subTitle>
                <div class="build-error-message">{{ previewBuildMessage }}</div>
              </template>
              <template #extra>
                <a-button type="primary" @click="checkPreviewBuildStatus(true)">重新检查</a-button>
              </template>
            </a-result>
          </div>
          <!-- 生成完成：显示预览 iframe -->
          <iframe
            v-else-if="previewUrl && previewBuildReady"
            ref="previewIframeRef"
            :key="iframeKey"
            :src="previewSrc"
            class="preview-frame"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
            @load="onPreviewIframeLoad"
          ></iframe>
          <div v-else class="preview-placeholder">
            <span>{{ isVueProject ? '等待构建完成...' : '预览加载中...' }}</span>
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
          <a-input v-model:value="deployedUrl" readonly style="width: calc(100% - 80px)" />
          <a-button type="primary" @click="copyUrl">复制</a-button>
        </a-input-group>
        <a-button type="link" @click="openDeployedUrl">访问应用</a-button>
      </div>
    </a-modal>

    <!-- 应用详情抽屉 -->
    <a-drawer
      v-model:open="showAppDetailDrawer"
      title="应用详情"
      placement="right"
      :width="360"
      :closable="true"
    >
      <div v-if="appInfo" class="app-detail-content">
        <!-- 应用基础信息 -->
        <div class="detail-section">
          <h3 class="section-title">应用信息</h3>
          <div class="detail-item">
            <span class="detail-label">应用名称：</span>
            <span class="detail-value">{{ appInfo.appName || '未命名应用' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">应用类型：</span>
            <a-tag color="blue">{{ getCodeGenTypeText(appInfo.codeGenType) }}</a-tag>
          </div>
          <div class="detail-item" v-if="appInfo.deployKey">
            <span class="detail-label">部署状态：</span>
            <a-tag color="green">已部署</a-tag>
          </div>
        </div>

        <!-- 创建者信息 -->
        <div class="detail-section">
          <h3 class="section-title">创建者</h3>
          <div class="creator-info">
            <img
              v-if="appInfo.user?.userAvatar"
              :src="appInfo.user.userAvatar"
              class="creator-avatar"
              alt="头像"
            />
            <img
              v-else
              src="@/assets/shark.svg"
              class="creator-avatar default-avatar"
              alt="默认头像"
            />
            <div class="creator-meta">
              <div class="creator-name">{{ appInfo.user?.userName || '匿名用户' }}</div>
              <div class="create-time">创建于 {{ formatCreateTime(appInfo.createTime) }}</div>
            </div>
          </div>
        </div>

        <!-- 操作栏（仅本人或管理员可见） -->
        <div v-if="canManageApp" class="detail-section">
          <h3 class="section-title">操作</h3>
          <div class="action-buttons">
            <a-button type="primary" block @click="goToEditPage"
              ><edit-outlined /> 修改应用</a-button
            >
            <a-button type="primary" danger block @click="confirmDeleteApp"
              ><delete-outlined /> 删除应用</a-button
            >
          </div>
        </div>
      </div>

      <a-skeleton v-else active :paragraph="{ rows: 6 }" />
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  DownOutlined,
  CloudUploadOutlined,
  UserOutlined,
  RobotOutlined,
  ArrowUpOutlined,
  LoadingOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  StopOutlined,
  InfoCircleOutlined,
  EditOutlined,
  DeleteOutlined,
  AimOutlined,
} from '@ant-design/icons-vue'
import { useVisualEditor } from '@/utils/visualEditor'
import { useLoginUserStore } from '@/stores/loginUser'
import { getAppVoById, deleteApp, deployApp, getVuePreviewBuildStatus } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { getCodeGenTypeText, CodeGenTypeEnum } from '@/constants/codeGenType'
import { getStaticPreviewUrl, API_BASE_URL } from '@/config/env'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用ID
const appId = computed(() => route.params.id as string)

// 应用信息
const appInfo = ref<API.AppVO>()

// 消息列表
const messages = ref<API.ChatHistory[]>([])

// 游标分页：每页条数
const CHAT_HISTORY_PAGE_SIZE = 10
const hasMoreHistory = ref(false)
const loadingMoreHistory = ref(false)
const historyCursor = ref<string>()

// 输入消息
const inputMessage = ref('')
const inputKey = ref(0)

// 状态
const sending = ref(false)
const generating = ref(false)
const deploying = ref(false)

// 代码生成进度
const codeProgress = ref<Array<{ fileName: string; filePath: string; completed: boolean }>>([])

// 预览相关 - 默认显示预览区域（双栏模式）
const showPreview = ref(true)
const previewUrl = ref('')
const canDeploy = ref(false)
const iframeKey = ref(0) // 用于强制刷新iframe
const previewBuildStatus = ref<'none' | 'building' | 'success' | 'failed'>('none')
const previewBuildMessage = ref('')
const previewBuildReady = ref(false)
const previewCacheVersion = ref(0)
const currentGenerationStartedAt = ref(0)
const sawBuildingSinceGeneration = ref(false)
const isAwaitingPreviewRebuild = ref(false)

let previewPollTimer: ReturnType<typeof setInterval> | null = null
let previewPollTimeout: ReturnType<typeof setTimeout> | null = null

const previewSrc = computed(() => {
  if (!previewUrl.value) return ''
  const separator = previewUrl.value.includes('?') ? '&' : '?'
  return `${previewUrl.value}${separator}v=${previewCacheVersion.value}`
})

const isVueProject = computed(
  () => appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT
)

// 部署弹窗
const deployModalVisible = ref(false)
const deployedUrl = ref('')

// 应用详情抽屉
const showAppDetailDrawer = ref(false)

// 消息容器ref
const messagesContainer = ref<HTMLElement>()

// 预览 iframe ref
const previewIframeRef = ref<HTMLIFrameElement>()

const getPreviewIframe = () => previewIframeRef.value ?? null

// 可视化编辑
const {
  isEditMode,
  selectedElement,
  canUseVisualEditor,
  reattachIframe,
  exitEditMode,
  clearSelectedElement,
  toggleEditMode,
  buildPromptWithElement,
} = useVisualEditor({
  previewReady: () => showPreview.value && previewBuildReady.value && !!previewUrl.value,
  onInjectFailed: () => {
    message.warning('无法在预览页面注入编辑脚本，请确认预览地址与当前页面同源后刷新重试')
  },
})

const isWideScreen = ref(true)

const updateScreenSize = () => {
  isWideScreen.value = window.innerWidth > 992
}

const showVisualEditorButton = computed(
  () => isAppOwner.value && !generating.value && isWideScreen.value && canUseVisualEditor.value,
)

const showVisualEditorWaiting = computed(
  () =>
    isAppOwner.value &&
    !generating.value &&
    isWideScreen.value &&
    showPreview.value &&
    !canUseVisualEditor.value,
)

const showVisualEditorMobileHint = computed(
  () => isAppOwner.value && !generating.value && !isWideScreen.value,
)

const inputPlaceholder = computed(() => {
  if (generating.value) return 'AI正在生成中...'
  if (selectedElement.value) return '描述你想如何修改选中的元素...'
  if (isEditMode.value) return '点击右侧预览选择元素，或先输入修改描述...'
  return '请描述你想生成的网站，越详细效果越好哦'
})

const onPreviewIframeLoad = () => {
  reattachIframe(getPreviewIframe())
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 格式化消息内容（支持Markdown风格）
const formatMessage = (content?: string) => {
  if (!content) return ''

  // 转义HTML特殊字符
  let formatted = content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // 处理代码块 ```language\ncode\n```
  formatted = formatted.replace(/```(\w*)\n?([\s\S]*?)```/g, (match, lang, code) => {
    return `<pre class="code-block"><code class="language-${lang || 'text'}">${code.trim()}</code></pre>`
  })

  // 处理行内代码 `code`
  formatted = formatted.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')

  // 处理粗体 **text**
  formatted = formatted.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')

  // 处理斜体 *text*
  formatted = formatted.replace(/\*([^*]+)\*/g, '<em>$1</em>')

  // 处理标题 ### Title
  formatted = formatted.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  formatted = formatted.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  formatted = formatted.replace(/^# (.+)$/gm, '<h2>$1</h2>')

  // 处理列表 - item
  formatted = formatted.replace(/^- (.+)$/gm, '<li>$1</li>')

  // 处理换行
  formatted = formatted.replace(/\n/g, '<br>')

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
        previewUrl.value = getStaticPreviewUrl(appInfo.value.codeGenType, appId.value)
      }
      if (isVueProject.value) {
        await checkPreviewBuildStatus()
      } else {
        previewBuildReady.value = true
        previewBuildStatus.value = 'success'
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

// 根据已加载消息决定是否展示网站预览
const updatePreviewByHistory = () => {
  const hasAiMessage = messages.value.some((msg) => msg.messageType === 'ai')
  if (messages.value.length >= 2 || hasAiMessage) {
    showPreview.value = true
    canDeploy.value = true
    if (isVueProject.value) {
      checkPreviewBuildStatus()
    } else {
      refreshPreviewFrame()
    }
  }
}

// 加载聊天历史（游标分页，首次加载最近一页）
const loadChatHistory = async () => {
  try {
    const res = await listAppChatHistory({
      appId: appId.value,
      pageSize: CHAT_HISTORY_PAGE_SIZE,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      hasMoreHistory.value = records.length >= CHAT_HISTORY_PAGE_SIZE
      messages.value = records.slice().reverse()
      if (records.length > 0) {
        historyCursor.value = records[records.length - 1].createTime
      } else {
        historyCursor.value = undefined
      }
      updatePreviewByHistory()
      scrollToBottom()
    }
  } catch (error) {
    message.error('加载聊天历史失败')
  }
}

// 加载更早的历史消息（追加到列表头部）
const loadMoreHistory = async () => {
  if (!hasMoreHistory.value || loadingMoreHistory.value || !historyCursor.value) {
    return
  }
  loadingMoreHistory.value = true
  const container = messagesContainer.value
  const prevScrollHeight = container?.scrollHeight ?? 0
  try {
    const res = await listAppChatHistory({
      appId: appId.value,
      pageSize: CHAT_HISTORY_PAGE_SIZE,
      lastCreateTime: historyCursor.value,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      hasMoreHistory.value = records.length >= CHAT_HISTORY_PAGE_SIZE
      if (records.length > 0) {
        const olderMessages = records.slice().reverse()
        const existingIds = new Set(messages.value.map((m) => m.id).filter(Boolean))
        const newMessages = olderMessages.filter((m) => !m.id || !existingIds.has(m.id))
        messages.value = [...newMessages, ...messages.value]
        historyCursor.value = records[records.length - 1].createTime
        nextTick(() => {
          if (container) {
            container.scrollTop = container.scrollHeight - prevScrollHeight
          }
        })
      } else {
        hasMoreHistory.value = false
      }
    } else {
      message.error('加载更多失败：' + res.data.message)
    }
  } catch (error) {
    message.error('加载更多失败')
  } finally {
    loadingMoreHistory.value = false
  }
}

// SSE 流式响应配置
const SSE_CONFIG = {
  // 节流配置：每 X 毫秒最多更新一次 DOM
  THROTTLE_MS: 16, // 约60fps，提供更流畅的视觉效果
  // 超时配置：X 秒没有收到数据视为超时
  TIMEOUT_MS: 60000, // 增加到60秒，给代码生成更多时间
  // 重试配置
  RETRY: {
    MAX_ATTEMPTS: 3,
    INITIAL_DELAY_MS: 1000,
    MAX_DELAY_MS: 10000,
    BACKOFF_MULTIPLIER: 2,
  },
  // 批处理配置：累积一定数量的字符后再更新DOM
  BATCH_SIZE: 10, // 每次累积10个字符后更新
}

// AbortController 用于取消请求
let currentAbortController: AbortController | null = null

// 节流函数 - 升级为支持尾调用的版本
const throttle = <T extends (...args: any[]) => void>(fn: T, limit: number) => {
  let inThrottle = false
  let lastArgs: Parameters<T> | null = null
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return (...args: Parameters<T>) => {
    lastArgs = args

    if (!inThrottle) {
      fn(...args)
      inThrottle = true
      lastArgs = null

      timeoutId = setTimeout(() => {
        inThrottle = false
        // 如果有待处理的调用，执行最后一次
        if (lastArgs !== null) {
          fn(...lastArgs)
          lastArgs = null
        }
      }, limit)
    }
  }
}

// 带重试的 fetch 请求
const fetchWithRetry = async (
  url: string,
  options: RequestInit,
  retryCount = 0,
): Promise<Response> => {
  try {
    const response = await fetch(url, options)
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    return response
  } catch (error) {
    const isNetworkError = error instanceof TypeError && error.message.includes('fetch')
    const shouldRetry = retryCount < SSE_CONFIG.RETRY.MAX_ATTEMPTS && isNetworkError

    if (shouldRetry) {
      const delay = Math.min(
        SSE_CONFIG.RETRY.INITIAL_DELAY_MS *
          Math.pow(SSE_CONFIG.RETRY.BACKOFF_MULTIPLIER, retryCount),
        SSE_CONFIG.RETRY.MAX_DELAY_MS,
      )
      console.warn(`请求失败，${delay}ms 后重试 (第 ${retryCount + 1} 次)...`)
      await new Promise((resolve) => setTimeout(resolve, delay))
      return fetchWithRetry(url, options, retryCount + 1)
    }

    throw error
  }
}

// 处理SSE流式响应（优化版 - 使用批处理和智能更新）
const handleSSEResponse = async (response: Response, aiMessageIndex: number): Promise<void> => {
  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('响应体不可读')
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let contentBuffer = '' // 内容批处理缓冲区
  let hasReceivedContent = false
  let lastReceiveTime = Date.now()
  let lastUpdateTime = Date.now()

  // 创建节流的更新函数（带批处理）
  const updateMessageContent = () => {
    if (contentBuffer && messages.value[aiMessageIndex]) {
      messages.value[aiMessageIndex].message += contentBuffer
      contentBuffer = ''
      hasReceivedContent = true
      lastUpdateTime = Date.now()
    }
  }

  // 节流的更新和滚动
  const throttledUpdate = throttle(() => {
    updateMessageContent()
    scrollToBottom()
  }, SSE_CONFIG.THROTTLE_MS)

  // 强制更新函数（用于流结束时）
  const forceUpdate = () => {
    if (contentBuffer) {
      updateMessageContent()
      scrollToBottom()
    }
  }

  // 超时检测定时器
  const timeoutChecker = setInterval(() => {
    const timeSinceLastReceive = Date.now() - lastReceiveTime
    if (timeSinceLastReceive > SSE_CONFIG.TIMEOUT_MS) {
      console.error(`SSE 连接超时 (${timeSinceLastReceive}ms 无数据)`)
      reader.cancel('超时')
      clearInterval(timeoutChecker)
    }
  }, 5000)

  // 性能统计
  let totalChunks = 0
  let totalBytes = 0
  const startTime = Date.now()

  try {
    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        // 流正常结束，强制更新所有剩余内容
        forceUpdate()
        const duration = Date.now() - startTime
        console.log(
          `✅ SSE 流正常结束 - 耗时: ${duration}ms, 接收块数: ${totalChunks}, 总字节: ${totalBytes}`,
        )
        break
      }

      // 更新统计
      totalChunks++
      totalBytes += value.length
      lastReceiveTime = Date.now()

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine) continue

        // 处理SSE格式：data: {...}
        if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.slice(5).trim()

          // 忽略空数据和结束标记
          if (!data || data === '[DONE]') continue

          try {
            const parsed = JSON.parse(data)

            // 处理心跳 ping
            if (parsed.type === 'ping' || parsed.event === 'ping') {
              console.log('💓 收到心跳')
              continue
            }

            // 后端返回格式: {"d": "内容"}
            if (parsed.d !== undefined && parsed.d !== null && parsed.d !== '') {
              contentBuffer += parsed.d

              // 批处理：累积到一定大小或时间间隔后更新
              const shouldUpdate =
                contentBuffer.length >= SSE_CONFIG.BATCH_SIZE ||
                Date.now() - lastUpdateTime > SSE_CONFIG.THROTTLE_MS * 2

              if (shouldUpdate) {
                throttledUpdate()
              }
            }
            // 处理代码进度更新
            else if (parsed.type === 'code_progress' && parsed.files) {
              codeProgress.value = parsed.files.map((f: any) => ({
                fileName: f.fileName || f.name || '',
                filePath: f.filePath || f.path || '',
                completed: f.completed || f.done || false,
              }))
              console.log('📦 代码进度更新:', codeProgress.value.length, '个文件')
            }
            // 处理普通内容更新
            else if (parsed.content !== undefined) {
              contentBuffer += parsed.content

              const shouldUpdate =
                contentBuffer.length >= SSE_CONFIG.BATCH_SIZE ||
                Date.now() - lastUpdateTime > SSE_CONFIG.THROTTLE_MS * 2

              if (shouldUpdate) {
                throttledUpdate()
              }
            }
            // 处理错误消息
            else if (parsed.error) {
              forceUpdate() // 先保存已接收的内容
              const errorMsg = typeof parsed.error === 'string' ? parsed.error : (parsed.message || '未知错误')
              throw new Error(errorMsg)
            }
          } catch (e) {
            // JSON解析失败
            if (e instanceof SyntaxError) {
              // 不是JSON格式，可能是纯文本流
              console.warn('⚠️ JSON解析失败，作为纯文本处理:', data.substring(0, 50))
              contentBuffer += data
              throttledUpdate()
            } else {
              // 其他错误（如业务错误）
              throw e
            }
          }
        }
        // 处理事件类型 (如 event: done)
        else if (trimmedLine.startsWith('event:')) {
          const eventType = trimmedLine.slice(6).trim()
          console.log('📨 收到事件:', eventType)

          if (eventType === 'done' || eventType === 'complete') {
            forceUpdate() // 确保所有内容都已更新
            generating.value = false
            canDeploy.value = true
            if (isVueProject.value && hasReceivedContent) {
              sawBuildingSinceGeneration.value = false
              previewBuildReady.value = false
              previewBuildStatus.value = 'building'
              previewBuildMessage.value = '正在重新构建预览...'
            }
          } else if (eventType === 'error') {
            forceUpdate()
            throw new Error('服务器报告错误')
          }
        }
      }
    }
  } catch (error) {
    console.error('❌ SSE 读取错误:', error)

    // 先保存已接收的内容
    forceUpdate()

    // 设置错误消息
    if (messages.value[aiMessageIndex]) {
      const currentContent = messages.value[aiMessageIndex].message
      if (!currentContent || currentContent.trim() === '') {
        const errMsg = error instanceof Error ? error.message : '连接中断'
        messages.value[aiMessageIndex].message = '❌ 生成失败：' + errMsg
      } else {
        messages.value[aiMessageIndex].message += '\n\n---\n⚠️ 生成中断，部分内容可能未保存'
      }
      scrollToBottom()
    }
    throw error
  } finally {
    clearInterval(timeoutChecker)
    generating.value = false
    reader.releaseLock()

    // 确保所有内容都已显示
    forceUpdate()

    // 如果收到了内容，显示预览
    if (hasReceivedContent) {
      showPreview.value = true
      canDeploy.value = true
    }

    // 清除代码进度
    codeProgress.value = []

    // 刷新预览 iframe（Vue 工程需等待异步 npm build）
    if (hasReceivedContent) {
      if (appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
        scheduleVuePreviewRefresh()
      } else {
        setTimeout(() => {
          previewCacheVersion.value = Date.now()
          refreshPreviewFrame()
        }, 1500)
      }
    }
  }
}

// 取消当前生成
const cancelGeneration = () => {
  if (currentAbortController) {
    console.log('🛑 用户取消生成')
    currentAbortController.abort()
    currentAbortController = null
    generating.value = false
    codeProgress.value = []
    message.info('已取消生成')
  }
}

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim() || sending.value || generating.value) return

  const displayMessage = inputMessage.value.trim()
  const apiMessage = buildPromptWithElement(displayMessage)

  // 先清空输入并等待 DOM 同步，再切换 disabled，避免 Textarea 保留旧值
  inputMessage.value = ''
  inputKey.value++
  await nextTick()

  sending.value = true
  generating.value = true
  currentGenerationStartedAt.value = Date.now()

  // 发送后退出编辑模式并清除选中元素
  exitEditMode(getPreviewIframe())

  // 添加用户消息（展示原始输入，不含元素上下文）
  messages.value.push({
    messageType: 'user',
    message: displayMessage,
    appId: appId.value,
    userId: loginUserStore.loginUser.id || '0',
  })
  scrollToBottom()

  // 添加AI消息占位
  messages.value.push({
    messageType: 'ai',
    message: '',
    appId: appId.value,
    userId: '0',
  })
  const aiMessageIndex = messages.value.length - 1

  // 创建新的 AbortController
  currentAbortController = new AbortController()

  try {
    // 使用fetch API处理SSE（带重试）
    const response = await fetchWithRetry(
      `${API_BASE_URL}/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(apiMessage)}`,
      {
        method: 'GET',
        credentials: 'include',
        signal: currentAbortController.signal,
      },
    )

    await handleSSEResponse(response, aiMessageIndex)
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.log('请求被取消')
    } else {
      console.error('发送消息失败:', error)
      message.error('发送消息失败：' + (error instanceof Error ? error.message : '未知错误'))
    }
  } finally {
    sending.value = false
    generating.value = false
    currentAbortController = null
  }
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

const stopPreviewPolling = () => {
  if (previewPollTimer) {
    clearInterval(previewPollTimer)
    previewPollTimer = null
  }
  if (previewPollTimeout) {
    clearTimeout(previewPollTimeout)
    previewPollTimeout = null
  }
}

const applyPreviewRefresh = (buildTimestamp?: number) => {
  previewCacheVersion.value = buildTimestamp || Date.now()
  refreshPreviewFrame()
}

// 刷新预览（通过修改key强制重新渲染iframe）
const refreshPreview = async () => {
  if (isVueProject.value) {
    await checkPreviewBuildStatus(true)
    return
  }
  previewCacheVersion.value = Date.now()
  refreshPreviewFrame()
  message.success('预览已刷新')
}

// 刷新预览iframe（不显示提示）
const refreshPreviewFrame = () => {
  iframeKey.value++
}

// 查询 Vue 工程预览构建状态
const checkPreviewBuildStatus = async (showToast = false) => {
  if (!isVueProject.value) {
    previewBuildReady.value = true
    previewBuildStatus.value = 'success'
    return
  }
  try {
    const res = await getVuePreviewBuildStatus({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      const status = (data.status as typeof previewBuildStatus.value) || 'none'
      previewBuildMessage.value = data.message || ''

      if (status === 'building') {
        sawBuildingSinceGeneration.value = true
        previewBuildStatus.value = 'building'
        previewBuildReady.value = false
        return
      }

      if (status === 'failed' || data.ready === false) {
        previewBuildStatus.value = 'failed'
        previewBuildReady.value = false
        isAwaitingPreviewRebuild.value = false
        stopPreviewPolling()
        if (showToast) {
          message.error(previewBuildMessage.value || '预览构建失败')
        }
        return
      }

      if (data.ready) {
        const buildTimestamp = data.timestamp || 0
        const canAccept =
          showToast ||
          !isAwaitingPreviewRebuild.value ||
          sawBuildingSinceGeneration.value ||
          buildTimestamp > currentGenerationStartedAt.value

        if (!canAccept) {
          return
        }

        previewBuildStatus.value = 'success'
        previewBuildReady.value = true
        isAwaitingPreviewRebuild.value = false
        stopPreviewPolling()
        applyPreviewRefresh(buildTimestamp)
        if (showToast) {
          message.success('预览已刷新')
        }
      }
    }
  } catch (error) {
    previewBuildStatus.value = 'failed'
    previewBuildMessage.value = '无法获取构建状态，请稍后重试'
    previewBuildReady.value = false
    isAwaitingPreviewRebuild.value = false
    stopPreviewPolling()
  }
}

// Vue 工程需在 npm install + build 完成后才有 dist，轮询构建状态直至成功或超时
const scheduleVuePreviewRefresh = () => {
  if (!isVueProject.value) {
    return
  }

  stopPreviewPolling()
  isAwaitingPreviewRebuild.value = true
  sawBuildingSinceGeneration.value = false
  previewBuildStatus.value = 'building'
  previewBuildReady.value = false
  previewBuildMessage.value = '正在重新构建预览...'

  void checkPreviewBuildStatus()
  previewPollTimer = setInterval(() => {
    void checkPreviewBuildStatus()
  }, 3000)
  previewPollTimeout = setTimeout(() => {
    stopPreviewPolling()
    if (isAwaitingPreviewRebuild.value) {
      previewBuildMessage.value = '构建时间较长，请稍后点击刷新按钮重试'
    }
  }, 180000)
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

// 是否是应用所有者
const isAppOwner = computed(() => {
  if (!appInfo.value?.userId || !loginUserStore.loginUser.id) return false
  return appInfo.value.userId === loginUserStore.loginUser.id
})

// 是否可以管理应用（本人或管理员）
const canManageApp = computed(() => {
  if (!appInfo.value?.userId || !loginUserStore.loginUser.id) return false
  const isAdmin = loginUserStore.loginUser.userRole === 'admin'
  return isAppOwner.value || isAdmin
})

// 格式化创建时间
const formatCreateTime = (time?: string) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 是否已经发送过初始提示词
const initialPromptSent = ref(false)

// 发送初始提示词（新应用创建后自动触发）
const sendInitialPrompt = async () => {
  if (!appInfo.value?.initPrompt || initialPromptSent.value) return

  initialPromptSent.value = true
  generating.value = true
  currentGenerationStartedAt.value = Date.now()

  // 添加用户消息（使用初始提示词）
  messages.value.push({
    messageType: 'user',
    message: appInfo.value.initPrompt,
    appId: appId.value,
    userId: loginUserStore.loginUser.id || '0',
  })
  scrollToBottom()

  // 添加AI消息占位
  messages.value.push({
    messageType: 'ai',
    message: '',
    appId: appId.value,
    userId: '0',
  })
  const aiMessageIndex = messages.value.length - 1

  // 创建新的 AbortController
  currentAbortController = new AbortController()

  try {
    const response = await fetchWithRetry(
      `${API_BASE_URL}/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(appInfo.value.initPrompt)}`,
      {
        method: 'GET',
        credentials: 'include',
        signal: currentAbortController.signal,
      },
    )

    await handleSSEResponse(response, aiMessageIndex)
  } catch (error) {
    if (error instanceof Error && error.name !== 'AbortError') {
      console.error('发送初始提示词失败:', error)
      message.error('发送初始提示词失败')
    }
  } finally {
    generating.value = false
    currentAbortController = null
  }
}

// 页面加载
onMounted(() => {
  updateScreenSize()
  window.addEventListener('resize', updateScreenSize)

  if (!appId.value) {
    message.error('应用ID不能为空')
    router.push('/')
    return
  }
  // 页面加载时滚动到顶部
  window.scrollTo(0, 0)
  loadAppInfo().then(() => {
    loadChatHistory().then(() => {
      // 自己的应用且无对话历史时，自动发送初始提示词
      if (isAppOwner.value && messages.value.length === 0 && appInfo.value?.initPrompt) {
        sendInitialPrompt()
      }
    })
  })
})

// 监听消息变化，自动滚动
watch(messages, scrollToBottom, { deep: true })

// 预览就绪或生成结束后重新绑定 iframe 脚本
watch(previewBuildReady, async (ready) => {
  if (ready) {
    await nextTick()
    reattachIframe(getPreviewIframe())
  }
})

watch(generating, async (val, oldVal) => {
  if (oldVal && !val) {
    await nextTick()
    if (previewBuildReady.value) {
      reattachIframe(getPreviewIframe())
    }
  }
})

watch(iframeKey, async () => {
  if (isEditMode.value && previewBuildReady.value) {
    await nextTick()
    reattachIframe(getPreviewIframe())
  }
})

// 页面卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', updateScreenSize)
  stopPreviewPolling()
  if (currentAbortController) {
    currentAbortController.abort()
    currentAbortController = null
  }
})
</script>

<style scoped>
#appChatPage {
  height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 顶部栏 */
.chat-header {
  height: 48px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  flex-shrink: 0;
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

/* 应用详情抽屉样式 */
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

.creator-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.creator-avatar.default-avatar {
  padding: 8px;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
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
  flex: 2;
  width: auto;
  min-width: 0;
  border-right: 1px solid #f0f0f0;
}

/* 消息区域 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.load-more-wrapper {
  text-align: center;
  padding-bottom: 8px;
}

.message-item {
  display: flex;
  gap: 8px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 10px;
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

/* 代码块样式 */
.message-content :deep(.code-block) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
}

/* 行内代码样式 */
.message-content :deep(.inline-code) {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
  color: #d63384;
}

.user-message .message-content :deep(.inline-code) {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* 标题样式 */
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 600;
}

.message-content :deep(h2) {
  font-size: 18px;
}

.message-content :deep(h3) {
  font-size: 16px;
}

.message-content :deep(h4) {
  font-size: 14px;
}

.user-message .message-content :deep(h2),
.user-message .message-content :deep(h3),
.user-message .message-content :deep(h4) {
  color: #fff;
}

/* 列表样式 */
.message-content :deep(li) {
  margin: 4px 0;
  list-style-position: inside;
}

/* 粗体和斜体 */
.message-content :deep(strong) {
  font-weight: 600;
}

.user-message .message-content :deep(strong) {
  color: #fff;
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
  padding: 12px;
  margin: 4px 0;
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
  padding: 10px 16px;
  background: #fff;
  flex-shrink: 0;
}

.chat-input {
  border: none !important;
  resize: none;
  background: #f7f8fa;
  border-radius: 10px;
  padding: 10px 12px;
  box-shadow: none !important;
}

.chat-input.disabled-input {
  background: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}

.input-disabled-mask {
  width: 100%;
  cursor: not-allowed;
}

.chat-input:focus {
  box-shadow: none !important;
  background: #f0f2f5;
}

.selected-element-alert {
  margin-bottom: 8px;
}

.selected-element-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  line-height: 1.5;
}

.element-tag {
  color: #1677ff;
  font-weight: 500;
}

.element-selector {
  display: block;
  padding: 2px 6px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 11px;
  word-break: break-all;
}

.element-text {
  color: #666;
  word-break: break-word;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.edit-mode-btn {
  border-color: #d9d9d9;
}

.edit-mode-btn-labeled {
  border-radius: 20px;
  height: 36px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.edit-mode-label {
  font-size: 12px;
}

.send-btn {
  background: #333;
  border: none;
}

.send-btn:disabled {
  background: #ccc;
}

.stop-btn {
  background: #ff4d4f;
  animation: pulse 1.5s ease-in-out infinite;
}

.stop-btn:hover {
  background: #ff7875;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 8px rgba(255, 77, 79, 0);
  }
}

/* 右侧预览区域（与左侧 2:3） */
.preview-section {
  flex: 3;
  width: auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.preview-header {
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  flex-shrink: 0;
}

.preview-title {
  font-weight: 500;
}

.preview-frame-wrapper {
  flex: 1;
  padding: 8px;
  overflow: hidden;
  position: relative;
}

.preview-frame-wrapper.edit-mode-active {
  box-shadow: inset 0 0 0 3px #1677ff;
  border-radius: 8px;
  background: rgba(22, 119, 255, 0.04);
}

.preview-frame-wrapper.edit-mode-active .preview-frame {
  cursor: crosshair;
}

.edit-mode-banner {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  max-width: calc(100% - 32px);
  background: rgba(22, 119, 255, 0.95);
  color: #fff;
  padding: 8px 12px 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.35);
  pointer-events: none;
}

.edit-mode-banner > span {
  flex: 1;
  line-height: 1.4;
}

.edit-mode-exit-btn {
  color: #fff !important;
  flex-shrink: 0;
  padding: 0 4px;
  pointer-events: auto;
}

.edit-mode-exit-btn:hover {
  color: rgba(255, 255, 255, 0.85) !important;
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

.preview-build-status {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.preview-build-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: auto;
  background: #fff;
}

.build-error-message {
  max-width: 520px;
  margin: 0 auto;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

/* ==================== Q弹加载动画 - 美团风格 ==================== */
.generating-loader {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.loader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

/* 小鲨鱼容器 - 用于弹跳动画 */
.shark-wrapper {
  width: 80px;
  height: 80px;
  position: relative;
  animation: bounce 0.8s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
}

/* 小鲨鱼图标 */
.shark-icon {
  width: 100%;
  height: 100%;
  object-fit: contain;
  animation: squish 0.8s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
  filter: drop-shadow(0 4px 8px rgba(24, 144, 255, 0.3));
}

/* 弹跳动画 */
@keyframes bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-40px);
  }
}

/* 挤压变形动画 - 模拟 Q 弹质感 */
@keyframes squish {
  0% {
    transform: scale(1, 1);
  }
  15% {
    /* 起飞前准备 - 轻微压扁 */
    transform: scale(1.1, 0.9);
  }
  50% {
    /* 最高点 - 轻微拉长 */
    transform: scale(0.95, 1.05);
  }
  75% {
    /* 落地前 - 拉长 */
    transform: scale(0.9, 1.1);
  }
  85% {
    /* 落地瞬间 - 压扁 */
    transform: scale(1.15, 0.8);
  }
  100% {
    /* 恢复 */
    transform: scale(1, 1);
  }
}

/* 动态阴影 */
.loader-shadow {
  width: 60px;
  height: 12px;
  background: radial-gradient(ellipse at center, rgba(0, 0, 0, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  animation: shadow-scale 0.8s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
  margin-top: -12px;
}

@keyframes shadow-scale {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    /* 图形在最高点时，阴影变小变淡 */
    transform: scale(0.5);
    opacity: 0.3;
  }
}

/* 加载文字 */
.loader-text {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.loading-text {
  letter-spacing: 0.5px;
}

/* 动态省略号 - 打字机效果 */
.loading-dots {
  display: inline-flex;
  width: 24px;
  text-align: left;
  overflow: hidden;
  animation: typing-dots 1.5s steps(4) infinite;
}

@keyframes typing-dots {
  0% {
    width: 0;
    opacity: 1;
  }
  25% {
    width: 8px;
  }
  50% {
    width: 16px;
  }
  75% {
    width: 24px;
    opacity: 1;
  }
  100% {
    width: 24px;
    opacity: 0.5;
  }
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
    flex: 1;
    width: 100%;
    border-right: none;
  }

  .preview-section {
    display: none;
  }
}

@media (max-width: 576px) {
  #appChatPage {
    height: calc(100vh - 64px);
  }

  .messages-container {
    padding: 10px 12px;
  }

  .input-section {
    padding: 8px 12px;
  }

  .message-bubble {
    max-width: 85%;
  }
}
</style>
