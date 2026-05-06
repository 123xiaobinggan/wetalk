<template>
  <div class="message-content-inner">
    <!-- 文本消息 -->
    <template v-if="msgType === 1">
      <span class="text-message">
        {{ normalizedContent?.text || '' }}
      </span>
    </template>

    <!-- 语音消息：不使用 audio 控件，点击后用系统默认播放器打开 -->
    <template v-else-if="msgType === 2">
      <div class="audio-message" @click.stop="openAudioPlayer">
        <div class="audio-main">
          <span class="audio-icon">🎤</span>
          <span class="audio-label">语音消息</span>
          <span v-if="normalizedContent.duration" class="audio-duration">
            {{ formatDuration(normalizedContent.duration) }}
          </span>
        </div>
      </div>
    </template>

    <!-- 图片消息 -->
    <template v-else-if="msgType === 3">
      <div class="image-message">
        <img
          v-if="displayUrl"
          class="message-image"
          :src="displayUrl"
          :alt="normalizedContent.fileName || 'image'"
          @click.stop="openImagePreview"
          @load="emit('media-loaded')"
        />

        <span v-else>[图片]</span>
      </div>
    </template>

    <!-- 视频消息：  -->
    <template v-else-if="msgType === 4">
      <div class="video-message" @click.stop="openVideoPlayer">
        <div class="video-card">
          <img
            v-if="normalizedContent.cover"
            class="video-cover"
            :src="normalizeUrl(normalizedContent.cover)"
            alt="video"
            @load="emit('media-loaded')"
          />

          <div v-else class="video-cover-placeholder">🎬</div>

          <div class="video-play-mask">▶</div>

          <div
            v-if="normalizedContent.duration || normalizedContent.size"
            class="video-meta-in-card"
          >
            <span v-if="normalizedContent.duration">
              {{ formatDuration(normalizedContent.duration) }}
            </span>
            <span v-if="normalizedContent.size">
              {{ formatFileSize(normalizedContent.size) }}
            </span>
          </div>
        </div>

        <span v-if="!normalizedContent.url && !normalizedContent.localUrl">[视频]</span>
      </div>
    </template>

    <!-- 文件消息 -->
    <template v-else-if="msgType === 5">
      <div class="file-message" @click.stop="downloadFileAndReveal">
        <div class="file-icon">
          {{ fileIcon }}
        </div>

        <div class="file-info">
          <div class="file-name">
            {{ normalizedContent.fileName || '未知文件' }}
          </div>

          <div class="file-meta">
            <span v-if="fileExt">{{ fileExt.toUpperCase() }}</span>
            <span v-if="normalizedContent.size">
              {{ formatFileSize(normalizedContent.size) }}
            </span>
            <span v-if="normalizedContent.uploadStatus === 'uploading'"> 上传中 </span>
          </div>
        </div>
      </div>
    </template>

    <!-- 兜底 -->
    <template v-else>
      <span class="unknown-message">
        {{ typeof content === 'string' ? content : '[暂不支持的消息类型]' }}
      </span>
    </template>
  </div>

  <!-- 图片预览器 -->
  <div
    v-if="imagePreviewVisible"
    class="image-viewer-mask"
    @click="closeImagePreview"
    @wheel.prevent="onImageWheel"
  >
    <div class="image-viewer-toolbar" @click.stop>
      <button type="button" class="preview-tool-btn" @click="zoomInImage">+</button>
      <button type="button" class="preview-tool-btn" @click="zoomOutImage">-</button>
      <button type="button" class="preview-tool-btn" @click="rotateLeftImage">左转</button>
      <button type="button" class="preview-tool-btn" @click="rotateRightImage">右转</button>
      <button type="button" class="preview-tool-btn" @click="toggleFlipImage">翻转</button>
      <button type="button" class="preview-tool-btn" @click="copyPreviewImage">复制</button>
      <button type="button" class="preview-tool-btn" @click="downloadPreviewImage">下载</button>
      <button type="button" class="preview-tool-btn" @click="resetImagePreview">重置</button>
      <button type="button" class="preview-tool-btn close" @click="closeImagePreview">关闭</button>
    </div>

    <div class="image-viewer-stage" @click.stop>
      <img
        class="preview-image"
        :src="displayUrl"
        alt="preview"
        draggable="false"
        :style="imagePreviewStyle"
        @mousedown.stop.prevent="startImageDrag"
        @dblclick.stop="resetImagePreview"
      />
    </div>
  </div>

  <!-- 视频播放器 -->
  <div v-if="videoPlayerVisible" class="media-mask" @click.stop="closeVideoPlayer">
    <div class="video-player-panel" @click.stop>
      <button type="button" class="media-close-btn" @click="closeVideoPlayer">×</button>

      <video class="video-player" :src="displayUrl" controls autoplay />
    </div>
  </div>

  <!-- 音频播放器 -->
  <div v-if="audioPlayerVisible" class="media-mask" @click.stop="closeAudioPlayer">
    <div class="audio-player-panel" @click.stop>
      <button type="button" class="media-close-btn" @click="closeAudioPlayer">×</button>

      <div class="audio-player-title">
        {{ normalizedContent.fileName || '语音消息' }}
      </div>

      <audio class="audio-player" :src="displayUrl" controls autoplay />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'

