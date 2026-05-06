<template>
  <div v-if="visible" class="search-mask" @click="handleClose">
    <div class="search-dialog" @click.stop>
      <div class="dialog-header">
        <span class="dialog-title">查找聊天内容</span>
        <button type="button" class="close-btn" @click="handleClose">×</button>
      </div>

      <div class="dialog-body">
        <div class="filter-panel">
          <div class="filter-row">
            <label class="filter-label">发送者</label>
            <select v-model="senderId" class="filter-control">
              <option value="">全部发送者</option>
              <option
                v-for="sender in senderOptions"
                :key="sender.userId"
                :value="String(sender.userId)"
              >
                {{ sender.name }}
              </option>
            </select>
          </div>

          <div class="filter-row">
            <label class="filter-label">日期</label>
            <input v-model="date" type="date" class="filter-control" />
          </div>

          <div class="filter-row">
            <label class="filter-label">关键字</label>
            <input
              v-model.trim="keyword"
              type="text"
              class="filter-control"
              placeholder="搜索文本内容或文件名"
            />
          </div>

          <div class="filter-row type-filter-row">
            <label class="filter-label">消息类型</label>

            <div class="type-options">
              <label v-for="item in typeOptions" :key="item.value" class="type-option">
                <input v-model="selectedTypes" type="checkbox" :value="item.value" />
                <span>{{ item.label }}</span>
              </label>
            </div>
          </div>

          <div class="filter-actions">
            <button type="button" class="reset-btn" @click="resetFilters">重置</button>
          </div>
        </div>

        <div class="result-panel">
          <div class="result-header">
            <span>搜索结果</span>
            <span class="result-count">{{ filteredMessages.length }} 条</span>
          </div>

          <div ref="resultListRef" class="result-list" @scroll="onResultScroll">
            <div
              v-for="msg in filteredMessages"
              :key="msg.msgId || msg.clientMsgId"
              class="result-row"
              @click="handleLocate(msg)"
            >
              <img :src="getSenderAvatar(msg.senderId)" class="avatar" />

              <div class="result-content">
                <div class="result-main">
                  <span class="sender-name">{{ getSenderName(msg.senderId) }}</span>
                  <span class="message-type">{{ getTypeLabel(msg.msgType) }}</span>
                </div>

                <div class="result-brief">
                  {{ getMessageBrief(msg) }}
                </div>

                <div class="result-time">
                  {{ formatTime(msg.createdTime) }}
                </div>
              </div>
            </div>

            <div v-if="filteredMessages.length === 0" class="empty-text">无符合条件的信息</div>
            <div v-if="loading" class="loading-text">正在加载更多...</div>

            <div v-else-if="!hasMore && filteredMessages.length > 0" class="loading-text">
              没有更多结果了
            </div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <button type="button" class="footer-btn cancel-btn" @click="handleClose">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useUserStore } from '../stores/userStore.js'
import { useGroupStore } from '../stores/groupStore.js'
import { useFriendStore } from '../stores/friendStore.js'

defineOptions({
  name: 'ChatSearchModal',
})

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  messages: {
    type: Array,
    default: () => [],
  },
  convType: {
    type: Boolean,
    default: false,
  },
  groupId: {
    type: Number,
    default: null,
  },
  defaultAvatar: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  hasMore: {
    type: Boolean,
    default: true,
  },
  bottomThreshold: {
    type: Number,
    default: 40,
  },
  peerId: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['close', 'locate', 'load-more'])

const userStore = useUserStore()
const groupStore = useGroupStore()
const friendStore = useFriendStore()

const senderId = ref('')
const date = ref('')
const keyword = ref('')
const selectedTypes = ref([])

const resultListRef = ref(null)

const typeOptions = [
  { value: 1, label: '文本' },
  { value: 3, label: '图片' },
  { value: 4, label: '视频' },
  { value: 5, label: '文件' },
  { value: 2, label: '语音' },
]

const searchableMessages = computed(() => {
  return sortMessagesByMsgIdDesc(
    (props.messages || []).filter((msg) => {
      return !isSystemMessage(msg) && msg?.recallFlag === false
    }),
  )
})

const senderOptions = computed(() => {
  if (props.convType === true) {
    return getGroupSenderOptions()
  }

  return getPrivateSenderOptions()
})

const filteredMessages = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const selectedSenderId = senderId.value
  const selectedDate = date.value
  const typeSet = new Set(selectedTypes.value.map((item) => Number(item)))

  const result = searchableMessages.value.filter((msg) => {
    if (selectedSenderId && String(msg.senderId) !== selectedSenderId) {
      return false
    }

    if (selectedDate && getMessageDate(msg.createdTime) !== selectedDate) {
      return false
    }

    if (typeSet.size > 0 && !typeSet.has(Number(msg.msgType))) {
      return false
    }

    if (kw) {
      const text = getSearchableText(msg).toLowerCase()
      if (!text.includes(kw)) {
        return false
      }
    }

    return true
  })

  return sortMessagesByMsgIdDesc(result)
})

