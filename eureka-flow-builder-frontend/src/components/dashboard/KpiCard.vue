<template>
  <article class="kpi-card">
    <div class="kpi-heading">
      <span>{{ title }}</span>
      <span class="kpi-icon" :style="{ color, backgroundColor: `${color}14` }">
        <component :is="icon" />
      </span>
    </div>
    <a-skeleton v-if="loading" active :paragraph="false" />
    <template v-else>
      <div class="kpi-value">{{ value }}</div>
      <div class="kpi-foot">
        <span>{{ hint }}</span>
        <span v-if="delta" class="kpi-delta">{{ delta }}</span>
      </div>
    </template>
  </article>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  title: string
  value: string
  hint: string
  delta?: string
  color: string
  icon: Component
  loading?: boolean
}>()
</script>

<style scoped>
.kpi-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #e7ebf0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgb(16 24 40 / 7%);
}

.kpi-heading,
.kpi-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #667085;
  font-size: 13px;
}

.kpi-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 9px;
  font-size: 17px;
}

.kpi-value {
  overflow: hidden;
  margin: 14px 0 10px;
  color: #17212f;
  font-size: clamp(24px, 2vw, 30px);
  font-weight: 650;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kpi-delta {
  color: #067647;
  font-weight: 600;
}
</style>
