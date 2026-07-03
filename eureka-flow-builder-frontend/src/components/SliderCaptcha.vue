<template>
  <div class="slider-captcha">
    <!-- 加载状态（骨架屏） -->
    <div v-if="loading" class="captcha-loading">
      <div class="skeleton-image"></div>
      <div class="skeleton-track"></div>
    </div>

    <!-- 错误状态 -->
    <a-result
      v-else-if="loadError"
      status="warning"
      title="验证码加载失败"
      :sub-title="loadError"
    >
      <template #extra>
        <a-button type="primary" @click="loadCaptcha">重新加载</a-button>
      </template>
    </a-result>

    <!-- 验证码主体 -->
    <div v-else class="captcha-content">
      <!-- 背景图区域 -->
      <div class="captcha-image-wrapper" ref="imageWrapperRef">
        <img
          :src="captchaData.backgroundImage"
          alt="验证码背景"
          class="captcha-bg"
          draggable="false"
        />
        <!-- 滑块图（绝对定位在背景图上） -->
        <img
          :src="captchaData.sliderImage"
          alt="滑块"
          class="slider-piece"
          :style="sliderPieceStyle"
          draggable="false"
        />
      </div>

      <!-- 滑块轨道 -->
      <div class="slider-track-wrapper">
        <div
          class="slider-track"
          :class="{
            'is-dragging': isDragging,
            'is-success': status === 'success',
            'is-failed': status === 'failed',
          }"
          @mousedown="handleDragStart"
          @touchstart.prevent="handleDragStart"
        >
          <div class="slider-fill" :style="{ width: `${sliderX + displayThumbWidth}px` }"></div>
          <div
            class="slider-thumb"
            :style="{ transform: `translateX(${sliderX}px)` }"
          >
            <DoubleRightOutlined v-if="status !== 'success'" :rotate="isDragging ? 90 : 0" />
            <CheckOutlined v-else />
          </div>
          <div class="slider-hint">
            <template v-if="status === 'success'">
              <CheckCircleFilled class="hint-icon success-icon" />
              <span v-if="countdown > 0">验证成功，{{ countdown }}s 后跳转</span>
              <span v-else>验证成功</span>
            </template>
            <template v-else-if="status === 'failed'">
              <CloseCircleFilled class="hint-icon error-icon" />
              <span>验证失败，请重新拖动</span>
              <a-button type="link" size="small" @click.stop="loadCaptcha">
                刷新
              </a-button>
            </template>
            <template v-else>
              <DragOutlined class="hint-icon" />
              <span>{{ isDragging ? '拖动滑块到缺口处' : '拖动左侧滑块完成拼图' }}</span>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import {
  DoubleRightOutlined,
  CheckOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  DragOutlined,
} from '@ant-design/icons-vue'
import { getCaptcha, verifyCaptcha } from '@/api/captchaController'

const emit = defineEmits<{
  (e: 'success', payload: { captchaId: string; sliderY?: number }): void
}>()

// ============ 常量（对应后端 CaptchaConstant） ============
const NATIVE_IMAGE_WIDTH = 320
const NATIVE_IMAGE_HEIGHT = 160
const NATIVE_SLIDER_WIDTH = 50
const NATIVE_SLIDER_HEIGHT = 40
const NATIVE_TRACK_WIDTH = NATIVE_IMAGE_WIDTH - NATIVE_SLIDER_WIDTH // 270
const NATIVE_BUMP_PADDING = 14 // 对应后端 BUMP_PADDING
const NATIVE_SLIDER_IMG_WIDTH = NATIVE_SLIDER_WIDTH + 2 * NATIVE_BUMP_PADDING // 78
const NATIVE_SLIDER_IMG_HEIGHT = NATIVE_SLIDER_HEIGHT + 2 * NATIVE_BUMP_PADDING // 68

