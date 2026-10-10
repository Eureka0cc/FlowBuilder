<template>
  <!-- 1. 最外层通栏背景（白色，占满全屏） -->
  <div class="user-manage-wrapper">
    <!-- 2. 安全宽度容器（限制最大宽度，水平居中，提供顶部和左右间距） -->
    <div class="user-manage-container">
      <!-- 3. 搜索表单区域 -->
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch" class="search-form">
          <a-form-item label="账号" class="form-item">
            <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" class="search-input" />
          </a-form-item>
          <a-form-item label="用户名" class="form-item">
            <a-input v-model:value="searchParams.userName" placeholder="输入用户名" class="search-input" />
          </a-form-item>
          <a-form-item class="form-item">
            <a-button type="primary" html-type="submit">搜索</a-button>
          </a-form-item>
        </a-form>
      </div>

      <!-- 4. 表格区域 -->
      <div class="table-section">
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          :loading="loading"
          @change="doTableChange"
          class="user-table"
        >
          <template #headerCell="{ column }">
            <template v-if="column.key === 'name'">
              <span>
                <smile-outlined />
                Name
              </span>
            </template>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'userAvatar'">
              <a-image v-if="record.userAvatar" :src="record.userAvatar" :width="80" />
              <div v-else class="default-avatar-cell">
                <div class="default-avatar-wrapper" :style="{ backgroundColor: getAvatarBgColor(record.id) }">
                  <img src="/default_avatar.svg" class="default-avatar-img" alt="默认头像" />
                </div>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'userRole'">
              <a-tag v-if="record.userRole === 'admin'" color="green">管理员</a-tag>
              <a-tag v-else color="blue">普通用户</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'planCode'">
              <a-tag>{{ planLabel(record.planCode) }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'creditDisplay'">
              {{ record.creditDisplay ?? 0 }}
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button size="small" @click="openRecharge(record)">充值</a-button>
                <a-button size="small" @click="openAssignPlan(record)">套餐</a-button>
                <a-button danger size="small" @click="doDelete(record.id)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <a-modal v-model:open="rechargeVisible" title="积分充值" @ok="submitRecharge" :confirm-loading="billingSubmitting">
      <a-form layout="vertical">
        <a-form-item label="用户">
          <a-input :value="billingTarget?.userAccount" disabled />
        </a-form-item>
        <a-form-item label="充值金额（元，将按 1 元 = 100 积分入账）" required>
          <a-input-number v-model:value="rechargeAmount" :min="0.01" :step="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="rechargeRemark" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="planVisible" title="分配套餐" @ok="submitAssignPlan" :confirm-loading="billingSubmitting">
      <a-form layout="vertical">
        <a-form-item label="用户">
          <a-input :value="billingTarget?.userAccount" disabled />
        </a-form-item>
        <a-form-item label="套餐" required>
          <a-select v-model:value="assignPlanCode" style="width: 100%">
            <a-select-option value="free">免费版</a-select-option>
            <a-select-option value="pro">专业版</a-select-option>
            <a-select-option value="enterprise">企业版</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="立即重置月额度">
          <a-switch v-model:checked="resetMonthlyQuota" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { SmileOutlined } from '@ant-design/icons-vue'
import { reactive, ref, onMounted, computed, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deleteUser, listUserVoByPage } from '@/api/userController'
import { adminAssignPlan, adminRecharge } from '@/api/adminBillingController'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 180,
    fixed: 'left',
    ellipsis: true,
    customRender: ({ text }: { text: string }) => {
      return h('span', { class: 'id-cell' }, String(text))
    },
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    ellipsis: true,
    width: 140,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    ellipsis: true,
    width: 120,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 100,
    align: 'center',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    ellipsis: true,
    width: 200,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    width: 100,
    align: 'center',
  },
  {
    title: '套餐',
    dataIndex: 'planCode',
    width: 100,
    align: 'center',
  },
  {
    title: '积分',
    dataIndex: 'creditDisplay',
    width: 100,
    align: 'center',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 170,
    align: 'center',
  },
  {
    title: '操作',
    key: 'action',
    width: 220,
    align: 'center',
    fixed: 'right',
  },
]

