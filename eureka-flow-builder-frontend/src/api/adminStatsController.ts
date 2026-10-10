import request from '@/request'

export interface BaseResponse<T> {
  code: number
  data?: T
  message?: string
}

export interface AdminOverview {
  totalUsers: number
  todayNewUsers: number
  totalApps: number
  todayNewApps: number
  todayConversations: number
  deploymentRate: number
  totalTokens: number
  todayTokens: number
  totalEstimatedCost: number
  todayEstimatedCost: number
  currency: string
}

export interface AdminTrendItem {
  statDate: string
  newUsers: number
  newApps: number
  conversations: number
  inputTokens: number
  outputTokens: number
  estimatedCost: number
}

export interface DistributionItem {
  name: string
  value: number
}

export interface AdminDistribution {
  codeGenTypes: DistributionItem[]
  modelUsages: DistributionItem[]
}

export interface TopUserItem {
  userId: string
  userName: string
  userAvatar?: string
  totalTokens: number
  estimatedCost: number
  callCount: number
}

export type InvocationStatus = 'success' | 'failed' | 'running' | string

export interface RecentInvocationItem {
  id: string
  userId: string
  appId: string
  modelName: string
  codeGenType: string
  invocationType: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  estimatedCost: number
  currency: string
  status: InvocationStatus
  durationMs: number
  createTime: string
}

export interface RecentInvocationPage {
  records: RecentInvocationItem[]
  totalRow: number
}

export function getAdminOverview(days: number) {
  return request<BaseResponse<AdminOverview>>('/admin/stats/overview', {
    method: 'GET',
    params: { days },
  })
}

export function getAdminTrend(days: number) {
  return request<BaseResponse<AdminTrendItem[]>>('/admin/stats/trend', {
    method: 'GET',
    params: { days },
  })
}

export function getAdminDistribution() {
  return request<BaseResponse<AdminDistribution>>('/admin/stats/distribution', {
    method: 'GET',
  })
}

export function getAdminTopUsers(days: number, limit = 10) {
  return request<BaseResponse<TopUserItem[]>>('/admin/stats/top-users', {
    method: 'GET',
    params: { days, limit },
  })
}

export function getAdminRecent(pageNum = 1, pageSize = 8) {
  return request<BaseResponse<RecentInvocationPage>>('/admin/stats/recent', {
    method: 'GET',
    params: { pageNum, pageSize },
  })
}
