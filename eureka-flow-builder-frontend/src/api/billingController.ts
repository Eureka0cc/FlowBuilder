// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取当前用户计费账户 GET /billing/account */
export async function getBillingAccount(options?: { [key: string]: any }) {
  return request<API.BaseResponseBillingAccountVO>('/billing/account', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 公开套餐列表 GET /billing/plans */
export async function listBillingPlans(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPlanOptionVO>('/billing/plans', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 当前用户积分流水 GET /billing/ledger/page */
export async function pageBillingLedger(
  params: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCreditLedgerVO>('/billing/ledger/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
