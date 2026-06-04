<template>
  <div id="appEditPage">
    <a-card :title="isAdmin ? '编辑应用信息（管理员）' : '编辑应用信息'" :loading="loading">
      <a-form
        :model="formState"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
        @finish="handleSubmit"
      >
        <a-form-item label="应用ID">
          <a-input v-model:value="formState.id" disabled />
        </a-form-item>

        <a-form-item
          label="应用名称"
          name="appName"
          :rules="[{ required: true, message: '请输入应用名称' }]"
        >
          <a-input
            v-model:value="formState.appName"
            placeholder="请输入应用名称"
            show-count
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="封面图片">
          <a-input
            v-model:value="formState.cover"
            placeholder="请输入封面图片URL"
          />
          <div v-if="formState.cover" class="cover-preview">
            <a-image :src="formState.cover" :width="200" />
          </div>
        </a-form-item>

        <a-form-item label="初始提示词">
          <a-textarea
            v-model:value="appInfo.initPrompt"
            :rows="4"
            disabled
          />
          <span class="form-hint">提示词在创建应用时已确定，不可修改</span>
        </a-form-item>

        <a-form-item label="生成类型">
          <a-input :value="getCodeGenTypeText(appInfo.codeGenType)" disabled />
        </a-form-item>

        <a-form-item label="部署状态">
          <a-tag v-if="appInfo.deployKey" color="green">已部署</a-tag>
          <a-tag v-else>未部署</a-tag>
        </a-form-item>

        <!-- 仅管理员可见的字段 -->
        <template v-if="isAdmin">
          <a-divider>管理员设置</a-divider>

          <a-form-item label="优先级">
            <a-input-number
              v-model:value="formState.priority"
              :min="0"
              :max="99"
              style="width: 150px"
            />
            <span class="form-hint">设置为 99 时将显示为精选应用</span>
          </a-form-item>

          <a-form-item label="创建用户">
            <div v-if="appInfo.user" class="user-info">
              <a-avatar v-if="appInfo.user.userAvatar" :src="appInfo.user.userAvatar" />
              <a-avatar v-else>
                <user-outlined />
              </a-avatar>
              <span>{{ appInfo.user.userName || appInfo.user.userAccount }}</span>
              <a-tag>ID: {{ appInfo.userId }}</a-tag>
            </div>
            <span v-else>用户ID: {{ appInfo.userId }}</span>
          </a-form-item>

          <a-form-item label="创建时间">
            <span>{{ dayjs(appInfo.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
          </a-form-item>

          <a-form-item label="更新时间">
            <span>{{ dayjs(appInfo.updateTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
          </a-form-item>
        </template>

        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">
              保存
            </a-button>
            <a-button @click="goBack">返回</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  updateApp,
  getAppVoByIdByAdmin,
  updateAppByAdmin,
} from '@/api/appController'
import { getCodeGenTypeText } from '@/constants/codeGenType'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用ID
const appId = computed(() => route.params.id as string)

// 是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

// 应用信息
const appInfo = ref<API.AppVO>({})

// 表单状态
const formState = ref<API.AppUpdateRequest & { priority?: number; cover?: string }>({
  id: '',
  appName: '',
  cover: '',
  priority: 0,
})

// 加载和提交状态
const loading = ref(false)
const submitting = ref(false)

// 加载应用信息
const loadAppInfo = async () => {
  loading.value = true
  try {
    // 管理员使用管理员接口，普通用户使用普通接口
    const api = isAdmin.value ? getAppVoByIdByAdmin : getAppVoById
    const res = await api({ id: appId.value })

    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 检查权限：普通用户只能编辑自己的应用
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此应用')
        router.push('/')
        return
      }

      // 初始化表单
      formState.value = {
        id: appInfo.value.id,
        appName: appInfo.value.appName || '',
        cover: appInfo.value.cover || '',
        priority: appInfo.value.priority || 0,
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      // 管理员更新
      res = await updateAppByAdmin({
        id: formState.value.id,
        appName: formState.value.appName,
        cover: formState.value.cover,
        priority: formState.value.priority,
      })
    } else {
      // 普通用户更新
      res = await updateApp({
        id: formState.value.id,
        appName: formState.value.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('保存成功')
      goBack()
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

// 返回
const goBack = () => {
  // 管理员返回应用管理页，普通用户返回首页
  if (isAdmin.value) {
    router.push('/admin/appManage')
  } else {
    router.push('/')
  }
}

// 页面加载
onMounted(() => {
  if (!appId.value) {
    message.error('应用ID不能为空')
    router.push('/')
    return
  }
  loadAppInfo()
})
</script>

<style scoped>
#appEditPage {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.cover-preview {
  margin-top: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.form-hint {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
