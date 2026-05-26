<template>
  <a-layout-header class="header">
    <div class="header-content">
      <div class="logo-section">
        <img src="@/assets/shark.svg" alt="Logo" class="logo" />
        <span class="title">Eureka Flow Builder</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="menu"
        @click="handleMenuClick"
      />
      <div class="user-section">
        <a-button type="primary">登录</a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'

const router = useRouter()

// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])

// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 菜单配置
const menuItems: MenuProps['items'] = [
  {
    key: '/',
    label: '首页',
  },
  {
    key: '/about',
    label: '关于',
  },
]

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 64px;
  line-height: 64px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.logo {
  height: 40px;
  width: 40px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-bottom: none;
  line-height: 64px;
  margin: 0 24px;
  min-width: 0;
}

.user-section {
  flex-shrink: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .title {
    font-size: 16px;
  }

  .menu {
    margin: 0 12px;
  }

  .logo {
    height: 32px;
    width: 32px;
  }
}

@media (max-width: 576px) {
  .title {
    display: none;
  }

  .menu {
    margin: 0 8px;
  }
}
</style>
