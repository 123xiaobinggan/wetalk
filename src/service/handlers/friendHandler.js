import { useFriendStore } from '/src/stores/friendStore.js'
import { useUserStore } from '/src/stores/userStore.js'
import { useConversationStore } from '/src/stores/conversationStore.js'
import { useToastStore } from '/src/stores/toastStore.js'
export function friendHandler(message) {
  switch (message.event) {
    case 'request_send':
      handleRequestSend(message)
      break
    case 'request_accept':
      handleRequestAccept(message)
      break
    case 'friend_update':
      handleFriendUpdate(message)
      break
    default:
      console.warn('未知的事件类型:', message.event)
      break
  }
}

function handleRequestSend(message) {
  const friendStore = useFriendStore()
  friendStore.receiveFriendRequest(message.data)
}

function handleRequestAccept(message) {
  const toastStore = useToastStore()
  const friendStore = useFriendStore()
  const conversationStore = useConversationStore()
  toastStore.success(message.msg)
  friendStore.applyAcceptedFriendship(message.data.friendship, message.data.friendRequestId)
  conversationStore.upsertConversation(message.data.conversation)
}

function handleFriendUpdate(message) {
  const userStore = useUserStore()
  userStore.userUpdate(message.data.user)
}
