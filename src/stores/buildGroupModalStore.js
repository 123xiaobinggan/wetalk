import { defineStore } from 'pinia'
import { useGroupStore } from './groupStore.js'
import { useUserStore } from './userStore.js'
import { useFriendStore } from './friendStore.js'
import { useToastStore } from './toastStore.js'
import request  from '/src/api/request.js'

export const useBuildGroupModalStore = defineStore('buildGroupModal', {
  state: () => ({
    visible: false,
  }),
  getters: {
    gettersFriends(state) {
      const friendStore = useFriendStore()
      const userStore = useUserStore()
      return Object.entries(friendStore.friendships).map(([friendUserId, info]) => {
        const user = userStore.getUser(friendUserId)
        return {
          userId: friendUserId,
          remark: info.remark || user?.username || '',
          avatar: user?.avatar || userStore.defaultAvatar,
        }
      })
    },
  },
  actions: {
    showModal() {
      this.visible = true
    },
    hideModal() {
      this.visible = false
    },
    cancel() {
      this.hideModal()
    },
    async complete(selectedUserIds) {
      const userStore = useUserStore()
      const groupStore = useGroupStore()
      const toastStore = useToastStore()

      selectedUserIds.unshift(userStore.userInfo.userId)
      if (selectedUserIds.length < 3) {
        toastStore.error('群聊人数不能小于3')
        return
      }
      const username0 = userStore.getUser(selectedUserIds[0])
      const username1 = userStore.getUser(selectedUserIds[1])
      const username2 = userStore.getUser(selectedUserIds[2])
      const groupName = username0.username + '、' + username1.username + '、' + username2.username
      try {
        const res = await request.post('buildGroup', {
          userIds: selectedUserIds,
          ownerUserId: userStore.userInfo.userId,
          groupName,
        })
        console.log('buildGroup', res)
        if (res.code !== 0) {
          toastStore.error('创建群聊失败')
          return
        }
        toastStore.success('创建群聊成功')
        this.hideModal()
      } catch (e) {
        console.log(e)
        toastStore.error('创建群聊失败')
      }
    },
  },
})
