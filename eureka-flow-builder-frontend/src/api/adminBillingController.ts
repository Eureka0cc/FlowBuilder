// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 管理员充值 POST /admin/billing/recharge */
export async function adminRecharge(body: API.AdminCreditRechargeRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBigDecimal>('/admin/billing/recharge', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员调账 POST /admin/billing/adjust */
export async function adminAdjust(body: API.AdminCreditAdjustRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBigDecimal>('/admin/billing/adjust', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员分配套餐 POST /admin/billing/plan/assign */
export async function adminAssignPlan(body: API.AdminAssignPlanRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/admin/billing/plan/assign', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 查询用户账户 GET /admin/billing/account */
export async function adminGetBillingAccount(
  params: { userId: string | number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBillingAccountVO>('/admin/billing/account', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