// ============ 状态 ============
const loading = ref(true)
const loadError = ref('')
const captchaData = reactive<API.CaptchaVO>({
  captchaId: '',
  backgroundImage: '',
  sliderImage: '',
  sliderY: 0,
})
const sliderX = ref(0)
const displayThumbWidth = ref(NATIVE_SLIDER_WIDTH) // thumb 轨道视觉宽度（无 padding）
const displaySliderWidth = ref(NATIVE_SLIDER_IMG_WIDTH) // 滑块图片显示宽度（含 padding）
const displaySliderHeight = ref(NATIVE_SLIDER_IMG_HEIGHT) // 滑块图片显示高度（含 padding）
const trackWidth = ref(NATIVE_TRACK_WIDTH)
const isDragging = ref(false)
const startX = ref(0)
const startSliderX = ref(0)
const status = ref<'idle' | 'success' | 'failed'>('idle')
const countdown = ref(3)
let countdownTimer: ReturnType<typeof setInterval> | null = null
const imageWrapperRef = ref<HTMLElement | null>(null)

// 显示比例（背景图实际渲染宽度 / 原生宽度 320）
const displayScale = ref(1)

// 滑块图在背景上的样式（padding 偏移使拼图块对准缺口）
const sliderPieceStyle = computed(() => ({
  top: `${((captchaData.sliderY || 0) - NATIVE_BUMP_PADDING) * displayScale.value}px`,
  left: `${sliderX.value - NATIVE_BUMP_PADDING * displayScale.value}px`,
  width: `${displaySliderWidth.value}px`,
  height: `${displaySliderHeight.value}px`,
}))

// ============ 方法 ============
/**
 * 加载验证码
 */
const loadCaptcha = async () => {
  loading.value = true
  loadError.value = ''
  status.value = 'idle'
  sliderX.value = 0
  countdown.value = 3
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  try {
    const res = await getCaptcha()
    if (res.data.code === 0 && res.data.data) {
      Object.assign(captchaData, res.data.data)
    } else {
      loadError.value = res.data.message || '验证码获取失败'
    }
  } catch (err: any) {
    loadError.value = err?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
    // 加载完成后测量实际宽度
    await nextTick()
    measureTrack()
  }
}

/**
 * 测量显示尺寸（滑轨宽度、滑块宽高都需随背景图缩放）
 */
const measureTrack = () => {
  const wrapper = imageWrapperRef.value
  if (!wrapper) return
  const scale = wrapper.getBoundingClientRect().width / NATIVE_IMAGE_WIDTH
  displayScale.value = scale
  trackWidth.value = NATIVE_TRACK_WIDTH * scale
  displayThumbWidth.value = NATIVE_SLIDER_WIDTH * scale
  displaySliderWidth.value = NATIVE_SLIDER_IMG_WIDTH * scale
  displaySliderHeight.value = NATIVE_SLIDER_IMG_HEIGHT * scale
}

/**
 * 拖动开始
 */
const handleDragStart = (e: MouseEvent | TouchEvent) => {
  if (status.value === 'success' || loading.value || loadError.value) return
  isDragging.value = true
  status.value = 'idle'
  const point = getPoint(e)
  startX.value = point.x
  startSliderX.value = sliderX.value
  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', handleDragEnd)
  document.addEventListener('touchmove', handleDragMove)
  document.addEventListener('touchend', handleDragEnd)
}

/**
 * 拖动中
 */
const handleDragMove = (e: MouseEvent | TouchEvent) => {
  if (!isDragging.value) return
  const point = getPoint(e)
  const deltaX = point.x - startX.value
  const newX = Math.max(0, Math.min(trackWidth.value, startSliderX.value + deltaX))
  sliderX.value = newX
}

/**
 * 拖动结束 - 提交验证
 */
const handleDragEnd = async () => {
  if (!isDragging.value) return
  isDragging.value = false
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  document.removeEventListener('touchmove', handleDragMove)
  document.removeEventListener('touchend', handleDragEnd)

  // 如果拖动距离很小，认为未操作
  if (sliderX.value < 5) {
    sliderX.value = 0
    return
  }

  await submitVerify()
}

/**
 * 提交验证
 */
