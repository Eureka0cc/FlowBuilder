// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取滑动验证码 GET /user/captcha */
export async function getCaptcha(options?: { [key: string]: any }) {
  return request<API.BaseResponseCaptchaVO>('/user/captcha', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 验证滑动验证码 POST /user/captcha/verify */
export async function verifyCaptcha(
  body: API.CaptchaVerifyRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/user/captcha/verify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}