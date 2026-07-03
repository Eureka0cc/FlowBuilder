<template>
  <Teleport to="body">
    <Transition name="cropper-fade">
      <div v-if="visible" class="cropper-overlay" @click.self="handleCancel">
        <div class="cropper-modal">
          <!-- 标题栏 -->
          <div class="cropper-header">
            <h3 class="cropper-title">裁剪头像</h3>
            <span class="cropper-close" @click="handleCancel">&times;</span>
          </div>

          <!-- 裁剪主体 -->
          <div class="cropper-body">
            <!-- 左侧：圆形裁剪预览区 -->
            <div class="cropper-preview-area">
              <div
                ref="containerRef"
                class="cropper-container"
                @mousedown="startDrag"
                @touchstart.prevent="startDrag"
              >
                <!-- 圆形裁剪框（固定） -->
                <div class="crop-circle" :style="circleStyle" />

                <!-- 拖拽中的图片 -->
                <img
                  v-if="imageSrc"
                  :src="imageSrc"
                  class="crop-image"
                  :style="imageStyle"
                  draggable="false"
                />
              </div>

              <!-- 缩放滑块 -->
              <div class="zoom-control">
                <span class="zoom-label">缩放</span>
                <input
                  type="range"
                  class="zoom-slider"
                  :min="minScale"
                  :max="maxScale"
                  :step="0.01"
                  :value="scale"
                  @input="onZoomChange"
                />
              </div>
            </div>

            <!-- 右侧：圆形预览 -->
            <div class="cropper-result">
              <p class="result-label">预览</p>
              <div class="result-circle">
                <img
                  v-if="resultSrc"
                  :src="resultSrc"
                  class="result-img"
                />
                <div v-else class="result-placeholder">拖拽调整</div>
              </div>
              <p class="result-size">{{ resultSize }}</p>
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="cropper-footer">
            <a-button @click="handleCancel">取消</a-button>
            <a-button type="primary" :loading="processing" @click="handleConfirm">
              确认
            </a-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'

interface Props {
  visible: boolean
  /** 用户选择的原始图片 File */
  file: File | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  /** 裁剪完成，返回压缩后的 Blob */
  (e: 'crop', blob: Blob): void
  (e: 'cancel'): void
}>()

// === 裁剪区 DOM refs ===
const containerRef = ref<HTMLElement | null>(null)

// === 图片源 ===
const imageSrc = ref('')

// === 裁剪参数 ===
const CROP_SIZE = 240 // 裁剪框直径 px（在屏幕上的显示大小）

// 图片自然尺寸
const naturalSize = ref({ width: 0, height: 0 })

// 缩放（1 = 图片短边撑满裁剪框）
const scale = ref(1)
const minScale = ref(0.2)
const maxScale = ref(3)

// 图片在容器内的偏移（相对于"填满裁剪框"位置的 delta）
const offsetX = ref(0)
const offsetY = ref(0)

const processing = ref(false)

// 圆形框样式
const circleStyle = computed(() => ({
  width: `${CROP_SIZE}px`,
  height: `${CROP_SIZE}px`,
}))

// 容器尺寸（与裁剪框相同）
const containerSize = computed(() => CROP_SIZE)

// 计算基础缩放（使图片短边刚好填满裁剪框）
const baseScale = computed(() => {
  const { width, height } = naturalSize.value
  if (!width || !height) return 1
  return CROP_SIZE / Math.min(width, height)
})

// 图片在容器中的显示样式
const imageStyle = computed(() => {
  const s = baseScale.value * scale.value
  const w = naturalSize.value.width * s
  const h = naturalSize.value.height * s
  // 图片居中于容器，加上拖拽偏移
  const dx = (containerSize.value - w) / 2 + offsetX.value
  const dy = (containerSize.value - h) / 2 + offsetY.value
  return {
    width: `${w}px`,
    height: `${h}px`,
    transform: `translate(${dx}px, ${dy}px)`,
  }
})

// 实时裁剪预览
const resultSrc = ref('')
const resultSize = ref('')

// 拖拽状态
let dragging = false
let dragStartX = 0
let dragStartY = 0
let dragStartOffsetX = 0
let dragStartOffsetY = 0

