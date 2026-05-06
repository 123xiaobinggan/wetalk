import { ref, computed } from 'vue'
import { useFriendStore } from '/src/stores/friendStore'
import { useUserStore } from '/src/stores/userStore'
import { useConversationStore } from '/src/stores/conversationStore'
import { confirm } from '/src/utils/confirm.js'
import { useCallStore } from '/src/stores/callStore'
import { useRouter } from 'vue-router'
export default function useFriendDetail() {
  const friendStore = useFriendStore()
  const router = useRouter()

  const friend = computed(() => friendStore.activeFriendDetail())

  // 事件处理
  const handleSendMessage = async (friend) => {
    console.log('Send message to', friend)
    const conversationStore = useConversationStore()
    await conversationStore.getConversation(false, friend.friendUserId, null)
    var convId = null
    for (const key of Object.keys(conversationStore.conversationsMap)) {
      const conv = conversationStore.conversationsMap[key]
      if (conv.convType === false && conv.peerId === friend.friendUserId) {
        convId = conv.convId
        break
      }
    }
    console.log('convId', convId)
    if (convId == null) return
    conversationStore.openConversation(convId)
    router.push('/home/talk/recentConversation/talkwindow')
  }

  const handleAudioCall = async (user) => {
    console.log('Audio call', user)
    const userStore = useUserStore()
    const conversationStore = useConversationStore()
    await conversationStore.getConversation(false, user.friendUserId, null)

    const conv = conversationStore.getConversations.find((conv) => {
      return conv.convType === false && conv.peerId === user.friendUserId
    })

    const peerUser = userStore.getUser(conv.peerId)
    const friendship = friendStore.gettersFriendship(conv.peerId)

    const payload = {
      callType: 'audio',
      convId: conv.convId,
      sessionId: conv.sessionId,
      peerId: conv.peerId,
      peerName: friendship?.remark || peerUser?.username || `用户${conv.peerId}`,
      peerAvatar: peerUser?.avatar || '',
      selfId: userStore.userInfo.userId,
      selfName: userStore.userInfo.username,
      selfAvatar: userStore.userInfo.avatar,
    }

    const res = await window.electronAPI.startAudioCall(payload)
    if (res.code !== 0) {
      toastStore.error(res.msg)
      return
    }
  }

  const handleVideoCall = (user) => {
    console.log('Video call', user.username)
  }

  const handleUpdateRemark = async ({ friendUserId, remark }) => {
    const friendship = friendStore.gettersFriendship(friendUserId)
    if (friendship?.remark === remark) return
    const userStore = useUserStore()
    await friendStore.updateFriendship({
      userId: userStore.userInfo.userId,
      friendUserId,
      remark,
    })
  }

  const handleUpdatePermissions = async (payload) => {
    const friendship = friendStore.gettersFriendship(payload.friendUserId)
    if (
      friendship.hideMyMoments === payload.hideMyMoments &&
      friendship.hideFriendMoments === payload.hideFriendMoments
    )
      return
    const userStore = useUserStore()
    await friendStore.updateFriendship({
      userId: userStore.userInfo.userId,
      friendUserId: payload.friendUserId,
      hideMyMoments: payload.hideMyMoments,
      hideFriendMoments: payload.hideFriendMoments,
    })
  }

  const handleBlockFriend = async (friendUserId) => {
    const sure = await confirm('确定要拉黑好友吗?', {
      title: '拉黑',
      confirmText: '删除',
      cancelText: '取消',
      danger: true,
    })
    if (!sure) {
      return
    } else {
      await friendStore.blockFriend(friendUserId)
      router.back()
    }
  }

  const handleDeleteFriend = async (friendUserId) => {
    const sure = await confirm('确定要删除好友吗?', {
      title: '删除好友',
      confirmText: '删除',
      cancelText: '取消',
      danger: true,
    })
    if (!sure) {
      return
    } else {
      await friendStore.deleteFriend(friendUserId)
      router.back() // 删除后返回
    }
  }

  return {
    friend,
    handleSendMessage,
    handleAudioCall,
    handleVideoCall,
    handleUpdateRemark,
    handleUpdatePermissions,
    handleBlockFriend,
    handleDeleteFriend,
  }
}
