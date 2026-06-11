<template>
  <div id="homePage">
    <!-- 科技感渐变 Hero 区域 -->
    <div class="hero-banner">
      <div class="hero-bg-grid" aria-hidden="true"></div>
      <div class="hero-container">
        <div class="hero-section">
          <h1 class="main-title">SeekWeb AI 应用生成平台</h1>
          <p class="sub-title">一句话轻松创建网站应用</p>
        </div>

        <div class="input-section">
          <a-textarea
            v-model:value="promptText"
            placeholder="帮我创建个人博客网站"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            class="prompt-input"
            @keydown.ctrl.enter="handleCreateApp"
          />
          <div class="input-actions">
            <a-button
              type="primary"
              shape="circle"
              size="large"
              class="send-btn"
              :loading="creating"
              :disabled="!promptText.trim() || creating"
              @click="handleCreateApp"
            >
              <template #icon>
                <arrow-up-outlined />
              </template>
            </a-button>
          </div>
        </div>

        <div class="examples-section">
          <a-tag
            v-for="example in quickExamples"
            :key="example.label"
            class="example-tag"
            @click="handleExampleClick(example)"
          >
            {{ example.label }}
          </a-tag>
        </div>
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
        <a-row :gutter="[16, 16]">
          <a-col v-for="app in myApps" :key="app.id" :xs="24" :sm="12" :md="8" :lg="4.8" :xl="4.8">
            <AppCard
              :app="app"
              :show-create-time="true"
              @view-chat="goToAppChat"
              @view-work="(app) => openDeployedApp(app.deployKey)"
            />
          </a-col>
        </a-row>
        <div v-if="myApps.length === 0" class="empty-state">
          <a-empty description="暂无应用，快去创建一个吧" />
        </div>
        <div v-if="myApps.length > 0" class="pagination-wrapper">
          <a-pagination
            v-model:current="myAppsPageNum"
            :total="myAppsTotal"
            :page-size="myAppsPageSize"
            :show-size-changer="false"
            :show-total="(total) => `共 ${total} 个应用`"
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
        <a-row :gutter="[16, 16]">
          <a-col
            v-for="app in goodApps"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="4.8"
            :xl="4.8"
          >
            <AppCard
              :app="app"
              :show-author="true"
              :show-type="true"
              @view-chat="goToAppChat"
              @view-work="(app) => openDeployedApp(app.deployKey)"
            />
          </a-col>
        </a-row>
        <div v-if="goodApps.length === 0" class="empty-state">
          <a-empty description="暂无精选应用" />
        </div>
        <div v-if="goodApps.length > 0" class="pagination-wrapper">
          <a-pagination
            v-model:current="goodAppsPageNum"
            :total="goodAppsTotal"
            :page-size="goodAppsPageSize"
            :show-size-changer="false"
            :show-total="(total) => `共 ${total} 个应用`"
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
import { ArrowUpOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import { getDeployUrl } from '@/config/env'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 封面图加载错误处理
const coverErrorSet = ref<Set<string>>(new Set())

const handleCoverError = (appId: string) => {
  coverErrorSet.value.add(appId)
}

// 输入框文本
const promptText = ref('')
const creating = ref(false)

// 快速示例（标签 + 约 100 字具体提示词）
const quickExamples = [
  {
    label: '个人博客',
    prompt:
      '帮我创建一个现代风格的个人技术博客网站，包含首页文章列表、文章详情页、关于我与联系方式页面。整体采用简洁浅色主题，支持 Markdown 文章展示、标签分类与搜索，移动端自适应，导航清晰，适合程序员记录学习笔记与项目复盘。',
  },
  {
    label: '企业官网',
    prompt:
      '帮我创建一个科技公司企业官网，包含首页、产品介绍、解决方案、客户案例、新闻动态与联系我们等页面。风格专业可信，主色蓝白，突出品牌价值与核心优势，配有轮播 Banner、数据亮点展示与在线咨询入口，整体响应式布局。',
  },
  {
    label: '电商落地页',
    prompt:
      '帮我创建一个单品电商促销落地页，包含产品卖点、用户评价、价格套餐对比、限时优惠倒计时、FAQ 常见问题与底部购买按钮。视觉吸睛、转化导向，支持商品图轮播与信任背书模块，配色活泼，移动端购买体验流畅。',
  },
  {
    label: '作品集网站',
    prompt:
      '帮我创建一个设计师作品集展示网站，包含首页个人介绍、项目案例网格、项目详情页（含设计说明与多图展示）、技能与时间线、联系合作表单。风格高级简约，大图留白，支持分类筛选与暗色模式切换，突出视觉作品质量。',
  },
]

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPageNum = ref(1)
const myAppsPageSize = ref(6)
const myAppsTotal = ref(0)
const myAppSearchText = ref('')

// 精选应用数据
const goodApps = ref<API.AppVO[]>([])
const goodAppsPageNum = ref(1)
const goodAppsPageSize = ref(6)
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

// 点击示例
const handleExampleClick = (example: { label: string; prompt: string }) => {
  promptText.value = example.prompt
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
      promptText.value = ''
      message.success('应用创建成功')
      // 跳转到对话页，标记为新应用以自动发送初始提示词
      router.push(`/app/chat/${appId}?new=true`)
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
const goToAppChat = (app: API.AppVO) => {
  if (!app.id) return
  router.push(`/app/chat/${app.id}`)
}

// 新窗口打开部署的应用
const openDeployedApp = (deployKey?: string) => {
  if (!deployKey) return
  window.open(getDeployUrl(deployKey), '_blank')
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPageNum.value,
      pageSize: myAppsPageSize.value,
      appName: myAppSearchText.value || undefined,
      sortField: 'createTime',
      sortOrder: 'descend',
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
      sortField: 'editTime',
      sortOrder: 'descend',
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
#homePage {
  width: 100%;
  background: #f4f6fb;
}

/* 科技感渐变 Hero */
.hero-banner {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: linear-gradient(125deg, #020617 0%, #0f172a 28%, #1e1b4b 55%, #0c4a6e 82%, #06b6d4 120%);
}

.hero-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.06) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 80% 70% at 50% 40%, black 20%, transparent 75%);
  pointer-events: none;
}

