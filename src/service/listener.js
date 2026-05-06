import { dispatchWsMessage } from './wsDispatcher.js'
import { useWsStatusStore } from '/src/stores/wsStatusStore.js'
import { useUserStore } from '/src/stores/userStore.js'
import { useFriendStore } from '/src/stores/friendStore.js'
import { useCallStore } from '/src/stores/callStore.js'
export function initListener() {
  const userStore = useUserStore()
  const friendStore = useFriendStore()
  const callStore = useCallStore()
  window.electronAPI.onAuthUpdated(async () => {
    const data = await window.electronAPI.authGet()
    userStore.setUserInfo(data.userInfo)
    console.log('auth updated', data)
  })

  window.electronAPI.onWsStatus((data) => {
    console.log('listener: wsStatus', data)
    const wsStatusStore = useWsStatusStore()
    wsStatusStore.setStatus(data.status)
  })

  window.electronAPI.onWsReceive((data) => {
    console.log('listener: ws receive', data)
    dispatchWsMessage(data)
  })

  window.electronAPI.onFriendRequestSend((friendRequest) => {
    console.log('listener: friendRequestSend', friendRequest)
    friendStore.sendFriendRequestSync(friendRequest)
  })

  window.electronAPI.onAudioCallPayload((data) => {
    console.log('listener: voiceCallPayload', data)
    if (data.direction === 'incoming') {
      callStore.startIncomingCall(data)
    } else {
      callStore.startOutgoingCall(data)
    }
  })

  window.electronAPI.onAudioCallWindowCloseRequest(async () => {
    console.log('listener: audio window close request')

    const callStore = useCallStore()

    if (
      callStore.status === 'connected' ||
      callStore.status === 'connecting' ||
      callStore.status === 'ringing'
    ) {
      await callStore.handleHangup(true)
      return
    }

    callStore.cleanupRtcOnly?.()
    await window.electronAPI.closeAudioCallWindow()
  })
}
