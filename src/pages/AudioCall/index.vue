<template>
  <div class="audio-call-page">
    <div class="drag-bar"></div>
    <div class="call-main">
      <audio ref="remoteAudioRef" autoplay playsinline></audio>
      <img :src="avatarUrl" class="peer-avatar" alt="avatar" />

      <div class="peer-name">
        {{ peerName }}
      </div>

      <div class="call-status">
        {{ callStatusText }}
      </div>

      <div class="call-duration">
        {{ callStore.formattedDuration }}
      </div>
    </div>

    <div v-if="isIncomingRinging" class="incoming-actions">
      <button type="button" class="reject-btn" @click="rejectCall">拒绝</button>

      <button type="button" class="accept-btn" @click="acceptCall">接听</button>
    </div>

    <div v-else class="call-actions">
      <button
        type="button"
        class="action-btn"
        :class="{ active: callStore.muted }"
        :disabled="!canOperate"
        @click="toggleMute"
      >
        <span class="action-icon">{{ callStore.muted ? '🔇' : '🎙️' }}</span>
        <span class="action-text">{{ callStore.muted ? '已静音' : '静音' }}</span>
      </button>

      <button type="button" class="hangup-btn" @click="hangup">
        <span class="hangup-icon">📞</span>
      </button>

      <button
        type="button"
        class="action-btn"
        :class="{ active: callStore.speakerOn }"
        :disabled="!canOperate"
        @click="toggleSpeaker"
      >
        <span class="action-icon">🔊</span>
        <span class="action-text">{{ callStore.speakerOn ? '免提开' : '免提' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useCallStore } from '/src/stores/callStore'

defineOptions({
  name: 'AudioCall',
})

const callStore = useCallStore()

const remoteAudioRef = ref(null)
let durationTimer = null
let outgoingStarted = false

const defaultAvatar = ''

const peerName = computed(() => {
  return callStore.peerName || '语音通话'
})

const avatarUrl = computed(() => {
  return normalizeUrl(callStore.peerAvatar) || defaultAvatar
})

const isIncomingRinging = computed(() => {
  return callStore.direction === 'incoming' && callStore.status === 'ringing'
})

const canOperate = computed(() => {
  return ['calling', 'ringing', 'connecting', 'connected'].includes(callStore.status)
})

const callStatusText = computed(() => {
  switch (callStore.status) {
    case 'idle':
      return '准备通话'
    case 'calling':
      return callStore.direction === 'outgoing' ? '正在呼叫...' : '来电中...'
    case 'ringing':
      return callStore.direction === 'outgoing' ? '等待对方接听...' : '邀请你进行语音通话'
    case 'connecting':
      return '正在连接...'
    case 'connected':
      return '通话中'
    case 'ended':
      return '通话已结束'
    case 'failed':
      return callStore.errorMsg || '通话失败'
    default:
      return '语音通话'
  }
})

onMounted(() => {
  startDurationTimer()
  bindRemoteStream()
  tryStartOutgoingWebrtc()
})

onBeforeUnmount(() => {
  stopDurationTimer()
})

watch(
  () => [callStore.callId, callStore.direction, callStore.status],
  () => {
    tryStartOutgoingWebrtc()
  },
  { immediate: true },
)

watch(
  () => callStore.remoteStream,
  async () => {
    await nextTick()
    bindRemoteStream()
  },
)

watch(
  () => callStore.status,
  (status) => {
    if (status === 'connected') {
      startDurationTimer()
    }

    if (status === 'ended' || status === 'failed' || status === 'idle') {
      stopDurationTimer()
    }
  },
)

function tryStartOutgoingWebrtc() {
  if (outgoingStarted) return
  if (!callStore.callId) return
  if (callStore.direction !== 'outgoing') return
  if (callStore.status !== 'calling') return

  outgoingStarted = true
  callStore.startOutgoingWebrtcCall()
}

function startDurationTimer() {
  if (durationTimer) return

  durationTimer = window.setInterval(() => {
    callStore.tickDuration()
  }, 1000)
}

function stopDurationTimer() {
  if (!durationTimer) return

  clearInterval(durationTimer)
  durationTimer = null
}

function bindRemoteStream() {
  const audio = remoteAudioRef.value
  if (!audio) return

  if (callStore.remoteStream) {
    audio.srcObject = callStore.remoteStream
    audio.play?.().catch(() => {})
  } else {
    audio.srcObject = null
  }
}

async function acceptCall() {
  await callStore.acceptIncomingCall()
}

async function rejectCall() {
  await callStore.rejectIncomingCall()
  await window.electronAPI?.closeAudioCallWindow?.()
}

function toggleMute() {
  if (!canOperate.value) return
  callStore.toggleMute()
}

function toggleSpeaker() {
  if (!canOperate.value) return
  callStore.toggleSpeaker()
}

async function hangup() {
  await callStore.handleHangup(true)
  await window.electronAPI?.closeAudioCallWindow?.()
}

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('data:') || url.startsWith('blob:')) return url

  const baseUrl = import.meta.VITE_SOURCES_URL

  if (url.startsWith('/')) {
    return baseUrl + url
  }

  return `${baseUrl}/${url}`
}
</script>

<style scoped>
.audio-call-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  box-sizing: border-box;
  background: linear-gradient(180deg, #f7fbff 0%, #ffffff 100%);
  display: flex;
  flex-direction: column;
  user-select: none;
}

.call-main {
  flex: 1;
  min-height: 0;
  padding: 58px 28px 20px;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  align-items: center;
}

.peer-avatar {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.16);
}

.peer-name {
  margin-top: 22px;
  max-width: 280px;
  font-size: 22px;
  font-weight: 600;
  color: #222;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.call-status {
  margin-top: 10px;
  font-size: 14px;
  color: #888;
}

.call-duration {
  margin-top: 12px;
  font-size: 18px;
  color: #333;
  letter-spacing: 1px;
}

.call-actions {
  height: 116px;
  padding: 0 22px 28px;
  box-sizing: border-box;

  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  align-items: end;
  justify-items: center;
  gap: 12px;
}

.action-btn {
  width: 74px;
  height: 74px;
  border: none;
  border-radius: 50%;
  background: #f1f3f5;
  color: #333;
  cursor: pointer;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.action-btn:hover:not(:disabled) {
  background: #e9ecef;
}

.action-btn.active {
  background: #dbeafe;
  color: #1677ff;
}

.action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.action-icon {
  font-size: 22px;
  line-height: 1;
}

.action-text {
  font-size: 12px;
}

.hangup-btn {
  width: 86px;
  height: 86px;
  border: none;
  border-radius: 50%;
  background: #f5222d;
  color: #fff;
  cursor: pointer;

  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(245, 34, 45, 0.28);
}

.hangup-btn:hover {
  background: #cf1322;
}

.hangup-icon {
  font-size: 30px;
  line-height: 1;
  transform: rotate(135deg);
}
.incoming-actions {
  height: 116px;
  padding: 0 46px 28px;
  box-sizing: border-box;

  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: end;
  justify-items: center;
  gap: 34px;
}

.reject-btn,
.accept-btn {
  width: 82px;
  height: 82px;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.16);
}

.reject-btn {
  background: #f5222d;
}

.reject-btn:hover {
  background: #cf1322;
}

.accept-btn {
  background: #22c55e;
}

.accept-btn:hover {
  background: #16a34a;
}

/* 顶部透明拖动区域 */
.drag-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 46px;
  z-index: 50;
  -webkit-app-region: drag;
}

.call-main,
.call-actions,
.incoming-actions {
  position: relative;
  z-index: 2;
}

button {
  -webkit-app-region: no-drag;
}
</style>
