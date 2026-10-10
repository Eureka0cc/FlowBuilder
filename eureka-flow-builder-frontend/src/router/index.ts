import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import DashboardPage from '@/pages/admin/DashboardPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import ChatHistoryManagePage from '@/pages/admin/ChatHistoryManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import PricingPage from '@/pages/PricingPage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: '主页',
          component: HomePage,
        },
        {
          path: 'user/login',
          name: '用户登录',
          component: UserLoginPage,
        },
        {
          path: 'user/register',
          name: '用户注册',
          component: UserRegisterPage,
        },
        {
          path: 'app/chat/:id',
          name: '应用对话',
          component: AppChatPage,
          // 对话页是整屏工作区（高度按 100vh 计算），不展示页脚
          meta: { hideFooter: true },
        },
        {
          path: 'app/edit/:id',
          name: '编辑应用',
          component: AppEditPage,
        },
        {
          path: 'user/profile',
          name: '个人中心',
          component: UserProfilePage,
        },
        {
          path: 'pricing',
          name: '定价',
          component: PricingPage,
        },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: '数据总览',
          component: DashboardPage,
          meta: { title: '数据总览' },
        },
        {
          path: 'userManage',
          name: '用户管理',
          component: UserManagePage,
          meta: { title: '用户管理' },
        },
        {
          path: 'appManage',
          name: '应用管理',
          component: AppManagePage,
          meta: { title: '应用管理' },
        },
        {
          path: 'chatHistoryManage',
          name: '对话管理',
          component: ChatHistoryManagePage,
          meta: { title: '对话管理' },
        },
      ],
    },
  ],
})

export default router
