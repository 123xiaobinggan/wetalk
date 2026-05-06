import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '/src/stores/userStore.js'
import { useFriendStore } from '/src/stores/friendStore.js'
import { useGroupStore } from '/src/stores/groupStore.js'
import { useConversationStore } from '/src/stores/conversationStore.js'
export default function useFriendsList() {
  const userStore = useUserStore()
  const friendStore = useFriendStore()
  const groupStore = useGroupStore()
  const conversationStore = useConversationStore()
  const router = useRouter()

  const friendRequests = computed(() => {
    const requests = friendStore.getFriendRequests
    return Object.fromEntries(
      Object.entries(requests).map(([id, request]) => {
        const userId =
          request.requesterUserId == userStore.userInfo.userId
            ? request.requesteeUserId
            : request.requesterUserId
        console.log('friendRequests', userId)
        const user = userStore.getUser(userId)
        return [
          id,
          {
            ...request,
            avatar: user?.avatar,
            accountId: user?.accountId,
          },
        ]
      }),
    )
  })

  const groups = computed(() => {
    return Object.keys(groupStore.getGroups)
      .filter((groupId) => String(groupStore.getGroup(groupId)?.status) !== '1')
      .map((groupId) => {
        const conv = conversationStore.getConversations.filter(
          (c) => String(c.groupId) === String(groupId),
        )[0]
        return {
          ...groupStore.getGroup(groupId),
          groupRemark: conv?.groupRemark,
        }
      })
  })

  const friends = computed(() =>
    Object.keys(friendStore.friendships)
      .filter(
        (friendUserId) =>
          friendStore.friendships[friendUserId].deleted !== true &&
          friendStore.friendships[friendUserId].blocked === false,
      )
      .map((friendUserId) => {
        console.log('friends', friendUserId)
        const { userId, ...friendUser } = userStore.getUser(friendUserId) || {}
        return {
          friendUserId,
          ...friendStore.friendships[friendUserId],
          ...friendUser,
        }
      }),
  )

  function clickFriendRequest(friendRequest) {
    console.log('clickFriendRequest', friendRequest)
    friendStore.openFriendRequestDetail(friendRequest.friendRequestId)

    router.push('/home/friends/friendsList/friendRequestDetail')
    console.log('router', router.currentRoute.value.fullPath)
  }

  function clickGroup(group) {
    console.log('clickGroup', group)
    groupStore.openGroup(group.groupId)
    router.push('/home/friends/friendsList/groupDetail')
  }

  function clickContact(contact) {
    console.log('clickContact', contact)
    friendStore.openFriendDetail(contact.friendUserId)

    router.push('/home/friends/friendsList/friendDetail')
    console.log('router', router.currentRoute.value.fullPath)
  }

  function handleAccept(friendRequest) {
    console.log('handleAccept', friendRequest)
    friendStore.acceptFriendRequest(friendRequest.friendRequestId)
  }

  return {
    friendRequests,
    groups,
    friends,
    clickFriendRequest,
    clickGroup,
    clickContact,
    handleAccept,
  }
}
