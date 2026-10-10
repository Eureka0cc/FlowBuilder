/**
 * 站点级品牌与合规信息
 *
 * 统一在这里维护：顶栏、页脚等位置都从这里取值，
 * 避免品牌名、备案号散落在多个组件里导致不一致。
 */

/** 品牌名（顶栏 Logo 旁名称、页脚品牌名） */
export const SITE_NAME = 'SeekWeb AI'

/** 主标语（首页 Hero、页脚品牌栏） */
export const SITE_SLOGAN = '一句话轻松创建网站应用'

/** 页脚品牌简介 */
export const SITE_DESCRIPTION =
  '面向开发者与创作者的 AI 应用生成平台：用自然语言描述需求，即可生成可预览、可下载、可部署的网站应用。'

/** 版权起始年份（页脚版权行显示起始年 ~ 当前年） */
export const SITE_COPYRIGHT_START_YEAR = 2025

/**
 * ICP 备案号 —— 部署到中国大陆服务器时必须填写
 * TODO: 备案通过后填入，例如 '京ICP备2025000000号-1'；留空则页脚不渲染备案号
 */
export const SITE_ICP = ''

/** 联系邮箱 —— 留空则页脚不渲染 */
export const SITE_CONTACT_EMAIL = ''

/** 原创项目作者（外部链接） */
export const SITE_AUTHOR_NAME = '程序员鱼皮'
export const SITE_AUTHOR_URL = 'https://www.codefather.cn'
