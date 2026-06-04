/**
 * 代码生成类型枚举（对齐后端 CodeGenTypeEnum）
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.HTML]: {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
    color: 'blue',
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
    color: 'green',
  },
  [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 工程模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
    color: 'purple',
  },
} as const

/**
 * 代码生成类型选项（用于下拉选择）
 */
export const CODE_GEN_TYPE_OPTIONS = Object.values(CODE_GEN_TYPE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/**
 * 获取代码生成类型的显示文本
 */
export const getCodeGenTypeText = (type?: string): string => {
  if (!type) return '-'
  return CODE_GEN_TYPE_CONFIG[type as CodeGenTypeEnum]?.label ?? type
}

/**
 * 获取代码生成类型的标签颜色
 */
export const getCodeGenTypeColor = (type?: string): string => {
  if (!type) return 'default'
  return CODE_GEN_TYPE_CONFIG[type as CodeGenTypeEnum]?.color ?? 'default'
}
