<template>
  <div class="talk-window">
    <div class="header">
      <div class="remark" @click="emit('more-information')">
        {{ headerTitle }}
      </div>
      <div
        class="more-information"
        ref="moreInfoRef"
        :class="{ active: detailPanelVisible }"
        @click.stop="toggleDetailPanel"
      >
        <span>...</span>
      </div>
    </div>

    <div class="divider"></div>

    <div class="body-container">
      <div class="main-content" @click="emit('mark-conversation-read')">
        <div
          class="chat-history"
          ref="chatHistoryRef"
          @scroll="onScroll"
          @click.stop="emit('mark-conversation-read')"
        >
          <div v-if="historyLoading" class="history-loading">正在加载更多消息...</div>
          <div v-else-if="!hasMoreHistory" class="history-loading">没有更多消息了</div>
          <div
            v-for="msg in messages"
            :key="msg.msgId || msg.clientMsgId"
            :data-msg-id="String(msg.msgId || '')"
            :data-client-msg-id="String(msg.clientMsgId || '')"
            :class="[
              'message',
              {
                'search-highlight': isHighlightedMessage(msg),
                'quote-highlight': isHighlightedMessage(msg),
              },
              isSystemMessage(msg)
                ? 'system'
                : msg.senderId === userStore.userInfo.userId
                  ? 'sent'
                  : 'received',
            ]"
            @contextmenu.prevent.stop="openMessageMenu($event, msg)"
          >
            <!-- 系统消息 -->
            <template v-if="isSystemMessage(msg)">
              <div class="system-message-content">
                {{ getSystemMessageText(msg) }}
              </div>
            </template>

            <!-- 普通消息 -->
            <template v-else>
              <img
                class="avatar"
                :src="resolveAvatar(msg)"
                @click.stop="businessCard.showCard(userStore.getUser(msg.senderId), $event)"
                alt="avatar"
              />

              <div class="message-content-wrapper">
                <div v-show="showSenderName(msg)" class="sender-name">
                  {{ getMemberName(msg.senderId) }}
                </div>

                <div
                  class="message-body-row"
                  :class="{
                    sent: msg.senderId === userStore.userInfo.userId,
                    received: msg.senderId !== userStore.userInfo.userId,
                  }"
                >
                  <div
                    :class="[
                      'message-content',
                      {
                        'no-bubble': isMediaMessage(msg),
                      },
                    ]"
                  >
                    <slot name="message" :msg="msg">
                      <MessageContent
                        :msg-type="msg.msgType"
                        :content="msg.content"
                        :status="msg.localStatus"
                        @media-loaded="onMessageMediaLoaded(msg)"
                      />
                    </slot>
                  </div>

                  <div v-if="isUploadingMessage(msg)" class="upload-indicator">
                    <span class="upload-spinner"></span>

                    <span
                      v-if="Number.isFinite(Number(msg.content?.uploadProgress))"
                      class="upload-progress"
                    >
                      {{ msg.content.uploadProgress }}%
                    </span>
                  </div>

                  <button
                    v-else-if="isFailedMessage(msg)"
                    type="button"
                    class="upload-failed-btn"
                    title="发送失败"
                    @click.stop="emit('retry-message', msg)"
                  >
                    !
                  </button>
                </div>

                <!-- 引用消息 -->
                <div
                  v-if="msg.quoteMsgId"
                  :class="[
                    'message-quote-bar',
                    msg.senderId === userStore.userInfo.userId ? 'sent' : 'received',
                    { media: isMediaMessage(msg) },
                  ]"
                  @click.stop="scrollToQuotedMessage(msg)"
                >
                  {{ buildMessageQuoteText(msg) }}
                </div>
                <!-- 时间 -->
                <div
                  :class="[
                    'message-time',
                    msg.senderId === userStore.userInfo.userId ? 'sent' : 'received',
                  ]"
                >
                  <slot name="time" :msg="msg">
                    {{ msg.createdTime }}
                  </slot>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 右键消息菜单 -->
        <div
          v-if="contextMenu.visible"
          ref="contextMenuRef"
          class="message-context-menu"
          :style="{
            left: contextMenu.x + 'px',
            top: contextMenu.y + 'px',
          }"
          @click.stop
        >
          <button type="button" @click="handleRecallMessage">撤回</button>
          <button type="button" @click="handleCopyMessage">复制</button>
          <button type="button" @click="handleForwardMessage">转发</button>
          <button type="button" @click="handleQuoteMessage">引用</button>
          <button type="button" class="danger" @click="handleDeleteMessage">删除</button>
        </div>

        <div v-if="showUnreadBubble" class="unread-bubble" @click="scrollToFirstUnread">
          ↑ {{ unreadCnt }} 条新消息
        </div>

        <div
          class="footer-resizer"
          @mousedown.prevent.stop="onResizeStart"
          title="拖动调整底部高度"
        ></div>

        <div class="footer" :style="{ height: `${footerHeight}px` }">
          <div v-if="showToolBar" class="icon-area">
            <div class="left-icons">
              <div ref="emojiPanelRef" class="emoji-wrapper">
                <button
                  type="button"
                  class="icon-btn"
                  :class="{ active: emojiPanelVisible }"
                  title="表情"
                  @click.stop="toggleEmojiPanel"
                >
                  😊
                </button>

                <div v-if="emojiPanelVisible" class="emoji-panel" @click.stop>
                  <button
                    v-for="emoji in emojiList"
                    :key="emoji"
                    type="button"
                    class="emoji-item"
                    @click="insertEmoji(emoji)"
                  >
                    {{ emoji }}
                  </button>
                </div>
              </div>
              <button type="button" class="icon-btn" title="收藏" @click="emit('collection')">
                ⭐
              </button>
              <button type="button" class="icon-btn" title="文件" @click="openFileSelector">
                📁
              </button>
              <button
                type="button"
                class="icon-btn"
                :class="{ recording: isRecording }"
                :title="isRecording ? '停止录音' : '语音输入'"
                @click="toggleAudioRecord"
              >
                {{ isRecording ? '⏹️' : '🎙️' }}
              </button>

              <input
                ref="fileInputRef"
                class="hidden-file-input"
                type="file"
                multiple
                accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.7z,.txt"
                @change="onFileInputChange"
              />
            </div>
            <div class="right-icons">
              <button
                v-if="showCallButtons"
                type="button"
                class="icon-btn"
                title="语音通话"
                @click="emit('call')"
              >
                📞
              </button>
              <button
                v-if="showCallButtons"
                type="button"
                class="icon-btn"
                title="视频通话"
                @click="emit('video-call')"
              >
                📹
              </button>
            </div>
          </div>

          <div
            class="input-area"
            :class="{ 'drag-over': isDragOver }"
            @dragenter.prevent="onDragEnter"
            @dragover.prevent="onDragOver"
            @dragleave.prevent="onDragLeave"
            @drop.prevent="onDropFiles"
          >
            <div v-if="props.pendingFiles.length" class="pending-file-list">
              <div v-for="item in props.pendingFiles" :key="item.id" class="pending-file-item">
                <img
                  v-if="item.msgType === 3"
                  class="pending-image"
                  :src="item.previewUrl"
                  alt="image"
                />

                <video
                  v-else-if="item.msgType === 4"
                  class="pending-video"
                  :src="item.previewUrl"
                  muted
                />

                <div v-else class="pending-file-icon">
                  {{ getPendingFileIcon(item) }}
                </div>

                <div class="pending-file-info">
                  <div class="pending-file-name">
                    {{ item.fileName }}
                  </div>
                  <div class="pending-file-size">
                    <template v-if="item.msgType === 2">
                      {{ formatDuration(item.duration) }}
                    </template>
                    <template v-else>
                      {{ formatFileSize(item.size) }}
                    </template>
                  </div>
                </div>

                <button
                  type="button"
                  class="pending-remove-btn"
                  @click.stop="removePendingFile(item.id)"
                >
                  ×
                </button>
              </div>
            </div>

            <div v-if="props.quoteMessage" class="quote-input-preview">
              <div class="quote-input-text">
                {{ buildQuotePreviewText(props.quoteMessage) }}
              </div>

              <button type="button" class="quote-close-btn" @click="emit('clear-quote-message')">
                ×
              </button>
            </div>

            <div class="input-row">
              <textarea
                ref="messageInputRef"
                v-model="draft"
                :placeholder="displayPlaceholder"
                class="message-input"
                :disabled="isInputDisabled"
                maxlength="2000"
                @click="saveInputCursor"
                @keyup="saveInputCursor"
                @select="saveInputCursor"
                @keydown="onKeyDown"
              />

              <button
                class="send-btn"
                type="button"
                :disabled="isInputDisabled || !canSend"
                @click="onSend"
              >
                发送
              </button>
            </div>

            <div v-if="isDragOver" class="drag-tip">松开添加到待发送</div>
          </div>
        </div>
      </div>

      <transition name="detail-panel-slide">
        <TalkDetailPanel
          v-if="detailPanelVisible"
          ref="detailPanelRef"
          :convType="props.convType"
          :user="peer"
          :group="group"
          :groupMembers="groupMembers"
          :myNickname="myNickname"
          :groupRemark="groupRemark"
          :default-avatar="defaultAvatar"
          :settings="settings"
          @click.stop
          @update-setting="(val) => emit('update-setting', val)"
          @save-group-field="(val) => emit('save-group-field', val)"
          @search-chat-history="emit('search-chat-history')"
          @change-group-avatar="emit('change-group-avatar')"
          @clear-chat-history="emit('clear-chat-history')"
          @delete-contact="emit('delete-contact')"
          @quit-group="emit('quit-group')"
          @add-member="emit('add-member')"
          @remove-member="emit('remove-member')"
          @add-admin="(val) => emit('update-group-member-role', val)"
          @remove-admin="(val) => emit('update-group-member-role', val)"
          @transfer-owner="(val) => emit('transfer-group-owner', val)"
          @disband-group="emit('disband-group')"
          @silence-all="emit('silence-all')"
          @unsilence-all="emit('unsilence-all')"
          @silence-members="(val) => emit('silence-members', val)"
          @unsilence-members="(val) => emit('unsilence-members', val)"
        />
      </transition>
    </div>
  </div>
