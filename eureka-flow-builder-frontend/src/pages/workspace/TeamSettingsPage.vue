<template>
  <main class="workspace-page">
    <div class="page-header">
      <div>
        <h1>团队设置</h1>
        <p>{{ workspace.currentTeam?.name || '请选择团队' }} · 成员与权限（只读）</p>
      </div>
      <a-space>
        <a-tag color="blue">{{ roleLabel(workspace.role) }}</a-tag>
        <a-button type="primary" @click="createVisible = true">新建团队</a-button>
      </a-space>
    </div>
    <a-card title="团队成员" class="members-card">
      <a-table
        :columns="columns"
        :data-source="members"
        :loading="loading"
        :pagination="false"
        :row-key="(record: TeamMember) => record.id || record.userId || record.userAccount || ''"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'member'">
            <a-space>
              <a-avatar :src="record.userAvatar">{{ record.userName?.slice(0, 1) }}</a-avatar>
              <div>
                <div>{{ record.userName || record.userAccount || '未命名成员' }}</div>
                <small>{{ record.userAccount }}</small>
              </div>
            </a-space>
          </template>
          <template v-else-if="column.key === 'role'">
            <a-tag>{{ roleLabel(record.role) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'joinedTime'">
            {{ record.joinedTime || record.joinTime || '-' }}
          </template>
        </template>
      </a-table>
      <a-empty v-if="!loading && !members.length" description="暂无成员信息" />
    </a-card>
    <a-modal
      v-model:open="createVisible"
      title="新建团队"
      ok-text="创建"
      :confirm-loading="creating"
      @ok="createTeam"
    >
      <a-input
        v-model:value="teamName"
        :maxlength="80"
        placeholder="请输入团队名称"
        @press-enter="createTeam"
      />
    </a-modal>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { addTeam, listTeamMembers } from '@/api/teamController'
import { useWorkspaceStore } from '@/stores/workspace'
import type { TeamMember, TeamRole } from '@/types/team'

const workspace = useWorkspaceStore()
const members = ref<TeamMember[]>([])
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const teamName = ref('')
const columns = [
  { title: '成员', key: 'member' },
  { title: '角色', key: 'role', width: 160 },
  { title: '加入时间', key: 'joinedTime', width: 220 },
]
const roleLabel = (role?: TeamRole) =>
  ({ owner: '所有者', editor: '编辑者', viewer: '访客' })[
    role?.toLowerCase() || ''
  ] ||
  role ||
  '成员'

const load = async () => {
  if (!workspace.currentTeamId) {
    members.value = []
    return
  }
  loading.value = true
  try {
    const res = await listTeamMembers(workspace.currentTeamId)
    if (res.data.code !== 0) throw new Error(res.data.message || '加载成员失败')
    members.value = res.data.data ?? []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载成员失败')
  } finally {
    loading.value = false
  }
}

const createTeam = async () => {
  const name = teamName.value.trim()
  if (!name) {
    message.warning('请输入团队名称')
    return
  }
  creating.value = true
  try {
    const res = await addTeam(name)
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '创建团队失败')
    }
    await workspace.fetchTeams(true)
    const createdId =
      typeof res.data.data === 'object' ? String(res.data.data.id) : String(res.data.data)
    workspace.selectTeam(createdId)
    teamName.value = ''
    createVisible.value = false
    message.success('团队创建成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '创建团队失败')
  } finally {
    creating.value = false
  }
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
  () => void load(),
)
</script>

<style scoped>
.workspace-page {
  max-width: 1000px;
  min-height: 70vh;
  margin: 0 auto;
  padding: 36px 24px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header p,
small {
  color: #888;
}
.members-card {
  margin-top: 24px;
}
@media (max-width: 576px) {
  .workspace-page {
    padding: 24px 12px;
  }
}
</style>
