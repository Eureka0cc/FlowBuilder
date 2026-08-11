<template>
  <a-modal
    :open="open"
    title="选择团队资产"
    width="860px"
    :footer="null"
    @cancel="emit('update:open', false)"
  >
    <div class="picker-toolbar">
      <a-input-search
        v-model:value="assetStore.name"
        placeholder="搜索资产名称"
        allow-clear
        @search="search"
      />
      <a-button v-if="canUpload" :loading="assetStore.uploading" @click="fileInput?.click()">
        上传图片
      </a-button>
      <input
        ref="fileInput"
        hidden
        type="file"
        accept="image/jpeg,image/png,image/gif"
        @change="handleFile"
      />
    </div>
    <a-spin :spinning="assetStore.loading">
      <div v-if="assetStore.records.length" class="asset-grid">
        <button
          v-for="asset in assetStore.records"
          :key="asset.id"
          type="button"
          class="asset-item"
          @click="selectAsset(asset)"
        >
          <img :src="asset.url" :alt="asset.name" />
          <span :title="asset.name">{{ asset.name }}</span>
        </button>
      </div>
      <a-empty v-else description="暂无团队资产" />
    </a-spin>
    <a-pagination
      v-if="assetStore.total > assetStore.pageSize"
      v-model:current="assetStore.pageNum"
      class="pagination"
      :total="assetStore.total"
      :page-size="assetStore.pageSize"
      :show-size-changer="false"
      @change="load"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useTeamAssetStore } from '@/stores/teamAsset'
import type { TeamAsset } from '@/types/team'

const props = withDefaults(
  defineProps<{ open: boolean; teamId?: string; canUpload?: boolean }>(),
  { teamId: '', canUpload: true },
)
const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'selected', asset: TeamAsset): void
}>()

const assetStore = useTeamAssetStore()
const fileInput = ref<HTMLInputElement>()

const load = async () => {
  if (!props.teamId) return
  try {
    await assetStore.fetchAssets(props.teamId)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载资产失败')
  }
}

const search = () => {
  assetStore.pageNum = 1
  void load()
}

const selectAsset = (asset: TeamAsset) => {
  emit('selected', asset)
  emit('update:open', false)
}

const handleFile = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
    message.error('仅支持 JPEG、PNG、GIF 图片')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    message.error('图片不能超过 10MB')
    return
  }
  try {
    const asset = await assetStore.upload(props.teamId, file)
    selectAsset(asset)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '上传失败')
  }
}

watch(
  () => [props.open, props.teamId] as const,
  ([open]) => {
    if (open) {
      assetStore.pageNum = 1
      void load()
    }
  },
)
</script>

<style scoped>
.picker-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  min-height: 220px;
}
.asset-item {
  padding: 8px;
  overflow: hidden;
  text-align: left;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
}
.asset-item:hover {
  border-color: #1677ff;
}
.asset-item img {
  width: 100%;
  height: 110px;
  object-fit: contain;
  background: #f5f5f5;
}
.asset-item span {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