</template>

<script setup lang="js">
import { computed, nextTick, ref, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../stores/userStore.js'
import { useGroupStore } from '../stores/groupStore.js'
import { useBusinessCardStore } from '../stores/businessCardStore.js'
import { useConversationStore } from '../stores/conversationStore.js'
import { useFriendStore } from '../stores/friendStore.js'
import { useToastStore } from '../stores/toastStore.js'
import TalkDetailPanel from './TalkDetailPanel.vue'
import MessageContent from './MessageContent.vue'

defineOptions({ name: 'TalkWindow' })

const userStore = useUserStore()
const groupStore = useGroupStore()
const businessCard = useBusinessCardStore()
const conversationStore = useConversationStore()
const friendStore = useFriendStore()
const toastStore = useToastStore()

const props = defineProps({
  peerId: { type: Number, default: null },
  groupId: { type: Number, default: null },
  convType: { type: Boolean, required: true },
  messages: { type: Array, default: () => [] },
  unreadCnt: { type: Number, default: 0 },
  defaultAvatar: {
    type: String,
    default: '',
  },
  modelValue: { type: String, default: '' },
  showToolBar: { type: Boolean, default: true },
  showCallButtons: { type: Boolean, default: true },
  autoScroll: { type: Boolean, default: true },
  autoScrollThreshold: { type: Number, default: 80 },
  historyLoading: { type: Boolean, default: false },
  hasMoreHistory: { type: Boolean, default: true },
  pendingFiles: {
    type: Array,
    default: () => [],
  },
  quoteMessage: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'retry-message',
  'sendText',
  'sendAudio',
  'sendImage',
  'sendVideo',
  'sendFile',
  'call',
  'video-call',
  'add-pending-file',
  'remove-pending-file',
  'clear-pending-files',
  'set-quote-message',
  'clear-quote-message',
  'recall-message',
  'delete-message',
  'forward-message',
  'emoji',
  'collection',
  'file',
  'load-more-history',
  'more-information',
  'delete-contact',
  'quit-group',
  'add-member',
  'remove-member',
  'search-chat-history',
  'change-group-avatar',
  'clear-chat-history',
  'update-setting',
  'save-group-field',
  'mark-conversation-read',
  'transfer-group-owner',
  'update-group-member-role',
  'silence-all',
  'unsilence-all',
  'silence-members',
  'unsilence-members',
])

onMounted(() => {
  nextTick(() => {
    console.log('TalkWindow mounted', props.unreadCnt)
    if (props.autoScroll) {
      scrollToBottom()
    }
  })
  emit('mark-conversation-read')
  window.addEventListener('pointerdown', handleGlobalPointerDown, true)
  console.log('TalkWindow mounted')
})

// from groupStore.groups
const peer = computed(() => userStore.getUser(props.peerId))
const friendship = computed(() => friendStore.gettersFriendship(props.peerId))
const group = computed(() => groupStore.getGroup(props.groupId))
const groupMembers = computed(() => groupStore.getGroupMembers(props.groupId))
//from conversationStore.groupMembers
const myNickname = computed(
  () =>
    groupStore.getGroupMemberProfile(props.groupId, userStore.userInfo.userId)?.value?.myNickname,
)
// from conversationStore.conversations
const groupRemark = computed(() => conversationStore.activeConversation?.groupRemark)

const headerTitle = computed(() => {
  if (props.convType === true) {
    return groupRemark.value || group.value?.groupName || '群聊'
  }
  return friendship.value?.remark || peer.value?.username || '私聊'
})

watch(
  () => [props.convType, props.peerId, props.groupId],
  async () => {
    if (props.convType) {
      if (!groupStore.getGroup(props.groupId)) {
        await groupStore.fetchGroups([props.groupId])
      }
    } else {
      userStore.getUser(props.peerId)
    }
  },
  { immediate: true },
)

const detailPanelRef = ref(null)
const detailPanelVisible = ref(false)
const moreInfoRef = ref(null)

const firstUnreadIndex = ref(null)
const unreadCnt = ref(props.unreadCnt)

const isInputDisabled = computed(() => {
  if (!props.convType) return false
  const currentGroup = group.value
  if (!currentGroup) return true

  const currentMember = groupMembers.value?.find(
    (m) => m.memberUserId === userStore.userInfo.userId,
  )
  const isAllSilenced = currentGroup.status === 2

  const isAdminOrOwner = currentMember && (currentMember.role === 1 || currentMember.role === 2)

  const isPersonallySilenced = currentMember?.silence === true

  if ((isAllSilenced && !isAdminOrOwner) || isPersonallySilenced) {
    return true
  }

  return false
})

const displayPlaceholder = computed(() => {
  if (isInputDisabled.value && props.convType === true) {
    if (!group.value) return '无法在退出的群聊中发送消息'
    else if (group.value?.status === 2) return '全员禁言中' // 或者根据具体原因显示 '你已被禁言'
    return '你已被禁言'
  }
  return '请输入内容...'
})

const showUnreadBubble = computed(() => {
  return unreadCnt.value > 0
})

watch(
  () => props.messages,
  () => {
    if (unreadCnt.value > 0) {
      firstUnreadIndex.value = props.messages.length - unreadCnt.value
    } else {
      firstUnreadIndex.value = null
    }
  },
  { immediate: true, deep: true },
)

function scrollToFirstUnread() {
  if (firstUnreadIndex.value === null) return

  const el = chatHistoryRef.value
  const msgEl = el.children[firstUnreadIndex.value]
  console.log('scrollToFirstUnread', msgEl, unreadCnt.value)
  if (msgEl) {
    msgEl.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
    })
  }

  unreadCnt.value = 0
}

