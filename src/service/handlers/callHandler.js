import { useCallStore } from '/src/stores/callStore.js'
import { useUserStore } from '/src/stores/userStore.js'
import { useConversationStore } from '/src/stores/conversationStore.js'
export function callHandler(message) {
  const event = message.event
  const data = message.data || {}

  switch (event) {
    case 'audio_call':
      handleAudioCall(data)
      break

    case 'audio_reject':
      handleAudioRejected(message)
      break

    case 'audio_accept':
      handleAudioAccept(data)
      break

    case 'audio_candidate':
      handleAudioCandidate(data)
      break

    case 'audio_end':
      handleAudioEnd(data)
      break

    case 'audio_busy':
      handleAudioBusy(data)
      break

    case 'video_call':
      handleVideoCall(data)
      break

    case 'video_reject':
      handleVideoReject(data)
      break

    case 'video_accept':
      handleVideoAccept(data)
      break

    case 'video_candidate':
      handleVideoCandidate(data)
      break

    case 'video_end':
      handleVideoEnd(data)
      break

    case 'video_busy':
      handleVideoBusy(data)
      break

    default:
      console.log('unknown call event:', event, data)
      break
  }
}

/**
 * 对方发起语音通话
 * data 中应该携带：
 * callId, callType, convId, sessionId,
 * fromUserId, toUserId,
 * offer,
 * fromName, fromAvatar
 */
async function handleAudioCall(data) {
  console.log('handleAudioCall', data)

  const userStore = useUserStore()
  const callStore = useCallStore()

  const selfId = userStore.userInfo.userId

  if (Number(data.toUserId) !== Number(selfId)) {
    return
  }

  // 当前窗口已经在通话中，返回 busy
  if (callStore.isCalling) {
    await sendAudioBusy(data)
    return
  }
  const offer = data.p2pData?.offer

  if (!offer) {
    console.warn('audio_call 缺少 offer:', data)
    return
  }

  const payload = {
    ...data,

    callType: 'audio',
    direction: 'incoming',
    status: 'ringing',

    selfId,
    selfName: userStore.userInfo.username,
    selfAvatar: userStore.userInfo.avatar,

    peerId: data.fromUserId,
    peerName: data.fromName || data.peerName || `用户${data.fromUserId}`,
    peerAvatar: data.fromAvatar || data.peerAvatar || '',

    offer,
    remoteOffer: offer,
  }

  // 打开语音通话窗口。
  // audioCall 窗口收到 call:payload 后，你的 listening.js 会调用 callStore.startIncomingCall(payload)
  const res = await window.electronAPI?.startAudioCall?.(payload)

  // main 里如果返回 code=1，说明已经有通话窗口，通知对方忙线
  if (res?.code === 1) {
    await sendAudioBusy(data)
  }
}

function handleAudioRejected(message) {
  console.log('handleAudioReject', message)

  const callStore = useCallStore()

  if (!isCurrentCall(callStore, message.data)) return

  callStore.handleRejected({
    ...message.data,
    reason: message.data.reason || message.msg || '对方已拒绝',
  })
}

async function handleAudioAccept(data) {
  console.log('handleAudioAccept', data)

  const callStore = useCallStore()

  if (!isCurrentCall(callStore, data)) return

  await callStore.handleAccept(data)
}

async function handleAudioCandidate(data) {
  console.log('handleAudioCandidate', data)

  const callStore = useCallStore()

  console.log('candidate callId check:', {
    localCallId: callStore.callId,
    remoteCallId: data.callId,
    status: callStore.status,
    direction: callStore.direction,
    href: window.location.href,
  })

  if (!isCurrentCall(callStore, data)) {
    console.warn('candidate 不属于当前通话，已忽略')
    return
  }

  await callStore.handleCandidate(data)
}

function handleAudioEnd(data) {
  console.log('handleAudioEnd', data)

  const callStore = useCallStore()
  const conversationStore = useConversationStore()

  conversationStore.receiveCallEndMessage(data)

  callStore.endCall()
}

function handleAudioBusy(data) {
  console.log('handleAudioBusy', data)

  const callStore = useCallStore()

  if (!isCurrentCall(callStore, data)) return

  callStore.handleRejected({
    ...data,
    reason: data.p2pData?.reason || '对方忙线中',
  })
}

/**
 * 视频通话先保留壳，后面接视频时再复用同样结构。
 */
function handleVideoCall(data) {
  console.log('handleVideoCall', data)
}

function handleVideoReject(data) {
  console.log('handleVideoReject', data)
}

function handleVideoAccept(data) {
  console.log('handleVideoAccept', data)
}

function handleVideoCandidate(data) {
  console.log('handleVideoCandidate', data)
}

function handleVideoEnd(data) {
  console.log('handleVideoEnd', data)
}

function handleVideoBusy(data) {
  console.log('handleVideoBusy', data)
}

function isCurrentCall(callStore, data) {
  if (!data?.callId) return false
  if (!callStore.callId) return false
  return String(data.callId) === String(callStore.callId)
}

async function sendAudioBusy(data) {
  await window.electronAPI?.wsSend?.({
    type: 'call',
    event: 'audio_busy',
    clientMsgId: `${data.callId}_busy_${Date.now()}`,
    data: {
      callId: data.callId,
      callType: 'audio',

      fromUserId: data.toUserId,
      toUserId: data.fromUserId,

      convType: data.convType ?? false,
      sessionId: data.sessionId,

      p2pData: {
        reason: '对方忙线中',
      },
    },
  })
}