defineOptions({
  name: 'MessageContent',
})

const props = defineProps({
  msgType: {
    type: Number,
    required: true,
  },
  content: {
    type: [Object, String, null],
    default: null,
  },
  status: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['media-loaded'])

const imagePreviewVisible = ref(false)
const videoPlayerVisible = ref(false)
const audioPlayerVisible = ref(false)

const imageScale = ref(1)
const imageTranslateX = ref(0)
const imageTranslateY = ref(0)

const imageRotate = ref(0)
const imageFlipX = ref(false)

const imagePreviewStyle = computed(() => {
  const sx = imageFlipX.value ? -1 : 1

  return {
    transform: `translate(${imageTranslateX.value}px, ${imageTranslateY.value}px) rotate(${imageRotate.value}deg) scale(${imageScale.value * sx}, ${imageScale.value})`,
  }
})

const MIN_IMAGE_SCALE = 0.25
const MAX_IMAGE_SCALE = 5

let imageDragging = false
let imageDragStartX = 0
let imageDragStartY = 0
let imageStartTranslateX = 0
let imageStartTranslateY = 0

const normalizedContent = computed(() => {
  if (!props.content) return {}

  if (typeof props.content === 'object') {
    return props.content
  }

  if (typeof props.content === 'string') {
    try {
      return JSON.parse(props.content)
    } catch (e) {
      return {
        text: props.content,
      }
    }
  }

  return {}
})

const displayUrl = computed(() => {
  return normalizeUrl(normalizedContent.value.localUrl || normalizedContent.value.url)
})

const fileExt = computed(() => {
  const content = normalizedContent.value

  if (content.ext) {
    return String(content.ext).toLowerCase()
  }

  if (content.format) {
    return String(content.format).toLowerCase()
  }

  const fileName = content.fileName || ''
  const index = fileName.lastIndexOf('.')

  if (index === -1) return ''

  return fileName.slice(index + 1).toLowerCase()
})

const fileIcon = computed(() => {
  const ext = fileExt.value

  if (props.msgType === 2) {
    return '🎤'
  }

  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) {
    return '🖼️'
  }

  if (['mp4', 'avi', 'mov', 'mkv', 'webm'].includes(ext)) {
    return '🎬'
  }

  if (['mp3', 'wav', 'aac', 'm4a', 'flac', 'ogg', 'webm'].includes(ext)) {
    return '🎵'
  }

  if (['doc', 'docx'].includes(ext)) {
    return '📄'
  }

  if (['xls', 'xlsx'].includes(ext)) {
    return '📊'
  }

  if (['ppt', 'pptx'].includes(ext)) {
    return '📑'
  }

  if (['pdf'].includes(ext)) {
    return '📕'
  }

  if (['zip', 'rar', '7z'].includes(ext)) {
    return '🗜️'
  }

  return '📎'
})

function normalizeUrl(url) {
  if (!url) return ''

  if (url.startsWith('blob:')) {
    return url
  }

  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  if (url.startsWith('data:')) {
    return url
  }

  const fileBaseUrl = import.meta.VITE_SOURCES_URL

  if (url.startsWith('/')) {
    return fileBaseUrl + url
  }

  return fileBaseUrl + '/' + url
}