function toggleDetailPanel() {
  detailPanelVisible.value = !detailPanelVisible.value
}

function closeDetailPanel() {
  detailPanelVisible.value = false
}

const settings = ref({
  muted: computed(() => conversationStore.activeConversation.muted ?? false),
  pinned: computed(() => conversationStore.activeConversation.pinned ?? false),
})

const showSenderName = (msg) => {
  return msg.senderId !== userStore.userInfo.userId && props.convType === true
}

// 去群里拿群成员名，私聊则不显示
const getMemberName = (senderId) => {
  const member = groupStore.getGroupMemberProfile(props.groupId, senderId)
  return (
    member?.value?.remark ||
    member?.value?.myNickname ||
    member?.value?.username ||
    '用户' + senderId
  )
}

function isMediaMessage(msg) {
  return [2, 3, 4, 5].includes(Number(msg?.msgType))
}

function isUploadingMessage(msg) {
  return (
    msg?.localStatus === 'uploading' ||
    msg?.localStatus === 'sending' ||
    msg?.content?.uploadStatus === 'uploading' ||
    msg?.content?.uploadStatus === 'sending'
  )
}

function isFailedMessage(msg) {
  return msg?.localStatus === 'failed' || msg?.content?.uploadStatus === 'failed'
}

function isSystemMessage(msg) {
  return msg?.msgType === 6 || msg?.senderId == null || msg?.recallFlag === true
}

function getSystemMessageText(msg) {
  if (msg?.recallFlag === true) {
    if (msg?.senderId === userStore.userInfo.userId) {
      return '你撤回了一条消息'
    } else if (msg?.convType === false) {
      return '对方撤回了一条消息'
    } else {
      return getMemberName(msg?.senderId) + '撤回了一条消息'
    }
  }
  const content = msg?.content || {}
  const systemType = content.systemType
  const data = content.data || {}
  if (systemType === 'group_create') {
    const ownerName = getMemberName(data.ownerUserId)
    return ownerName + '创建了群聊'
  } else if (systemType === 'members_join') {
    const inviterName = getMemberName(data.inviterUserId)
    const inviteesName = data.inviteeUserIds
      .map((userId) => {
        return getMemberName(userId)
      })
      .join('、')
    return inviterName + '邀请了' + inviteesName + '加入了群聊'
  } else if (systemType === 'announcement_post') {
    const posterName = getMemberName(data.posterUserId)
    return posterName + '发布了新公告'
  } else if (systemType === 'silence_all') {
    const operatorName = getMemberName(data.userId)
    return operatorName + '将群聊禁言'
  } else if (systemType === 'unsilence_all') {
    const operatorName = getMemberName(data.userId)
    return operatorName + '将群聊解除禁言'
  } else if (systemType === 'group_disband') {
    return '群聊已解散'
  }

  return '系统消息'
}

const chatHistoryRef = ref(null)

const footerMinHeight = 180
const footerMaxHeight = 360
const footerHeight = ref(220)

const topLoadThreshold = 30
const loadingMoreLock = ref(false)
const prependPending = ref(false)
const prevScrollHeight = ref(0)

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v))
}

let resizing = false
let startY = 0
let startH = 0

function onResizeStart(e) {
  e.preventDefault()
  resizing = true
  startY = e.clientY
  startH = footerHeight.value

  window.addEventListener('mousemove', onResizing)
  window.addEventListener('mouseup', onResizeEnd, { once: true })
}

function onResizing(e) {
  if (!resizing) return
  const delta = startY - e.clientY
  footerHeight.value = clamp(startH + delta, footerMinHeight, footerMaxHeight)
}

function onResizeEnd() {
  resizing = false
  window.removeEventListener('mousemove', onResizing)
}

const draft = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const messageInputRef = ref(null)
const emojiPanelRef = ref(null)
const emojiPanelVisible = ref(false)

const cursorStart = ref(0)
const cursorEnd = ref(0)

const emojiList = [
  '😀',
  '😃',
  '😄',
  '😁',
  '😆',
  '😂',
  '🤣',
  '😊',
  '😇',
  '🙂',
  '🙃',
  '😉',
  '😍',
  '😘',
  '😗',
  '😙',
  '😚',
  '😋',
  '😜',
  '😝',
  '😛',
  '🤑',
  '🤗',
  '🤔',
  '🤭',
  '🤫',
  '😐',
  '😑',
  '😶',
  '😏',
  '😒',
  '🙄',
  '😬',
  '😌',
  '😔',
  '😪',
  '🤤',
  '😴',
  '😷',
  '🤒',
  '🤕',
  '🤢',
  '🤮',
  '🥵',
  '🥶',
  '🥳',
  '😎',
  '🤓',
  '🧐',
  '😕',
  '😟',
  '🙁',
  '☹️',
  '😮',
  '😯',
  '😲',
  '😳',
  '🥺',
  '😦',
  '😧',
  '😨',
  '😰',
  '😥',
  '😢',
  '😭',
  '😱',
  '😖',
  '😣',
  '😞',
  '😓',
  '😩',
  '😫',
  '😤',
  '😡',
  '😠',
  '🤬',
  '👍',
  '👎',
  '👏',
  '🙌',
  '🙏',
  '💪',
  '❤️',
  '💔',
  '🔥',
  '🎉',
]

function saveInputCursor() {
  const el = messageInputRef.value

  if (!el) {
    cursorStart.value = draft.value.length
    cursorEnd.value = draft.value.length
    return
  }

  cursorStart.value = el.selectionStart ?? draft.value.length
  cursorEnd.value = el.selectionEnd ?? draft.value.length
}

function toggleEmojiPanel() {
  if (isInputDisabled.value) return

  saveInputCursor()

  emojiPanelVisible.value = !emojiPanelVisible.value

  if (emojiPanelVisible.value) {
    window.removeEventListener('pointerdown', handleEmojiGlobalPointerDown, true)
    window.addEventListener('pointerdown', handleEmojiGlobalPointerDown, true)
  } else {
    window.removeEventListener('pointerdown', handleEmojiGlobalPointerDown, true)
  }
}

