<template>
  <main class="workspace-page">
    <div class="page-header">
      <div>
        <h1>团队站点</h1>
        <p>{{ workspace.currentTeam?.name || '请选择团队' }} · 团队成员共享的站点</p>
      </div>
      <a-button type="primary" :disabled="!workspace.canEdit" @click="showCreate = !showCreate">
        创建站点
      </a-button>
    </div>

    <a-card v-if="showCreate" class="create-card">
      <a-textarea
        v-model:value="prompt"
        placeholder="描述要创建的网站，例如：创建一个现代风格的团队产品官网"
        :auto-size="{ minRows: 2, maxRows: 5 }"
      />
      <div class="create-actions">
        <a-button @click="router.push('/')">去首页创建</a-button>
        <a-button type="primary" :loading="creating" :disabled="!prompt.trim()" @click="createSite">
          立即创建
        </a-button>
      </div>
    </a-card>

    <a-input-search
      v-model:value="searchName"
      class="search"
      placeholder="搜索团队站点"
      allow-clear
      @search="search"
    />
    <a-spin :spinning="loading">
      <a-row v-if="sites.length" :gutter="[16, 16]">
        <a-col v-for="app in sites" :key="app.id" :xs="24" :sm="12" :md="8" :lg="6">
          <AppCard
            :app="app"
            show-create-time
            @click="goToChat"
            @view-chat="goToChat"
            @view-work="openWork"
          />
        </a-col>
      </a-row>
      <a-empty v-else :description="workspace.currentTeamId ? '暂无团队站点' : '请先选择团队'" />
    </a-spin>
    <a-pagination
      v-if="total > pageSize"
      v-model:current="pageNum"
      class="pagination"
      :total="total"
      :page-size="pageSize"
      :show-size-changer="false"
      @change="load"
    />
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import AppCard from '@/components/AppCard.vue'
import { addApp } from '@/api/appController'
import { listTeamSites } from '@/api/teamController'
import { useWorkspaceStore } from '@/stores/workspace'
import { getDeployUrl } from '@/config/env'

const router = useRouter()
const workspace = useWorkspaceStore()
const sites = ref<API.AppVO[]>([])
const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const prompt = ref('')
const searchName = ref('')
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)

const load = async () => {
  if (!workspace.currentTeamId) {
    sites.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await listTeamSites({
      teamId: workspace.currentTeamId,
      appName: searchName.value || undefined,
      pageNum: pageNum.value,
      pageSize,
    })
    if (res.data.code !== 0) throw new Error(res.data.message || '加载站点失败')
    sites.value = res.data.data?.records ?? []
    total.value = res.data.data?.total ?? res.data.data?.totalRow ?? sites.value.length
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载站点失败')
  } finally {
    loading.value = false
  }
}
const search = () => {
  pageNum.value = 1
  void load()
}
const createSite = async () => {
  creating.value = true
  try {
    const res = await addApp({ initPrompt: prompt.value.trim(), teamId: workspace.currentTeamId })
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '创建失败')
    const id = res.data.data
    if (!id) throw new Error('创建成功但未返回应用 ID')
    await router.push(`/app/chat/${id}`)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    creating.value = false
  }
}
const goToChat = (app: API.AppVO) => app.id && router.push(`/app/chat/${app.id}`)
const openWork = (app: API.AppVO) => {
  if (app.deployKey) window.open(getDeployUrl(app.deployKey), '_blank')
}

onMounted(async () => {
  try {
    await workspace.fetchTeams()
    await load()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载团队失败')
  }
})
watch(
  () => workspace.currentTeamId,
  () => {
    pageNum.value = 1
    void load()
  },
)
</script>

<style scoped>
.workspace-page {
  max-width: 1200px;
  min-height: 70vh;
  margin: 0 auto;
  padding: 36px 24px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.page-header p {
  color: #777;
}
.create-card {
  margin-top: 20px;
}
.create-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
.search {
  max-width: 360px;
  margin: 24px 0;
}
.pagination {
  margin-top: 24px;
  text-align: center;
}
@media (max-width: 576px) {
  .workspace-page {
    padding: 24px 16px;
  }
}
</style>
