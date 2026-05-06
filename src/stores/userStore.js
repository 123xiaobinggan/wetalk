import { defineStore } from 'pinia'
import { useConversationStore } from './conversationStore.js'
import { useToastStore } from './toastStore.js'
import request from '/src/api/request.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: {
      userId: 1,
      accountId: 'mm',
      username: 'kk',
      avatar: '',
      sex: 1,
      areaName: 'zz',
      areaCode: 'xx',
      personalSignature: '',
    },
    token: '',
    isLogin: false,
    url: import.meta.env.VITE_BASE_URL,
    uploadUrl: import.meta.env.VITE_SOURCES_URL,
    inited: false,
    defaultAvatar: '',
    // 存放所有users数据
    users: {},
    fetchingUsers: {},
  }),

  getters: {
    getUser(state) {
      return (userId) => {
        if (userId === state.userInfo.userId) {
          return state.userInfo
        } else if (state.users[userId]) {
          return state.users[userId]
        } else {
          this.fetchUsers([userId])
        }
        return null
      }
    },
  },

  actions: {
    async init() {
      if (this.inited) {
        return
      }
      this.inited = true

      const auth = await window.electronAPI.authGet()

      console.log('auth', auth)
      if (auth?.userInfo) {
        this.setUserInfo(auth.userInfo)
      }
      if (auth?.token) {
        this.setToken(auth.token)
      }
    },

    setUserInfo(userInfo) {
      console.log('setUserInfo', userInfo)
      Object.assign(this.userInfo, userInfo)
      this.users[userInfo.userId] = userInfo
      this.isLogin = true
      console.log('this.userInfo', this.userInfo)
    },

    setToken(token) {
      this.token = token
    },

    isOther(accountId) {
      return this.userInfo.accountId !== accountId
    },

    async fetchUsers(ids) {
      ids = ids.filter((id) => {
        if (id == null || id in this.fetchingUsers) {
          return false
        }
        this.fetchingUsers[id] = true
        return true
      })
      if (ids.length === 0) {
        return
      }
      console.log('fetchUsers', ids)
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchUsers', { userIds: ids })
        if (res.code === 0) {
          res.data.users.forEach((user) => {
            this.users[user.userId] = user
          })
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        toastStore.error('获取用户信息失败')
        console.log(e)
      } finally {
        ids.forEach((id) => {
          delete this.fetchingUsers[id]
        })
      }
    },


    /* ws 更新 */
    
    userUpdate(user) {
      this.users[user.userId] = user
    },
  },
})
