export type TeamRole = 'owner' | 'admin' | 'member' | 'viewer' | string

export interface Team {
  id: string
  name: string
  role?: TeamRole
  memberRole?: TeamRole
  currentUserRole?: TeamRole
  canEdit?: boolean
  createTime?: string
}

export interface TeamMember {
  id?: string
  userId?: string
  userName?: string
  userAccount?: string
  userAvatar?: string
  role?: TeamRole
  joinTime?: string
  joinedTime?: string
}

export interface TeamAsset {
  id: string
  teamId: string
  name: string
  url: string
  tags?: string[] | string
  mimeType?: string
  contentType?: string
  size?: number
  createTime?: string
  updateTime?: string
}

export interface TeamAssetPageRequest {
  teamId: string
  name?: string
  tags?: string[]
  pageNum: number
  pageSize: number
}

export interface TeamAssetPage {
  records: TeamAsset[]
  total: number
  pageNum?: number
  pageSize?: number
}

export interface TeamAssetUpdateRequest {
  id: string
  teamId?: string
  name?: string
  tags?: string[]
}

export interface TeamSitePageRequest {
  teamId: string
  appName?: string
  pageNum: number
  pageSize: number
}

export interface ReplaceImageRequest {
  appId: string
  assetId: string
  oldUrl?: string
  selector: string
  kind: 'img' | 'background'
}

export interface ApiResponse<T> {
  code?: number
  data?: T
  message?: string
}
