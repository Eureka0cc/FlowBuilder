import request from '@/request'
import type {
  ApiResponse,
  ReplaceImageRequest,
  Team,
  TeamAsset,
  TeamAssetPage,
  TeamAssetPageRequest,
  TeamAssetUpdateRequest,
  TeamMember,
  TeamSitePageRequest,
} from '@/types/team'

type AppPage = {
  records?: API.AppVO[]
  total?: number
  totalRow?: number
  pageNum?: number
  pageNumber?: number
  pageSize?: number
}

export function listMyTeams() {
  return request<ApiResponse<Team[]>>('/team/my', { method: 'GET' })
}

export function addTeam(name: string) {
  return request<ApiResponse<string | Team>>('/team/create', {
    method: 'POST',
    data: { name },
  })
}

export function listTeamMembers(teamId: string) {
  return request<ApiResponse<TeamMember[]>>(`/team/${teamId}/members`, { method: 'GET' })
}

export function uploadTeamAsset(
  teamId: string,
  file: File,
  name: string,
  tags: string[] = [],
) {
  const form = new FormData()
  form.append('teamId', teamId)
  form.append('file', file)
  form.append('name', name)
  form.append('tags', tags.join(','))
  return request<ApiResponse<TeamAsset>>('/team/asset/upload', {
    method: 'POST',
    data: form,
  })
}

export function listTeamAssets(body: TeamAssetPageRequest) {
  return request<ApiResponse<TeamAssetPage>>('/team/asset/list/page', {
    method: 'POST',
    data: {
      teamId: body.teamId,
      searchText: body.name,
      tag: body.tags?.join(','),
      pageNum: body.pageNum,
      pageSize: body.pageSize,
    },
  })
}

export function updateTeamAsset(body: TeamAssetUpdateRequest) {
  return request<ApiResponse<boolean | TeamAsset>>('/team/asset/update', {
    method: 'POST',
    data: {
      id: body.id,
      name: body.name,
      tags: body.tags?.join(','),
    },
  })
}

export function deleteTeamAsset(id: string, teamId?: string) {
  return request<ApiResponse<boolean>>('/team/asset/delete', {
    method: 'POST',
    data: { id, teamId },
  })
}

export function listTeamSites(body: TeamSitePageRequest) {
  return request<ApiResponse<AppPage>>('/app/team/list/page/vo', {
    method: 'POST',
    data: body,
  })
}

export function replaceAppImage(body: ReplaceImageRequest) {
  return request<ApiResponse<{ replacementCount: number; affectedPaths: string[] }>>(
    '/app/replace-image',
    {
    method: 'POST',
    data: body,
    },
  )
}
