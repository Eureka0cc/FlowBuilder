<template>
  <main class="dashboard-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">OPERATIONS OVERVIEW</p>
        <h1>数据总览</h1>
        <p class="page-description">监控平台增长、模型消耗与生成服务运行情况</p>
      </div>
      <div class="header-controls">
        <a-segmented v-model:value="days" :options="dayOptions" @change="fetchDashboard" />
        <a-button :loading="loading" @click="fetchDashboard">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </header>

    <a-alert
      v-if="errorMessage"
      class="error-alert"
      type="warning"
      show-icon
      closable
      :message="errorMessage"
      description="部分数据可能未更新，请检查服务状态后重试。"
      @close="errorMessage = ''"
    />

    <section class="kpi-grid" aria-label="核心指标">
      <KpiCard
        title="累计用户"
        :value="formatInteger(overview?.totalUsers)"
        hint="今日新增"
        :delta="formatDelta(overview?.todayNewUsers)"
        color="#1677ff"
        :icon="TeamOutlined"
        :loading="loading"
      />
      <KpiCard
        title="累计应用"
        :value="formatInteger(overview?.totalApps)"
        hint="今日新增"
        :delta="formatDelta(overview?.todayNewApps)"
        color="#7f56d9"
        :icon="AppstoreOutlined"
        :loading="loading"
      />
      <KpiCard
        title="今日对话"
        :value="formatInteger(overview?.todayConversations)"
        hint="实时业务调用"
        color="#f79009"
        :icon="MessageOutlined"
        :loading="loading"
      />
      <KpiCard
        title="部署成功率"
        :value="formatPercent(overview?.deploymentRate)"
        hint="应用构建部署"
        color="#12b76a"
        :icon="CloudServerOutlined"
        :loading="loading"
      />
      <KpiCard
        title="累计 Token"
        :value="formatCompact(overview?.totalTokens)"
        hint="今日消耗"
        :delta="formatCompact(overview?.todayTokens)"
        color="#0e9384"
        :icon="ThunderboltOutlined"
        :loading="loading"
      />
      <KpiCard
        title="累计预估费用"
        :value="formatCurrency(overview?.totalEstimatedCost, overview?.currency)"
        hint="今日费用"
        :delta="formatCurrency(overview?.todayEstimatedCost, overview?.currency)"
        color="#d92d20"
        :icon="DollarOutlined"
        :loading="loading"
      />
    </section>

    <section class="chart-grid">
      <article class="panel panel-wide">
        <div class="panel-heading">
          <div>
            <h2>运营趋势</h2>
            <p>新增用户、应用及对话量变化</p>
          </div>
          <span class="panel-range">近 {{ days }} 天</span>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 7 }" />
        <DashboardChart v-else :option="operationOption" :empty="trend.length === 0" />
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <h2>Token 消耗</h2>
            <p>输入与输出 Token 堆叠</p>
          </div>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 7 }" />
        <DashboardChart v-else :option="tokenOption" :empty="trend.length === 0" />
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <h2>费用趋势</h2>
            <p>模型调用预估成本</p>
          </div>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 7 }" />
        <DashboardChart v-else :option="costOption" :empty="trend.length === 0" />
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <h2>生成类型</h2>
            <p>各代码生成模式占比</p>
          </div>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 7 }" />
        <DashboardChart
          v-else
          :option="codeGenOption"
          :empty="distribution.codeGenTypes.length === 0"
        />
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <h2>模型使用</h2>
            <p>各模型调用量分布</p>
          </div>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 7 }" />
        <DashboardChart
          v-else
          :option="modelOption"
          :empty="distribution.modelUsages.length === 0"
        />
      </article>
    </section>

    <section class="bottom-grid">
      <article class="panel ranking-panel">
        <div class="panel-heading">
          <div>
            <h2>用户消耗排行</h2>
            <p>按 Token 使用量排序</p>
          </div>
          <span class="panel-range">TOP 10</span>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 8 }" />
        <a-empty
          v-else-if="topUsers.length === 0"
          description="暂无用户消耗数据"
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
        />
        <div v-else class="ranking-list">
          <div v-for="(user, index) in topUsers" :key="user.userId" class="ranking-row">
            <span class="rank" :class="{ podium: index < 3 }">{{ index + 1 }}</span>
            <UserAvatar
              :user-avatar="user.userAvatar"
              :user-id="user.userId"
              :size="34"
            />
            <div class="rank-user">
              <strong>{{ user.userName || `用户 ${user.userId}` }}</strong>
              <span>{{ formatInteger(user.callCount) }} 次调用</span>
            </div>
            <div class="rank-usage">
              <strong>{{ formatCompact(user.totalTokens) }}</strong>
              <span>{{ formatCurrency(user.estimatedCost, overview?.currency) }}</span>
            </div>
            <a-progress
              :percent="getRankPercent(user.totalTokens)"
              :show-info="false"
              size="small"
              stroke-color="#1677ff"
            />
          </div>
        </div>
      </article>

      <article class="panel recent-panel">
        <div class="panel-heading">
          <div>
            <h2>最近调用</h2>
            <p>模型服务实时调用记录</p>
          </div>
          <span class="live-badge"><i></i> LIVE</span>
        </div>
        <a-table
          row-key="id"
          size="middle"
          :columns="recentColumns"
          :data-source="recent.records"
          :loading="recentLoading"
          :scroll="{ x: 960 }"
          :pagination="recentPagination"
          @change="handleRecentPage"
        >
          <template #emptyText>
            <a-empty description="暂无调用记录" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'model'">
              <div class="model-cell">
                <strong>{{ record.modelName || '—' }}</strong>
                <span>{{ record.invocationType || record.codeGenType || '—' }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'tokens'">
              {{ formatInteger(record.totalTokens) }}
            </template>
            <template v-else-if="column.key === 'cost'">
              {{ formatCurrency(record.estimatedCost, record.currency) }}
            </template>
            <template v-else-if="column.key === 'duration'">
              {{ formatDuration(record.durationMs) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusMeta(record.status).color">
                {{ statusMeta(record.status).label }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'time'">
              {{ formatDateTime(record.createTime) }}
            </template>
          </template>
        </a-table>
      </article>
    </section>

    <footer class="dashboard-footer">
      <span><i></i> 数据服务连接正常</span>
      <span>最后刷新：{{ refreshedAt || '—' }}</span>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { Empty } from 'ant-design-vue'
import {
  AppstoreOutlined,
  CloudServerOutlined,
  DollarOutlined,
  MessageOutlined,
  ReloadOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import DashboardChart from '@/components/dashboard/DashboardChart.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import {
  getAdminDistribution,
  getAdminOverview,
  getAdminRecent,
  getAdminTopUsers,
  getAdminTrend,
  type AdminDistribution,
  type AdminOverview,
  type AdminTrendItem,
  type RecentInvocationPage,
  type TopUserItem,
} from '@/api/adminStatsController'

const dayOptions = [
  { label: '7 天', value: 7 },
  { label: '30 天', value: 30 },
  { label: '90 天', value: 90 },
]
const days = ref(30)
const loading = ref(false)
const recentLoading = ref(false)
const errorMessage = ref('')
const refreshedAt = ref('')
const overview = ref<AdminOverview>()
const trend = ref<AdminTrendItem[]>([])
const distribution = ref<AdminDistribution>({ codeGenTypes: [], modelUsages: [] })
const topUsers = ref<TopUserItem[]>([])
const recent = ref<RecentInvocationPage>({ records: [], totalRow: 0 })
const recentPage = ref(1)

const recentColumns = [
  { title: '模型 / 类型', key: 'model', width: 180 },
  { title: 'Token', key: 'tokens', width: 105, align: 'right' },
  { title: '预估费用', key: 'cost', width: 110, align: 'right' },
  { title: '耗时', key: 'duration', width: 90, align: 'right' },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '调用时间', key: 'time', width: 165 },
]

const chartBase = {
  textStyle: { color: '#667085', fontFamily: '"Segoe UI", sans-serif' },
  tooltip: {
    trigger: 'axis' as const,
    backgroundColor: 'rgba(17, 24, 39, .94)',
    borderWidth: 0,
    textStyle: { color: '#fff' },
  },
  grid: { top: 44, right: 18, bottom: 28, left: 54, containLabel: false },
  legend: { top: 2, right: 0, itemWidth: 9, itemHeight: 9, textStyle: { color: '#667085' } },
}

const axisStyle = {
  axisLine: { show: false },
  axisTick: { show: false },
  axisLabel: { color: '#98a2b3', fontSize: 11 },
  splitLine: { lineStyle: { color: '#eef1f5', type: 'dashed' as const } },
}

const operationOption = computed<EChartsOption>(() => ({
  ...chartBase,
  xAxis: {
    type: 'category',
    data: trend.value.map((item) => shortDate(item.statDate)),
    ...axisStyle,
    splitLine: { show: false },
  },
  yAxis: { type: 'value', ...axisStyle },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: 0.35,
      showSymbol: false,
      data: trend.value.map((item) => item.newUsers),
      lineStyle: { width: 2, color: '#1677ff' },
      itemStyle: { color: '#1677ff' },
      areaStyle: { color: 'rgba(22,119,255,.08)' },
    },
    {
      name: '新增应用',
      type: 'line',
      smooth: 0.35,
      showSymbol: false,
      data: trend.value.map((item) => item.newApps),
      lineStyle: { width: 2, color: '#7f56d9' },
      itemStyle: { color: '#7f56d9' },
    },
    {
      name: '对话量',
      type: 'bar',
      barMaxWidth: 12,
      data: trend.value.map((item) => item.conversations),
      itemStyle: { color: 'rgba(247,144,9,.42)', borderRadius: [3, 3, 0, 0] },
    },
  ],
}))

const tokenOption = computed<EChartsOption>(() => ({
  ...chartBase,
  xAxis: {
    type: 'category',
    data: trend.value.map((item) => shortDate(item.statDate)),
    ...axisStyle,
    splitLine: { show: false },
  },
  yAxis: { type: 'value', ...axisStyle },
  series: [
    {
      name: '输入 Token',
      type: 'bar',
      stack: 'token',
      barMaxWidth: 18,
      data: trend.value.map((item) => item.inputTokens),
      itemStyle: { color: '#1677ff' },
    },
    {
      name: '输出 Token',
      type: 'bar',
      stack: 'token',
      barMaxWidth: 18,
      data: trend.value.map((item) => item.outputTokens),
      itemStyle: { color: '#84adff', borderRadius: [3, 3, 0, 0] },
    },
  ],
}))

const costOption = computed<EChartsOption>(() => ({
  ...chartBase,
  xAxis: {
    type: 'category',
    data: trend.value.map((item) => shortDate(item.statDate)),
    ...axisStyle,
    splitLine: { show: false },
  },
  yAxis: { type: 'value', ...axisStyle },
  series: [
    {
      name: '预估费用',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trend.value.map((item) => item.estimatedCost),
      lineStyle: { width: 2, color: '#12b76a' },
      itemStyle: { color: '#12b76a' },
      areaStyle: { color: 'rgba(18,183,106,.11)' },
    },
  ],
}))

const donutOption = (data: { name: string; value: number }[], colors: string[]): EChartsOption => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} ({d}%)' },
  legend: {
    orient: 'vertical',
    right: 2,
    top: 'center',
    itemWidth: 9,
    itemHeight: 9,
    textStyle: { color: '#667085', fontSize: 11 },
  },
  color: colors,
  series: [
    {
      type: 'pie',
      radius: ['50%', '72%'],
      center: ['35%', '51%'],
      avoidLabelOverlap: true,
      label: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
      data,
    },
  ],
})

