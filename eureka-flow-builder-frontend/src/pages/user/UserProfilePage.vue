<template>
  <!-- 个人中心页面 - 最外层白色通栏背景 -->
  <div class="profile-page-wrapper">
    <div class="profile-page-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理您的个人信息和头像</p>
      </div>

      <a-spin :spinning="pageLoading">
        <!-- 主体卡片 -->
        <div class="profile-card">
          <!-- ========== 头像区域 ========== -->
          <div class="avatar-section">
            <div class="avatar-upload-area" @click="triggerFileInput">
              <a-spin :spinning="avatarUploading">
                <!-- 已有头像时显示真实头像 -->
                <div
                  v-if="formState.userAvatar"
                  class="avatar-preview"
                  :style="{ backgroundImage: `url(${formState.userAvatar})` }"
                />
                <!-- 无头像时显示默认头像 -->
                <div v-else class="default-avatar-wrapper" :style="{ backgroundColor: avatarBgColor }">
                  <img src="/default_avatar.svg" class="default-avatar-img" alt="默认头像" />
                </div>
                <!-- 遮罩层：提示更换头像 -->
                <div class="avatar-overlay">
                  <CameraOutlined class="overlay-icon" />
                  <span class="overlay-text">更换头像</span>
                </div>
              </a-spin>
            </div>
            <p class="avatar-hint">点击更换头像，支持 PNG / JPEG / GIF / WebP，最大 10MB</p>
            <!-- 隐藏的文件选择器 -->
            <input
              ref="fileInputRef"
              type="file"
              accept="image/png,image/jpeg,image/gif,image/webp"
              style="display: none"
              @change="handleFileChange"
            />

            <!-- 头像裁剪对话框 -->
            <AvatarCropper
              v-model:visible="cropperVisible"
              :file="selectedFile"
              @crop="handleCropped"
              @cancel="resetFileInput"
            />
          </div>

          <!-- ========== 基本信息表单 ========== -->
          <a-form
            :model="formState"
            layout="vertical"
            class="profile-form"
          >
            <!-- 账号（只读） -->
            <a-form-item label="账号">
              <a-input
                :value="formState.userAccount"
                disabled
                class="readonly-input"
              />
            </a-form-item>

            <!-- 昵称（可编辑） -->
            <a-form-item label="昵称">
              <a-input
                v-model:value="formState.userName"
                :maxlength="20"
                placeholder="请输入昵称"
                show-count
              />
            </a-form-item>

            <!-- 简介（可编辑） -->
            <a-form-item label="简介">
              <a-textarea
                v-model:value="formState.userProfile"
                :maxlength="200"
                :rows="4"
                placeholder="介绍一下自己吧…"
                show-count
              />
            </a-form-item>

            <!-- 用户角色（只读，管理员才显示） -->
            <a-form-item v-if="formState.userRole === 'admin'" label="角色">
              <a-tag color="green">管理员</a-tag>
            </a-form-item>

            <!-- 注册时间（只读） -->
            <a-form-item label="注册时间">
              <a-input
                :value="dayjs(formState.createTime).format('YYYY-MM-DD HH:mm:ss')"
                disabled
                class="readonly-input"
              />
            </a-form-item>

            <!-- 套餐与积分 -->
            <a-form-item label="当前套餐">
              <a-space>
                <a-tag color="blue">{{ billingAccount?.planName || '免费版' }}</a-tag>
                <a-button type="link" @click="router.push('/pricing')">查看定价</a-button>
              </a-space>
            </a-form-item>
            <a-form-item label="积分余额">
              <a-input
                :value="`${billingAccount?.creditDisplay ?? 0} 积分`"
                disabled
                class="readonly-input"
              />
            </a-form-item>
            <a-form-item label="应用用量">
              <a-input
                :value="appUsageText"
                disabled
                class="readonly-input"
              />
            </a-form-item>

            <!-- 保存按钮 -->
            <a-form-item>
              <a-button
                type="primary"
                size="large"
                :loading="saving"
                class="save-button"
                @click="handleSave"
              >
                保存修改
              </a-button>
            </a-form-item>
          </a-form>

          <div class="ledger-section">
            <h2 class="ledger-title">积分流水</h2>
            <a-table
              :columns="ledgerColumns"
              :data-source="ledgerRecords"
              :pagination="ledgerPagination"
              :loading="ledgerLoading"
              row-key="id"
              size="middle"
              @change="onLedgerTableChange"
            />
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CameraOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useLoginUserStore } from '@/stores/loginUser'
import { uploadAvatar, updateProfile } from '@/api/userController'
import { getBillingAccount, pageBillingLedger } from '@/api/billingController'
import AvatarCropper from '@/components/AvatarCropper.vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 页面级 loading
const pageLoading = ref(false)
// 头像上传中的 loading
const avatarUploading = ref(false)
// 保存中的 loading
const saving = ref(false)
const billingAccount = ref<API.BillingAccountVO | null>(null)
const ledgerLoading = ref(false)
const ledgerRecords = ref<API.CreditLedgerVO[]>([])
const ledgerPagination = reactive({
  current: 1,
  pageSize: 5,
  total: 0,
  showSizeChanger: false,
})