// 全局 mousemove/mouseup（挂载在 window 上，防止拖出容器后失效）
function onMouseMove(e: MouseEvent) {
  if (!dragging) return
  offsetX.value = dragStartOffsetX + (e.clientX - dragStartX)
  offsetY.value = dragStartOffsetY + (e.clientY - dragStartY)
  updateResultPreview()
}

function onMouseUp() {
  dragging = false
}

function onTouchMove(e: TouchEvent) {
  if (!dragging || !e.touches.length) return
  offsetX.value = dragStartOffsetX + (e.touches[0].clientX - dragStartX)
  offsetY.value = dragStartOffsetY + (e.touches[0].clientY - dragStartY)
  updateResultPreview()
}

function onTouchEnd() {
  dragging = false
}

function startDrag(e: MouseEvent | TouchEvent) {
  dragging = true
  if (e instanceof MouseEvent) {
    dragStartX = e.clientX
    dragStartY = e.clientY
  } else if (e.touches?.length) {
    dragStartX = e.touches[0].clientX
    dragStartY = e.touches[0].clientY
  }
  dragStartOffsetX = offsetX.value
  dragStartOffsetY = offsetY.value
}

function onZoomChange(e: Event) {
  const target = e.target as HTMLInputElement
  scale.value = parseFloat(target.value)
  updateResultPreview()
}

// === 监听 file 变化，加载图片 ===
watch(
  () => props.file,
  async (file) => {
    if (!file) return
    const url = URL.createObjectURL(file)
    imageSrc.value = url

    // 等待图片加载，获取自然尺寸
    const img = new Image()
    img.src = url
    await new Promise<void>((resolve) => {
      img.onload = () => {
        naturalSize.value = { width: img.naturalWidth, height: img.naturalHeight }
        resolve()
      }
    })

    // 重置缩放和偏移
    scale.value = 1
    offsetX.value = 0
    offsetY.value = 0
    minScale.value = Math.max(0.1, CROP_SIZE / Math.max(img.naturalWidth, img.naturalHeight))
    maxScale.value = 3

    await nextTick()
    updateResultPreview()
  },
  { immediate: true }
)

// === Canvas 裁剪 + 压缩 ===
function doCrop(): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.src = imageSrc.value
    img.onload = () => {
      const outputSize = 400 // 输出头像 400x400

      // 计算裁剪参数
      const s = baseScale.value * scale.value
      const imgW = naturalSize.value.width * s
      const imgH = naturalSize.value.height * s

      // 图片在容器中的偏移
      const containerW = CROP_SIZE
      const containerH = CROP_SIZE
      const ix = (containerW - imgW) / 2 + offsetX.value
      const iy = (containerH - imgH) / 2 + offsetY.value

      // 圆形中心对应原图的坐标
      const centerX = (CROP_SIZE / 2 - ix) / s
      const centerY = (CROP_SIZE / 2 - iy) / s

      // 裁剪半边长（在原图坐标系中）
      const cropHalf = (CROP_SIZE / 2) / s

      // 源裁剪区域（在原图上的矩形）
      const srcX = centerX - cropHalf
      const srcY = centerY - cropHalf
      const srcSize = cropHalf * 2

      const canvas = document.createElement('canvas')
      canvas.width = outputSize
      canvas.height = outputSize
      const ctx = canvas.getContext('2d')!

      // 先画圆形裁剪路径
      ctx.beginPath()
      ctx.arc(outputSize / 2, outputSize / 2, outputSize / 2, 0, Math.PI * 2)
      ctx.clip()

      // 将原图裁剪区域画到 canvas 上
      ctx.drawImage(
        img,
        srcX, srcY, srcSize, srcSize,
        0, 0, outputSize, outputSize
      )

      // 输出为 PNG（保留透明背景）并压缩
      canvas.toBlob(
        (blob) => {
          if (blob) resolve(blob)
          else reject(new Error('Canvas toBlob failed'))
        },
        'image/png'
      )
    }
    img.onerror = () => reject(new Error('图片加载失败'))
  })
}