.hero-banner::after {
  content: '';
  position: absolute;
  top: -40%;
  right: -10%;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.25) 0%, transparent 70%);
  pointer-events: none;
}

.hero-container {
  position: relative;
  z-index: 1;
  max-width: 880px;
  margin: 0 auto;
  padding: 72px 24px 56px;
}

.hero-section {
  text-align: center;
  margin-bottom: 36px;
}

.main-title {
  font-size: 44px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: 0.02em;
  background: linear-gradient(90deg, #f8fafc 0%, #e0f2fe 45%, #67e8f9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.sub-title {
  font-size: 18px;
  color: rgba(226, 232, 240, 0.85);
  margin: 0;
  font-weight: 400;
}

.input-section {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16px;
  padding: 16px 16px 12px;
  margin-bottom: 20px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.18),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
  backdrop-filter: blur(12px);
}

.prompt-input {
  border: none !important;
  resize: none;
  font-size: 16px;
  background: transparent !important;
  box-shadow: none !important;
  padding: 4px 4px 0;
}

.prompt-input:focus {
  box-shadow: none !important;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 8px;
}

.send-btn {
  background: linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%);
  border: none;
  transition: all 0.25s ease;
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #0284c7 0%, #4f46e5 100%);
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.45);
}

.send-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.examples-section {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.example-tag {
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.45);
  color: #e2e8f0;
  transition: all 0.25s;
}

.example-tag:hover {
  background: rgba(14, 165, 233, 0.35);
  border-color: rgba(56, 189, 248, 0.6);
  color: #fff;
}

/* ==================== 作品展示区域（通栏全屏） ==================== */
.section {
  width: 100%;
  padding: 48px 24px;
  /* py-12 px-6 */
}

.works-section {
  background: #f4f6fb;
}

.featured-section {
  background: #fff;
  padding-bottom: 64px;
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
  transition:
    transform 0.3s,
    box-shadow 0.3s;
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

.app-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.app-actions .ant-btn {
  flex: 1;
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
    padding: 48px 16px 40px;
  }

  .main-title {
    font-size: 30px;
  }

  .sub-title {
    font-size: 16px;
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