function getPrivateSenderOptions() {
  const map = new Map()

  addSenderOption(map, userStore.userInfo.userId)

  if (props.peerId) {
    addSenderOption(map, props.peerId)
  }

  return Array.from(map.values())
}

function getGroupSenderOptions() {
  const map = new Map()

  const members = groupStore.getGroupMembers(props.groupId) || []

  for (const member of members) {
    const userId = member.memberUserId || member.userId

    if (!userId) continue

    const profile = groupStore.getGroupMemberProfile(props.groupId, userId)?.value

    map.set(String(userId), {
      userId,
      name: profile?.remark || profile?.myNickname || profile?.username || `用户${userId}`,
      avatar: normalizeUrl(profile?.avatar) || props.defaultAvatar,
    })
  }

  // 兜底：如果群成员还没加载出来，就至少用当前已加载消息里的发送者补一下
  for (const msg of searchableMessages.value) {
    if (!msg.senderId) continue
    addSenderOption(map, msg.senderId)
  }

  return Array.from(map.values())
}

function addSenderOption(map, userId) {
  if (!userId) return

  const key = String(userId)

  if (map.has(key)) return

  const user = userStore.getUser(userId)
  const friendship = friendStore.gettersFriendship(userId)

  map.set(key, {
    userId,
    name: getSenderName(userId),
    avatar: normalizeUrl(user?.avatar) || props.defaultAvatar,
    remark: friendship?.remark || '',
    username: user?.username || '',
  })
}

function sortMessagesByMsgIdDesc(list) {
  return [...list].sort(compareMsgIdDesc)
}

function compareMsgIdDesc(a, b) {
  const aId = getMsgIdBigInt(a)
  const bId = getMsgIdBigInt(b)

  if (aId !== null && bId !== null) {
    if (aId === bId) return 0
    return aId > bId ? -1 : 1
  }

  const aTime = new Date(a.createdTime || 0).getTime()
  const bTime = new Date(b.createdTime || 0).getTime()

  return bTime - aTime
}

function getMsgIdBigInt(msg) {
  const id = msg?.msgId

  if (id === null || id === undefined || id === '') {
    return null
  }

  try {
    return BigInt(String(id))
  } catch {
    return null
  }
}

function isSystemMessage(msg) {
  return msg?.msgType === 6 || msg?.senderId == null
}

function normalizeContent(content) {
  if (!content) return {}

  if (typeof content === 'object') {
    return content
  }

  try {
    return JSON.parse(content)
  } catch {
    return {
      text: String(content),
    }
  }
}

function getSearchableText(msg) {
  const content = normalizeContent(msg.content)

  if (Number(msg.msgType) === 1) {
    return content.text || ''
  }

  return [getMessageBrief(msg), content.fileName, content.url, content.ext, content.format]
    .filter(Boolean)
    .join(' ')
}

function getMessageBrief(msg) {
  const type = Number(msg.msgType)
  const content = normalizeContent(msg.content)

  if (type === 1) {
    return content.text || ''
  }

  if (type === 2) return '[语音]'
  if (type === 3) return '[图片]'
  if (type === 4) return '[视频]'
  if (type === 5) return content.fileName ? `[文件] ${content.fileName}` : '[文件]'

  return '[消息]'
}

function getTypeLabel(msgType) {
  const item = typeOptions.find((type) => type.value === Number(msgType))
  return item?.label || '消息'
}

function getSenderName(userId) {
  if (Number(userId) === Number(userStore.userInfo.userId)) {
    return '我'
  }

  if (props.convType) {
    const member = groupStore.getGroupMemberProfile(props.groupId, userId)
    return (
      member?.value?.remark ||
      member?.value?.myNickname ||
      member?.value?.username ||
      `用户${userId}`
    )
  }

  const friendship = friendStore.gettersFriendship(userId)
  const user = userStore.getUser(userId)

  return friendship?.remark || user?.username || `用户${userId}`
}

function getSenderAvatar(userId) {
  const user = userStore.getUser(userId)
  return normalizeUrl(user?.avatar) || props.defaultAvatar
}

function normalizeUrl(url) {
  if (!url) return ''

  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  if (url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }

  const baseUrl = import.meta.VITE_SOURCES_URL

  if (url.startsWith('/')) {
    return baseUrl + url
  }

  return baseUrl + '/' + url
}

function getMessageDate(time) {
  if (!time) return ''

  const str = String(time)

  if (str.length >= 10) {
    return str.slice(0, 10)
  }

  return ''
}

function formatTime(time) {
  if (!time) return ''

  return String(time).replace('T', ' ').slice(0, 19)
}

