// src/stores/callStore.js
import { defineStore } from 'pinia'
import { markRaw } from 'vue'
import { useToastStore } from './toastStore'

const ICE_CONFIG = {
  iceServers: [
    {
      urls: ['stun:stun.l.google.com:19302'],
    },
  ],
}

function buildCallId() {
  return `call_${Date.now()}_${Math.random().toString(16).slice(2)}`
}

function toPlainIceCandidate(candidate) {
  if (!candidate) return null

  if (typeof candidate.toJSON === 'function') {
    return candidate.toJSON()
  }

  return {
    candidate: candidate.candidate,
    sdpMid: candidate.sdpMid,
    sdpMLineIndex: candidate.sdpMLineIndex,
    usernameFragment: candidate.usernameFragment,
  }
}

function toPlainSessionDescription(desc) {
  if (!desc) return null

  return {
    type: desc.type,
    sdp: desc.sdp,
  }
}

export const useCallStore = defineStore('call', {
  state: () => ({
    callId: null,
    callType: 'audio',
    direction: 'outgoing',
    status: 'idle',

    convId: null,
    sessionId: null,

    selfId: null,
    selfName: '',
    selfAvatar: '',

    peerId: null,
    peerName: '',
    peerAvatar: '',

    startedAt: null,
    connectedAtMs: null,
    duration: 0,

    muted: false,
    speakerOn: false,

    localStream: null,
    remoteStream: null,
    peerConnection: null,

    remoteOffer: null,
    pendingRemoteCandidates: [],
    errorMsg: '',
  }),

  getters: {
    formattedDuration(state) {
      const safeDuration = Math.max(0, Number(state.duration) || 0)

      const min = Math.floor(safeDuration / 60)
      const sec = safeDuration % 60

      return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
    },

    isCalling(state) {
      return ['calling', 'ringing', 'connecting', 'connected'].includes(state.status)
    },
  },

  actions: {
    // 初始化
    startOutgoingCall(payload) {
      this.cleanupRtcOnly()

      this.callId = payload.callId || buildCallId()
      this.callType = payload.callType || 'audio'
      this.direction = 'outgoing'
      this.status = 'calling'

      this.convId = payload.convId
      this.sessionId = payload.sessionId

      this.selfId = payload.selfId
      this.selfName = payload.selfName || ''
      this.selfAvatar = payload.selfAvatar || ''

      this.peerId = payload.peerId
      this.peerName = payload.peerName || ''
      this.peerAvatar = payload.peerAvatar || ''

      this.startedAt = null
      this.connectedAtMs = null
      this.duration = 0
      this.muted = false
      this.speakerOn = false
      this.remoteOffer = null
      this.pendingRemoteCandidates = []
      this.errorMsg = ''
    },

    // 有通话进入
    startIncomingCall(payload) {
      this.cleanupRtcOnly()

      this.callId = payload.callId
      this.callType = payload.callType || 'audio'
      this.direction = 'incoming'
      this.status = 'ringing'

      this.convId = payload.convId || null
      this.sessionId = payload.sessionId

      this.selfId = payload.selfId || payload.toUserId
      this.selfName = payload.selfName || ''
      this.selfAvatar = payload.selfAvatar || ''

      this.peerId = payload.peerId || payload.fromUserId
      this.peerName = payload.remark || payload.username || `用户${this.peerId}`
      this.peerAvatar = payload.avatar || ''

      this.remoteOffer = payload.p2pData?.offer || payload.offer || payload.remoteOffer || null

      this.startedAt = null
      this.connectedAtMs = null
      this.duration = 0
      this.muted = false
      this.speakerOn = false
      this.pendingRemoteCandidates = []
      this.errorMsg = ''
    },

    // 启动外呼
    async startOutgoingWebrtcCall() {
      if (!this.callId || this.direction !== 'outgoing') return
      if (this.peerConnection) return

      try {
        this.status = 'connecting'

        await this.prepareLocalAudio()
        this.createPeerConnection()

        const offer = await this.peerConnection.createOffer({
          offerToReceiveAudio: true,
          offerToReceiveVideo: false,
        })

        await this.peerConnection.setLocalDescription(offer)

        await this.sendCallSignal('invite', {
          p2pData: {
            offer,
          },
        })

        this.status = 'ringing'
      } catch (e) {
        console.error('startOutgoingWebrtcCall error:', e)
        this.errorMsg = e.message || '发起通话失败'
        this.status = 'failed'
        this.cleanupRtcOnly()
      }
    },

    // 接听
    async acceptIncomingCall() {
      if (!this.callId || this.direction !== 'incoming') return

      if (!this.remoteOffer) {
        this.status = 'failed'
        this.errorMsg = '缺少 offer，无法接听'
        return
      }

      try {
        this.status = 'connecting'

        await this.prepareLocalAudio()
        this.createPeerConnection()

        await this.peerConnection.setRemoteDescription(new RTCSessionDescription(this.remoteOffer))

        await this.flushPendingRemoteCandidates()

        const answer = await this.peerConnection.createAnswer()
        await this.peerConnection.setLocalDescription(answer)

        await this.sendCallSignal('accept', {
          p2pData: {
            answer: toPlainSessionDescription(answer),
          },
        })
      } catch (e) {
        console.error('acceptIncomingCall error:', e)
        this.errorMsg = e.message || '接听失败'
        this.status = 'failed'

        await this.sendCallSignal('reject', {
          p2pData: {
            reason: this.errorMsg,
          },
        })

        this.cleanupRtcOnly()
      }
    },

    // 拒绝
    async rejectIncomingCall() {
      if (!this.callId) return

      await this.sendCallSignal('reject', {
        p2pData: {
          reason: '对方已拒绝',
        },
      })
      await this.sendCallSignal('hangup', {})

      this.endCall()
    },

    // 发起的通话被对方接听
    async handleAccept(data) {
      if (!this.isCurrentCall(data)) return
      if (!this.peerConnection) return

      const answer = data.p2pData?.answer || data.answer

      if (!answer) {
        console.warn('handleAccept 缺少 answer:', data)
        return
      }

      try {
        await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer))

        await this.flushPendingRemoteCandidates()
        this.status = 'connecting'
      } catch (e) {
        console.error('handleAccept error:', e)
        this.errorMsg = e.message || '处理 answer 失败'
        this.status = 'failed'
      }
    },

    // 发起的通话被对方拒绝
    handleRejected(data) {
      if (!this.isCurrentCall(data)) return

      this.errorMsg = data.p2pData?.reason || data.reason || '对方已拒绝'
      this.status = 'ended'
      this.cleanupRtcOnly()

      const toastStore = useToastStore()
      toastStore.success(this.errorMsg)

      setTimeout(() => {
        this.endCall()
      }, 1500)
    },

    async prepareLocalAudio() {
      if (this.localStream) return

      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
        video: false,
      })

      this.localStream = markRaw(stream)

      if (this.muted) {
        this.localStream.getAudioTracks().forEach((track) => {
          track.enabled = false
        })
      }
    },

    createPeerConnection() {
      if (this.peerConnection) return

      const pc = new RTCPeerConnection(ICE_CONFIG)

      pc.onicecandidate = async (event) => {
        if (!event.candidate) return

        const candidate = toPlainIceCandidate(event.candidate)

        console.log('send plain candidate:', candidate)

        await this.sendCallSignal('candidate', {
          p2pData: {
            candidate,
          },
        })
      }

      pc.ontrack = (event) => {
        const stream = event.streams?.[0]
        if (stream) {
          this.remoteStream = markRaw(stream)
        }
      }

      pc.oniceconnectionstatechange = () => {
        console.log('pc iceConnectionState:', pc.iceConnectionState)

        if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
          this.setConnected()
        }

        if (pc.iceConnectionState === 'failed') {
          this.status = 'failed'
          this.errorMsg = 'ICE 连接失败'
        }
      }

      pc.onicegatheringstatechange = () => {
        console.log('pc iceGatheringState:', pc.iceGatheringState)
      }

      pc.onsignalingstatechange = () => {
        console.log('pc signalingState:', pc.signalingState)
      }

      pc.onconnectionstatechange = () => {
        console.log('pc connectionState:', pc.connectionState)

        if (pc.connectionState === 'connected') {
          this.setConnected()
        }

        if (pc.connectionState === 'failed') {
          this.status = 'failed'
          this.errorMsg = 'P2P 连接失败'
        }

        if (pc.connectionState === 'disconnected') {
          if (this.status === 'connected') {
            this.status = 'connecting'
          }
        }

        if (pc.connectionState === 'closed') {
          if (this.status !== 'ended') {
            this.status = 'ended'
          }
        }
      }

      if (this.localStream) {
        this.localStream.getTracks().forEach((track) => {
          pc.addTrack(track, this.localStream)
        })
      }

      this.peerConnection = markRaw(pc)
    },

    async flushPendingRemoteCandidates() {
      if (!this.peerConnection || !this.peerConnection.remoteDescription) return
      if (!this.pendingRemoteCandidates.length) return

      const candidates = [...this.pendingRemoteCandidates]
      this.pendingRemoteCandidates = []

      for (const candidate of candidates) {
        try {
          await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
        } catch (e) {
          console.warn('flush candidate failed:', e)
        }
      }
    },

    setConnected() {
      if (this.status === 'connected' && this.connectedAtMs != null) {
        return
      }

      this.status = 'connected'

      // startedAt 可以保留给日志、后端上报或显示真实时间
      this.startedAt = Date.now()

      // connectedAtMs 专门用于本地计时，避免系统时间变化导致负数
      this.connectedAtMs = performance.now()

      this.duration = 0
    },

    tickDuration() {
      if (this.status !== 'connected') return
      if (this.connectedAtMs == null) return

      const seconds = Math.floor((performance.now() - this.connectedAtMs) / 1000)

      this.duration = Math.max(0, seconds)
    },

    // 静音
    toggleMute() {
      this.muted = !this.muted

      if (this.localStream) {
        this.localStream.getAudioTracks().forEach((track) => {
          track.enabled = !this.muted
        })
      }
    },

    // 扬声器
    toggleSpeaker() {
      this.speakerOn = !this.speakerOn
    },

    setLocalStream(stream) {
      this.localStream = stream ? markRaw(stream) : null
    },

    setRemoteStream(stream) {
      this.remoteStream = stream ? markRaw(stream) : null
    },

    setPeerConnection(pc) {
      this.peerConnection = pc ? markRaw(pc) : null
    },

    async handleCandidate(data) {
      if (!this.isCurrentCall(data)) return

      const candidate = data.p2pData?.candidate || data.candidate

      if (!candidate) {
        console.warn('handleCandidate 缺少 candidate:', data)
        return
      }

      console.log('handleCandidate candidate:', candidate)

      if (!this.peerConnection || !this.peerConnection.remoteDescription) {
        console.log('peerConnection 或 remoteDescription 未准备好，先缓存 candidate')
        this.pendingRemoteCandidates.push(candidate)
        return
      }

      try {
        await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
        console.log('addIceCandidate success')
      } catch (e) {
        console.warn('addIceCandidate failed:', e)
      }
    },

    // 我方挂断
    async handleHangup(needNotify = true) {
      if (needNotify && this.callId && this.peerId) {
        await this.sendCallSignal('hangup', {})
      }

      this.endCall()
    },

    async endCall() {
      this.status = 'ended'
      this.cleanupRtcOnly()
      const toastStore = useToastStore()
      toastStore.success('通话结束')
      setTimeout(async () => {
        await window.electronAPI?.closeAudioCallWindow?.()
      }, 1500)
    },

    async cleanupRtcOnly() {
      if (this.peerConnection) {
        this.peerConnection.close()
      }

      if (this.localStream) {
        this.localStream.getTracks().forEach((track) => track.stop())
      }

      if (this.remoteStream) {
        this.remoteStream.getTracks?.().forEach((track) => track.stop())
      }

      this.peerConnection = null
      this.localStream = null
      this.remoteStream = null
      this.remoteOffer = null
      this.pendingRemoteCandidates = []
    },

    resetCall() {
      this.cleanupRtcOnly()

      this.callId = null
      this.callType = 'audio'
      this.direction = 'outgoing'
      this.status = 'idle'

      this.convId = null
      this.sessionId = null

      this.selfId = null
      this.selfName = ''
      this.selfAvatar = ''

      this.peerId = null
      this.peerName = ''
      this.peerAvatar = ''

      this.startedAt = null
      this.duration = 0

      this.muted = false
      this.speakerOn = false
      this.errorMsg = ''
    },

    isCurrentCall(data = {}) {
      return data.callId && this.callId && String(data.callId) === String(this.callId)
    },

    async sendCallSignal(event, extra = {}) {
      const wsEvent = this.buildWsCallEvent(event)

      const payload = {
        type: 'call',
        event: wsEvent,
        data: {
          ...this.buildBaseSignal(),
          ...extra,
        },
      }

      console.log('send call signal:', payload)

      await window.electronAPI?.wsSend?.(payload)
    },

    buildBaseSignal() {
      return {
        callId: this.callId,
        callType: this.callType,

        fromUserId: this.selfId,
        toUserId: this.peerId,

        convType: false,
        sessionId: this.sessionId,
      }
    },

    buildWsCallEvent(event) {
      const prefix = this.callType === 'video' ? 'video' : 'audio'

      const map = {
        invite: `${prefix}_call`,
        accept: `${prefix}_accept`,
        reject: `${prefix}_reject`,
        candidate: `${prefix}_candidate`,
        hangup: `${prefix}_end`,
        busy: `${prefix}_busy`,
      }

      return map[event] || event
    },
  },
})
