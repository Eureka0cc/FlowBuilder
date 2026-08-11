<template>
  <main class="workspace-page">
    <div class="page-header">
      <div>
        <h1>团队资产</h1>
        <p>{{ workspace.currentTeam?.name || '请选择团队' }} · 统一管理站点图片素材</p>
      </div>
      <a-button
        type="primary"
        :disabled="!workspace.currentTeamId || !workspace.canEdit"
        :loading="assets.uploading"
        @click="fileInput?.click()"
      >
        上传图片
      </a-button>
      <input
        ref="fileInput"
        hidden
        type="file"
        accept="image/jpeg,image/png,image/gif"
        @change="handleUpload"
      />
    </div>

    <a-input-search
      v-model:value="assets.name"
      class="search"
      placeholder="按名称搜索资产"
      allow-clear
      @search="search"
    />

    <a-spin :spinning="assets.loading">
      <div v-if="assets.records.length" class="asset-grid">
        <a-card v-for="asset in assets.records" :key="asset.id" hoverable class="asset-card">
          <template #cover>
            <a-image :src="asset.url" :alt="asset.name" class="asset-image" />
          </template>
          <a-card-meta :title="asset.name">
            <template #description>
              <span>{{ formatSize(asset.size) }}</span>
            </template>
          </a-card-meta>
          <template #actions>
            <a-button type="link" @click="previewUrl = asset.url">预览</a-button>
            <a-popconfirm
              title="确定删除这个资产？"
              ok-text="删除"
              :disabled="!workspace.canEdit"
              @confirm="remove(asset.id)"
            >
              <a-button type="link" danger :disabled="!workspace.canEdit">删除</a-button>
            </a-popconfirm>
          </template>
        </a-card>
      </div>
      <a-empty v-else :description="workspace.currentTeamId ? '暂无资产' : '请先选择团队'" />
    </a-spin>

    <a-pagination
      v-if="assets.total > assets.pageSize"
      v-model:current="assets.pageNum"
      class="pagination"
      :total="assets.total"
      :page-size="assets.pageSize"
      :show-size-changer="false"
      @change="load"
    />
    <a-modal v-model:open="previewVisible" title="资产预览" :footer="null" centered>
      <img :src="previewUrl" alt="资产预览" class="preview-image" />
    </a-modal>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useWorkspaceStore } from '@/stores/workspace'
import { useTeamAssetStore } from '@/stores/teamAsset'

const workspace = useWorkspaceStore()
const assets = useTeamAssetStore()
const fileInput = ref<HTMLInputElement>()
const previewUrl = ref('')
const previewVisible = computed({
  get: () => !!previewUrl.value,
  set: (value) => {
    if (!value) previewUrl.value = ''
  },
})

const load = async () => {
  try {
    await assets.fetchAssets(workspace.currentTeamId)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载资产失败')
  }
}
const search = () => {
  assets.pageNum = 1
  void load()
}
const handleUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
    return message.error('仅支持 JPEG、PNG、GIF 图片')
  }
  if (file.size > 10 * 1024 * 1024) return message.error('图片不能超过 10MB')
  try {
    await assets.upload(workspace.currentTeamId, file)
    message.success('上传成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '上传失败')
  }
}
const remove = async (id: string) => {
  try {
    await assets.remove(workspace.currentTeamId, id)
    message.success('删除成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}
const formatSize = (size?: number) => {
  if (!size) return '图片'
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
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
    assets.pageNum = 1
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
.page-header h1 {
  margin-bottom: 4px;
}
.page-header p {
  color: #777;
}
.search {
  max-width: 360px;
  margin: 24px 0;
}
.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}
.asset-card {
  overflow: hidden;
}
:deep(.asset-image) {
  width: 100%;
  height: 180px;
  object-fit: contain;
  background: #f5f5f5;
}
.pagination {
  margin-top: 24px;
  text-align: center;
}
.preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: auto;
}
@media (max-width: 576px) {
  .workspace-page {
    padding: 24px 16px;
  }
  .page-header {
    align-items: flex-start;
  }
}
</style>
