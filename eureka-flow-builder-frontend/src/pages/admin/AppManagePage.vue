<template>
  <div id="appManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用ID">
        <a-input v-model:value="searchParams.id" placeholder="输入应用ID" style="width: 150px" />
      </a-form-item>
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="用户ID">
        <a-input v-model:value="searchParams.userId" placeholder="输入用户ID" style="width: 150px" />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="searchParams.codeGenType"
          placeholder="选择类型"
          style="width: 150px"
          allowClear
        >
          <a-select-option
            v-for="option in CODE_GEN_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
        <a-button style="margin-left: 8px" @click="doReset">重置</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :loading="loading"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          <a-tag :color="getCodeGenTypeColor(record.codeGenType)">
            {{ getCodeGenTypeText(record.codeGenType) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'deployKey'">
          <a-tag v-if="record.deployKey" color="green">已部署</a-tag>
          <a-tag v-else>未部署</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag v-if="record.priority === 99" color="gold">精选</a-tag>
          <span v-else>{{ record.priority }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          <div v-if="record.user" class="user-info">
            <UserAvatar
              :user-avatar="record.user.userAvatar"
              :user-id="record.user.id"
              size="small"
            />
            <span>{{ record.user.userName || record.user.userAccount }}</span>
          </div>
          <div v-else class="user-info">
            <UserAvatar
              :user-id="record.userId"
              size="small"
            />
            <span>用户{{ record.userId }}</span>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="doEdit(record)">编辑</a-button>
            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
            <a-button v-if="record.priority !== 99" type="link" @click="doFeature(record)">
              精选
            </a-button>
            <a-button v-else type="link" @click="doUnfeature(record)">取消精选</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import {
  listAppVoByPageByAdmin,
  deleteAppByAdmin,
  updateAppByAdmin,
} from '@/api/appController'
import UserAvatar from '@/components/UserAvatar.vue'
import {
  CODE_GEN_TYPE_OPTIONS,
  getCodeGenTypeText,
  getCodeGenTypeColor,
} from '@/constants/codeGenType'

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    ellipsis: true,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 100,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 120,
  },
  {
    title: '部署状态',
    dataIndex: 'deployKey',
    width: 100,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 100,
  },
  {
    title: '创建用户',
    dataIndex: 'user',
    width: 150,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right',
  },
]

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
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

// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 重置
const doReset = () => {
  searchParams.pageNum = 1
  searchParams.id = undefined
  searchParams.appName = undefined
  searchParams.userId = undefined
  searchParams.codeGenType = undefined
  fetchData()
}

// 删除数据
const doDelete = async (id?: string) => {
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: '删除后将无法恢复，是否确认删除此应用？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteAppByAdmin({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

// 编辑
const doEdit = (record: API.AppVO) => {
  router.push(`/app/edit/${record.id}`)
}

// 设为精选
const doFeature = async (record: API.AppVO) => {
  Modal.confirm({
    title: '确认精选',
    content: `确定将应用 "${record.appName || '未命名'}" 设为精选吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await updateAppByAdmin({
        id: record.id,
        appName: record.appName,
        cover: record.cover,
        priority: 99,
      })
      if (res.data.code === 0) {
        message.success('设为精选成功')
        fetchData()
      } else {
        message.error('操作失败，' + res.data.message)
      }
    },
  })
}

// 取消精选
const doUnfeature = async (record: API.AppVO) => {
  Modal.confirm({
    title: '确认取消精选',
    content: `确定取消应用 "${record.appName || '未命名'}" 的精选状态吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await updateAppByAdmin({
        id: record.id,
        appName: record.appName,
        cover: record.cover,
        priority: 0,
      })
      if (res.data.code === 0) {
        message.success('取消精选成功')
        fetchData()
      } else {
        message.error('操作失败，' + res.data.message)
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
#appManagePage {
  padding: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
