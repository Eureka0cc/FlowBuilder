import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteTeamAsset,
  listTeamAssets,
  updateTeamAsset,
  uploadTeamAsset,
} from '@/api/teamController'
import type { TeamAsset, TeamAssetUpdateRequest } from '@/types/team'

export const useTeamAssetStore = defineStore('teamAsset', () => {
  const records = ref<TeamAsset[]>([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(12)
  const name = ref('')
  const tags = ref<string[]>([])
  const loading = ref(false)
  const uploading = ref(false)

  async function fetchAssets(teamId: string) {
    if (!teamId) {
      records.value = []
      total.value = 0
      return
    }
    loading.value = true
    try {
      const res = await listTeamAssets({
        teamId,
        name: name.value || undefined,
        tags: tags.value.length ? tags.value : undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      })
      if (res.data.code !== 0) throw new Error(res.data.message || '加载资产失败')
      records.value = res.data.data?.records ?? []
      total.value =
        res.data.data?.total ??
        (res.data.data as { totalRow?: number } | undefined)?.totalRow ??
        records.value.length
    } finally {
      loading.value = false
    }
  }

  async function upload(teamId: string, file: File, assetName?: string, assetTags: string[] = []) {
    uploading.value = true
    try {
      const res = await uploadTeamAsset(
        teamId,
        file,
        assetName?.trim() || file.name.replace(/\.[^.]+$/, ''),
        assetTags,
      )
      if (res.data.code !== 0 || !res.data.data) {
        throw new Error(res.data.message || '上传资产失败')
      }
      await fetchAssets(teamId)
      return res.data.data
    } finally {
      uploading.value = false
    }
  }

  async function update(payload: TeamAssetUpdateRequest) {
    const res = await updateTeamAsset(payload)
    if (res.data.code !== 0) throw new Error(res.data.message || '更新资产失败')
    const record = records.value.find((item) => item.id === payload.id)
    if (record) Object.assign(record, payload)
  }

  async function remove(teamId: string, id: string) {
    const res = await deleteTeamAsset(id, teamId)
    if (res.data.code !== 0) throw new Error(res.data.message || '删除资产失败')
    if (records.value.length === 1 && pageNum.value > 1) pageNum.value--
    await fetchAssets(teamId)
  }

  function resetSearch() {
    pageNum.value = 1
    name.value = ''
    tags.value = []
  }

  return {
    records,
    total,
    pageNum,
    pageSize,
    name,
    tags,
    loading,
    uploading,
    fetchAssets,
    upload,
    update,
    remove,
    resetSearch,
  }
})
