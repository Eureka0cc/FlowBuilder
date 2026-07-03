<template>
  <!-- 全局顶部导航栏，固定在页面顶部 -->
  <a-layout-header class="header">
    <div class="header-content">
      <!-- 左侧 Logo 区域 -->
      <div class="logo-section">
        <img src="@/assets/shark.svg" alt="Logo" class="logo" />
        <span class="title">Eureka Flow Builder</span>
      </div>

      <!-- 中间导航菜单 - 固定宽度，防止切换时抖动 -->
      <div class="menu-wrapper">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          class="menu"
          @click="handleMenuClick"
        />
      </div>

      <!-- 右侧用户区域：已登录显示头像和用户名，未登录显示登录按钮 -->
      <div class="user-section">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space>
              <UserAvatar
                :user-avatar="loginUserStore.loginUser.userAvatar"
                :user-id="loginUserStore.loginUser.id"
                size="medium"
              />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goToProfile">
                  <UserOutlined />
                  个人中心
                </a-menu-item>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <a-button type="primary" href="/user/login">登录</a-button>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { logout } from '@/api/userController'
import UserAvatar from './UserAvatar.vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()

// 当前高亮的菜单项 key
const selectedKeys = ref<string[]>([route.path])

// 页面加载时根据当前路由设置选中状态
onMounted(() => {
  selectedKeys.value = [route.path]
})

// 路由变化时同步菜单高亮状态
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 顶部导航菜单项配置 - 固定三个菜单项，管理员菜单通过disabled控制显示
const originItems: MenuProps['items'] = [
  {
    key: '/',
    label: '首页',
    title: '首页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
    disabled: computed(() => {
      const loginUser = loginUserStore.loginUser
      return !loginUser || loginUser.userRole !== 'admin'
    }).value,
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
    disabled: computed(() => {
      const loginUser = loginUserStore.loginUser
      return !loginUser || loginUser.userRole !== 'admin'
    }).value,
  },
  {
    key: '/admin/chatHistoryManage',
    label: '对话管理',
    title: '对话管理',
    disabled: computed(() => {
      const loginUser = loginUserStore.loginUser
      return !loginUser || loginUser.userRole !== 'admin'
    }).value,
  },
]

// 展示在菜单的路由数组 - 不再过滤，保持固定数量
const menuItems = computed<MenuProps['items']>(() => {
  return originItems.map((item: any) => {
    // 动态计算disabled状态
    if (item.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      item.disabled = !loginUser || loginUser.userRole !== 'admin'
    }
    return item
  })
})

// 菜单点击回调：更新选中状态并跳转路由
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 仅对内部路径做 SPA 跳转，外链直接由浏览器处理
  if (key.startsWith('/')) {
    router.push(key)
  }
}

/**
 * 跳转到个人中心
 */
const goToProfile = () => {
  router.push('/user/profile')
}

/**
 * 退出登录
 */
const doLogout = async () => {
  const res = await logout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* 顶栏容器：固定定位，防止抖动 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 64px;
  line-height: 64px;
}

/* 内容居中，最大宽度 1200px，三栏 flex 布局 - 使用固定比例 */
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
}

/* Logo 区域：固定宽度，不允许收缩 */
.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  width: 200px;
}

.logo {
  height: 40px;
  width: 40px;
}

/* 标题使用品牌蓝色 */
.title {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
  white-space: nowrap;
}

/* 菜单包装器 - 固定宽度，防止抖动 */
.menu-wrapper {
  flex: 0 0 auto;
  width: 380px;
  display: flex;
  justify-content: center;
}

/* 菜单样式 */
.menu {
  line-height: 64px;
  background: transparent;
  border-bottom: none;
  width: 100%;
}

/* 菜单项基础样式 - 无下划线 */
:deep(.menu .ant-menu-item) {
  transition: all 0.3s ease-in-out;
  border-bottom: none !important;
  margin: 0 8px !important;
  padding: 0 16px !important;
  position: relative;
}

/* 选中状态的下划线 - 只有选中的才有 */
:deep(.menu .ant-menu-item-selected) {
  color: #1890ff !important;
  font-weight: 500;
}

/* 使用伪元素实现下划线，只有选中项显示 */
:deep(.menu .ant-menu-item-selected::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #1890ff;
  transition: all 0.3s ease-in-out;
}

/* 悬停状态 */
:deep(.menu .ant-menu-item:hover:not(.ant-menu-item-selected)) {
  color: #1890ff !important;
}

/* 禁用项样式 - 完全隐藏 */
:deep(.menu .ant-menu-item-disabled) {
  display: none !important;
}

/* 用户区域：固定宽度，不允许收缩 */
.user-section {
  flex-shrink: 0;
  width: 200px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

/* ===== 响应式适配 ===== */

/* 平板 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .logo-section {
    width: auto;
  }

  .title {
    font-size: 16px;
  }

  .menu-wrapper {
    width: 220px;
  }

  :deep(.menu .ant-menu-item) {
    margin: 0 4px !important;
    padding: 0 12px !important;
  }

  .user-section {
    width: auto;
  }

  .logo {
    height: 32px;
    width: 32px;
  }
}

/* 手机 */
@media (max-width: 576px) {
  .title {
    display: none;
  }

  .menu-wrapper {
    width: auto;
    flex: 1;
  }
}
</style>
