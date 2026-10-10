<template>
  <div class="chart-shell">
    <VChart v-if="!empty" class="chart" :option="option" autoresize />
    <a-empty v-else :description="emptyText" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import { Empty } from 'ant-design-vue'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
])

const props = withDefaults(
  defineProps<{
    option: EChartsOption
    empty?: boolean
    emptyText?: string
  }>(),
  {
    empty: false,
    emptyText: '暂无统计数据',
  },
)

const option = computed(() => props.option)
</script>

<style scoped>
.chart-shell {
  display: flex;
  min-height: 300px;
  align-items: center;
  justify-content: center;
}

.chart {
  width: 100%;
  height: 300px;
}
</style>
