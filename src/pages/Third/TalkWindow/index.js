import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useUserStore } from '/src/stores/userStore.js'
import { useConversationStore } from '/src/stores/conversationStore.js'
import { useGroupStore } from '/src/stores/groupStore.js'
import { useFriendStore } from '/src/stores/friendStore.js'
import { useToastStore } from '/src/stores/toastStore.js'
import { confirm } from '/src/utils/confirm.js'
import request from '/src/api/request.js'

export default function useTalkWindow() {
  const userStore = useUserStore()
  const conversationStore = useConversationStore()
  const groupStore = useGroupStore()
  const friendStore = useFriendStore()
  const toastStore = useToastStore()

  // ====== 来自 store 的单一数据源 ======
  const conversation = computed(() => conversationStore.activeConversation)

  const peerId = computed(() =>
    !conversation?.value?.convType ? conversation?.value?.peerId : null,
  )

  const groupId = computed(() =>
    conversation?.value?.convType ? conversation?.value?.groupId : null,
  )

  const candidates = ref([])

  const messages = computed(() => conversationStore.activeMessages)

  const draft = computed({
    get: () => conversationStore.activeDraft ?? '',
    set: (v) => {
      const convId = conversationStore.activeConversationId
      if (!convId) return
      conversationStore.setDraft(convId, v)
    },
  })

  const activeComposer = computed(() => conversationStore.activeComposer)

  const pendingFiles = computed(() => {
    return activeComposer.value?.pendingFiles || []
  })

  const quoteMessage = computed(() => {
    return activeComposer.value?.quoteMessage || null
  })

  const unreadCnt = computed(() => conversation.value?.unreadCnt)

  const historyLoading = ref(false)
  const hasMoreHistory = ref(true)

  const forwardConversations = computed(() => {
    return conversationStore.getConversations.map((conv) => {
      if (conv.convType === false) {
        const friendUser = userStore.getUser(conv?.peerId)
        const friendship = friendStore.gettersFriendship(conv.peerId)
        return {
          convId: conv?.convId,
          peerId: conv?.peerId,
          convType: conv?.convType,
          sessionId: conv?.sessionId,
          remark: friendship?.remark,
          username: friendUser?.username,
          avatar: friendUser?.avatar,
        }
      } else {
        const group = groupStore.getGroup(conv.groupId)
        return {
          convId: conv?.convId,
          sessionId: conv?.sessionId,
          groupId: conv?.groupId,
          convType: conv?.convType,
          groupRemark: conv?.groupRemark,
          groupName: group?.groupName,
          groupAvatar: group?.groupAvatar,
        }
      }
    })
  })

  const forwardFriends = computed(() =>
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

  let forwardMsg = null

  // ====== UI 状态 ======
  const showProfilePanel = ref(false)
  const chatHistoryRef = ref(null)

  const chatSearchVisible = ref(false)
  const chatSearchLoading = ref(false)
  const chatSearchHasMore = ref(true)

  const groupFriendModalVisible = ref(false)
  const membersManageMode = ref('add')

  const forwardModalVisible = ref(false)

  const autoScroll = true

  async function scrollToBottom() {
    if (!autoScroll) return

    await nextTick()

    const el = chatHistoryRef.value
    if (!el) return

    el.scrollTop = el.scrollHeight

    requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight
    })
  }

  onMounted(async () => {
    await scrollToBottom()
    console.log('conversationWindow mounted', conversation)
  })

  async function handleSendText({ msgType = 1, content, quoteMsgId = null }) {
    console.log('conversation', conversation.value, content)

    const convId = conversation.value?.convId

    if (!content) return

    if (!convId) {
      console.warn('未选择聊天对象 convId 为空')
      return
    }

    await conversationStore.sendMessage({
      msgType,
      quoteMsgId,
      content,
    })

    conversationStore.setDraft(convId, '')

    await scrollToBottom()
  }

  async function handleSendMedia(payload) {
    const { msgType, file, content, quoteMsgId = null } = payload

    const convId = conversation.value?.convId

    if (!convId) {
      toastStore.error('未选择会话')
      return
    }

    const clientMsgId = `local_${Date.now()}_${Math.random().toString(16).slice(2)}`
    const localUrl = URL.createObjectURL(file)

    let localPath = ''

    try {
      localPath = window.electronAPI?.getPathForFile ? window.electronAPI.getPathForFile(file) : ''
    } catch (e) {
      console.warn('getPathForFile failed:', e)
      localPath = ''
    }

    const localContent = buildLocalMediaContent({
      msgType,
      file,
      content,
      localUrl,
      localPath,
    })

    conversationStore.addLocalMessage(convId, {
      msgType,
      msgId: clientMsgId,
      clientMsgId,
      sessionId: conversation.value.sessionId,
      convType: conversation.value.convType,
      senderId: userStore.userInfo.userId,
      peerId: conversation.value.peerId,
      groupId: conversation.value.groupId,
      quoteMsgId,
      content: localContent,
      createdTime: formatNowTime(),
      localStatus: 'uploading',
    })

    await scrollToBottom()

    try {
      const url = await uploadMedia(file, msgType, {
        onProgress: (progress) => {
          conversationStore.patchLocalMessage(convId, clientMsgId, {
            content: {
              uploadProgress: progress,
            },
          })
        },
      })

      if (!url) {
        conversationStore.patchLocalMessage(convId, clientMsgId, {
          localStatus: 'failed',
          content: {
            uploadStatus: 'failed',
          },
        })
        toastStore.error('上传文件失败')
        return
      }

      const finalContent = buildFinalMediaContent({
        msgType,
        file,
        content,
        url,
      })

      conversationStore.patchLocalMessage(convId, clientMsgId, {
        localStatus: 'sending',
        content: {
          ...finalContent,
          localUrl,
          localPath,
          uploadStatus: 'sending',
          uploadProgress: 100,
        },
      })

      await conversationStore.sendMessage({
        clientMsgId,
        msgType,
        quoteMsgId,
        content: finalContent,
      })

      await scrollToBottom()
    } catch (e) {
      console.error('发送文件消息失败', e)

      conversationStore.patchLocalMessage(convId, clientMsgId, {
        localStatus: 'failed',
        content: {
          uploadStatus: 'failed',
        },
      })

      toastStore.error('发送文件消息失败')
    }
  }

  async function uploadMedia(file, msgType, options = {}) {
    const formData = new FormData()

    formData.append('media', file)
    formData.append('msgType', String(msgType))
    formData.append('senderId', String(userStore.userInfo.userId))
    formData.append('convId', String(conversation.value.convId))

    try {
      const res = await request.post('uploadMedia', formData, {
        timeout: 300000,
        onUploadProgress: (e) => {
          if (!e.total) return

          const progress = Math.round((e.loaded / e.total) * 100)
          options.onProgress?.(progress)
        },
      })

      console.log('uploadMedia res', res)

      if (res.code !== 0) {
        toastStore.error(res.msg)
        return null
      }

      return res.data
    } catch (e) {
      console.log('uploadMedia error', e)
      return null
    }
  }

  function addPendingFile(item) {
    const convId = conversationStore.activeConversationId
    if (!convId) return

    conversationStore.addPendingFile(convId, item)
  }

  function removePendingFile(id) {
    const convId = conversationStore.activeConversationId
    if (!convId) return

    conversationStore.removePendingFile(convId, id)
  }

  function clearPendingFiles() {
    const convId = conversationStore.activeConversationId
    if (!convId) return

    conversationStore.clearPendingFiles(convId)
  }

  function setQuoteMessage(msg) {
    const convId = conversationStore.activeConversationId
    if (!convId) return

    conversationStore.setQuoteMessage(convId, msg)
  }

  function clearQuoteMessage() {
    const convId = conversationStore.activeConversationId
    if (!convId) return

    conversationStore.clearQuoteMessage(convId)
  }

  function openForwardModal(msg) {
    forwardMsg = JSON.parse(JSON.stringify(msg))
    forwardModalVisible.value = true
  }

  function closeForwardModal() {
    forwardMsg = null
    forwardModalVisible.value = false
  }

  async function handleForwardConfirm(selected) {
    console.log('handleForwardConfirm', selected, forwardMsg)
    for (const item of selected) {
      if (item.type === 'conversation') {
        await conversationStore.forwardMessage({
          msgType: forwardMsg.msgType,
          convId: item.convId,
          sessionId: item.sessionId,
          peerId: item.peerId,
          groupId: forwardMsg.groupId,
          convType: item.convType,
          content: forwardMsg.content,
        })
      } else {
        await conversationStore.getConversation(false, item.friendUserId, null)
        for (const convId of Object.keys(conversationStore.conversationsMap)) {
          const conv = conversationStore.conversationsMap[convId]
          if (conv.convType === item.convType && conv.peerId === item.friendUserId) {
            await conversationStore.forwardMessage({
              msgType: forwardMsg.msgType,
              convId: conv.convId,
              sessionId: conv.sessionId,
              peerId: conv.peerId,
              groupId: conv.groupId,
              convType: false,
              content: forwardMsg.content,
            })
            break
          }
        }
      }
    }
    forwardMsg = null
    closeForwardModal()
  }

  async function recallMessage(msg) {
    if (!msg?.msgId) return

    if (Date.now() - new Date(msg.createdTime).getTime() > 5 * 60 * 1000) {
      toastStore.error('超过5分钟不能撤回')
      return
    }

    await conversationStore.recallMessage?.({
      msgId: msg.msgId,
      sessionId: msg.sessionId,
      convType: msg.convType,
      peerId: msg?.peerId,
    })
  }

  async function deleteMessage(msg) {
    if (!msg?.msgId) return

    await conversationStore.deleteMessage?.({
      msgId: msg?.msgId,
      userId: userStore.userInfo.userId,
    })
  }

  function buildLocalMediaContent({ msgType, file, content, localUrl, localPath }) {
    const fileName = content.fileName || file.name || buildDefaultNameByMsgType(msgType, content)
    const ext = content.ext || getExtFromFileName(fileName) || content.format || ''
    const format = content.format || ext

    if (msgType === 2) {
      return {
        fileName,
        url: localUrl,
        localUrl,
        localPath,
        duration: content.duration,
        size: content.size,
        format,
        ext,
        uploadStatus: 'uploading',
        uploadProgress: 0,
      }
    }

    if (msgType === 3) {
      return {
        fileName,
        url: localUrl,
        localUrl,
        localPath,
        width: content.width,
        height: content.height,
        size: content.size,
        format,
        ext,
        uploadStatus: 'uploading',
        uploadProgress: 0,
      }
    }

    if (msgType === 4) {
      return {
        fileName,
        url: localUrl,
        localUrl,
        localPath,
        duration: content.duration,
        width: content.width,
        height: content.height,
        size: content.size,
        cover: content.cover,
        format,
        ext,
        uploadStatus: 'uploading',
        uploadProgress: 0,
      }
    }

    return {
      fileName,
      url: localUrl,
      localUrl,
      localPath,
      size: content.size,
      ext,
      format,
      uploadStatus: 'uploading',
      uploadProgress: 0,
    }
  }

  function buildFinalMediaContent({ msgType, file, content, url }) {
    const fileName = content.fileName || file.name || buildDefaultNameByMsgType(msgType, content)
    const ext = content.ext || getExtFromFileName(fileName) || content.format || ''
    const format = content.format || ext

    if (msgType === 2) {
      return {
        fileName,
        url,
        duration: content.duration,
        size: content.size,
        format,
        ext,
      }
    }

    if (msgType === 3) {
      return {
        fileName,
        url,
        width: content.width,
        height: content.height,
        size: content.size,
        format,
        ext,
      }
    }

    if (msgType === 4) {
      return {
        fileName,
        url,
        duration: content.duration,
        width: content.width,
        height: content.height,
        size: content.size,
        cover: content.cover,
        format,
        ext,
      }
    }

    return {
      fileName,
      url,
      size: content.size,
      ext,
      format,
    }
  }

  function getExtFromFileName(fileName = '') {
    const index = fileName.lastIndexOf('.')
    if (index === -1) return ''
    return fileName.slice(index + 1).toLowerCase()
  }

  function buildDefaultNameByMsgType(msgType, content = {}) {
    const format = content.format || content.ext || ''

    if (msgType === 2) {
      return `audio.${format || 'webm'}`
    }

    if (msgType === 3) {
      return `image.${format || 'jpg'}`
    }

    if (msgType === 4) {
      return `video.${format || 'mp4'}`
    }

    return `file.${format || 'dat'}`
  }

  function formatNowTime() {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hours = String(now.getHours()).padStart(2, '0')
    const minutes = String(now.getMinutes()).padStart(2, '0')
    const seconds = String(now.getSeconds()).padStart(2, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  }

  function onToggleProfile() {
    showProfilePanel.value = !showProfilePanel.value
  }

  async function startAudioCall() {
    const conv = conversation.value

    if (!conv || conv.convType === true) {
      toastStore.error('暂不支持群聊语音通话')
      return
    }

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

    if (!window.electronAPI?.startAudioCall) {
      toastStore.error('当前环境不支持语音通话窗口')
      return
    }

    const res = await window.electronAPI.startAudioCall(payload)
    if (res.code !== 0) {
      toastStore.error(res.msg)
      return
    }
  }

  function startVideoCall() {
    console.log('startVideoCall ->', conversationStore.activepeerId)
  }

  async function loadMoreHistory() {
    if (historyLoading.value || !hasMoreHistory.value) return

    const conv = conversation.value
    if (!conv?.convId) return

    historyLoading.value = true

    try {
      const beforeLength = messages.value.length

      await conversationStore.getMessages(conv.convId)

      const afterLength = messages.value.length

      if (afterLength === beforeLength) {
        hasMoreHistory.value = false
      }
    } catch (e) {
      console.log('loadMoreHistory error', e)
      toastStore.error('加载历史消息失败')
    } finally {
      historyLoading.value = false
    }
  }

  async function openConversation(opponentId) {
    await conversationStore.openConversation(opponentId)
    showProfilePanel.value = false
    await scrollToBottom()
  }

  function updateConversation({ key, value }) {
    const conv = {
      convId: conversationStore.activeConversationId,
    }

    conv[key] = value
    conversationStore.updateConversation(conv)
  }

  async function updateGroup({ key, value }) {
    const group = {
      groupId: groupId.value,
    }

    group[key] = value
    await groupStore.updateGroup(group)
  }

  function updateGroupMemberMyNickname({ key, value, memberUserId }) {
    const member = {
      memberUserId: memberUserId ?? userStore.userInfo.userId,
      groupId: groupId.value,
    }

    member[key] = value
    groupStore.updateGroupMembers([member])
  }

  function updateGroupMembersRole({ memberUserIds, role }) {
    const groupMembers = memberUserIds.map((memberUserId) => {
      return {
        memberUserId,
        groupId: groupId.value,
        role,
      }
    })

    groupStore.setGroupMembersRole(groupMembers)
  }

  async function silenceAll() {
    const group = {
      groupId: groupId.value,
      status: 2,
    }

    await groupStore.setGroupSilenceAll(group)
  }

  async function unsilenceAll() {
    const group = {
      groupId: groupId.value,
      status: 0,
    }

    await groupStore.setGroupUnSilenceAll(group)
  }

  async function silenceMembers({ memberUserIds }) {
    console.log('silenceMembers', memberUserIds)

    const groupMembers = memberUserIds.map((memberUserId) => {
      return {
        memberUserId,
        groupId: groupId.value,
        silence: true,
      }
    })

    await groupStore.updateGroupMembers(groupMembers)
  }

  async function unsilenceMembers({ memberUserIds }) {
    const groupMembers = memberUserIds.map((memberUserId) => {
      return {
        memberUserId,
        groupId: groupId.value,
        silence: false,
      }
    })

    await groupStore.updateGroupMembers(groupMembers)
  }

  function saveGroupField({ key, value, memberId }) {
    console.log('saveGroupField', key, value, memberId)

    switch (key) {
      case 'groupRemark':
        updateConversation({ key, value })
        break
      case 'myNickname':
        updateGroupMemberMyNickname({ key, value, memberUserId: memberId })
        break
      case 'groupName':
      case 'announcement':
        updateGroup({ key, value })
        break
      default:
        console.log('unknown key', key)
    }
  }

  async function changeGroupAvatar() {
    console.log('changeGroupAvatar ->')

    const result = await window.electronAPI.pickAvatar()
    if (!result) return

    const { buffer, filename } = result
    const file = new File([new Uint8Array(buffer)], filename)

    try {
      const formData = new FormData()
      formData.append('avatars', file)
      formData.append('names', 'group:' + groupId.value)

      const res = await request.post('uploadAvatars', formData)

      console.log('uploadAvatars res', res)

      if (res.code !== 0) {
        toastStore.error('上传失败')
      } else {
        await updateGroup({
          key: 'groupAvatar',
          value: res.data.urls[0],
        })
      }
    } catch (e) {
      console.log('上传失败:', e)
      toastStore.error('上传失败')
    }
  }

  function searchChatHistory() {
    chatSearchVisible.value = true
    chatSearchHasMore.value = true
  }

  async function loadMoreChatSearchMessages(params) {
    if (chatSearchLoading.value) return
    if (!chatSearchHasMore.value) return

    const conv = conversation.value
    if (!conv?.convId || !conv?.sessionId) return

    chatSearchLoading.value = true
    const length = messages.value.length
    try {
      await conversationStore.fetchMessagesByParams({
        ...params,
        convId: conv.convId,
        sessionId: conv.sessionId,
      })
      if (length === messages.value.length) {
        chatSearchHasMore.value = false
      } else {
        chatSearchHasMore.value = true
      }
    } catch (e) {
      console.log('loadMoreChatSearchMessages error', e)
      toastStore.error('加载搜索结果失败')
    } finally {
      chatSearchLoading.value = false
    }
  }

  async function locateSearchMessage(msg) {
    chatSearchVisible.value = false

    await nextTick()

    chatHistoryRef.value?.closeDetailPanel?.()
    chatHistoryRef.value?.scrollToMessage?.(msg)
  }

  async function clearChatHistory() {
    console.log('clearChatHistory ->')

    const sure = await confirm('确定要清空该聊天记录吗？', {
      title: '清空聊天记录',
      confirmText: '清空',
      cancelText: '取消',
      danger: true,
    })

    if (!sure) return

    await conversationStore.clearChatHistory()
  }

  async function deleteFriend() {
    console.log('deleteFriend ->')

    const sure = await confirm('确定要删除该联系人吗?', {
      title: '删除联系人',
      confirmText: '删除',
      cancelText: '取消',
      danger: true,
    })

    if (!sure) return

    await friendStore.deleteFriend(peerId.value)
  }

  async function quitGroup() {
    console.log('quitGroup ->')

    const sure = await confirm('确定要退出群聊吗？', {
      title: '退出群聊',
      confirmText: '退出',
      cancelText: '取消',
      danger: true,
    })

    if (!sure) return

    await groupStore.quitGroup(groupId.value)
  }

  async function transferGroupOwner({ transfereeUserId }) {
    await groupStore.transferGroupOwner(groupId.value, transfereeUserId)
  }

  function addMember() {
    const groupMembers = groupStore.getGroupMembers(groupId.value)

    candidates.value = Object.entries(friendStore.friendships)
      .filter(([friendUserId]) => {
        return !groupMembers.some((member) => {
          return Number(member.memberUserId) === Number(friendUserId)
        })
      })
      .map(([friendUserId, friendship]) => {
        return {
          remark: friendship.remark,
          ...userStore.getUser(friendUserId),
        }
      })

    membersManageMode.value = 'add'
    groupFriendModalVisible.value = true
  }

  function removeMember() {
    const groupMembers = groupStore.getGroupMembers(groupId.value)

    const myMember = groupMembers.find((member) => {
      return Number(member.memberUserId) === Number(userStore.userInfo.userId)
    })

    if (myMember?.role === 0) {
      return
    }

    if (myMember?.role === 1) {
      candidates.value = groupMembers
        .filter((member) => member.role === 0)
        .map((member) => {
          return {
            myNickname: member.myNickname,
            remark: friendStore.gettersFriendship(member.memberUserId)?.remark,
            role: member.role,
            ...userStore.getUser(member.memberUserId),
          }
        })
    } else if (myMember?.role === 2) {
      candidates.value = groupMembers
        .filter((member) => member.role === 0 || member.role === 1)
        .map((member) => {
          return {
            myNickname: member.myNickname,
            remark: friendStore.gettersFriendship(member.memberUserId)?.remark,
            role: member.role,
            ...userStore.getUser(member.memberUserId),
          }
        })
    } else {
      return
    }

    membersManageMode.value = 'remove'
    groupFriendModalVisible.value = true
  }

  async function handleMembersManage({ selectedUserIds }) {
    console.log('handleMembersManage', selectedUserIds)

    if (membersManageMode.value === 'add') {
      await groupStore.addGroupMembers({
        groupId: groupId.value,
        userIds: selectedUserIds,
      })
    } else if (membersManageMode.value === 'remove') {
      await groupStore.removeGroupMembers({
        groupId: groupId.value,
        userIds: selectedUserIds,
      })
    } else {
      console.log('unknown membersManageMode', membersManageMode.value)
      return
    }

    groupFriendModalVisible.value = false
  }

  async function disbandGroup() {
    const sure = await confirm('确定要解散群聊吗？', {
      title: '解散群聊',
      confirmText: '解散',
      cancelText: '取消',
      danger: true,
    })

    if (!sure) return

    await groupStore.disbandGroup(groupId.value)
  }

  function markConversationRead() {
    conversationStore.markConversationRead()
  }

  return {
    peerId,
    groupId,
    draft,
    messages,
    unreadCnt,
    candidates,

    showProfilePanel,
    chatHistoryRef,
    historyLoading,
    hasMoreHistory,
    groupFriendModalVisible,
    membersManageMode,
    forwardModalVisible,

    handleSendText,
    handleSendMedia,
    onToggleProfile,
    startAudioCall,
    startVideoCall,

    pendingFiles,
    quoteMessage,
    addPendingFile,
    removePendingFile,
    clearPendingFiles,
    setQuoteMessage,
    clearQuoteMessage,
    recallMessage,
    deleteMessage,

    updateConversation,
    saveGroupField,
    searchChatHistory,
    changeGroupAvatar,
    clearChatHistory,
    deleteFriend,
    quitGroup,
    addMember,
    removeMember,
    disbandGroup,
    transferGroupOwner,
    updateGroupMembersRole,
    silenceAll,
    unsilenceAll,
    silenceMembers,
    unsilenceMembers,
    markConversationRead,
    loadMoreHistory,
    handleMembersManage,

    openConversation,
    scrollToBottom,

    chatSearchVisible,
    locateSearchMessage,
    chatSearchLoading,
    chatSearchHasMore,
    loadMoreChatSearchMessages,

    openForwardModal,
    closeForwardModal,
    forwardConversations,
    forwardFriends,
    handleForwardConfirm,
  }
}
