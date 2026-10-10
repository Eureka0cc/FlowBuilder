<template>
  <a-layout class="admin-layout">
    <a-layout-sider
      v-if="!isMobile"
      v-model:collapsed="collapsed"
      class="admin-sider"
      :width="236"
      :collapsed-width="72"
      collapsible
    >
      <button class="brand" type="button" @click="router.push('/admin/dashboard')">
        <span class="brand-mark">E</span>
        <span v-if="!collapsed" class="brand-copy">
          <strong>Eureka</strong>
          <small>ADMIN CONSOLE</small>
        </span>
      </button>
      <a-menu
        :selected-keys="[route.path]"
        theme="dark"
        mode="inline"
        :items="menuItems"
        @click="handleMenuClick"
      />
      <div v-if="!collapsed" class="environment">
        <span class="status-dot"></span>
        服务运行中
        <span>PROD</span>
      </div>
    </a-layout-sider>

    <a-drawer
      v-model:open="drawerOpen"
      placement="left"
      :closable="false"
      :width="260"
      class="admin-drawer"
      :body-style="{ padding: 0, background: '#111827' }"
    >
      <div class="drawer-content">
        <button class="brand" type="button" @click="router.push('/admin/dashboard')">
          <span class="brand-mark">E</span>
          <span class="brand-copy">
            <strong>Eureka</strong>
            <small>ADMIN CONSOLE</small>
          </span>
        </button>
        <a-menu
          :selected-keys="[route.path]"
          theme="dark"
          mode="inline"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </div>
    </a-drawer>

    <a-layout class="admin-main">
      <a-layout-header class="admin-header">
        <div class="header-left">
          <a-button v-if="isMobile" type="text" class="mobile-trigger" @click="drawerOpen = true">
            <MenuOutlined />
          </a-button>
          <a-breadcrumb>
            <a-breadcrumb-item>管理后台</a-breadcrumb-item>
            <a-breadcrumb-item>{{ route.meta.title ?? '数据总览' }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-actions">
          <a-button type="text" class="home-link" @click="router.push('/')">
            <HomeOutlined />
            <span>返回主页</span>
          </a-button>
          <span class="header-divider"></span>
          <a-dropdown>
            <button class="admin-profile" type="button">
              <UserAvatar
                :user-avatar="loginUserStore.loginUser.userAvatar"
                :user-id="loginUserStore.loginUser.id"
                :size="32"
              />
              <span class="admin-copy">
                <strong>{{ loginUserStore.loginUser.userName || '管理员' }}</strong>
                <small>系统管理员</small>
              </span>
              <DownOutlined />
            </button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="router.push('/user/profile')">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-item @click="doLogout"><LogoutOutlined /> 退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="admin-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { h, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  BarChartOutlined,
  DownOutlined,
  HomeOutlined,
  LogoutOutlined,
  MenuOutlined,
  MessageOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { logout } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const collapsed = ref(false)
const drawerOpen = ref(false)
const isMobile = ref(false)
let mediaQuery: MediaQueryList | undefined

const menuItems: MenuProps['items'] = [
  { key: '/admin/dashboard', icon: () => h(BarChartOutlined), label: '数据总览' },
  { key: '/admin/userManage', icon: () => h(TeamOutlined), label: '用户管理' },
  { key: '/admin/appManage', icon: () => h(AppstoreOutlined), label: '应用管理' },
  { key: '/admin/chatHistoryManage', icon: () => h(MessageOutlined), label: '对话管理' },
]

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  drawerOpen.value = false
  router.push(String(key))
}

const updateViewport = (event: MediaQueryListEvent | MediaQueryList) => {
  isMobile.value = event.matches
  if (!event.matches) drawerOpen.value = false
}

const doLogout = async () => {
  const response = await logout()
  if (response.data.code !== 0) {
    message.error(response.data.message || '退出登录失败')
    return
  }
  loginUserStore.setLoginUser({ userName: '未登录' })
  await router.push('/user/login')
}

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 900px)')
  updateViewport(mediaQuery)
  mediaQuery.addEventListener('change', updateViewport)
})

onBeforeUnmount(() => mediaQuery?.removeEventListener('change', updateViewport))
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f4f6f8;
}

.admin-sider {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: auto;
  background: #111827;
  box-shadow: 5px 0 20px rgb(15 23 42 / 8%);
}

.brand {
  display: flex;
  width: 100%;
  height: 72px;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border: 0;
  background: transparent;
  color: #fff;
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  border-radius: 10px;
  background: #1677ff;
  box-shadow: 0 6px 18px rgb(22 119 255 / 35%);
  font-family: Georgia, serif;
  font-size: 21px;
  font-weight: 700;
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-copy strong {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 18px;
  letter-spacing: 0.2px;
}

.brand-copy small {
  margin-top: 2px;
  color: #718096;
  font-size: 9px;
  letter-spacing: 1.7px;
}

:deep(.ant-menu-dark) {
  background: transparent;
}

:deep(.ant-menu-dark .ant-menu-item) {
  height: 44px;
  margin: 5px 10px;
  line-height: 44px;
}

:deep(.ant-menu-dark .ant-menu-item-selected) {
  background: #1677ff;
}

.environment {
  position: absolute;
  right: 18px;
  bottom: 56px;
  left: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #263244;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 11px;
}

.environment > span:last-child {
  margin-left: auto;
  color: #667085;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #32d583;
  box-shadow: 0 0 0 3px rgb(50 213 131 / 12%);
}

.admin-main {
  min-width: 0;
  background: #f4f6f8;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  height: 64px;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid #e5e9ef;
  background: rgb(255 255 255 / 94%);
  line-height: normal;
  backdrop-filter: blur(12px);
}

.header-left,
.header-actions,
.admin-profile {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 8px;
}

.header-actions {
  gap: 12px;
}

.home-link {
  color: #475467;
}

.header-divider {
  width: 1px;
  height: 24px;
  background: #e5e7eb;
}

.admin-profile {
  gap: 9px;
  padding: 4px;
  border: 0;
  background: transparent;
  color: #667085;
  cursor: pointer;
}

.admin-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.admin-copy strong {
  color: #1d2939;
  font-size: 13px;
  font-weight: 600;
}

.admin-copy small {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 10px;
}

.admin-content {
  min-width: 0;
}

.drawer-content {
  height: 100%;
  background: #111827;
}

@media (max-width: 900px) {
  .admin-header {
    padding: 0 16px;
  }

  .mobile-trigger {
    font-size: 18px;
  }
}

@media (max-width: 600px) {
  .admin-copy,
  .home-link span,
  .header-divider {
    display: none;
  }

  .admin-header {
    height: 58px;
  }

  :deep(.ant-breadcrumb li:first-child) {
    display: none;
  }
}
</style>
