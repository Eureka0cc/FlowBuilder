<template>
  <AuthCard>
    <!-- 验证通过前：显示滑动验证码 -->
    <div v-if="!captchaPassed" class="captcha-step">
      <p class="step-tip">请先完成滑动验证</p>
      <SliderCaptcha @success="onCaptchaSuccess" />
    </div>

    <!-- 验证通过后：显示注册表单 -->
    <a-form
      v-else
      :model="formState"
      name="basic"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <div class="captcha-badge">
        <CheckCircleFilled class="badge-icon" />
        <span>已通过验证</span>
        <a-button type="link" size="small" @click="resetCaptcha">重新验证</a-button>
      </div>
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input
          v-model:value="formState.userAccount"
          placeholder="请输入账号"
          size="large"
        />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password
          v-model:value="formState.userPassword"
          placeholder="请输入密码"
          size="large"
        />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { min: 8, message: '密码不能小于 8 位' },
          { validator: validatePassword },
        ]"
      >
        <a-input-password
          v-model:value="formState.checkPassword"
          placeholder="请确认密码"
          size="large"
        />
      </a-form-item>
      <div class="auth-tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          class="auth-submit-btn"
          :loading="submitting"
        >
          注册
        </a-button>
      </a-form-item>
    </a-form>
  </AuthCard>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { register } from '@/api/userController.ts'
import AuthCard from '@/components/AuthCard.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'

const formState = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const submitting = ref(false)
const captchaPassed = ref(false)
const captchaId = ref('')
const router = useRouter()

/**
 * 滑动验证码通过
 */
const onCaptchaSuccess = (payload: { captchaId: string }) => {
  captchaPassed.value = true
  captchaId.value = payload.captchaId
}

/**
 * 重新进行验证码验证
 */
const resetCaptcha = () => {
  captchaPassed.value = false
  captchaId.value = ''
}

/**
 * 验证两次密码是否一致
 */
const validatePassword = (_rule: any, value: string) => {
  if (value && value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
  submitting.value = true
  try {
    const res = await register(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      router.push({
        path: '/user/login',
        replace: true,
      })
    } else {
      message.error('注册失败，' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.captcha-step {
  padding: 4px 0 12px;
}

.step-tip {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px;
}

.captcha-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  font-size: 13px;
  color: #047857;
  margin-bottom: 20px;
}

.captcha-badge :deep(.ant-btn) {
  margin-left: auto;
  padding: 0 4px;
  height: auto;
  font-size: 12px;
  color: #047857;
}

.badge-icon {
  color: #10b981;
  font-size: 14px;
}
</style>