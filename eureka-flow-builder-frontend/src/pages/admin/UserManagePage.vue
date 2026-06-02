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
              <span v-else class="text-gray-400">-</span>
            </template>
            <template v-else-if="column.dataIndex === 'userRole'">
              <a-tag v-if="record.userRole === 'admin'" color="green">管理员</a-tag>
              <a-tag v-else color="blue">普通用户</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button danger size="small" @click="doDelete(record.id)">删除</a-button>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { SmileOutlined } from '@ant-design/icons-vue'
import { reactive, ref, onMounted, computed, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deleteUser, listUserVoByPage } from '@/api/userController'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 180,
    fixed: 'left',
    ellipsis: true,
    customRender: ({ text }: { text: number }) => {
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
    title: '创建时间',
    dataIndex: 'createTime',
    width: 170,
    align: 'center',
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
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
const doDelete = async (id?: number) => {
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

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
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