function resetFilters() {
  senderId.value = ''
  date.value = ''
  keyword.value = ''
  selectedTypes.value = []
}

function handleClose() {
  emit('close')
  resetFilters()
}

function handleLocate(msg) {
  emit('locate', msg)
}

function onResultScroll() {
  const el = resultListRef.value
  if (!el) return

  const distance = el.scrollHeight - el.scrollTop - el.clientHeight

  if (distance <= props.bottomThreshold) {
    loadMoreSearchResult()
  }
}

function loadMoreSearchResult() {
  if (props.loading) return
  if (!props.hasMore) return

  emit('load-more', getCurrentSearchParams())
}

function getCurrentSearchParams() {
  return {
    senderId: senderId.value || null,
    date: date.value || null,
    keyword: keyword.value.trim() || null,
    msgTypes: selectedTypes.value.map((item) => Number(item)),
    msgId: getLastResultMsgId(),
  }
}

function getLastResultMsgId() {
  const last = filteredMessages.value[filteredMessages.value.length - 1]
  return last?.msgId || null
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      resetFilters()
    }
  },
)

function searchBySender(targetSenderId) {
  if (!targetSenderId) {
    return searchableMessages.value
  }

  return sortMessagesByMsgIdDesc(
    searchableMessages.value.filter((msg) => {
      return String(msg.senderId) === String(targetSenderId)
    }),
  )
}

function searchByDate(targetDate) {
  if (!targetDate) {
    return searchableMessages.value
  }

  return sortMessagesByMsgIdDesc(
    searchableMessages.value.filter((msg) => {
      return getMessageDate(msg.createdTime) === targetDate
    }),
  )
}

function searchByKeyword(targetKeyword) {
  const kw = String(targetKeyword || '')
    .trim()
    .toLowerCase()

  if (!kw) {
    return searchableMessages.value
  }

  return sortMessagesByMsgIdDesc(
    searchableMessages.value.filter((msg) => {
      return getSearchableText(msg).toLowerCase().includes(kw)
    }),
  )
}

function searchByMsgType(targetMsgType) {
  if (targetMsgType === null || targetMsgType === undefined || targetMsgType === '') {
    return searchableMessages.value
  }

  const type = Number(targetMsgType)

  return sortMessagesByMsgIdDesc(
    searchableMessages.value.filter((msg) => {
      return Number(msg.msgType) === type
    }),
  )
}

defineExpose({
  loadMoreSearchResult,
  searchBySender,
  searchByDate,
  searchByKeyword,
  searchByMsgType,
  getCurrentSearchParams,
})
</script>

<style scoped>
.search-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-dialog {
  width: 820px;
  height: 580px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-header {
  height: 56px;
  padding: 0 18px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  font-size: 22px;
  color: #888;
  cursor: pointer;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.dialog-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.filter-panel {
  width: 300px;
  padding: 16px;
  border-right: 1px solid #f0f0f0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filter-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: #666;
}

.filter-control {
  width: 100%;
  height: 36px;
  padding: 0 10px;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  color: #333;
}

.filter-control:focus {
  border-color: #22c55e;
}

.type-filter-row {
  min-height: 130px;
}

.type-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-option {
  min-width: 84px;
  height: 32px;
  padding: 0 8px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  user-select: none;
}

.type-option:hover {
  background: #eef6ff;
}

.filter-actions {
  margin-top: auto;
}

.reset-btn {
  width: 100%;
  height: 36px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #fff;
  color: #333;
  cursor: pointer;
}

.reset-btn:hover {
  background: #fafafa;
}

.result-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.result-header {
  height: 52px;
  padding: 0 16px;
  border-bottom: 1px solid #f6f6f6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #333;
  font-size: 15px;
  font-weight: 600;
}

.result-count {
  font-size: 13px;
  color: #999;
  font-weight: 400;
}

.result-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.result-row {
  min-height: 72px;
  padding: 10px 16px;
  box-sizing: border-box;
  display: flex;
  align-items: flex-start;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.result-row:hover {
  background: #f8faf8;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
}

.result-content {
  flex: 1;
  min-width: 0;
  margin-left: 12px;
}

.result-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sender-name {
  font-size: 14px;
  color: #222;
  font-weight: 500;
}

.message-type {
  padding: 2px 6px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1677ff;
  font-size: 11px;
}

.result-brief {
  margin-top: 5px;
  font-size: 13px;
  color: #555;
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.result-time {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.empty-text {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: #999;
}

.dialog-footer {
  height: 64px;
  padding: 0 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.footer-btn {
  min-width: 84px;
  height: 36px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  box-sizing: border-box;
}

.cancel-btn {
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #333;
}

.cancel-btn:hover {
  background: #fafafa;
}
.loading-text {
  padding: 14px 16px;
  text-align: center;
  font-size: 13px;
  color: #999;
}
</style>