function closeEmojiPanel() {
  emojiPanelVisible.value = false
  window.removeEventListener('pointerdown', handleEmojiGlobalPointerDown, true)
}

function handleEmojiGlobalPointerDown(e) {
  if (!emojiPanelVisible.value) return

  const panelEl = emojiPanelRef.value

  if (panelEl && panelEl.contains(e.target)) {
    return
  }

  closeEmojiPanel()
}

function insertEmoji(emoji) {
  if (isInputDisabled.value) return

  const oldText = draft.value || ''

  const start = Math.min(cursorStart.value ?? oldText.length, oldText.length)
  const end = Math.min(cursorEnd.value ?? oldText.length, oldText.length)

  const nextText = oldText.slice(0, start) + emoji + oldText.slice(end)

  if (nextText.length > 2000) {
    toastStore.error('消息内容不能超过 2000 字')
    return
  }

  draft.value = nextText

  const nextCursor = start + emoji.length

  nextTick(() => {
    const el = messageInputRef.value
    if (!el) return

    el.focus()
    el.setSelectionRange(nextCursor, nextCursor)

    cursorStart.value = nextCursor
    cursorEnd.value = nextCursor

    closeEmojiPanel()
  })
}

function formatDuration(duration) {
  const total = Number(duration)

  if (!Number.isFinite(total) || total <= 0) {
    return '0s'
  }

  const minutes = Math.floor(total / 60)
  const seconds = total % 60

  if (minutes <= 0) {
    return `${seconds}s`
  }

  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('data:')) return url
  if (url.startsWith('/')) return url
  try {
    return new URL(url, import.meta.url).href
  } catch (e) {
    return url
  }
}

function resolveAvatar(msg) {
  const url = normalizeUrl(userStore.getUser(msg.senderId)?.avatar)
  return url || props.defaultAvatar
}

const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  msg: null,
})

const contextMenuRef = ref(null)

function openMessageMenu(e, msg) {
  if (isSystemMessage(msg)) return

  contextMenu.value = {
    visible: true,
    x: e.clientX,
    y: e.clientY,
    msg,
  }

  window.removeEventListener('pointerdown', handleGlobalPointerDown, true)
  window.addEventListener('pointerdown', handleGlobalPointerDown, true)

  window.removeEventListener('contextmenu', handleGlobalContextMenu, true)
  window.addEventListener('contextmenu', handleGlobalContextMenu, true)
}

function closeMessageMenu() {
  contextMenu.value.visible = false
  contextMenu.value.msg = null

  window.removeEventListener('pointerdown', handleGlobalPointerDown, true)
  window.removeEventListener('contextmenu', handleGlobalContextMenu, true)
}

function handleGlobalPointerDown(e) {
  if (contextMenu.value.visible) {
    const menuEl = contextMenuRef.value

    if (!menuEl || !menuEl.contains(e.target)) {
      closeMessageMenu()
    }
  }

  if (!detailPanelVisible.value) return

  const target = e.target

  const panel = detailPanelRef.value?.$el || detailPanelRef.value
  const moreBtn = moreInfoRef.value

  // 点击详情面板内部，不关闭
  if (panel && panel.contains(target)) {
    return
  }

  // 点击右上角 more 按钮，不由全局关闭，让 toggleDetailPanel 自己处理
  if (moreBtn && moreBtn.contains(target)) {
    return
  }

  closeDetailPanel()
}

function handleGlobalContextMenu(e) {
  if (!contextMenu.value.visible) return

  const menuEl = contextMenuRef.value

  // 在菜单内部右键，不关闭
  if (menuEl && menuEl.contains(e.target)) {
    return
  }

  closeMessageMenu()
}

function handleRecallMessage() {
  const msg = contextMenu.value.msg
  closeMessageMenu()

  if (!msg) return

  emit('recall-message', {
    ...msg,
    recallFlag: true,
  })
}

async function handleCopyMessage() {
  const msg = contextMenu.value.msg
  closeMessageMenu()

  if (!msg) return

  const text = getCopyText(msg)

  if (!text) return

  await navigator.clipboard.writeText(text)
}

function getCopyText(msg) {
  const content = normalizeMessageContent(msg.content)

  if (Number(msg.msgType) === 1) {
    return content.text || ''
  }

  if (content.url) {
    return content.url
  }

  return getMessageBrief(msg)
}

function handleForwardMessage() {
  const msg = contextMenu.value.msg
  closeMessageMenu()

  if (!msg) return

  emit('forward-message', msg)
}

function handleQuoteMessage() {
  const msg = contextMenu.value.msg
  closeMessageMenu()

  if (!msg) return

  emit('set-quote-message', msg)

  nextTick(() => {
    focusInput()
  })
}

function handleDeleteMessage() {
  const msg = contextMenu.value.msg
  closeMessageMenu()

  if (!msg) return

  emit('delete-message', msg)
}

function focusInput() {
  const textarea = messageInputRef.value
  textarea?.focus?.()

  nextTick(() => {
    saveInputCursor()
  })
}

async function onMessageMediaLoaded(msg) {
  // 正在上滑加载历史消息时，不能因为旧图片/旧视频加载完成而滚到底
  if (prependPending.value || loadingMoreLock.value) {
    return
  }

  // 用户本来就在底部，媒体加载后可以继续贴底
  if (isNearBottom.value) {
    await scrollToBottom(false)
    return
  }

  // 自己刚发送的最后一条媒体消息，可以滚到底
  const lastMsg = props.messages[props.messages.length - 1]
  const isLastMsg =
    String(lastMsg?.msgId || lastMsg?.clientMsgId || '') ===
    String(msg?.msgId || msg?.clientMsgId || '')

  if (isLastMsg && msg.senderId === userStore.userInfo.userId) {
    await scrollToBottom(true)
  }
}

const fileInputRef = ref(null)
const isDragOver = ref(false)
const dragCounter = ref(0)

// 文件数量限制
const MAX_PENDING_FILE_COUNT = 20

// 单文件大小限制
const MAX_IMAGE_SIZE = 20 * 1024 * 1024 // 图片 20MB
const MAX_VIDEO_SIZE = 200 * 1024 * 1024 // 视频 200MB
const MAX_AUDIO_FILE_SIZE = 50 * 1024 * 1024 // 音频文件 50MB
const MAX_FILE_SIZE = 100 * 1024 * 1024 // 普通文件 100MB

const canSend = computed(() => {
  return draft.value.trim().length > 0 || props.pendingFiles.length > 0
})

function hasDirectoryInDropEvent(e) {
  const items = Array.from(e.dataTransfer?.items || [])

  for (const item of items) {
    if (item.kind !== 'file') continue

    const entry = item.webkitGetAsEntry?.()

    if (entry?.isDirectory) {
      return true
    }
  }

  return false
}

function openFileSelector() {
  if (isInputDisabled.value) return
  if (!fileInputRef.value) return

  fileInputRef.value.value = ''
  fileInputRef.value.click()
}

function onFileInputChange(e) {
  const files = Array.from(e.target.files || [])
  addPendingFiles(files)
}

function onDragEnter() {
  if (isInputDisabled.value) return

  dragCounter.value++
  isDragOver.value = true
}

