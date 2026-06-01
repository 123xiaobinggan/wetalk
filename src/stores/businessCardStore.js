import { defineStore } from 'pinia'
import { useUserStore } from './userStore'
import { useFriendStore } from './friendStore'
import { useConversationStore } from './conversationStore'
import { useAddFriendModalStore } from './addFriendModalStore'
import { useCallStore } from './callStore'
import { confirm } from '/src/utils/confirm.js'

export const useBusinessCardStore = defineStore('businessCard', {
  state: () => ({
    visible: false,
    isEditingRemark: false,
    remarkDraft: '',
    isPermissionsDialogVisible: false,
    permissionsDraft: {
      hideMyMoments: false,
      hideFriendMoments: false,
    },
    userInfo: {
      userId: 2,
      accountId: '',
      username: '',
      remark: '',
      avatar: '',
      sex: 1,
      areaName: '',
      areaCode: '',
      personalSignature: '',
      moments: [],
      commonGroups: 0,
      source: '',
      hideMyMoments: false,
      hideFriendMoments: false,
    },
    isMenuVisible: false,
    position: { x: 0, y: 0 },
    cardWidth: 255,
    cardHeight: 360,
    edgeGap: 12,
  }),
  getters: {
    isOther(state) {
      const userStore = useUserStore()
      return state.userInfo.userId !== userStore.userInfo.userId
    },

    isNotFriendOrDeleted(state) {
      if (!this.isOther) return true
      const friendStore = useFriendStore()
      friendStore.getFriendship(state.userInfo.userId)
      const friendship = friendStore.gettersFriendship(state.userInfo.userId)
      console.log('friendship', friendship)
      if (friendship) {
        console.log('friendship.deleted', friendship)
        return friendship?.deleted
      }
      return true
    },

    isBlocked(state) {
      const friendStore = useFriendStore()
      const friendship = friendStore.gettersFriendship(state.userInfo.userId)
      return friendship && friendship.blocked
    },
  },

  actions: {
    clear() {
      this.userInfo = {
        userId: 2,
        accountId: '',
        username: '',
        remark: '',
        avatar: '',
        sex: 1,
        areaName: '',
        areaCode: '',
        personalSignature: '',
        moments: [],
        commonGroups: 0,
        source: '',
        hideMyMoments: false,
        hideFriendMoments: false,
      }
    },
    async showCard(userInfo, e) {
      console.log('showCard', userInfo)
      this.clear()
      const friendStore = useFriendStore()
      const userStore = useUserStore()

      this.visible = true
      this.isMenuVisible = false
      this.isEditingRemark = false
      this.isPermissionsDialogVisible = false

      if (userInfo.userId !== userStore.userInfo.userId) {
        const friendship = friendStore.getFriendship(userInfo.userId)
        console.log('friendship', friendship)
        if (friendship) {
          userInfo.remark = friendship.remark
          userInfo.hideMyMoments = friendship.hideMyMoments
          userInfo.hideFriendMoments = friendship.hideFriendMoments
        }
      }
      Object.assign(this.userInfo, userInfo)

      let x = e.clientX
      let y = e.clientY

      const maxX = window.innerWidth - this.cardWidth - this.edgeGap
      const maxY = window.innerHeight - this.cardHeight - this.edgeGap

      // 右边界修正
      if (x > maxX) {
        x = maxX
      }

      // 下边界修正
      if (y > maxY) {
        y = maxY
      }

      // 左边界修正
      if (x < this.edgeGap) {
        x = this.edgeGap
      }

      // 上边界修正
      if (y < this.edgeGap) {
        y = this.edgeGap
      }

      this.position.x = x
      this.position.y = y

      console.log('businessCard-this.userInfo', this.userInfo)
    },

    hideCard() {
      this.visible = false
      this.isMenuVisible = false
      this.isEditingRemark = false
      this.isPermissionsDialogVisible = false
      this.remarkDraft = ''
    },

    toggleMenu() {
      this.isMenuVisible = !this.isMenuVisible
    },

    closeMenu() {
      this.isMenuVisible = false
    },

    showMoments() {
      console.log('show moments')
    },

    setRemark() {
      this.closeMenu()
      this.isEditingRemark = true
      this.remarkDraft = this.userInfo.remark || ''
      console.log('setRemark', this.remarkEdiatorVisible)
    },

    async saveRemark() {
      this.isEditingRemark = false
      const newRemark = (this.remarkDraft || '').trim()
      if (newRemark === (this.userInfo.remark || '')) {
        this.isEditingRemark = false
        return
      }
      const userStore = useUserStore()
      const friendStore = useFriendStore()
      await friendStore.updateFriendship({
        userId: userStore.userInfo.userId,
        friendUserId: this.userInfo.userId,
        remark: newRemark,
      })
    },

    setPermissions() {
      this.closeMenu()
      this.isEditingRemark = false

      this.permissionsDraft.hideMyMoments = !!this.userInfo.hideMyMoments
      this.permissionsDraft.hideFriendMoments = !!this.userInfo.hideFriendMoments

      this.isPermissionsDialogVisible = true
    },

    cancelPermissions() {
      this.isPermissionsDialogVisible = false
    },

    async confirmPermissions() {
      const friendStore = useFriendStore()
      var friendship = friendStore.gettersFriendship(this.userInfo.userId)
      friendship = { userId: friendship.userId, friendUserId: friendship.friendUserId }
      if (
        friendship.hideMyMoments === this.permissionsDraft.hideMyMoments &&
        friendship.hideFriendMoments === this.permissionsDraft.hideFriendMoments
      ) {
        this.isPermissionsDialogVisible = false
        return
      }
      friendship.hideMyMoments = this.permissionsDraft.hideMyMoments
      friendship.hideFriendMoments = this.permissionsDraft.hideFriendMoments

      try {
        const friendStore = useFriendStore()
        await friendStore.updateFriendship(friendship)

        this.userInfo.hideMyMoments = friendship.hideMyMoments
        this.userInfo.hideFriendMoments = friendship.hideFriendMoments

        this.isPermissionsDialogVisible = false
      } catch (error) {
        console.error('保存朋友圈权限失败', error)
      }
    },

    recommendToFriend() {},

    async blockFriend() {
      const friendStore = useFriendStore()
      const sure = await confirm('确定要拉黑该联系人吗?', {
        title: '拉黑联系人',
        confirmText: '拉黑',
        cancelText: '取消',
        danger: true,
      })
      if (!sure) {
        return
      } else {
        this.closeMenu()
        this.hideCard()
        await friendStore.blockFriend(this.userInfo.userId)
      }
    },

    async unBlockFriend() {
      const friendStore = useFriendStore()
      this.closeMenu()
      this.hideCard()
      await friendStore.unBlockFriend(this.userInfo.userId)
    },

    async deleteFriend() {
      const friendStore = useFriendStore()
      const sure = await confirm('确定要删除该联系人吗?', {
        title: '删除联系人',
        confirmText: '删除',
        cancelText: '取消',
        danger: true,
      })
      if (!sure) {
        return
      } else {
        this.closeMenu()
        this.hideCard()
        await friendStore.deleteFriend(this.userInfo.userId)
      }
    },

    async addFriend() {
      const addFriendModalStore = useAddFriendModalStore()
      addFriendModalStore.open(this.userInfo)
    },

    async sendMessage(router) {
      this.closeMenu()
      this.hideCard()
      const conversationStore = useConversationStore()
      await conversationStore.getConversation(false, this.userInfo.userId, null)
      var convId = null
      for (const key of Object.keys(conversationStore.conversationsMap)) {
        const conv = conversationStore.conversationsMap[key]
        if (conv.convType === false && conv.peerId === this.userInfo.userId) {
          convId = conv.convId
          break
        }
      }
      console.log('convId', convId)
      if (convId == null) return
      conversationStore.openConversation(convId)
      if (router) {
        router.push('/home/talk/recentConversation/talkwindow')
      }
    },

    async audioCall() {
      const userStore = useUserStore()
      const friendStore = useFriendStore()
      const conversationStore = useConversationStore()
      await conversationStore.getConversation(false, this.userInfo.userId, null)

      const conv = conversationStore.getConversations.find((conv) => {
        return conv.convType === false && conv.peerId === this.userInfo.userId
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
    },

    videoCall() {},
  },
})