const codeGenOption = computed(() =>
  donutOption(distribution.value.codeGenTypes, ['#1677ff', '#84adff', '#7f56d9', '#f79009']),
)
const modelOption = computed(() =>
  donutOption(distribution.value.modelUsages, ['#0e9384', '#53b1a5', '#667085', '#fdb022']),
)

const recentPagination = computed(() => ({
  current: recentPage.value,
  pageSize: 8,
  total: recent.value.totalRow,
  showSizeChanger: false,
  size: 'small' as const,
  showTotal: (total: number) => `共 ${total} 条`,
}))

const fetchDashboard = async () => {
  loading.value = true
  recentLoading.value = true
  errorMessage.value = ''
  recentPage.value = 1
  const requests = await Promise.allSettled([
    getAdminOverview(days.value),
    getAdminTrend(days.value),
    getAdminDistribution(),
    getAdminTopUsers(days.value, 10),
    getAdminRecent(1, 8),
  ])

  const failed: string[] = []
  const assign = <T,>(index: number, label: string, setter: (value: T) => void) => {
    const result = requests[index]
    if (result?.status === 'fulfilled' && result.value.data.code === 0 && result.value.data.data) {
      setter(result.value.data.data as T)
    } else {
      failed.push(label)
    }
  }

  assign<AdminOverview>(0, '核心指标', (value) => (overview.value = value))
  assign<AdminTrendItem[]>(1, '趋势数据', (value) => (trend.value = value))
  assign<AdminDistribution>(2, '分布数据', (value) => (distribution.value = value))
  assign<TopUserItem[]>(3, '用户排行', (value) => (topUsers.value = value))
  assign<RecentInvocationPage>(4, '调用记录', (value) => (recent.value = value))

  if (failed.length) errorMessage.value = `${failed.join('、')}加载失败`
  refreshedAt.value = formatDateTime(new Date())
  loading.value = false
  recentLoading.value = false
}