function onDragOver(e) {
  if (isInputDisabled.value) return

  e.dataTransfer.dropEffect = 'copy'
  isDragOver.value = true
}

function onDragLeave() {
  dragCounter.value--

  if (dragCounter.value <= 0) {
    dragCounter.value = 0
    isDragOver.value = false
  }
}

function onDropFiles(e) {
  if (isInputDisabled.value) return

  dragCounter.value = 0
  isDragOver.value = false

  if (hasDirectoryInDropEvent(e)) {
    showFileError('不支持发送文件夹，请选择具体文件')
    return
  }

  const files = Array.from(e.dataTransfer.files || [])
  addPendingFiles(files)
}

async function addPendingFiles(files) {
  if (!files.length) return

  const remainCount = MAX_PENDING_FILE_COUNT - props.pendingFiles.length

  if (remainCount <= 0) {
    showFileError(`最多只能同时发送 ${MAX_PENDING_FILE_COUNT} 个文件`)
    return
  }

  const selectedFiles = files.slice(0, remainCount)

  if (files.length > remainCount) {
    showFileError(`最多只能同时发送 ${MAX_PENDING_FILE_COUNT} 个文件，已自动忽略多余文件`)
  }

  for (const file of selectedFiles) {
    const msgType = getFileMsgType(file)

    if (!validateFile(file, msgType)) {
      continue
    }

    const item = await buildPendingFileItem(file, msgType)

    emit('add-pending-file', item)
  }
}

async function buildPendingFileItem(file, msgType) {
  const ext = getFileExt(file.name)
  const format = ext || getFormatFromMimeType(file.type)
  const previewUrl = URL.createObjectURL(file)

  const baseItem = {
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    file,
    msgType,
    fileName: file.name,
    size: file.size,
    ext,
    format,
    previewUrl,
    width: 0,
    height: 0,
    duration: 0,
    cover: '',
  }

  if (msgType === 3) {
    const imageInfo = await getImageInfo(previewUrl)
    baseItem.width = imageInfo.width
    baseItem.height = imageInfo.height
  }

  if (msgType === 4) {
    const videoInfo = await getVideoInfo(previewUrl)
    baseItem.width = videoInfo.width
    baseItem.height = videoInfo.height
    baseItem.duration = videoInfo.duration
  }

  return baseItem
}

function getFileMsgType(file) {
  const type = file.type || ''

  if (type.startsWith('image/')) {
    return 3
  }

  if (type.startsWith('video/')) {
    return 4
  }

  // 这里故意不返回 2。
  // 拖入或选择的音频文件，建议按普通文件发送。
  // msgType = 2 留给麦克风录音，也就是语音消息。
  return 5
}

function validateFile(file, msgType) {
  const ext = getFileExt(file.name)
  const isAudioFile = isAudioExt(ext) || (file.type || '').startsWith('audio/')

  if (msgType === 3) {
    if (file.size > MAX_IMAGE_SIZE) {
      showFileError(`图片不能超过 ${formatFileSize(MAX_IMAGE_SIZE)}`)
      return false
    }
    return true
  }

  if (msgType === 4) {
    if (file.size > MAX_VIDEO_SIZE) {
      showFileError(`视频不能超过 ${formatFileSize(MAX_VIDEO_SIZE)}`)
      return false
    }
    return true
  }

  if (msgType === 5) {
    if (isAudioFile && file.size > MAX_AUDIO_FILE_SIZE) {
      showFileError(`音频文件不能超过 ${formatFileSize(MAX_AUDIO_FILE_SIZE)}`)
      return false
    }

    if (!isAudioFile && file.size > MAX_FILE_SIZE) {
      showFileError(`文件不能超过 ${formatFileSize(MAX_FILE_SIZE)}`)
      return false
    }

    return true
  }

  return false
}

function removePendingFile(id) {
  const item = props.pendingFiles.find((item) => item.id === id)

  if (!item) return

  if (item.previewUrl) {
    URL.revokeObjectURL(item.previewUrl)
  }

  emit('remove-pending-file', id)
}

function clearPendingFiles() {
  for (const item of props.pendingFiles) {
    if (item.previewUrl) {
      URL.revokeObjectURL(item.previewUrl)
    }
  }

  emit('clear-pending-files')
}

function onSend() {
  if (isInputDisabled.value) return

  const text = draft.value.trim()
  let quoteMsgId = props.quoteMessage?.msgId || null
  let quoteUsed = false

  if (text) {
    emit('sendText', {
      msgType: 1,
      quoteMsgId,
      content: {
        text,
      },
    })

    quoteUsed = true
    emit('update:modelValue', '')
  }

  if (props.pendingFiles.length) {
    const filesToSend = [...props.pendingFiles]

    clearPendingFiles()

    for (const item of filesToSend) {
      emitPendingFile(item, quoteUsed ? null : quoteMsgId)
      quoteUsed = true
    }
  }

  if (quoteUsed) {
    emit('clear-quote-message')
  }
}

function emitPendingFile(item, quoteMsgId = null) {
  if (item.msgType === 2) {
    emit('sendAudio', {
      msgType: 2,
      file: item.file,
      quoteMsgId,
      content: {
        fileName: item.fileName,
        url: '',
        duration: item.duration,
        size: item.size,
        format: item.format,
        ext: item.ext,
      },
    })
    return
  }

  if (item.msgType === 3) {
    emit('sendImage', {
      msgType: 3,
      file: item.file,
      quoteMsgId,
      content: {
        fileName: item.fileName,
        url: '',
        width: item.width,
        height: item.height,
        size: item.size,
        format: item.format,
        ext: item.ext,
      },
    })
    return
  }

  if (item.msgType === 4) {
    emit('sendVideo', {
      msgType: 4,
      file: item.file,
      quoteMsgId,
      content: {
        fileName: item.fileName,
        url: '',
        duration: item.duration,
        width: item.width,
        height: item.height,
        size: item.size,
        cover: item.cover,
        format: item.format,
        ext: item.ext,
      },
    })
    return
  }

  emit('sendFile', {
    msgType: 5,
    file: item.file,
    quoteMsgId,
    content: {
      fileName: item.fileName,
      url: '',
      size: item.size,
      ext: item.ext,
    },
  })
}

function getImageInfo(url) {
  return new Promise((resolve) => {
    const img = new Image()

    img.onload = () => {
      resolve({
        width: img.width || 0,
        height: img.height || 0,
      })
    }

    img.onerror = () => {
      resolve({
        width: 0,
        height: 0,
      })
    }

    img.src = url
  })
}

function getVideoInfo(url) {
  return new Promise((resolve) => {
    const video = document.createElement('video')

    video.preload = 'metadata'

    video.onloadedmetadata = () => {
      resolve({
        width: video.videoWidth || 0,
        height: video.videoHeight || 0,
        duration: Math.round(video.duration || 0),
      })
    }

    video.onerror = () => {
      resolve({
        width: 0,
        height: 0,
        duration: 0,
      })
    }

    video.src = url
  })
}

function getFileExt(fileName = '') {
  const index = fileName.lastIndexOf('.')
  if (index === -1) return ''
  return fileName.slice(index + 1).toLowerCase()
}

function getFormatFromMimeType(mimeType = '') {
  if (!mimeType.includes('/')) return ''
  return mimeType.split('/')[1] || ''
}

