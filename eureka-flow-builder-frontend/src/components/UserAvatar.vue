<template>
  <div
    class="user-avatar"
    :class="{ 'is-default': !userAvatar }"
    :style="avatarStyle"
  >
    <img
      v-if="userAvatar"
      :src="userAvatar"
      class="avatar-img"
      :alt="altText"
    />
    <img
      v-else
      src="/default_avatar.svg"
      class="avatar-img default"
      :alt="altText"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 用户头像URL */
  userAvatar?: string
  /** 用户ID（用于生成默认头像背景色） */
  userId?: string
  /** 头像尺寸 */
  size?: number | 'small' | 'medium' | 'large'
  /** 替代文本 */
  alt?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 32,
  alt: '头像',
})

// 预定义的背景色列表（排除黑白）
const avatarColors = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8',
  '#F7DC6F', '#BB8FCE', '#85C1E2', '#F8B739', '#52C41A',
  '#FA8C16', '#EB2F96', '#722ED1', '#13C2C2', '#1890FF',
  '#FF4D4F', '#FAAD14', '#FF7A45', '#36CFC9', '#FF7875',
  '#FFC069', '#95DE64', '#5CDBD3', '#69C0FF', '#B37FEB'
]

// 根据用户ID生成固定的背景色
const getAvatarBgColor = (userId?: string): string => {
  if (!userId) return avatarColors[0]
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  const index = Math.abs(hash) % avatarColors.length
  return avatarColors[index]
}

// 计算尺寸
const sizeValue = computed(() => {
  if (typeof props.size === 'number') return props.size
  const sizeMap = {
    small: 24,
    medium: 32,
    large: 48
  }
  return sizeMap[props.size]
})

// 头像样式
const avatarStyle = computed(() => {
  const size = sizeValue.value
  const bgColor = !props.userAvatar ? getAvatarBgColor(props.userId) : 'transparent'

  return {
    width: `${size}px`,
    height: `${size}px`,
    backgroundColor: bgColor,
    borderRadius: '50%',
  }
})

// 替代文本
const altText = computed(() => props.alt || '头像')
</script>

<style scoped>
.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-img.default {
  object-fit: contain;
  padding: 15%;
}
</style>
