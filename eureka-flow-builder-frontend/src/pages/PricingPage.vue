<template>
  <div class="pricing-page">
    <div class="pricing-hero">
      <h1>选择适合你的套餐</h1>
      <p>积分用于 AI 代码生成消耗。当前支持管理员充值开通，支付网关即将接入。</p>
    </div>

    <a-spin :spinning="loading">
      <div class="plan-grid">
        <div
          v-for="plan in plans"
          :key="plan.planCode"
          class="plan-card"
          :class="{ featured: plan.planCode === 'pro' }"
        >
          <div class="plan-name">{{ plan.name }}</div>
          <div class="plan-desc">{{ plan.description }}</div>
          <div class="plan-quota">
            <strong>{{ plan.monthlyQuotaDisplay ?? 0 }}</strong>
            <span>积分 / 月</span>
          </div>
          <ul>
            <li>应用上限：{{ plan.appLimit == null ? '不限' : plan.appLimit }}</li>
            <li>对话限流：{{ plan.chatRate }} 次 / 分钟</li>
            <li v-if="plan.signupGiftDisplay">注册赠送：{{ plan.signupGiftDisplay }} 积分</li>
          </ul>
          <a-button
            :type="plan.planCode === 'pro' ? 'primary' : 'default'"
            block
            @click="contactAdmin(plan)"
          >
            联系管理员开通
          </a-button>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listBillingPlans } from '@/api/billingController'

const loading = ref(false)
const plans = ref<API.PlanOptionVO[]>([])

const loadPlans = async () => {
  loading.value = true
  try {
    const res = await listBillingPlans()
    if (res.data.code === 0) {
      plans.value = res.data.data ?? []
    } else {
      message.error(res.data.message || '加载套餐失败')
    }
  } catch {
    message.error('加载套餐失败')
  } finally {
    loading.value = false
  }
}

const contactAdmin = (plan: API.PlanOptionVO) => {
  message.info(`请联系管理员开通「${plan.name}」并充值积分`)
}

onMounted(loadPlans)
</script>

<style scoped>
.pricing-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 48px 24px 80px;
}

.pricing-hero {
  text-align: center;
  margin-bottom: 40px;
}

.pricing-hero h1 {
  font-size: 36px;
  margin: 0 0 12px;
  color: #0f172a;
}

.pricing-hero p {
  margin: 0;
  color: #64748b;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.plan-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px 24px;
}

.plan-card.featured {
  border-color: #2563eb;
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.12);
}

.plan-name {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.plan-desc {
  margin-top: 8px;
  color: #64748b;
  min-height: 44px;
}

.plan-quota {
  margin: 20px 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.plan-quota strong {
  font-size: 40px;
  color: #0f172a;
}

.plan-quota span {
  color: #94a3b8;
}

.plan-card ul {
  margin: 0 0 24px;
  padding-left: 18px;
  color: #475569;
}

.plan-card li + li {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}
</style>
