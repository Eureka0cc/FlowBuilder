/**
 * 环境变量配置
 */
import { CodeGenTypeEnum } from '@/constants/codeGenType.ts'

// 应用部署域名（如 https://myapp.com，不含末尾斜杠）
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://localhost:8123/api/deploy'

// API 基础地址（浏览器环境默认走同源代理，开发时与 iframe 预览同域）
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  (typeof window !== 'undefined' ? '/api' : 'http://localhost:8123/api')

// 静态资源地址
export const STATIC_BASE_URL = `${API_BASE_URL}/static`

/**
 * 获取部署应用的完整 URL
 * @param deployKey 部署标识
 */
export const getDeployUrl = (deployKey: string): string => {
  return `${DEPLOY_DOMAIN}/${deployKey}/`
}

/**
 * 获取静态资源预览 URL（用于 iframe 嵌入）
 * 必须使用与前端同源的相对路径，否则无法向 iframe 注入可视化编辑脚本
 * @param codeGenType 代码生成类型
 * @param appId 应用ID
 */
export const getStaticPreviewUrl = (codeGenType: string, appId: string): string => {
  const baseUrl =
    typeof window !== 'undefined'
      ? `/api/static/${codeGenType}_${appId}/`
      : `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
  if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
    return `${baseUrl}dist/index.html`
  }
  return `${baseUrl}index.html`
}