const ledgerColumns = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', customRender: ({ text }: any) => dayjs(text).format('YYYY-MM-DD HH:mm') },
  { title: '类型', dataIndex: 'bizType', key: 'bizType', width: 120 },
  { title: '变动(积分)', dataIndex: 'changeDisplay', key: 'changeDisplay', width: 120 },
  { title: '余额(积分)', dataIndex: 'balanceAfterDisplay', key: 'balanceAfterDisplay', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
]

const appUsageText = computed(() => {
  const count = billingAccount.value?.appCount ?? 0
  const limit = billingAccount.value?.appLimit
  return limit == null ? `${count} / 不限` : `${count} / ${limit}`
})

// 文件选择器 ref
const fileInputRef = ref<HTMLInputElement | null>(null)

// 裁剪器状态
const cropperVisible = ref(false)
const selectedFile = ref<File | null>(null)

// 表单数据
const formState = reactive({
  id: '',
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
  createTime: '',
})

// 默认头像背景色（与 UserAvatar 组件的逻辑一致）
const avatarColors = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8',
  '#F7DC6F', '#BB8FCE', '#85C1E2', '#F8B739', '#52C41A',
  '#FA8C16', '#EB2F96', '#722ED1', '#13C2C2', '#1890FF',
  '#FF4D4F', '#FAAD14', '#FF7A45', '#36CFC9', '#FF7875',
  '#FFC069', '#95DE64', '#5CDBD3', '#69C0FF', '#B37FEB'
]

const avatarBgColor = computed(() => {
  const userId = formState.id
  if (!userId) return avatarColors[0]
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  const index = Math.abs(hash) % avatarColors.length
  return avatarColors[index]
})

// 从 Pinia store 填充表单
const loadUserData = () => {
  const loginUser = loginUserStore.loginUser
  if (loginUser) {
    formState.id = loginUser.id ?? ''
    formState.userAccount = loginUser.userAccount ?? ''
    formState.userName = loginUser.userName ?? ''
    formState.userAvatar = loginUser.userAvatar ?? ''
    formState.userProfile = loginUser.userProfile ?? ''
    formState.userRole = loginUser.userRole ?? ''
    formState.createTime = loginUser.createTime ?? ''
  }
}

// 触发文件选择器
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理文件选择 → 弹出裁剪器
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 校验文件类型
  const allowedTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    message.error('仅支持 PNG、JPEG、GIF、WebP 格式的图片')
    resetFileInput()
    return
  }

  // 校验文件大小
  if (file.size > 10 * 1024 * 1024) {
    message.error('文件大小不能超过 10MB')
    resetFileInput()
    return
  }

  // 保存选中的文件，打开裁剪器
  selectedFile.value = file
  cropperVisible.value = true
}