// 数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } finally {
    loading.value = false
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 获取数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id?: string) => {
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: '删除后将无法恢复，是否确认删除此用户？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteUser({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        // 刷新数据
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

const planLabel = (planCode?: string) => {
  if (planCode === 'pro') return '专业版'
  if (planCode === 'enterprise') return '企业版'
  return '免费版'
}

const billingTarget = ref<API.UserVO | null>(null)
const billingSubmitting = ref(false)
const rechargeVisible = ref(false)
const planVisible = ref(false)
const rechargeAmount = ref<number>(1)
const rechargeRemark = ref('')
const assignPlanCode = ref('pro')
const resetMonthlyQuota = ref(true)

const openRecharge = (record: API.UserVO) => {
  billingTarget.value = record
  rechargeAmount.value = 1
  rechargeRemark.value = ''
  rechargeVisible.value = true
}

const openAssignPlan = (record: API.UserVO) => {
  billingTarget.value = record
  assignPlanCode.value = record.planCode || 'free'
  resetMonthlyQuota.value = true
  planVisible.value = true
}

const submitRecharge = async () => {
  if (!billingTarget.value?.id || !rechargeAmount.value || rechargeAmount.value <= 0) {
    message.warning('请输入有效充值金额')
    return
  }
  billingSubmitting.value = true
  try {
    const res = await adminRecharge({
      userId: billingTarget.value.id,
      amount: rechargeAmount.value,
      remark: rechargeRemark.value,
    })
    if (res.data.code === 0) {
      message.success(`充值成功，当前积分 ${res.data.data}`)
      rechargeVisible.value = false
      fetchData()
    } else {
      message.error(res.data.message || '充值失败')
    }
  } catch {
    message.error('充值失败')
  } finally {
    billingSubmitting.value = false
  }
}

const submitAssignPlan = async () => {
  if (!billingTarget.value?.id || !assignPlanCode.value) {
    message.warning('请选择套餐')
    return
  }
  billingSubmitting.value = true
  try {
    const res = await adminAssignPlan({
      userId: billingTarget.value.id,
      planCode: assignPlanCode.value,
      resetMonthlyQuota: resetMonthlyQuota.value,
    })
    if (res.data.code === 0) {
      message.success('套餐已更新')
      planVisible.value = false
      fetchData()
    } else {
      message.error(res.data.message || '更新套餐失败')
    }
  } catch {
    message.error('更新套餐失败')
  } finally {
    billingSubmitting.value = false
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 预定义的背景色列表（排除黑白）
const avatarColors = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8',
  '#F7DC6F', '#BB8FCE', '#85C1E2', '#F8B739', '#52C41A',
  '#FA8C16', '#EB2F96', '#722ED1', '#13C2C2', '#1890FF',
  '#FF4D4F', '#52C41A', '#FAAD14', '#EB2F96', '#722ED1',
  '#13C2C2', '#1890FF', '#FF7A45', '#36CFC9', '#FF7875',
  '#FFC069', '#95DE64', '#5CDBD3', '#69C0FF', '#B37FEB'
]

// 根据用户ID生成固定的背景色
const getAvatarBgColor = (userId?: string) => {
  if (!userId) return avatarColors[0]
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  const index = Math.abs(hash) % avatarColors.length
  return avatarColors[index]
}
</script>

<style scoped>
/* ==================== 外层通栏容器 ==================== */
.user-manage-wrapper {
  width: 100%;
  min-height: calc(100vh - 64px - 72px);
  background: #fff;
  /* 纯白色背景 */
}

/* ==================== 安全宽度容器 ==================== */
.user-manage-container {
  max-width: 1440px;
  /* 限制最大宽度 */
  margin: 0 auto;
  /* 水平居中 */
  padding: 32px 24px 48px;
  /* pt-8 px-6 pb-12 */
}

@media (min-width: 768px) {
  .user-manage-container {
    padding: 32px 32px 48px;
    /* md:px-8 */
  }
}

@media (min-width: 1024px) {
  .user-manage-container {
    padding: 32px 48px 48px;
    /* lg:px-12 */
  }
}

/* ==================== 搜索表单区域 ==================== */
.search-section {
  margin-bottom: 24px;
  /* 搜索表单与表格之间的间距 */
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  /* 表单项之间的间距 */
  align-items: flex-end;
}

.form-item {
  margin-bottom: 0;
  /* 移除表单默认底部间距 */
}

.search-input {
  width: 200px;
}

/* ==================== 表格区域 ==================== */
.table-section {
  width: 100%;
  overflow-x: auto;
  /* 小屏幕横向滚动 */
}

.user-table {
  width: 100%;
}

/* 表格样式优化 */
/* ID列样式 */
:deep(.id-cell) {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #666;
}

/* 表格单元格通用样式 */
:deep(.user-table .ant-table-cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 确保表头也有ellipsis */
:deep(.user-table .ant-table-thead > tr > th) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.user-table .ant-table-thead > tr > th) {
  font-weight: 600;
  background: #fafafa;
}

/* 默认头像样式 */
.default-avatar-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.default-avatar-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.default-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 768px) {
  .user-manage-container {
    padding: 24px 16px 32px;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  :deep(.user-table .ant-table-cell) {
    padding: 12px 8px;
  }
}
</style>