function formatFileSize(size) {
  const n = Number(size)

  if (!Number.isFinite(n) || n <= 0) {
    return ''
  }

  if (n < 1024) {
    return `${n} B`
  }

  if (n < 1024 * 1024) {
    return `${(n / 1024).toFixed(1)} KB`
  }

  if (n < 1024 * 1024 * 1024) {
    return `${(n / 1024 / 1024).toFixed(1)} MB`
  }

  return `${(n / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function formatDuration(duration) {
  const total = Number(duration)

  if (!Number.isFinite(total) || total <= 0) {
    return ''
  }

  const minutes = Math.floor(total / 60)
  const seconds = Math.floor(total % 60)

  if (minutes <= 0) {
    return `${seconds}s`
  }

  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

function buildDefaultFileName() {
  const ext = fileExt.value || 'dat'

  if (props.msgType === 2) {
    return `voice.${ext}`
  }

  if (props.msgType === 3) {
    return `image.${ext}`
  }

  if (props.msgType === 4) {
    return `video.${ext}`
  }

  return normalizedContent.value.fileName || `file.${ext}`
}

function openImagePreview() {
  if (!displayUrl.value) return

  resetImagePreview()
  imagePreviewVisible.value = true
}

function closeImagePreview() {
  imagePreviewVisible.value = false
  stopImageDrag()
}

function resetImagePreview() {
  imageScale.value = 1
  imageTranslateX.value = 0
  imageTranslateY.value = 0
  imageRotate.value = 0
  imageFlipX.value = false
}

function zoomInImage() {
  setImageScale(imageScale.value + 0.25)
}

function zoomOutImage() {
  setImageScale(imageScale.value - 0.25)
}

function onImageWheel(e) {
  if (e.deltaY < 0) {
    setImageScale(imageScale.value + 0.15)
  } else {
    setImageScale(imageScale.value - 0.15)
  }
}

function setImageScale(scale) {
  imageScale.value = Math.min(MAX_IMAGE_SCALE, Math.max(MIN_IMAGE_SCALE, scale))
}

function startImageDrag(e) {
  imageDragging = true

  imageDragStartX = e.clientX
  imageDragStartY = e.clientY
  imageStartTranslateX = imageTranslateX.value
  imageStartTranslateY = imageTranslateY.value

  window.addEventListener('mousemove', onImageDragging)
  window.addEventListener('mouseup', stopImageDrag, { once: true })
}

function onImageDragging(e) {
  if (!imageDragging) return

  imageTranslateX.value = imageStartTranslateX + e.clientX - imageDragStartX
  imageTranslateY.value = imageStartTranslateY + e.clientY - imageDragStartY
}

function stopImageDrag() {
  if (!imageDragging) return

  imageDragging = false
  window.removeEventListener('mousemove', onImageDragging)
}

function rotateLeftImage() {
  imageRotate.value -= 90
}

function rotateRightImage() {
  imageRotate.value += 90
}

function toggleFlipImage() {
  imageFlipX.value = !imageFlipX.value
}

async function copyPreviewImage() {
  const url = normalizeUrl(normalizedContent.value.url)

  if (!url || url.startsWith('blob:') || url.startsWith('data:')) {
    console.warn('本地临时图片暂不支持复制，请等待上传完成')
    return
  }

  if (!window.electronAPI?.copyImageFromUrl) {
    console.warn('electronAPI.copyImageFromUrl 未暴露')
    return
  }

  await window.electronAPI.copyImageFromUrl(url)
}

async function downloadPreviewImage() {
  const url = normalizeUrl(normalizedContent.value.url)

  if (!url || url.startsWith('blob:') || url.startsWith('data:')) {
    console.warn('本地临时图片暂不支持下载，请等待上传完成')
    return
  }

  if (!window.electronAPI?.saveImageFromUrl) {
    console.warn('electronAPI.saveImageFromUrl 未暴露')
    return
  }

  await window.electronAPI.saveImageFromUrl(url)
}

function openVideoPlayer() {
  if (!displayUrl.value) return
  videoPlayerVisible.value = true
}

function closeVideoPlayer() {
  videoPlayerVisible.value = false
}

function openAudioPlayer() {
  if (!displayUrl.value) return
  audioPlayerVisible.value = true
}

function closeAudioPlayer() {
  audioPlayerVisible.value = false
}

async function downloadFileAndReveal() {
  const content = normalizedContent.value

  if (content.uploadStatus === 'uploading') {
    return
  }

  const url = normalizeUrl(content.url)
  const fileName = content.fileName || buildDefaultFileName()

  if (!url || url.startsWith('blob:') || url.startsWith('data:')) {
    console.warn('文件还没有可下载地址')
    return
  }

  if (!window.electronAPI?.downloadFileAndReveal) {
    console.warn('electronAPI.downloadFileAndReveal 未暴露')
    return
  }

  const res = await window.electronAPI.downloadFileAndReveal({
    url,
    fileName,
  })

  if (res && res.success === false) {
    console.warn('downloadFileAndReveal failed:', res.error)
  }
}

onUnmounted(() => {
  stopImageDrag()
})
</script>

<style scoped>
.message-content-inner {
  display: block;
  min-width: 0;
}

.text-message,
.unknown-message {
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.5;
}

/* 语音 */
.audio-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.audio-main {
  min-width: 150px;
  height: 36px;
  padding: 0 12px;
  border-radius: 18px;
  background-color: #eef6ff;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.audio-main:hover {
  background-color: #e0f0ff;
}

.audio-icon {
  font-size: 16px;
}

.audio-label {
  font-size: 14px;
  color: #333;
}

.audio-duration {
  margin-left: auto;
  font-size: 12px;
  color: #666;
}

/* 图片 */
.image-message {
  display: block;
}

.message-image {
  max-width: 220px;
  max-height: 220px;
  border-radius: 8px;
  display: block;
  object-fit: cover;
  cursor: pointer;
}

/* 视频 */
.video-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
}

.video-card {
  position: relative;
  width: 220px;
  height: 140px;
  border-radius: 8px;
  overflow: hidden;
  background-color: #000;
}

.video-cover,
.video-cover-placeholder {
  width: 100%;
  height: 100%;
}

.video-cover {
  display: block;
  object-fit: cover;
}

.video-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 36px;
  background-color: #111;
}

.video-play-mask {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.18);
  color: #fff;
  font-size: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-meta-in-card {
  position: absolute;
  right: 8px;
  bottom: 6px;
  z-index: 3;

  display: flex;
  gap: 6px;
  align-items: center;

  padding: 3px 7px;
  border-radius: 999px;

  font-size: 11px;
  line-height: 1;
  color: #fff;
  background-color: rgba(0, 0, 0, 0.58);

  pointer-events: none;
}

/* 文件 */
.file-message {
  width: 250px;
  min-height: 68px;
  box-sizing: border-box;
  padding: 10px;
  border-radius: 8px;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.file-message:hover {
  background-color: #f5f7fa;
}

.file-icon {
  width: 44px;
  height: 44px;
  flex: none;
  border-radius: 8px;
  background-color: #eef3ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  margin-top: 5px;
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.media-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background-color: rgba(0, 0, 0, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
}


.preview-scale-text {
  min-width: 52px;
  text-align: center;
  font-size: 13px;
}

.image-viewer-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  outline: none;
}

.image-viewer-toolbar {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 10001;

  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;

  max-width: calc(100vw - 32px);
  padding: 8px;

  border-radius: 10px;
  background: rgba(20, 20, 20, 0.68);
  backdrop-filter: blur(6px);
}

.preview-tool-btn {
  height: 30px;
  padding: 0 10px;

  border: none;
  border-radius: 8px;

  background: rgba(255, 255, 255, 0.16);
  color: #fff;

  font-size: 13px;
  cursor: pointer;
  user-select: none;
}

.preview-tool-btn:hover {
  background: rgba(255, 255, 255, 0.28);
}

.preview-tool-btn.close {
  background: rgba(255, 255, 255, 0.28);
}

.preview-tool-btn.close:hover {
  background: rgba(255, 255, 255, 0.28);
}

.image-viewer-stage {
  width: 100vw;
  height: 100vh;

  display: flex;
  align-items: center;
  justify-content: center;

  overflow: hidden;
}

.preview-image {
  max-width: 78vw;
  max-height: 78vh;

  border-radius: 10px;
  object-fit: contain;

  user-select: none;
  -webkit-user-drag: none;

  cursor: grab;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);

  transform-origin: center center;
  transition: transform 0.08s linear;
  will-change: transform;
}

.preview-image:active {
  cursor: grabbing;
}

.video-player-panel {
  position: relative;
  width: 72vw;
  max-width: 960px;
  max-height: 82vh;
  background-color: #000;
  border-radius: 10px;
  overflow: hidden;
}

.video-player {
  width: 100%;
  max-height: 82vh;
  display: block;
  background-color: #000;
}

.audio-player-panel {
  position: relative;
  width: 420px;
  box-sizing: border-box;
  padding: 24px;
  border-radius: 12px;
  background-color: #fff;
}

.audio-player-title {
  margin-bottom: 16px;
  font-size: 15px;
  color: #333;
  word-break: break-all;
}

.audio-player {
  width: 100%;
}

.media-close-btn {
  position: absolute;
  top: 10px;
  right: 12px;
  z-index: 2;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 20px;
  line-height: 28px;
  cursor: pointer;
}

.audio-player-panel .media-close-btn {
  background-color: #f2f3f5;
  color: #333;
}
</style>