// 裁剪完成 → 上传
const handleCropped = async (blob: Blob) => {
  resetFileInput()

  // 将 Blob 转为 File
  const fileName = `avatar_${Date.now()}.png`
  const file = new File([blob], fileName, { type: 'image/png' })

  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.data.code === 0 && res.data.data) {
      const newAvatarUrl = res.data.data
      formState.userAvatar = newAvatarUrl
      // 同步更新 Pinia store
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userAvatar: newAvatarUrl,
      })
      message.success('头像上传成功')
    } else {
      message.error(res.data.message || '头像上传失败')
    }
  } catch {
    message.error('头像上传失败，请稍后重试')
  } finally {
    avatarUploading.value = false
  }
}

// 重置文件选择器（允许重复选择同一文件）
const resetFileInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

// 保存修改
const handleSave = async () => {
  if (!formState.userName?.trim()) {
    message.warning('昵称不能为空')
    return
  }

  saving.value = true
  try {
    const res = await updateProfile({
      id: formState.id,
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile,
    })
    if (res.data.code === 0) {
      message.success('个人信息更新成功')
      // 同步更新 Pinia store
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
      })
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch {
    message.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loadUserData()
  await Promise.all([loadBillingAccount(), loadLedger()])
})

const loadBillingAccount = async () => {
  try {
    const res = await getBillingAccount()
    if (res.data.code === 0) {
      billingAccount.value = res.data.data ?? null
    }
  } catch {
    // ignore
  }
}

const loadLedger = async () => {
  ledgerLoading.value = true
  try {
    const res = await pageBillingLedger({
      pageNum: ledgerPagination.current,
      pageSize: ledgerPagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      ledgerRecords.value = res.data.data.records ?? []
      ledgerPagination.total = res.data.data.totalRow ?? 0
    }
  } catch {
    message.error('加载积分流水失败')
  } finally {
    ledgerLoading.value = false
  }
}

const onLedgerTableChange = (pagination: any) => {
  ledgerPagination.current = pagination.current
  ledgerPagination.pageSize = pagination.pageSize
  loadLedger()
}
</script>

<style scoped>
/* ==================== 外层通栏容器 ==================== */
.profile-page-wrapper {
  width: 100%;
  /* 撑满内容区剩余高度，不再按固定页脚高度做减法估算 */
  flex: 1 0 auto;
  background: #fff;
}

/* ==================== 安全宽度容器 ==================== */
.profile-page-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

/* ==================== 页面标题 ==================== */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 14px;
  color: #888;
  margin: 0;
}

/* ==================== 主体卡片 ==================== */
.profile-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 40px 48px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

/* ==================== 头像区域 ==================== */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 36px;
  padding-bottom: 36px;
  border-bottom: 1px solid #f0f0f0;
}

.avatar-upload-area {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.default-avatar-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.default-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 15%;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-upload-area:hover .avatar-overlay {
  opacity: 1;
}

.overlay-icon {
  font-size: 24px;
  color: #fff;
  margin-bottom: 4px;
}

.overlay-text {
  font-size: 12px;
  color: #fff;
  white-space: nowrap;
}

.avatar-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #aaa;
  text-align: center;
}

/* ==================== 表单 ==================== */
.profile-form {
  max-width: 480px;
  margin: 0 auto;
}

.readonly-input {
  color: #999;
}

.save-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin-top: 8px;
}

.ledger-section {
  margin-top: 36px;
  padding-top: 28px;
  border-top: 1px solid #f0f0f0;
}

.ledger-title {
  font-size: 18px;
  margin: 0 0 16px;
  color: #0f172a;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .profile-page-container {
    padding: 24px 16px 32px;
  }

  .profile-card {
    padding: 24px 20px;
  }

  .page-title {
    font-size: 22px;
  }
}
</style>
