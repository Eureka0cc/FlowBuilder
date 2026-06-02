<template>
  <div id="homePage">
    <!-- 核心交互区域（Hero Section）- 限制最大宽度并居中 -->
    <div class="hero-container">
      <!-- 网站标题区域 -->
      <div class="hero-section">
        <h1 class="main-title">
          一句话
          <span class="logo-wrapper">
            <img src="@/assets/shark.svg" alt="logo" class="title-logo" />
          </span>
          呈所想
        </h1>
        <p class="sub-title">与 AI 对话轻松创建应用和网站</p>
      </div>

      <!-- 提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="promptText"
          :placeholder="'使用 NoCode 创建一个高效的小工具，帮我计算......'"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          class="prompt-input"
        />
        <div class="input-actions">
          <div class="left-actions">
            <a-button type="text" class="action-btn">
              <paper-clip-outlined />
              上传
            </a-button>
            <a-button type="text" class="action-btn">
              <thunderbolt-outlined />
              优化
            </a-button>
          </div>
          <a-button
            type="primary"
            shape="circle"
            size="large"
            class="send-btn"
            :loading="creating"
            @click="handleCreateApp"
          >
            <arrow-up-outlined />
          </a-button>
        </div>
      </div>

      <!-- 快速示例标签 -->
      <div class="examples-section">
        <a-tag
          v-for="example in quickExamples"
          :key="example"
          class="example-tag"
          @click="handleExampleClick(example)"
        >
          {{ example }}
        </a-tag>
      </div>
    </div>

    <!-- 我的应用列表 - 通栏全屏流式布局 -->
    <div v-if="loginUserStore.loginUser.id" class="section works-section">
      <div class="section-container">
        <div class="section-header">
          <h2 class="section-title">我的作品</h2>
          <a-input-search
            v-model:value="myAppSearchText"
            placeholder="搜索应用"
            class="search-input"
            @search="loadMyApps"
          />
        </div>
        <a-row :gutter="[24, 24]">
          <a-col
            v-for="app in myApps"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            :xl="5"
          >
            <a-card hoverable class="app-card" @click="goToAppChat(app.id)">
              <div class="app-cover-wrapper">
                <img
                  v-if="app.cover"
                  :src="app.cover"
                  :alt="app.appName"
                  class="app-cover"
                />
                <div v-else class="app-cover-placeholder">
                  <file-image-outlined />
                </div>
              </div>
              <div class="app-info">
                <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
                <p class="app-meta">创建于 {{ formatTime(app.createTime) }}</p>
              </div>
            </a-card>
          </a-col>
        </a-row>
        <div v-if="myApps.length === 0" class="empty-state">
          <a-empty description="暂无应用，快去创建一个吧" />
        </div>
        <div v-if="myAppsTotal > myAppsPageSize" class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPageNum"
            :total="myAppsTotal"
            :page-size="myAppsPageSize"
            @change="loadMyApps"
          />
        </div>
      </div>
    </div>

    <!-- 精选应用列表 - 通栏全屏流式布局 -->
    <div class="section featured-section">
      <div class="section-container">
        <div class="section-header">
          <h2 class="section-title">精选案例</h2>
          <a-input-search
            v-model:value="goodAppSearchText"
            placeholder="搜索精选应用"
            class="search-input"
            @search="loadGoodApps"
          />
        </div>
        <a-row :gutter="[24, 24]">
          <a-col
            v-for="app in goodApps"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            :xl="5"
          >
            <a-card hoverable class="app-card" @click="goToAppChat(app.id)">
              <div class="app-cover-wrapper">
                <img
                  v-if="app.cover"
                  :src="app.cover"
                  :alt="app.appName"
                  class="app-cover"
                />
                <div v-else class="app-cover-placeholder">
                  <file-image-outlined />
                </div>
              </div>
              <div class="app-info">
                <div class="app-author">
                  <a-avatar
                    v-if="app.user?.userAvatar"
                    :src="app.user.userAvatar"
                    size="small"
                  />
                  <a-avatar v-else size="small">
                    <user-outlined />
                  </a-avatar>
                  <span class="author-name">{{ app.user?.userName || '匿名用户' }}</span>
                  <a-tag v-if="app.priority === 99" color="gold">精选</a-tag>
                </div>
                <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
                <p class="app-type">{{ getCodeGenTypeText(app.codeGenType) }}</p>
              </div>
            </a-card>
          </a-col>
        </a-row>
        <div v-if="goodApps.length === 0" class="empty-state">
          <a-empty description="暂无精选应用" />
        </div>
        <div v-if="goodAppsTotal > goodAppsPageSize" class="pagination-wrapper">
          <a-pagination
            v-model:current="goodAppsPageNum"
            :total="goodAppsTotal"
            :page-size="goodAppsPageSize"
            @change="loadGoodApps"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PaperClipOutlined,
  ThunderboltOutlined,
  ArrowUpOutlined,
  FileImageOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 输入框文本
const promptText = ref('')
const creating = ref(false)