function isAudioExt(ext = '') {
  return ['mp3', 'wav', 'aac', 'm4a', 'flac', 'ogg'].includes(ext.toLowerCase())
}

function getPendingFileIcon(item) {
  if (item.msgType === 2) {
    return '🎤'
  }

  if (isAudioExt(item.ext)) {
    return '🎵'
  }

  if (['pdf'].includes(item.ext)) {
    return '📕'
  }

  if (['doc', 'docx'].includes(item.ext)) {
    return '📄'
  }

  if (['xls', 'xlsx'].includes(item.ext)) {
    return '📊'
  }

  if (['ppt', 'pptx'].includes(item.ext)) {
    return '📑'
  }

  if (['zip', 'rar', '7z'].includes(item.ext)) {
    return '🗜️'
  }

  return '📎'
}

function formatFileSize(size) {
  const n = Number(size)

  if (!Number.isFinite(n) || n <= 0) return '0 B'

  if (n < 1024) {
    return `${n} B`
  }

  if (n < 1024 * 1024) {
    return `${(n / 1024).toFixed(1)} KB`
  }

  if (n < 1024 * 1024 * 1024) {
    return `${(n / 1024 / 1024).toFixed(1)} MB`
  }

  return `${(n / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function showFileError(msg) {
  toastStore.error(msg)
}

const highlightedQuoteMsgId = ref(null)

function buildMessageQuoteText(msg) {
  const quotedMsg = getQuotedMessage(msg)

  if (!quotedMsg) {
    return '引用消息不可用或已被删除'
  }

  return buildQuotePreviewText(quotedMsg)
}

function getQuotedMessage(msg) {
  if (!msg?.quoteMsgId) return null

  const quoteMsgId = String(msg.quoteMsgId)

  return props.messages.find((item) => {
    return String(item.msgId) === quoteMsgId || String(item.clientMsgId) === quoteMsgId
  })
}

async function scrollToQuotedMessage(msg) {
  const quotedMsg = getQuotedMessage(msg)

  if (!quotedMsg) {
    toastStore.error('引用消息不在当前已加载的聊天记录中')
    return
  }

  const targetId = String(quotedMsg.msgId || quotedMsg.clientMsgId || '')

  if (!targetId) {
    toastStore.error('无法定位引用消息')
    return
  }

  await nextTick()

  const el = chatHistoryRef.value
  if (!el) return

  const targetEl = el.querySelector(
    `[data-msg-id="${targetId}"], [data-client-msg-id="${targetId}"]`,
  )

  if (!targetEl) {
    toastStore.error('引用消息不在当前可见记录中')
    return
  }

  targetEl.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  })

  highlightedQuoteMsgId.value = targetId

  window.setTimeout(() => {
    if (highlightedQuoteMsgId.value === targetId) {
      highlightedQuoteMsgId.value = null
    }
  }, 1600)
}

function buildQuotePreviewText(msg) {
  if (!msg) return ''

  const name = getQuoteSenderName(msg)
  const brief = getMessageBrief(msg)

  return `${name}：${brief}`
}

function getQuoteSenderName(msg) {
  if (msg.senderId === userStore.userInfo.userId) {
    return '我'
  }

  if (props.convType) {
    return getMemberName(msg.senderId)
  }

  const friendship = friendStore.gettersFriendship(msg.senderId)
  const user = userStore.getUser(msg.senderId)

  return friendship?.remark || user?.username || `用户${msg.senderId}`
}

function getMessageBrief(msg) {
  const type = Number(msg.msgType)
  const content = normalizeMessageContent(msg.content)

  if (type === 1) {
    const text = content.text || ''
    return text.length > 50 ? text.slice(0, 50) + '...' : text
  }

  if (type === 2) return '[语音]'
  if (type === 3) return '[图片]'
  if (type === 4) return '[视频]'
  if (type === 5) return '[文件]'

  return '[消息]'
}

function normalizeMessageContent(content) {
  if (!content) return {}

  if (typeof content === 'object') return content

  try {
    return JSON.parse(content)
  } catch {
    return {
      text: String(content),
    }
  }
}

function onKeyDown(e) {
  if (e.key === 'Backspace') {
    handleBackspaceRemoveFile(e)
    return
  }

  if (e.key !== 'Enter') return
  if (e.shiftKey) return

  e.preventDefault()
  onSend()
}

function handleBackspaceRemoveFile(e) {
  if (draft.value.length > 0) return
  if (!props.pendingFiles.length) return

  e.preventDefault()

  const lastFile = props.pendingFiles[props.pendingFiles.length - 1]
  removePendingFile(lastFile.id)
}

const isRecording = ref(false)
const audioChunks = ref([])

let mediaRecorder = null
let audioStream = null
let audioStartTime = 0
let audioStopTimer = null

const MAX_AUDIO_DURATION = 60 // 最长 60 秒
const MIN_AUDIO_DURATION = 1 // 最短 1 秒
const MAX_AUDIO_SIZE = 10 * 1024 * 1024 // 语音最大 10MB

async function toggleAudioRecord() {
  if (isInputDisabled.value) return

  if (isRecording.value) {
    stopAudioRecord()
    return
  }

  await startAudioRecord()
}

async function startAudioRecord() {
  if (!navigator.mediaDevices?.getUserMedia) {
    showFileError('当前环境不支持录音')
    return
  }

  if (props.pendingFiles.length >= MAX_PENDING_FILE_COUNT) {
    showFileError(`最多只能同时发送 ${MAX_PENDING_FILE_COUNT} 个文件`)
    return
  }

  try {
    audioStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
    })

    const mimeType = getSupportedAudioMimeType()

    mediaRecorder = mimeType
      ? new MediaRecorder(audioStream, { mimeType })
      : new MediaRecorder(audioStream)

    audioChunks.value = []
    audioStartTime = Date.now()

    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) {
        audioChunks.value.push(e.data)
      }
    }

    mediaRecorder.onstop = handleAudioRecordStop

    mediaRecorder.start()
    isRecording.value = true

    audioStopTimer = window.setTimeout(() => {
      if (isRecording.value) {
        stopAudioRecord()
      }
    }, MAX_AUDIO_DURATION * 1000)
  } catch (e) {
    console.error('录音失败', e)
    showFileError('无法使用麦克风，请检查权限')
    cleanupAudioRecord()
  }
}

function stopAudioRecord() {
  if (!mediaRecorder) return

  if (mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  isRecording.value = false
}

async function handleAudioRecordStop() {
  const duration = Math.round((Date.now() - audioStartTime) / 1000)

  const mimeType = mediaRecorder?.mimeType || 'audio/webm'
  const blob = new Blob(audioChunks.value, { type: mimeType })

  cleanupAudioRecord()

  if (duration < MIN_AUDIO_DURATION) {
    showFileError('录音时间太短')
    return
  }

  if (blob.size > MAX_AUDIO_SIZE) {
    showFileError(`语音不能超过 ${formatFileSize(MAX_AUDIO_SIZE)}`)
    return
  }

  const format = getFormatFromMimeType(mimeType) || 'webm'
  const fileName = `audio_${Date.now()}.${format}`
  const file = new File([blob], fileName, { type: mimeType })

  const previewUrl = URL.createObjectURL(file)

  emit('add-pending-file', {
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    file,
    msgType: 2,
    fileName: '语音消息',
    size: file.size,
    ext: format,
    format,
    previewUrl,
    width: 0,
    height: 0,
    duration,
    cover: '',
    isAudio: true,
  })
}

function cleanupAudioRecord() {
  if (audioStopTimer) {
    clearTimeout(audioStopTimer)
    audioStopTimer = null
  }

  if (audioStream) {
    audioStream.getTracks().forEach((track) => track.stop())
    audioStream = null
  }

  mediaRecorder = null
  audioChunks.value = []
  isRecording.value = false
}

function getSupportedAudioMimeType() {
  const types = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/ogg']

  for (const type of types) {
    if (MediaRecorder.isTypeSupported(type)) {
      return type
    }
  }

  return ''
}

onUnmounted(() => {
  cleanupAudioRecord()
  closeMessageMenu()
  closeEmojiPanel()
  window.removeEventListener('pointerdown', handleGlobalPointerDown, true)
})

const isNearBottom = ref(true)

function onScroll() {
  const el = chatHistoryRef.value
  if (!el) return

  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  isNearBottom.value = distance <= props.autoScrollThreshold

  const isNearTop = el.scrollTop <= topLoadThreshold
  if (isNearTop && props.hasMoreHistory && !props.historyLoading && !loadingMoreLock.value) {
    loadingMoreLock.value = true
    prependPending.value = true
    prevScrollHeight.value = el.scrollHeight

    emit('load-more-history')
  }
}

async function scrollToBottom(force = false) {
  const el = chatHistoryRef.value
  if (!el) return

  if (!force && !isNearBottom.value) return

  await nextTick()

  const run = () => {
    el.scrollTop = el.scrollHeight
  }

  run()
  requestAnimationFrame(() => {
    run()
    requestAnimationFrame(run)
  })
}

const highlightedMsgId = ref(null)

async function scrollToMessage(msg) {
  const id = String(msg.msgId || msg.clientMsgId || '')

  if (!id) {
    toastStore.error('无法定位该消息')
    return
  }

  await nextTick()

  const el = chatHistoryRef.value
  if (!el) return

  const targetEl = el.querySelector(`[data-msg-id="${id}"], [data-client-msg-id="${id}"]`)

  if (!targetEl) {
    toastStore.error('该消息不在当前已加载的聊天记录中')
    return
  }

  targetEl.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  })

  highlightedMsgId.value = id

  window.setTimeout(() => {
    if (highlightedMsgId.value === id) {
      highlightedMsgId.value = null
    }
  }, 1600)
}

function isHighlightedMessage(msg) {
  const id = String(msg.msgId || msg.clientMsgId || '')
  return id && highlightedMsgId.value === id
}

defineExpose({
  scrollToMessage,
  scrollToBottom,
  closeDetailPanel,
})

watch(
  () => props.messages.length,
  async (newLen, oldLen) => {
    const el = chatHistoryRef.value
    if (!el) return

    const isPrepending = prependPending.value

    await nextTick()

    if (isPrepending && newLen > oldLen) {
      const newScrollHeight = el.scrollHeight
      const delta = newScrollHeight - prevScrollHeight.value

      el.scrollTop = delta

      requestAnimationFrame(() => {
        el.scrollTop = el.scrollTop + 0
      })

      prependPending.value = false
      loadingMoreLock.value = false
      return
    }

    const lastMsg = props.messages[props.messages.length - 1]
    const isMyNewMsg = lastMsg?.senderId === userStore.userInfo.userId

    if (isMyNewMsg) {
      await scrollToBottom(true)
      return
    }

    if (!props.autoScroll) return
    if (!isNearBottom.value) return

    await scrollToBottom(false)
  },
)

watch(
  () => props.historyLoading,
  (loading) => {
    if (!loading) {
      loadingMoreLock.value = false
      if (prependPending.value && !props.hasMoreHistory) {
        prependPending.value = false
      }
    }
  },
)
</script>

<style scoped>
.talk-window {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: calc(100% - 25% - 100% / 15);
  background-color: hsl(0, 0%, 96%);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  flex: none;
}

.remark {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  user-select: none;
}

.more-information {
  font-size: 20px;
  cursor: pointer;
  color: #999;
  user-select: none;
  transition: color 0.2s ease;
}

.more-information:hover,
.more-information.active {
  color: #333;
}

.divider {
  height: 1px;
  background-color: #e0e0e0;
  flex: none;
}

.body-container {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.main-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-history {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow-y: auto;
  background-color: #fafafa;
}

.history-loading {
  text-align: center;
  font-size: 12px;
  color: #999;
  padding: 8px 0 12px;
}

.message {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  cursor: default;
}

.message.sent {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message.received {
  margin-right: auto;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 10px;
  user-select: none;
  cursor: pointer;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message.sent .message-content-wrapper {
  align-items: flex-end;
}

.message.received .message-content-wrapper {
  align-items: flex-start;
}

.message-content {
  display: inline-block;
  position: relative;
  padding: 8px 12px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  white-space: pre-wrap;
  max-width: 32vw;
}

.message.received .message-content::before {
  content: '';
  position: absolute;
  top: 12px;
  left: -9px;
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-right: 9px solid rgba(0, 0, 0, 0.1);
}

.message.received .message-content::after {
  content: '';
  position: absolute;
  top: 12px;
  left: -8px;
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-right: 8px solid #fff;
}

.message.sent .message-content::before {
  content: '';
  position: absolute;
  top: 12px;
  right: -9px;
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 9px solid rgba(0, 0, 0, 0.1);
}

.message.sent .message-content::after {
  content: '';
  position: absolute;
  top: 12px;
  right: -8px;
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 8px solid #ccebff;
}

.message.sent .message-content {
  /* background-color: #1890ff; */
  background-color: #ccebff;
  color: #000000;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.message-time.sent {
  text-align: right;
  margin-right: 5px;
}

.message-time.received {
  text-align: left;
  margin-left: 5px;
}

.message-quote-bar {
  box-sizing: border-box;
  max-width: 32vw;
  margin-top: 6px;
  padding: 6px 9px;

  border-radius: 6px;
  border-left: 3px solid #c8c8c8;
  background-color: #eeeeee;

  color: #666;
  font-size: 12px;
  line-height: 1.4;

  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.message-quote-bar.media {
  max-width: 36vw;
}

.message-quote-bar.sent {
  align-self: flex-end;
  text-align: left;
  margin-right: 5px;
}

.message-quote-bar.received {
  align-self: flex-start;
  text-align: left;
  margin-left: 5px;
}

.message.system {
  justify-content: center;
  margin: 12px 0;
  cursor: default;
}

.system-message-content {
  max-width: 70%;
  padding: 5px 14px;
  border-radius: 999px;
  background-color: #c8c8c8;
  color: #fff;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
  word-break: break-word;
  box-shadow: none;
}

.footer {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
  box-sizing: border-box;
  flex: none;
  overflow: visible;
  user-select: none;
  z-index: 20;
}

.footer-resizer {
  height: 8px;
  flex: none;
  cursor: row-resize;
  background: transparent;
}

.icon-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 6px;
}

.left-icons,
.right-icons {
  display: flex;
  gap: 12px;
}

.icon-btn {
  font-size: 18px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

.emoji-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.icon-btn.active {
  color: #1890ff;
}

.emoji-panel {
  position: absolute;
  left: 0;
  bottom: 34px;
  z-index: 10030;

  width: 280px;
  max-height: 220px;
  padding: 10px;

  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;

  overflow-y: auto;

  border-radius: 10px;
  background-color: #fff;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
  border: 1px solid #eee;
}

.emoji-item {
  width: 28px;
  height: 28px;
  padding: 0;
  margin: 0;

  border: none;
  border-radius: 6px;
  background: transparent;

  display: flex;
  align-items: center;
  justify-content: center;

  font-size: 20px;
  line-height: 1;

  cursor: pointer;
  user-select: none;
  box-sizing: border-box;
  font-family: 'Apple Color Emoji', 'Segoe UI Emoji', 'Noto Color Emoji', sans-serif;
}

.emoji-item:hover {
  background-color: #f2f3f5;
}

.input-area {
  display: flex;
  align-items: stretch;
  gap: 8px;
  flex: 1;
  min-height: 0;
  margin-top: 10px;
}

.message-input {
  flex: 1;
  padding: 8px 12px;
  border: none;
  outline: none;
  font-size: 14px;
  resize: none;
  overflow-y: auto;
  line-height: 1.5;
  background: transparent;
  min-height: 0;
  max-height: none;
}

.send-btn {
  padding: 8px 16px;
  background-color: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  height: 32px;
  align-self: flex-end;
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.detail-panel-slide-enter-active,
.detail-panel-slide-leave-active {
  transition:
    transform 0.25s ease,
    opacity 0.25s ease;
}

.detail-panel-slide-enter-from,
.detail-panel-slide-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.detail-panel-slide-enter-to,
.detail-panel-slide-leave-from {
  opacity: 1;
  transform: translateX(0);
}

.sender-name {
  font-size: 12px;
  color: #999;
  margin-left: 5px;
  margin-bottom: 3px;
}

.unread-bubble {
  position: absolute;
  top: 20px;
  right: 20px;

  background: #1890ff;
  color: white;

  padding: 6px 12px;
  border-radius: 16px;

  font-size: 12px;
  cursor: pointer;

  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);

  transition: all 0.2s ease;
}

.unread-bubble:hover {
  background: #40a9ff;
  transform: translateY(-1px);
}

.hidden-file-input {
  display: none;
}

.input-area {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-height: 0;
  margin-top: 10px;
}

.input-row {
  display: flex;
  align-items: stretch;
  gap: 8px;
  flex: 1;
  min-height: 0;
}

.pending-file-list {
  max-height: 96px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0;
}

.pending-file-item {
  position: relative;
  width: 180px;
  height: 52px;
  box-sizing: border-box;
  padding: 6px 24px 6px 6px;
  border-radius: 8px;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  gap: 8px;
  user-select: none;
}

.pending-image,
.pending-video {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
  flex: none;
  background-color: #000;
}

.pending-file-icon {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 6px;
  background-color: #eef3ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.pending-file-info {
  flex: 1;
  min-width: 0;
}

.pending-file-name {
  font-size: 12px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-file-size {
  margin-top: 3px;
  font-size: 11px;
  color: #999;
}

.pending-remove-btn {
  position: absolute;
  top: 3px;
  right: 4px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  line-height: 18px;
  padding: 0;
}

.pending-remove-btn:hover {
  color: #333;
  background-color: #e5e7eb;
}

.input-area.drag-over {
  border: 2px dashed #1890ff;
  border-radius: 8px;
  background-color: #eef6ff;
}

.drag-tip {
  position: absolute;
  inset: 0;
  z-index: 10;
  border-radius: 8px;
  background-color: rgba(24, 144, 255, 0.12);
  color: #1890ff;
  font-size: 16px;
  font-weight: 500;

  display: flex;
  align-items: center;
  justify-content: center;

  pointer-events: none;
}

.icon-btn.recording {
  color: #f5222d;
}

.message-body-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-body-row.sent {
  flex-direction: row-reverse;
}

.message-body-row.received {
  flex-direction: row;
}

/* 媒体消息不包气泡 */
.message-content.no-bubble {
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  max-width: 36vw;
  white-space: normal;
}

/* 去掉媒体消息的小三角 */
.message.received .message-content.no-bubble::before,
.message.received .message-content.no-bubble::after,
.message.sent .message-content.no-bubble::before,
.message.sent .message-content.no-bubble::after {
  display: none;
}

.message-context-menu {
  position: fixed;
  z-index: 10020;
  width: 110px;
  padding: 6px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.message-context-menu button {
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  padding: 0 10px;
  font-size: 13px;
  cursor: pointer;
  color: #333;
}

.message-context-menu button:hover {
  background-color: #f2f3f5;
}

.message-context-menu button.danger {
  color: #f5222d;
}

/* 上传状态 */
.upload-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #999;
  font-size: 12px;
  user-select: none;
}

.upload-spinner {
  width: 15px;
  height: 15px;
  box-sizing: border-box;
  border: 2px solid rgba(0, 0, 0, 0.16);
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: upload-spin 0.8s linear infinite;
}

.upload-progress {
  min-width: 34px;
  color: #999;
}

.upload-failed-btn {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background-color: #f5222d;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  padding: 0;
  cursor: pointer;
  user-select: none;
}

@keyframes upload-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

.quote-input-preview {
  min-height: 34px;
  box-sizing: border-box;
  padding: 7px 10px;
  border-left: 3px solid #bfbfbf;
  border-radius: 6px;
  background-color: #f2f3f5;
  color: #666;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.quote-input-text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.quote-close-btn {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #999;
  cursor: pointer;
}

.message-quote-bar {
  cursor: pointer;
}

.message-quote-bar:hover {
  background-color: #e5e5e5;
}

.message.quote-highlight .message-content {
  animation: quote-highlight-flash 1.6s ease;
}

.message.quote-highlight .message-content.no-bubble {
  border-radius: 8px;
  animation: quote-highlight-flash-media 1.6s ease;
}

@keyframes quote-highlight-flash {
  0% {
    box-shadow: 0 0 0 0 rgba(24, 144, 255, 0);
  }

  20% {
    box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.25);
  }

  100% {
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }
}

@keyframes quote-highlight-flash-media {
  0% {
    background-color: transparent;
    box-shadow: none;
  }

  20% {
    background-color: rgba(24, 144, 255, 0.12);
    box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.18);
  }

  100% {
    background-color: transparent;
    box-shadow: none;
  }
}

.message.search-highlight .message-content {
  animation: search-highlight-flash 1.6s ease;
}

.message.search-highlight .message-content.no-bubble {
  border-radius: 8px;
  animation: search-highlight-flash-media 1.6s ease;
}

@keyframes search-highlight-flash {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }

  20% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.28);
  }

  100% {
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }
}

@keyframes search-highlight-flash-media {
  0% {
    background-color: transparent;
    box-shadow: none;
  }

  20% {
    background-color: rgba(34, 197, 94, 0.12);
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.18);
  }

  100% {
    background-color: transparent;
    box-shadow: none;
  }
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  transition: all 0.2s;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

::-webkit-scrollbar-corner {
  background: transparent;
}

.dark ::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
}

.dark ::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style>
