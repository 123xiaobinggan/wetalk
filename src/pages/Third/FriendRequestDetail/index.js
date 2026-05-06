import { ref, computed } from 'vue'
import { useFriendStore } from '/src/stores/friendStore.js'
import { useUserStore } from '/src/stores/userStore.js'
export default function useFriendRequestDetail() {
  const friendStore = useFriendStore()
  const userStore = useUserStore()

  const friendRequest = computed(() => {
    const request = friendStore.activeFriendRequestDetail
    if(!request) return null
    const detail = {
      requesterUserId: request.requesterUserId,
      userId:
        request.requesterUserId == userStore.userInfo.userId
          ? request.requesteeUserId
          : request.requesterUserId,
      status: request.status,
      requestMsg: request.requestMsg,
      hideMyMoments: request.hideMyMoments,
      hideFriendMoments: request.hideFriendMoments,
      createdTime: request.createdTime,
      friendRequestId: request.friendRequestId,
    }
    const user = userStore.getUser(detail.userId) || {}
    console.log('friendRequest', detail, user)
    return {
      ...detail,
      ...user,
    }
  })

  const handleAccept = (payload) => {
    console.log('handleAccept', payload)
    friendStore.acceptFriendRequest(payload)
  }

  const handleBlock = (friendRequestId) => {
    console.log('handleBlock', friendRequestId)
    friendStore.blockFriend(friendRequestId)
  }

  return {
    friendRequest,
    handleAccept,
    handleBlock,
  }
}