const fetchRecent = async () => {
  recentLoading.value = true
  try {
    const response = await getAdminRecent(recentPage.value, 8)
    if (response.data.code === 0 && response.data.data) {
      recent.value = response.data.data
    } else {
      errorMessage.value = response.data.message || '调用记录加载失败'
    }
  } catch {
    errorMessage.value = '调用记录加载失败'
  } finally {
    recentLoading.value = false
  }
}

const handleRecentPage = (pagination: { current?: number }) => {
  recentPage.value = pagination.current ?? 1
  fetchRecent()
}

const formatInteger = (value?: number) =>
  typeof value === 'number' ? new Intl.NumberFormat('zh-CN').format(value) : '—'
const formatCompact = (value?: number) =>
  typeof value === 'number'
    ? new Intl.NumberFormat('zh-CN', { notation: 'compact', maximumFractionDigits: 1 }).format(value)
    : '—'
const formatDelta = (value?: number) => (typeof value === 'number' ? `+${formatInteger(value)}` : '')
const formatPercent = (value?: number) => {
  if (typeof value !== 'number') return '—'
  const normalized = value <= 1 ? value * 100 : value
  return `${normalized.toFixed(1)}%`
}
const formatCurrency = (value?: number, currency = 'CNY') => {
  if (typeof value !== 'number') return '—'
  try {
    return new Intl.NumberFormat('zh-CN', {
      style: 'currency',
      currency: currency || 'CNY',
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    }).format(value)
  } catch {
    return `${value.toFixed(4)} ${currency || ''}`.trim()
  }
}
const shortDate = (value: string) => {
  const parts = value?.split('-')
  return parts?.length === 3 ? `${parts[1]}/${parts[2]}` : value
}
const formatDuration = (value?: number) => {
  if (typeof value !== 'number') return '—'
  return value >= 1000 ? `${(value / 1000).toFixed(1)}s` : `${value}ms`
}
const formatDateTime = (value: string | Date) => {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}
const statusMeta = (status: string) => {
  const normalized = status?.toLowerCase()
  if (['success', 'succeeded', 'completed'].includes(normalized)) {
    return { color: 'success', label: '成功' }
  }
  if (['running', 'processing', 'pending'].includes(normalized)) {
    return { color: 'processing', label: '进行中' }
  }
  if (['failed', 'error'].includes(normalized)) return { color: 'error', label: '失败' }
  return { color: 'default', label: status || '未知' }
}
const getRankPercent = (tokens: number) => {
  const maximum = topUsers.value[0]?.totalTokens || 1
  return Math.max(3, Math.round((tokens / maximum) * 100))
}

