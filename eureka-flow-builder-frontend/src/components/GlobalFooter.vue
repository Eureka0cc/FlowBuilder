<template>
  <!--
    全站页脚：品牌栏 + 链接栏 + 底部版权/备案行
    链接分组在下方 linkGroups 中维护；后续新增「帮助文档」「更新日志」等页面时，
    建好路由后在这里加一条即可（external 为 true 时按外链渲染）。
  -->
  <a-layout-footer class="global-footer">
    <div class="footer-inner">
      <div class="footer-main">
        <!-- 品牌栏 -->
        <div class="brand-col">
          <div class="brand">
            <img src="@/assets/shark.svg" :alt="SITE_NAME" class="brand-logo" />
            <span class="brand-name">{{ SITE_NAME }}</span>
          </div>
          <p class="brand-slogan">{{ SITE_SLOGAN }}</p>
          <p class="brand-desc">{{ SITE_DESCRIPTION }}</p>
        </div>

        <!-- 链接栏 -->
        <div v-for="group in linkGroups" :key="group.title" class="link-col">
          <h3 class="col-title">{{ group.title }}</h3>
          <ul class="col-list">
            <li v-for="link in group.links" :key="link.label">
              <a
                v-if="link.external"
                :href="link.href"
                target="_blank"
                rel="noopener noreferrer"
                class="footer-link"
              >
                {{ link.label }}
              </a>
              <RouterLink v-else :to="link.href" class="footer-link">
                {{ link.label }}
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>

      <!-- 底部版权 / 备案行 -->
      <div class="footer-bottom">
        <span class="copyright">© {{ copyrightYears }} {{ SITE_NAME }}. All rights reserved.</span>
        <span class="bottom-meta">
          <a
            v-if="SITE_ICP"
            href="https://beian.miit.gov.cn"
            target="_blank"
            rel="noopener noreferrer"
            class="footer-link"
          >
            {{ SITE_ICP }}
          </a>
          <a v-if="SITE_CONTACT_EMAIL" :href="`mailto:${SITE_CONTACT_EMAIL}`" class="footer-link">
            {{ SITE_CONTACT_EMAIL }}
          </a>
          <span class="credit">
            原创项目 by
            <a
              :href="SITE_AUTHOR_URL"
              target="_blank"
              rel="noopener noreferrer"
              class="footer-link"
            >
              {{ SITE_AUTHOR_NAME }}
            </a>
          </span>
        </span>
      </div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  SITE_AUTHOR_NAME,
  SITE_AUTHOR_URL,
  SITE_CONTACT_EMAIL,
  SITE_COPYRIGHT_START_YEAR,
  SITE_DESCRIPTION,
  SITE_ICP,
  SITE_NAME,
  SITE_SLOGAN,
} from '@/constants/site'

interface FooterLink {
  /** 站内路由路径或完整外链地址 */
  href: string
  label: string
  /** true 时按外链渲染（新标签页打开） */
  external?: boolean
}

interface FooterLinkGroup {
  title: string
  links: FooterLink[]
}

// 页脚链接分组：只放已存在的路由，避免出现死链
const linkGroups: FooterLinkGroup[] = [
  {
    title: '产品',
    links: [
      { label: '首页', href: '/' },
      { label: '定价与套餐', href: '/pricing' },
    ],
  },
  {
    title: '快速开始',
    links: [
      { label: '登录', href: '/user/login' },
      { label: '注册账号', href: '/user/register' },
      { label: '个人中心', href: '/user/profile' },
    ],
  },
  {
    title: '关于',
    links: [{ label: '作者主页', href: SITE_AUTHOR_URL, external: true }],
  },
]

// 版权年份：起始年与当前年相同时只显示一个年份
const copyrightYears = computed(() => {
  const currentYear = new Date().getFullYear()
  return SITE_COPYRIGHT_START_YEAR >= currentYear
    ? String(currentYear)
    : `${SITE_COPYRIGHT_START_YEAR}-${currentYear}`
})
</script>

<style scoped>
/* 深色页脚：与首页 Hero 的深色渐变首尾呼应 */
.global-footer {
  padding: 0;
  background: #0b1220;
  border-top: 1px solid rgba(56, 189, 248, 0.18);
  color: rgba(255, 255, 255, 0.65);
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 48px 24px 0;
}

.footer-main {
  display: grid;
  grid-template-columns: 1.8fr 1fr 1fr 1fr;
  gap: 32px;
}

/* ===== 品牌栏 ===== */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 32px;
  height: 32px;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.brand-slogan {
  margin: 16px 0 0;
  font-size: 14px;
  color: #38bdf8;
}

.brand-desc {
  max-width: 380px;
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.55);
}

/* ===== 链接栏 ===== */
.col-title {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.col-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 13px;
}

.footer-link {
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  transition: color 0.2s ease-in-out;
}

.footer-link:hover {
  color: #38bdf8;
}

/* ===== 底部版权行 ===== */
.footer-bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 40px;
  padding: 20px 0 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

.bottom-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .footer-main {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 28px;
  }

  /* 品牌栏独占一行 */
  .brand-col {
    grid-column: 1 / -1;
  }
}

@media (max-width: 576px) {
  .footer-inner {
    padding: 32px 16px 0;
  }

  .footer-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
    margin-top: 28px;
  }
}
</style>
