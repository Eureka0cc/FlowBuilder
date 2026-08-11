import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { listMyTeams } from '@/api/teamController'
import type { Team } from '@/types/team'

const CURRENT_TEAM_KEY = 'eureka-current-team-id'

export const useWorkspaceStore = defineStore('workspace', () => {
  const teams = ref<Team[]>([])
  const currentTeamId = ref(localStorage.getItem(CURRENT_TEAM_KEY) || '')
  const loading = ref(false)
  const loaded = ref(false)

  const currentTeam = computed(
    () => teams.value.find((team) => String(team.id) === currentTeamId.value) ?? null,
  )
  const role = computed(
    () =>
      currentTeam.value?.currentUserRole ??
      currentTeam.value?.role ??
      currentTeam.value?.memberRole ??
      'viewer',
  )
  const canEdit = computed(
    () =>
      currentTeam.value?.canEdit ??
      ['owner', 'editor'].includes(role.value.toLowerCase()),
  )

  function selectTeam(teamId: string) {
    currentTeamId.value = teamId
    if (teamId) localStorage.setItem(CURRENT_TEAM_KEY, teamId)
    else localStorage.removeItem(CURRENT_TEAM_KEY)
  }

  async function fetchTeams(force = false) {
    if ((loaded.value && !force) || loading.value) return
    loading.value = true
    try {
      const res = await listMyTeams()
      if (res.data.code === 0 && Array.isArray(res.data.data)) {
        teams.value = res.data.data
        const selectionExists = teams.value.some(
          (team) => String(team.id) === currentTeamId.value,
        )
        if (!selectionExists) selectTeam(teams.value[0] ? String(teams.value[0].id) : '')
        loaded.value = true
      } else {
        throw new Error(res.data.message || '加载团队失败')
      }
    } finally {
      loading.value = false
    }
  }

  return {
    teams,
    currentTeamId,
    currentTeam,
    role,
    canEdit,
    loading,
    loaded,
    selectTeam,
    fetchTeams,
  }
})
