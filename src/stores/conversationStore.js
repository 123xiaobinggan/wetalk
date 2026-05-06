// src/stores/talkStore.js
import { defineStore } from 'pinia'
import { useGroupStore } from './groupStore.js'
import { useUserStore } from './userStore.js'
import { useToastStore } from './toastStore.js'
import request from '../api/request.js'

function genId() {
  return `${Date.now()}_${Math.random().toString(16).slice(2)}`
}

function formatTime() {
  const date = new Date()

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

function formatAckTime(time) {
  return time.slice(0, 19).replace(' ', 'T')
}

export const useConversationStore = defineStore('conversation', {
  state: () => ({
    activeConversationId: null,
    conversationsMap: {},
    messagesMap: {},
    composerMap: {},
    fetchingMessages: {},
  }),

  getters: {
    // ===== 新 getters =====
    activeConversation(state) {
      return state.activeConversationId ? state.conversationsMap[state.activeConversationId] : null
    },
    activeMessages(state) {
      if (state.activeConversationId) {
        if (state.messagesMap[state.activeConversationId]) {
          const messages = state.messagesMap[state.activeConversationId]
          return messages.sort((a, b) => {
            return a.msgId - b.msgId
          })
        }
      }
      return []
    },
    activeComposer(state) {
      const convId = state.activeConversationId
      if (!convId) {
        return {
          text: '',
          pendingFiles: [],
          quoteMessage: null,
        }
      }

      if (!state.composerMap[convId]) {
        state.composerMap[convId] = {
          text: '',
          pendingFiles: [],
          quoteMessage: null,
        }
      }

      return state.composerMap[convId]
    },
    activeDraft(state) {
      const convId = state.activeConversationId
      if (!convId) return ''

      if (!state.composerMap[convId]) {
        state.composerMap[convId] = {
          text: '',
          pendingFiles: [],
          quoteMessage: null,
        }
      }

      return state.composerMap[convId].text || ''
    },
    getConversations(state) {
      const result = Object.entries(state.conversationsMap)
        .filter(([id, conv]) => {
          return conv && conv.convId && conv.deleted !== true
        })
        .sort(([idA, convA], [idB, convB]) => {
          // 置顶优先
          if (convA.pinned && !convB.pinned) return -1
          if (!convA.pinned && convB.pinned) return 1
          // 置顶内部按最后消息时间排序
          if (convA.pinned && convB.pinned) {
            return new Date(convB.lastMsgTime) - new Date(convA.lastMsgTime)
          }
          // 非置顶按最后消息时间排序
          return new Date(convB.lastMsgTime) - new Date(convA.lastMsgTime)
        })
        .map(([convId, conv]) => {
          return {
            convId,
            ...conv,
          }
        })
      console.log('getConversations', result)
      return result
    },
    gettersConversation(state) {
      return (convId) => {
        return state.conversationsMap[convId] || {}
      }
    },
    unread(state) {
      let unread = 0
      for (const convId in state.conversationsMap) {
        if (state.conversationsMap[convId].muted === false) {
          unread += state.conversationsMap[convId].unreadCnt
        }
      }
      return unread
    },
  },

  actions: {
    async init() {
      await this.fetchConversations()
      for (const convId in this.conversationsMap) {
        await this.getMessages(convId)
      }
    },

    async fetchConversations() {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchConversations', { userId: userStore.userInfo.userId })
        console.log('fetchConversations', res)
        if (res.code === 0) {
          this.conversationsMap = {}
          this.messagesMap = {}
          this.composerMap = {}
          res.data.conversations.map((conv) => {
            this.conversationsMap[conv.convId] = conv
            this.messagesMap[conv.convId] = []
          })
        } else {
          console.log(res.msg)
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取会话失败')
      }
    },

    async getConversation(convType, friendUserId, groupId) {
      console.log('getConversation', convType, friendUserId, groupId)
      if ((!friendUserId && !groupId) || convType == null) {
        return
      }
      var exists = false
      var existsConvId = null
      if (convType === false) {
        for (const convId of Object.keys(this.conversationsMap)) {
          const conv = this.conversationsMap[convId]
          if (conv.convType === convType && conv.peerId === friendUserId) {
            exists = true
            existsConvId = convId
            break
          }
        }
      } else {
        for (const convId of Object.keys(this.conversationsMap)) {
          const conv = this.conversationsMap[convId]
          if (
            conv.convType === convType &&
            conv.sessionId === groupId &&
            conv.groupId === groupId
          ) {
            exists = true
            existsConvId = convId
            break
          }
        }
      }
      if (exists === true && this.conversationsMap[existsConvId].deleted === false) {
        return
      }
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('getConversation', {
          userId: userStore.userInfo.userId,
          convType,
          friendUserId,
          groupId,
        })
        console.log('getConversation', res)
        if (res.code === 0) {
          this.conversationsMap[res.data.conv.convId] = res.data.conv
          if (!this.messagesMap[res.data.conv.convId]) {
            this.messagesMap[res.data.conv.convId] = []
          }
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取会话失败')
      }
    },

    async getMessages(convId) {
      if (!convId || convId in this.fetchingMessages) {
        return
      }
      this.fetchingMessages[convId] = true
      const userStore = useUserStore()
      const toastStore = useToastStore()
      console.log(this.conversationsMap[convId], this.conversationsMap[convId].sessionId)
      try {
        const res = await request.post('fetchMessages', {
          myUserId: userStore.userInfo.userId,
          sessionId: this.conversationsMap[convId].sessionId,
          msgId: this.messagesMap[convId]?.[0]?.msgId ?? null,
        })
        console.log('fetchMessages', res)
        if (res.code === 0) {
          const messages = res.data.messages.filter((m) => {
            return this.messagesMap[convId].findIndex((msg) => msg.msgId === m.msgId) === -1
          })
          this.messagesMap[convId].unshift(...messages)
        } else {
          console.log(res.msg)
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取消息失败')
      } finally {
        delete this.fetchingMessages[convId]
      }
    },

    async fetchMessagesByParams(params) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchMessagesByParams', params)
        console.log('fetchMessagesByParams', res)
        if (res.code === 0) {
          this.messagesMap[params.convId].unshift(...res.data.messages)
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取消息失败')
      }
    },

    async openConversation(convId) {
      this.activeConversationId = convId
      if (convId && this.conversationsMap[convId]) {
        this.conversationsMap[convId].deleted = false
      }
      if (!(convId in this.messagesMap) || !this.messagesMap[convId].length) {
        await this.getMessages(convId)
      }
      if (this.conversationsMap[convId].convType === true) {
        const groupStore = useGroupStore()
        groupStore.openGroup(this.conversationsMap[convId].groupId)
      }
      console.log('activeConversationId、conversation', convId, this.conversationsMap[convId])
    },

    async markConversationRead(convId) {
      if (!convId) {
        convId = this.activeConversationId
      }
      if (this.conversationsMap[convId].unreadCnt == 0) {
        return
      }
      try {
        const res = await request.post('markConversationRead', {
          convId,
        })
        if (res.code !== 0) {
          console.log('setConversationRead', res.msg)
        } else {
          this.conversationsMap[convId].unreadCnt = 0
        }
      } catch (e) {
        console.log(e)
      }
    },

    async updateConversation(conv) {
      const toastStore = useToastStore()
      console.log('updateConversation', conv)
      try {
        const res = await request.post('updateConversation', conv)
        console.log('res', res)
        if (res.code === 0) {
          const c = this.conversationsMap[conv.convId] || {}
          this.conversationsMap[conv.convId] = { ...c, ...res.data?.conv }
        } else {
          console.log('updateConversation', res.msg)
          toastStore.error('更新失败')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('更新失败')
      }
    },

    async clearChatHistory() {
      const toastStore = useToastStore()
      const convId = this.activeConversationId
      try {
        const res = await request.post('clearChatHistory', {
          convId,
        })
        if (res.code === 0) {
          this.messagesMap[convId] = []
          this.conversationsMap[convId].lastClearedTime = res.data.lastClearedTime
          this.conversationsMap[convId].lastMsgTime = ''
          this.conversationsMap[convId].lastMsgBrief = ''
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('清空聊天记录失败')
      }
    },

    //草稿：按会话存
    ensureComposer(convId) {
      if (!convId) return null

      if (!this.composerMap[convId]) {
        this.composerMap[convId] = {
          text: '',
          pendingFiles: [],
          quoteMessage: null,
        }
      }

      return this.composerMap[convId]
    },

    setDraft(convId, text) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.text = text
    },

    setPendingFiles(convId, files) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.pendingFiles = files
    },

    addPendingFile(convId, fileItem) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.pendingFiles.push(fileItem)
    },

    removePendingFile(convId, id) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.pendingFiles = composer.pendingFiles.filter((item) => item.id !== id)
    },

    clearPendingFiles(convId) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.pendingFiles = []
    },

    setQuoteMessage(convId, msg) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.quoteMessage = msg
    },

    clearQuoteMessage(convId) {
      const composer = this.ensureComposer(convId)
      if (!composer) return
      composer.quoteMessage = null
    },

    addLocalMessage(convId, message) {
      if (!convId) return

      if (!this.messagesMap[convId]) {
        this.messagesMap[convId] = []
      }

      this.messagesMap[convId].push(message)

      if (this.conversationsMap[convId]) {
        this.conversationsMap[convId].lastMsgBrief = this.buildMsgBrief(
          message.msgType,
          message.content,
        )
        this.conversationsMap[convId].lastMsgSenderId = message.senderId
        this.conversationsMap[convId].lastMsgTime = message.createdTime || formatTime()
      }
    },

    patchLocalMessage(convId, clientMsgId, patch = {}) {
      if (!convId || !clientMsgId) return

      const list = this.messagesMap[convId]
      if (!list) return

      const msg = list.find((item) => item.clientMsgId === clientMsgId)
      if (!msg) return

      Object.assign(msg, patch)

      if (patch.content) {
        msg.content = {
          ...(msg.content || {}),
          ...patch.content,
        }
      }
    },

    findMessageByClientMsgId(clientMsgId) {
      for (const convId of Object.keys(this.messagesMap)) {
        const list = this.messagesMap[convId] || []
        const msg = list.find((item) => item.clientMsgId === clientMsgId)

        if (msg) {
          return {
            convId,
            msg,
          }
        }
      }

      return null
    },

    removeLocalMessage(convId, clientMsgId) {
      if (!convId || !clientMsgId) return

      const list = this.messagesMap[convId]
      if (!list) return

      const index = list.findIndex((item) => item.clientMsgId === clientMsgId)
      if (index === -1) return

      list.splice(index, 1)
    },

    async forwardMessage({
      msgType = 1,
      convId,
      sessionId,
      peerId,
      groupId,
      convType,
      content,
      clientMsgId = genId(),
      quoteMsgId = null,
    }) {
      const userStore = useUserStore()
      if (!convId || !this.conversationsMap[convId]) {
        return
      }
      if (!this.messagesMap[convId]) {
        this.messagesMap[convId] = []
      }
      const msg = {
        type: 'chat',
        event: 'send',
        clientMsgId,
        data: {
          msgType,
          msgId: clientMsgId,
          clientMsgId,
          sessionId,
          convType,
          senderId: userStore.userInfo.userId,
          peerId,
          groupId,
          content,
          quoteMsgId,
        },
      }
      const existed = this.messagesMap[convId].find((item) => item.clientMsgId === clientMsgId)

      if (existed) {
        existed.content = {
          ...(existed.content || {}),
          ...content,
        }

        existed.quoteMsgId = quoteMsgId
        existed.localStatus = 'sending'

        if (existed.content) {
          existed.content.uploadStatus = 'sending'
          existed.content.uploadProgress = 100
        }
      } else {
        this.messagesMap[convId].push({
          ...msg.data,
          createdTime: formatTime(),
          localStatus: 'sending',
        })
      }

      this.conversationsMap[convId].lastMsgBrief = this.buildMsgBrief(msgType, content)
      this.conversationsMap[convId].lastMsgSenderId = msg.data.senderId
      this.conversationsMap[convId].lastMsgTime = formatTime()

      console.log('sendMessage', msg)

      await window.electronAPI.wsSend(msg)
    },

    // 发送文本
    async sendMessage({ msgType = 1, content, clientMsgId = genId(), quoteMsgId = null }) {
      const userStore = useUserStore()
      const convId = this.activeConversationId
      if (!convId || !this.conversationsMap[convId]) {
        return
      } else {
        this.conversationsMap[convId].deleted = false
      }

      if (!this.messagesMap[convId]) {
        this.messagesMap[convId] = []
      }

      const msg = {
        type: 'chat',
        event: 'send',
        clientMsgId,
        data: {
          msgType,
          msgId: clientMsgId,
          clientMsgId,
          sessionId: this.conversationsMap[convId].sessionId,
          convType: this.activeConversation.convType,
          senderId: userStore.userInfo.userId,
          peerId: this.activeConversation.peerId,
          groupId: this.activeConversation.groupId,
          content,
          quoteMsgId,
        },
      }

      const existed = this.messagesMap[convId].find((item) => item.clientMsgId === clientMsgId)

      if (existed) {
        existed.content = {
          ...(existed.content || {}),
          ...content,
        }

        existed.quoteMsgId = quoteMsgId
        existed.localStatus = 'sending'

        if (existed.content) {
          existed.content.uploadStatus = 'sending'
          existed.content.uploadProgress = 100
        }
      } else {
        this.messagesMap[convId].push({
          ...msg.data,
          createdTime: formatTime(),
          localStatus: 'sending',
        })
      }

      this.conversationsMap[convId].lastMsgBrief = this.buildMsgBrief(msgType, content)
      this.conversationsMap[convId].lastMsgSenderId = msg.data.senderId
      this.conversationsMap[convId].lastMsgTime = formatTime()

      console.log('sendMessage', msg)

      await window.electronAPI.wsSend(msg)
    },
    // 转发消息
    async sendMessageToConversation(
      convId,
      { msgType = 1, content, clientMsgId = genId(), quoteMsgId = null },
    ) {
      const userStore = useUserStore()
      const conv = this.conversationsMap[convId]

      if (!conv) return

      if (!this.messagesMap[convId]) {
        this.messagesMap[convId] = []
      }

      const msg = {
        type: 'chat',
        event: 'send',
        clientMsgId,
        data: {
          msgType,
          msgId: clientMsgId,
          clientMsgId,
          sessionId: conv.sessionId,
          convType: conv.convType,
          senderId: userStore.userInfo.userId,
          peerId: conv.peerId,
          groupId: conv.groupId,
          quoteMsgId,
          content,
        },
      }

      this.messagesMap[convId].push({
        ...msg.data,
        createdTime: formatTime(),
        localStatus: 'sending',
      })

      conv.lastMsgBrief = this.buildMsgBrief(msgType, content)
      conv.lastMsgSenderId = userStore.userInfo.userId
      conv.lastMsgTime = formatTime()

      await window.electronAPI.wsSend(msg)
    },

    async deleteMessage({ msgId, userId }) {
      const toastStore = useToastStore()
      try {
        const res = await request.post('deleteMessage', { msgId, userId })
        console.log('deleteMessage', res)
        if (res.code === 0) {
          for (const convId of Object.keys(this.messagesMap)) {
            let index = this.messagesMap[convId].findIndex((item) => item.msgId === msgId)
            if (index !== -1) {
              this.messagesMap[convId].splice(index, 1)
            }
          }
          toastStore.error('删除成功')
        } else {
          toastStore.error('删除失败')
        }
      } catch (err) {
        console.log(err)
        toastStore.error('删除失败')
      }
    },

    async recallMessage(payload) {
      const msg = {
        type: 'chat',
        event: 'recall',
        data: payload,
      }
      await window.electronAPI.wsSend(msg)
    },

    /*-- ws --*/
    receiveMessage(data) {
      console.log('receiveMessage', data)

      const message = data.message
      const conv = data.conversation

      this.conversationsMap[conv.convId] = conv

      if (!this.messagesMap[conv.convId]) {
        this.messagesMap[conv.convId] = []
      }

      message.createdTime = formatAckTime(message.createdTime)

      const list = this.messagesMap[conv.convId]

      const existedByMsgId = list.find((msg) => msg.msgId === message.msgId)
      if (existedByMsgId) {
        return
      }

      const existedByClientMsgId = message.clientMsgId
        ? list.find((msg) => msg.clientMsgId === message.clientMsgId)
        : null

      if (existedByClientMsgId) {
        Object.assign(existedByClientMsgId, message)
        existedByClientMsgId.localStatus = 'sent'

        if (existedByClientMsgId.content) {
          existedByClientMsgId.content.uploadStatus = 'sent'
          existedByClientMsgId.content.uploadProgress = 100
        }

        return
      }

      list.push(message)
    },

    sendMsgSuccess({ clientMsgId, data }) {
      const found = this.findMessageByClientMsgId(clientMsgId)

      if (!found) {
        return
      }

      const { convId, msg } = found

      msg.msgId = data.msgId
      msg.createdTime = formatAckTime(data.createdTime)
      msg.localStatus = 'sent'

      if (msg.content) {
        msg.content.uploadStatus = 'sent'
        msg.content.uploadProgress = 100
      }

      this.conversationsMap[convId].lastMsgId = data.msgId
      this.conversationsMap[convId].lastMsgTime = formatAckTime(data.createdTime)
    },

    sendMsgFailed({ clientMsgId, msg }) {
      const toastStore = useToastStore()
      const found = this.findMessageByClientMsgId(clientMsgId)

      if (!found) {
        toastStore.error(msg)
        return
      }

      const { msg: localMsg } = found

      localMsg.localStatus = 'failed'

      if (localMsg.content) {
        localMsg.content.uploadStatus = 'failed'
      }

      toastStore.error(msg)
    },

    messageRecall(message) {
      console.log('msgRecall', message)
      for (const convId of Object.keys(this.messagesMap)) {
        const list = this.messagesMap[convId] || []
        const index = list.findIndex((item) => item.msgId === message?.msgId)
        if (index !== -1) {
          list[index] = { ...list[index], ...message }
          return
        }
      }
    },

    upsertConversation(conv) {
      this.conversationsMap[conv.convId] = {
        ...this.conversationsMap[conv.convId],
        ...conv,
      }

      if (!this.messagesMap[conv.convId]) {
        this.messagesMap[conv.convId] = []
      }

      console.log('upsertConversation', this.conversationsMap[conv.convId])
    },

    receiveCallEndMessage(message) {
      console.log('receiveCallEndMessage', message)
      if (message.sessionId && message.content) {
        for (const convId of Object.keys(this.conversationsMap)) {
          if (String(this.conversationsMap[convId].sessionId) === String(message.sessionId)) {
            message.createdTime = formatAckTime(message.createdTime)
            if (!this.messagesMap[convId]) {
              this.messagesMap[convId] = []
            }
            const list = this.messagesMap[convId]
            const existedByMsgId = list.find((msg) => msg.msgId === message.msgId)
            if (existedByMsgId) {
              return
            }
            list.push(message)
          }
        }
      }
    },

    /* 工具方法 */
    buildMsgBrief(type, content) {
      const c =
        typeof content === 'string'
          ? (() => {
              try {
                return JSON.parse(content)
              } catch {
                return { text: content }
              }
            })()
          : content || {}

      switch (Number(type)) {
        case 1:
          return c.text || ''
        case 2:
          return '[语音]'
        case 3:
          return '[图片]'
        case 4:
          return '[视频]'
        case 5:
          return '[文件]'
        default:
          return '[消息]'
      }
    },
  },
})