const submitVerify = async () => {
  try {
    const res = await verifyCaptcha({
      captchaId: captchaData.captchaId,
      sliderX: Math.round(sliderX.value / displayScale.value),
    })
    if (res.data.code === 0 && res.data.data) {
      status.value = 'success'
      message.success('验证成功')
      startCountdown()
      emit('success', {
        captchaId: captchaData.captchaId || '',
        sliderY: captchaData.sliderY,
      })
    } else {
      status.value = 'failed'
      // 失败后 1 秒自动刷新验证码
      setTimeout(() => {
        loadCaptcha()
      }, 1000)
    }
  } catch (err: any) {
    status.value = 'failed'
    message.error('验证请求失败')
    setTimeout(() => {
      loadCaptcha()
    }, 1000)
  }
}

/**
 * 启动成功倒计时
 */
const startCountdown = () => {
  countdown.value = 3
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
    }
  }, 1000)
}

/**
 * 提取事件坐标
 */
const getPoint = (e: MouseEvent | TouchEvent): { x: number; y: number } => {
  if ('touches' in e && e.touches.length) {
    return { x: e.touches[0].clientX, y: e.touches[0].clientY }
  }
  if ('changedTouches' in e && e.changedTouches.length) {
    return { x: e.changedTouches[0].clientX, y: e.changedTouches[0].clientY }
  }
  return { x: (e as MouseEvent).clientX, y: (e as MouseEvent).clientY }
}

// nextTick 工具
const nextTick = () => new Promise<void>((resolve) => setTimeout(resolve, 0))

// 窗口大小变化时重新测量
const handleResize = () => {
  measureTrack()
}

onMounted(() => {
  loadCaptcha()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  document.removeEventListener('touchmove', handleDragMove)
  document.removeEventListener('touchend', handleDragEnd)
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped>
.slider-captcha {
  width: 100%;
  margin-bottom: 8px;
}

.captcha-loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-image,
.skeleton-track {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 8px;
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 320 / 160;
}

.skeleton-track {
  width: 100%;
  height: 40px;
  border-radius: 22px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.captcha-content {
  width: 100%;
}

/* 背景图区域 */
.captcha-image-wrapper {
  position: relative;
  width: 100%;
  /* 等比缩放保持 320:160 = 2:1 */
  aspect-ratio: 320 / 160;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f5f9;
  user-select: none;
}

.captcha-bg {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: fill;
}

.slider-piece {
  position: absolute;
  display: block;
  pointer-events: none;
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.3));
  transition: filter 0.15s ease;
}

/* 滑块轨道 */
.slider-track-wrapper {
  margin-top: 12px;
}

.slider-track {
  position: relative;
  height: 40px;
  background: #f1f5f9;
  border-radius: 22px;
  cursor: pointer;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  transition: background 0.2s ease;
}

.slider-track:hover:not(.is-success) {
  background: #e2e8f0;
}

.slider-track.is-dragging {
  cursor: grabbing;
}

.slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, #06b6d4, #6366f1);
  transition: width 0.05s linear;
  opacity: 0.85;
}

.slider-track.is-success .slider-fill {
  background: linear-gradient(90deg, #10b981, #059669);
}

.slider-track.is-failed .slider-fill {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}

.slider-track.is-failed {
  animation: shake 0.4s ease;
}

.slider-thumb {
  position: absolute;
  top: 0;
  left: 0;
  width: 50px;
  height: 40px;
  background: #ffffff;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: transform 0.05s linear, background 0.2s ease;
  z-index: 2;
  cursor: grab;
}

.slider-track.is-dragging .slider-thumb {
  cursor: grabbing;
}

.slider-track.is-success .slider-thumb {
  background: #10b981;
  color: #fff;
}

.slider-track.is-failed .slider-thumb {
  background: #ef4444;
  color: #fff;
}

.slider-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  pointer-events: none;
  z-index: 1;
  white-space: nowrap;
}

.slider-hint :deep(.ant-btn) {
  pointer-events: auto;
  padding: 0 4px;
  height: auto;
  font-size: 12px;
}

.hint-icon {
  font-size: 14px;
}

.success-icon {
  color: #10b981;
}

.error-icon {
  color: #ef4444;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}
</style>