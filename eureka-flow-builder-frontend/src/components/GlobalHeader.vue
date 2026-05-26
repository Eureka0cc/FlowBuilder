<template>
  <!-- 全局顶部导航栏，固定在页面顶部 -->
  <a-layout-header class="header">
    <div class="header-content">
      <!-- 左侧 Logo 区域 -->
      <div class="logo-section">
        <img src="@/assets/shark.svg" alt="Logo" class="logo" />
        <span class="title">Eureka Flow Builder</span>
      </div>

      <!-- 中间导航菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="menu"
        @click="handleMenuClick"
      />

      <!-- 右侧用户区域：已登录显示头像和用户名，未登录显示登录按钮 -->
      <div class="user-section">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space>
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { logout } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 当前高亮的菜单项 key，默认选中首页
const selectedKeys = ref<string[]>(['/'])

// 路由变化时同步菜单高亮状态，确保刷新或直接访问子页面时菜单正确选中
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 顶部导航菜单项配置，新增页面时在此添加即可
const originItems: MenuProps['items'] = [
  {
    key: '/',
    label: '首页',
    title: '首页',
  },
  {
    key: '/admin/userMange',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
  },
]
// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}
// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))
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
/* 顶栏容器：白色背景 + 底部阴影 */
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 64px;
  line-height: 64px;
}

/* 内容居中，最大宽度 1200px，三栏 flex 布局 */
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
}

/* Logo 区域：图标 + 标题，不允许收缩 */
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

/* 标题使用品牌蓝色 */
.title {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
  white-space: nowrap;
}

/* 菜单占据剩余空间，去掉默认底部边框 */
.menu {
  flex: 1;
  border-bottom: none;
  line-height: 64px;
  margin: 0 24px;
  min-width: 0;
}

/* 用户区域不允许收缩 */
.user-section {
  flex-shrink: 0;
}

/* ===== 响应式适配 ===== */

/* 平板：缩小间距和 Logo 尺寸 */
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

/* 手机：隐藏标题文字，菜单进一步压缩 */
@media (max-width: 576px) {
  .title {
    display: none;
  }

  .menu {
    margin: 0 8px;
  }
}
</style>
