import { defineStore } from 'pinia'
import { useFriendStore } from './friendStore'
import { useUserStore } from './userStore'
import { useToastStore } from './toastStore'
import request from '/src/api/request'

export const useAddFriendModalStore = defineStore('addFriendModal', {
  state: () => ({
    visible: false,
    user: null,
  }),

  actions: {
    open(user) {
      console.log('open', user)
      this.user = user
      this.visible = true
    },

    close() {
      this.visible = false
      this.user = null
    },

    async submit(payload) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      const friendStore = useFriendStore()
      try {
        payload.requesterUserId = userStore.userInfo.userId
        friendStore.sendFriendRequestSync(payload)
        const res = await request.post('sendFriendRequest', payload)
        console.log('sendFriendRequest', res)
        if (res.code === 0) {
          toastStore.success(res.msg)
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
      } finally {
        this.close()
      }
    },
  },
})