onMounted(fetchDashboard)
</script>

<style scoped>
.dashboard-page {
  --ink: #17212f;
  width: 100%;
  max-width: 1680px;
  min-height: calc(100vh - 64px);
  margin: 0 auto;
  padding: 28px;
  color: var(--ink);
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.eyebrow {
  margin-bottom: 7px;
  color: #1677ff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.7px;
}

.page-header h1 {
  margin: 0;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 29px;
  font-weight: 600;
  letter-spacing: -0.5px;
}

.page-description,
.panel-heading p {
  margin: 5px 0 0;
  color: #7b8493;
  font-size: 13px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.error-alert {
  margin-bottom: 18px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.panel {
  min-width: 0;
  padding: 20px;
  border: 1px solid #e7ebf0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.panel-wide {
  grid-column: span 2;
}

.panel-heading {
  display: flex;
  min-height: 44px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-heading h2 {
  margin: 0;
  color: #253044;
  font-size: 15px;
  font-weight: 650;
}

.panel-range {
  padding: 4px 8px;
  border: 1px solid #eaecf0;
  border-radius: 6px;
  color: #667085;
  font-size: 10px;
  font-weight: 600;
}

.bottom-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.8fr) minmax(600px, 1.7fr);
  gap: 14px;
  margin-top: 14px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
}

.ranking-row {
  display: grid;
  grid-template-columns: 24px 34px minmax(80px, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 11px 0;
  border-bottom: 1px solid #f0f2f5;
}

.ranking-row:last-child {
  border-bottom: 0;
}

.rank {
  color: #98a2b3;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.rank.podium {
  color: #1677ff;
}

.rank-user,
.rank-usage,
.model-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.rank-user strong,
.model-cell strong {
  overflow: hidden;
  color: #344054;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-user span,
.rank-usage span,
.model-cell span {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 10px;
}

.rank-usage {
  align-items: flex-end;
}

.rank-usage strong {
  color: #344054;
  font-size: 12px;
}

.ranking-row :deep(.ant-progress) {
  grid-column: 3 / 5;
  margin: -5px 0 0;
  line-height: 1;
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #067647;
  font-size: 10px;
  font-weight: 700;
}

.live-badge i,
.dashboard-footer i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #12b76a;
}

.recent-panel :deep(.ant-table) {
  font-size: 12px;
}

.recent-panel :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #667085;
  font-size: 11px;
  font-weight: 600;
}

.dashboard-footer {
  display: flex;
  justify-content: space-between;
  padding: 18px 2px 0;
  color: #98a2b3;
  font-size: 11px;
}

.dashboard-footer span {
  display: flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 1450px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-page {
    min-height: calc(100vh - 58px);
    padding: 18px 14px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-controls {
    width: 100%;
    justify-content: space-between;
  }

  .kpi-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .panel-wide {
    grid-column: span 1;
  }

  .panel {
    padding: 16px;
  }

  .dashboard-footer {
    gap: 10px;
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