// 快速示例
const quickExamples = ['波普风电商页面', '企业网站', '电商运营后台', '暗黑话题社区']

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPageNum = ref(1)
const myAppsPageSize = ref(20)
const myAppsTotal = ref(0)
const myAppSearchText = ref('')

// 精选应用数据
const goodApps = ref<API.AppVO[]>([])
const goodAppsPageNum = ref(1)
const goodAppsPageSize = ref(20)
const goodAppsTotal = ref(0)
const goodAppSearchText = ref('')

// 格式化时间
const formatTime = (time?: string) => {
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

// 获取代码生成类型文本
const getCodeGenTypeText = (type?: string) => {
  const typeMap: Record<string, string> = {
    HTML: '单页应用',
    MULTI_FILE: '多文件应用',
    VUE_PROJECT: 'Vue项目',
  }
  return typeMap[type || ''] || '应用'
}

// 点击示例
const handleExampleClick = (example: string) => {
  promptText.value = `帮我创建一个${example}`
}

// 创建应用
const handleCreateApp = async () => {
  if (!promptText.value.trim()) {
    message.warning('请输入提示词')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: promptText.value.trim(),
    })
    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      message.success('应用创建成功')
      router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建应用失败：' + res.data.message)
    }
  } catch (error) {
    message.error('创建应用失败')
  } finally {
    creating.value = false
  }
}

// 跳转到应用对话页
const goToAppChat = (appId?: number) => {
  if (!appId) return
  router.push(`/app/chat/${appId}`)
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPageNum.value,
      pageSize: myAppsPageSize.value,
      appName: myAppSearchText.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('加载我的应用失败')
  }
}

// 加载精选应用
const loadGoodApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: goodAppsPageNum.value,
      pageSize: goodAppsPageSize.value,
      appName: goodAppSearchText.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      goodApps.value = res.data.data.records || []
      goodAppsTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('加载精选应用失败')
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadGoodApps()
})
</script>

<style scoped>
/* ==================== 通栏全屏基础布局 ==================== */
#homePage {
  width: 100%;
  background: #fff;
}

/* ==================== 核心交互区域（限制宽度居中） ==================== */
.hero-container {
  max-width: 896px;
  /* max-w-4xl 约等于 896px */
  margin: 0 auto;
  padding: 64px 24px 48px;
  /* pt-16 pb-12 px-6 */
}

/* 标题区域 */
.hero-section {
  text-align: center;
  margin-bottom: 40px;
}

.main-title {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.logo-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  border-radius: 50%;
}

.title-logo {
  width: 36px;
  height: 36px;
  filter: brightness(0) invert(1);
}

.sub-title {
  font-size: 18px;
  color: #666;
  margin: 0;
}

/* 输入区域 - 保持精致样式 */
.input-section {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 20px;
  margin-bottom: 24px;
}

.prompt-input {
  border: none;
  resize: none;
  font-size: 16px;
}

.prompt-input:focus {
  box-shadow: none;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
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

/* 示例标签 */
.examples-section {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.example-tag {
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.example-tag:hover {
  background: #1890ff;
  color: #fff;
}

/* ==================== 作品展示区域（通栏全屏） ==================== */
.section {
  width: 100%;
  padding: 48px 24px;
  /* py-12 px-6 */
}

.works-section {
  background: #fafafa;
  /* 轻微的背景色区分 */
}

.featured-section {
  background: #fff;
  padding-bottom: 80px;
  /* 底部留出足够空间 */
}

/* 内容容器 - 控制最大宽度并居中 */
.section-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  /* 舒适的左右内边距 */
}

@media (min-width: 768px) {
  .section-container {
    padding: 0 48px;
    /* md:px-12 */
  }
}

@media (min-width: 1024px) {
  .section-container {
    padding: 0 96px;
    /* lg:px-24 */
  }
}

@media (min-width: 1280px) {
  .section-container {
    padding: 0 120px;
    /* xl:px-30 更宽的屏幕更大的内边距 */
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.section-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.search-input {
  width: 280px;
}

/* 应用卡片 */
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
}

.app-cover-placeholder {
  font-size: 48px;
  color: #ccc;
}

.app-info {
  padding-top: 16px;
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

.app-type {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.empty-state {
  padding: 64px 0;
}

.pagination-wrapper {
  text-align: center;
  margin-top: 48px;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 768px) {
  .hero-container {
    padding: 48px 16px 36px;
  }

  .main-title {
    font-size: 32px;
  }

  .logo-wrapper {
    width: 40px;
    height: 40px;
  }

  .title-logo {
    width: 24px;
    height: 24px;
  }

  .section {
    padding: 36px 16px;
  }

  .section-container {
    padding: 0 16px;
  }

  .section-title {
    font-size: 22px;
  }

  .section-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .search-input {
    width: 100%;
  }

  .app-cover-wrapper {
    height: 160px;
  }
}

@media (max-width: 576px) {
  .hero-container {
    padding: 36px 12px 24px;
  }

  .section {
    padding: 24px 12px;
  }

  .section-container {
    padding: 0 12px;
  }

  .input-section {
    padding: 16px;
  }
}
</style>