// 实时更新预览
function updateResultPreview() {
  const img = new Image()
  img.src = imageSrc.value
  img.onload = () => {
    const previewSize = 100
    const canvas = document.createElement('canvas')
    canvas.width = previewSize
    canvas.height = previewSize
    const ctx = canvas.getContext('2d')!

    const s = baseScale.value * scale.value
    const imgW = naturalSize.value.width * s
    const imgH = naturalSize.value.height * s
    const ix = (CROP_SIZE - imgW) / 2 + offsetX.value
    const iy = (CROP_SIZE - imgH) / 2 + offsetY.value
    const centerX = (CROP_SIZE / 2 - ix) / s
    const centerY = (CROP_SIZE / 2 - iy) / s
    const cropHalf = (CROP_SIZE / 2) / s
    const srcX = centerX - cropHalf
    const srcY = centerY - cropHalf
    const srcSize = cropHalf * 2

    ctx.beginPath()
    ctx.arc(previewSize / 2, previewSize / 2, previewSize / 2, 0, Math.PI * 2)
    ctx.clip()
    ctx.drawImage(img, srcX, srcY, srcSize, srcSize, 0, 0, previewSize, previewSize)

    resultSrc.value = canvas.toDataURL('image/png')
    resultSize.value = '400×400 px'
  }
}

// === 按钮事件 ===
async function handleConfirm() {
  processing.value = true
  try {
    const blob = await doCrop()
    emit('crop', blob)
    emit('update:visible', false)
  } catch {
    // 失败时静默
  } finally {
    processing.value = false
  }
}

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}

// 全局事件
if (typeof window !== 'undefined') {
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
  window.addEventListener('touchmove', onTouchMove, { passive: false })
  window.addEventListener('touchend', onTouchEnd)
}

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('mousemove', onMouseMove)
    window.removeEventListener('mouseup', onMouseUp)
    window.removeEventListener('touchmove', onTouchMove)
    window.removeEventListener('touchend', onTouchEnd)
  }
  // 清理 blob URL
  if (imageSrc.value) URL.revokeObjectURL(imageSrc.value)
})
</script>

<style scoped>
.cropper-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cropper-modal {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.2);
  width: 560px;
  max-width: 94vw;
  overflow: hidden;
}

.cropper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.cropper-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.cropper-close {
  font-size: 22px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  transition: color 0.2s;
}

.cropper-close:hover {
  color: #333;
}

.cropper-body {
  display: flex;
  gap: 32px;
  padding: 32px 24px;
  align-items: flex-start;
  justify-content: center;
}

/* ===== 左侧裁剪区 ===== */
.cropper-preview-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.cropper-container {
  width: 240px;
  height: 240px;
  position: relative;
  overflow: hidden;
  cursor: grab;
  user-select: none;
  background: #f0f0f0;
  border-radius: 4px;
}

.cropper-container:active {
  cursor: grabbing;
}

/* 圆形裁剪框 - 使用 box-shadow 实现外部暗角 */
.crop-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
  z-index: 2;
  pointer-events: none;
}

/* 图片 - 在暗角下面 */
.crop-image {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
  pointer-events: none;
}

/* 缩放滑块 */
.zoom-control {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 240px;
}

.zoom-label {
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

.zoom-slider {
  flex: 1;
  accent-color: #1890ff;
}

/* ===== 右侧预览 ===== */
.cropper-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.result-label {
  font-size: 13px;
  color: #888;
  margin: 0;
}

.result-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f5f5;
  border: 2px solid #e8e8e8;
}

.result-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.result-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #ccc;
}

.result-size {
  font-size: 11px;
  color: #aaa;
  margin: 0;
}

/* ===== 底部按钮 ===== */
.cropper-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

/* ===== 过渡动画 ===== */
.cropper-fade-enter-active,
.cropper-fade-leave-active {
  transition: opacity 0.25s ease;
}

.cropper-fade-enter-from,
.cropper-fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 600px) {
  .cropper-body {
    flex-direction: column;
    align-items: center;
    gap: 20px;
    padding: 20px 16px;
  }

  .cropper-result {
    flex-direction: row;
    gap: 12px;
  }

  .result-circle {
    width: 56px;
    height: 56px;
  }
}
</style>
