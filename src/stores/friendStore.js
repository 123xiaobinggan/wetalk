import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { defineStore } from 'pinia'
import { useConversationStore } from './conversationStore.js'
import { useUserStore } from './userStore.js'
import request from '/src/api/request.js'
import { useToastStore } from '/src/stores/toastStore.js'

export const useFriendStore = defineStore('friend', {
  state: () => ({
    //存放联系人数据
    friendships: {}, // 好友列表
    friendRequests: {}, // 好友请求列表
    activeFriendRequestId: null,
    activeFriendUserId: null,
    fetchingFriendships: {},
  }),
  getters: {
    // 获取所有好友申请
    getFriendRequests(state) {
      return state.friendRequests
    },
    // 获取特定好友关系
    gettersFriendship(state) {
      return (friendUserId) => {
        if (state.friendships[friendUserId]) {
          return state.friendships[friendUserId]
        } else {
          this.getFriendship(friendUserId)
          return {}
        }
      }
    },
    isFriend(state) {
      return (userId) => userId in state.friendships
    },
    unread(state) {
      var unread = 0
      const userStore = useUserStore()
      Object.values(state.friendRequests).forEach((req) => {
        if (req.status === 1 && req.requesterUserId !== userStore.userInfo.userId) {
          unread++
        }
      })
      return unread
    },
    // 获取当前好友申请详情
    activeFriendRequestDetail(state) {
      return state.friendRequests[state.activeFriendRequestId]
    },
    // 获取当前好友详情(friendship + user)
    activeFriendDetail(state) {
      const userStore = useUserStore()
      return () => {
        if (!state.activeFriendUserId) return {}
        const { userId, ...user } = userStore.getUser(state.activeFriendUserId) || {}
        const friendship = state.friendships[state.activeFriendUserId] || {}
        console.log('activeFriendDetail', user, friendship)
        return {
          ...user,
          ...friendship,
        }
      }
    },
    // 获取当前好友关系(friendship)
    activeFriendshipsDetail(state) {
      if (state.friendships[state.activeFriendUserId] !== undefined) {
        return state.friendships[state.activeFriendUserId]
      } else {
        this.getFriendship(state.activeFriendUserId)
        return null
      }
    },
  },
  actions: {
    async init() {
      await Promise.all([this.fetchFriendships(), this.fetchFriendRequests()])
    },
    async fetchFriendships() {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchFriendships', { userId: userStore.userInfo.userId })
        console.log('fetchFriendships', res)
        if (res.code === 0) {
          this.friendships = {}
          res.data.friendships.map((item) => {
            this.friendships[item.friendUserId] = item
          })
        } else {
          console.log(res.msg)
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取好友列表失败')
      }
    },
    async fetchFriendRequests() {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchFriendRequests', { userId: userStore.userInfo.userId })
        console.log('fetchFriendRequests', res)
        if (res.code === 0) {
          this.friendRequests = {}
          res.data.friendRequests.map((item) => {
            this.friendRequests[item.friendRequestId] = {
              ...item,
              status: item.status,
            }
          })
        } else {
          console.log(res.msg)
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取好友请求失败')
      }
    },
    async getFriendship(friendUserId) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      if (userStore.userInfo.userId === friendUserId) return
      if (this.friendships[friendUserId]) {
        return this.friendships[friendUserId]
      }
      if (friendUserId in this.fetchingFriendships) {
        return
      }
      this.fetchingFriendships[friendUserId] = true
      try {
        const res = await request.post('getFriendship', {
          userId: userStore.userInfo.userId,
          friendUserId,
        })
        console.log('getFriendship', res)
        if (res.code === 0) {
          this.friendships[friendUserId] = res.data.friendship
        } else {
          console.log(res.msg)
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取好友关系失败')
      } finally {
        delete this.fetchingFriendships[friendUserId]
      }
    },
    sendFriendRequestSync(friendRequest) {
      this.friendRequests[friendRequest.friendRequestId] = friendRequest
    },
    openFriendRequestDetail(friendRequestId) {
      const userStore = useUserStore()
      this.activeFriendRequestId = friendRequestId
      if (
        this.friendRequests[friendRequestId].requesterUserId !== userStore.userInfo.userId &&
        this.friendRequests[friendRequestId].status === 1
      ) {
        this.readFriendRequest(friendRequestId)
      }
      console.log('openFriendRequestDetail', this.activeFriendRequestId)
    },
    openFriendDetail(friendUserId) {
      console.log('openFriendDetail', friendUserId)
      this.activeFriendUserId = friendUserId
    },
    async updateFriendship(friendship) {
      const toastStore = useToastStore()
      try {
        const res = await request.post('updateFriendship', friendship)
        console.log('updateFriendship', res)
        if (res.code === 0) {
          this.friendships[friendship.friendUserId] = {
            ...this.friendships[friendship.friendUserId],
            ...friendship,
          }
        }
      } catch (e) {
        console.log(e)
        toastStore.error('更新好友关系失败')
      }
    },
    async deleteFriend(friendUserId) {
      if (!this.isFriend(friendUserId)) {
        return
      }
      console.log('ready to deleteFriend', friendUserId)
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('deleteFriend', {
          userId: userStore.userInfo.userId,
          friendUserId,
        })
        console.log('deleteFriend', res)
        if (res.code === 0) {
          this.friendships[friendUserId].deleted = true
          toastStore.success('删除好友成功')
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('删除好友失败')
      }
    },
    async readFriendRequest(friendRequestId) {
      const toastStore = useToastStore()
      try {
        const res = await request.post('readFriendRequest', { friendRequestId })
        console.log('readFriendRequest', res)
        if (res.code === 0) {
          // 0: 已读 1: 未读
          this.friendRequests[friendRequestId].status = 0
        } else {
          const toastStore = useToastStore()
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('操作失败')
      }
    },
    async acceptFriendRequest(payload) {
      const conversationStore = useConversationStore()
      const toastStore = useToastStore()
      try {
        await request.post('acceptFriendRequest', payload)
      } catch (e) {
        console.log(e)
        toastStore.error('操作失败')
      }
    },
    async blockFriend(friendUserId) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('blockFriend', {
          userId: userStore.userInfo.userId,
          friendUserId,
        })
        console.log(res)
        if (res.code === 0) {
          toastStore.success(res.msg)
          this.friendships[friendUserId].blocked = true
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('操作失败')
      }
    },
    async unBlockFriend(friendUserId) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('unBlockFriend', {
          userId: userStore.userInfo.userId,
          friendUserId,
        })
        console.log(res)
        if (res.code === 0) {
          toastStore.success(res.msg)
          this.friendships[friendUserId].blocked = false
        }
      } catch (e) {
        console.log(e)
        toastStore.error('操作失败')
      }
    },

    /* ws处理 */
    receiveFriendRequest(request) {
      this.friendRequests[request.friendRequestId] = request
    },
    applyAcceptedFriendship(friendship, friendRequestId) {
      this.friendships[friendship.friendUserId] = friendship
      // 已同意
      if (this.friendRequests[friendRequestId]) {
        this.friendRequests[friendRequestId].status = 2
      }
    },
  },
})
