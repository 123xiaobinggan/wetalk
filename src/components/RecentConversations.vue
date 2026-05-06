<!-- src/components/RecentConversations.vue -->
<template>
  <div class="recent-conversation">
    <div class="search-bar">
      <input type="text" placeholder="搜索" class="search-input" v-model="searchQuery" />
      <div class="add-button" ref="plusButton" @click="handlePlusClick">
        <img src="../assets/sidebar/add.png" class="add-icon" />
      </div>
    </div>
    <transition name="slide-fade">
      <div v-if="showStatusTip" class="connection-status-bar" :class="statusClass">
        {{ wsStatus }}
      </div>
    </transition>
    <div class="conversation-list">
      <div
        class="conversation-item"
        v-for="conversation in conversationsList"
        :key="conversation.convId"
        :class="{
          active: activeConversationId === conversation.convId,
          pinned: conversation.pinned === true,
        }"
        @click="selectConversation(conversation.convId)"
        @contextmenu.prevent="handleContextMenu($event, conversation)"
      >
        <!-- 左侧头像 -->
        <img :src="getAvatar(conversation)" class="avatar" />

        <!-- 右侧内容区域 -->
        <div class="content">
          <!-- 上方：姓名 -->
          <div class="remark">{{ getConversationRemark(conversation) }}</div>

          <!-- 下方：未读消息数 + 最新消息内容 -->
          <div class="message-info">
            <span
              class="unread-count"
              :class="{ 'muted-unread': conversation.muted === true }"
              v-if="conversation.unreadCnt > 0"
            >
              {{ conversation.unreadCnt }}
            </span>
            <span class="latest-message">{{ getLastMsgBrief(conversation) }}</span>
          </div>
        </div>

        <!-- 最右侧：时间和免打扰图标 -->
        <div class="right-section">
          <div class="time">{{ formatTime(conversation.lastMsgTime) }}</div>
          <div class="do-not-disturb" v-if="conversation.muted">
            <!-- 免打扰图标：闹铃 + 斜杠 -->
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="feather feather-bell-off"
            >
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
              <path d="M18.63 13A17.89 17.89 0 0 1 18 8"></path>
              <path d="M6.26 6.26A5.86 5.86 0 0 0 6 8c0 7-3 9-3 9h14"></path>
              <path d="M18 8a6 6 0 0 0-9.35-4.93"></path>
              <line x1="1" y1="1" x2="23" y2="23"></line>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <transition name="fade">
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px` }"
      >
        <div class="menu-item" @click="togglePin">
          <span>{{ isTargetPinned ? '取消置顶' : '置顶' }}</span>
        </div>
        <div class="menu-item" @click="toggleMute">
          <span>{{ isTargetMuted ? '取消免打扰' : '免打扰' }}</span>
        </div>
        <div class="menu-item" @click="markAsRead">
          <span>标为已读</span>
        </div>
        <div class="menu-item danger" @click="deleteConversation">
          <span>删除</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, defineProps, computed, watch, reactive } from 'vue'
import { useGroupStore } from '/src/stores/groupStore.js'
import { useUserStore } from '/src/stores/userStore.js'
import { useFriendStore } from '/src/stores/friendStore.js'
defineOptions({
  name: 'RecentConversations',
})
// 定义接收的 props
const props = defineProps({
  wsStatus: {
    type: String,
    required: true,
    default: 'connected',
  },
  conversationsList: {
    type: Array,
    required: true,
    default: () => [],
  },
  activeConversationId: {
    type: [Number, null],
    required: true,
    default: null,
  },
})

const groupStore = useGroupStore()
const userStore = useUserStore()
const friendStore = useFriendStore()

const emit = defineEmits([
  'selectConversation',
  'handlePlusClick',
  'togglePin',
  'toggleMute',
  'markAsRead',
  'deleteConversation',
])
const searchQuery = ref('')

const plusButton = ref(null)

const isTargetPinned = computed(() => {
  const conv = props.conversationsList.find((c) => c.convId === contextMenu.targetConvId)
  return conv?.pinned === true
})

const isTargetMuted = computed(() => {
  const conv = props.conversationsList.find((c) => c.convId === contextMenu.targetConvId)
  return conv?.muted === true
})
const handlePlusClick = () => {
  const button = plusButton.value
  const rect = button.getBoundingClientRect()
  const right = rect.right
  const bottom = rect.bottom
  console.log(right, bottom)
  emit('handlePlusClick', { x: right - 5, y: bottom - 10 })
}

const selectConversation = (convId) => {
  console.log(`点击了conversation: ${convId}`)
  emit('selectConversation', convId)
}

const getAvatar = (conversation) => {
  if (conversation.convType === false) {
    const user = userStore.getUser(conversation.peerId)
    return user?.avatar
  } else {
    const group = groupStore.getGroup(conversation.groupId)
    return group?.groupAvatar
  }
}

const getConversationRemark = (conversation) => {
  if (conversation.convType === false) {
    const user = userStore.getUser(conversation.peerId)
    const friendship = friendStore.gettersFriendship(conversation.peerId)
    return friendship?.remark || user?.username
  } else {
    const group = groupStore.getGroup(conversation.groupId)
    return conversation?.groupRemark || group?.groupName
  }
}

const getLastMsgBrief = (conversation) => {
  if (conversation.convType === false) {
    return conversation.lastMsgBrief
  } else {
    const member = groupStore.getGroupMemberProfile(
      conversation.groupId,
      conversation.lastMsgSenderId,
    )
    var memberName =
      member?.value?.remark || member?.value?.myNickname || member?.value?.username || ''
    if (memberName) {
      memberName += ': '
    }
    return (memberName ?? '') + (conversation.lastMsgBrief ?? '')
  }
}

const formatTime = (time) => {
  if (!time) {
    return ''
  }
  const date = typeof time === 'string' ? new Date(time) : time
  const now = new Date()

  // 获取今天零点
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  // 获取昨天零点
  const yesterdayStart = new Date(todayStart)
  yesterdayStart.setDate(yesterdayStart.getDate() - 1)
  // 获取一周前零点（7天内）
  const weekStart = new Date(todayStart)
  weekStart.setDate(weekStart.getDate() - 6)

  const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  // 格式化小时和分钟（两位数）
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const timeStr = `${hours}:${minutes}`

  // 判断日期范围
  if (targetDate.getTime() === todayStart.getTime()) {
    // 今天
    return timeStr
  } else if (targetDate.getTime() === yesterdayStart.getTime()) {
    // 昨天
    return `昨天 ${timeStr}`
  } else if (targetDate.getTime() >= weekStart.getTime()) {
    // 一周内（今天和昨天已排除）
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return weekdays[date.getDay()]
  } else {
    // 更早
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${month}-${day}`
  }
}

const showStatusTip = ref(false)
let statusTimer = null

// 计算属性用于绑定 class
const statusClass = computed(() => {
  return {
    'status-connected': props.wsStatus === 'connected',
    'status-disconnected': props.wsStatus === 'disconnected',
    'status-reconnecting': props.wsStatus === 'reconnecting',
  }
})

watch(
  () => props.wsStatus,
  (newStatus) => {
    if (statusTimer) {
      clearTimeout(statusTimer)
      statusTimer = null
    }
    console.log('wsStatus:', newStatus)
    if (newStatus === 'connected') {
      showStatusTip.value = true

      statusTimer = setTimeout(() => {
        showStatusTip.value = false
      }, 1500)
    } else {
      showStatusTip.value = true
    }
  },
  { immediate: true },
)

// --- 新增：右键菜单相关状态 ---
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  targetConvId: null, // 当前右键点击的会话ID
})

// 关闭菜单
const closeContextMenu = () => {
  contextMenu.visible = false
  contextMenu.targetConvId = null
}

// 处理右键点击
const handleContextMenu = (event, conversation) => {
  event.preventDefault() // 阻止默认浏览器右键菜单

  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.targetConvId = conversation.convId
  contextMenu.visible = true
}
// 1. 置顶/取消置顶
const togglePin = () => {
  const conv = props.conversationsList.find((c) => c.convId === contextMenu.targetConvId)
  if (!conv) return

  emit('togglePin', { convId: conv.convId, pinned: !conv.pinned })
  closeContextMenu()
}

// 2. 免打扰/取消免打扰
const toggleMute = () => {
  const conv = props.conversationsList.find((c) => c.convId === contextMenu.targetConvId)
  if (!conv) return

  emit('toggleMute', { convId: conv.convId, muted: !conv.muted })
  closeContextMenu()
}

// 3. 标为已读
const markAsRead = () => {
  const conv = props.conversationsList.find((c) => c.convId === contextMenu.targetConvId)
  if (!conv) return

  emit('markAsRead', { convId: conv.convId })

  closeContextMenu()
}

// 4. 删除会话
const deleteConversation = () => {
  const conv = props.conversationsList.find((c) => c.convId === contextMenu.targetConvId)
  if (!conv) return

  emit('deleteConversation', { convId: conv.convId })

  closeContextMenu()
}

// 监听全局点击，点击其他地方关闭菜单
watch(contextMenu, (newVal) => {
  if (newVal.visible) {
    const handleClickOutside = () => {
      closeContextMenu()
      window.removeEventListener('click', handleClickOutside)
    }
    // 使用 setTimeout 确保不会立即触发关闭（因为当前的 right-click 可能冒泡）
    setTimeout(() => {
      window.addEventListener('click', handleClickOutside)
    }, 0)
  }
})
</script>

<style scoped>
.recent-conversation {
  width: calc(100% / 4);
  /* 宽度为父容器的四分之一 */
  margin-left: 0px;
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  height: 100vh;
  overflow-y: hidden;
  overflow-x: hidden;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #ffffff;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.search-input:focus {
  border-color: #1890ff;
}

.add-button {
  margin-left: 10px;
  border-radius: 5px;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  /* background-color: #f0f0f0; */
  transition: color 0.2s ease;
}

.add-icon {
  width: 25px;
}

.context-menu {
  position: fixed; /* 使用 fixed 以相对于视口定位，避免受父容器 overflow 影响 */
  z-index: 1000;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 120px;
  font-size: 14px;
  color: #333;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item.danger {
  color: #ff4d4f;
}

.menu-item.danger:hover {
  background-color: #fff1f0;
}

/* 菜单淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/*状态栏样式 */
.connection-status-bar {
  padding: 8px 16px;
  font-size: 12px;
  text-align: center;
  font-weight: 500;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.status-connected {
  background-color: #f6ffed; /* 浅绿色背景 */
  color: #52c41a; /* 绿色文字 */
  border-bottom: 1px solid #b7eb8f;
}

.status-disconnected {
  background-color: #fff1f0; /* 淡红色背景 */
  color: #ff4d4f; /* 红色文字 */
  border-bottom: 1px solid #ffa39e;
}

.status-reconnecting {
  background-color: #fffbe6; /* 浅黄色背景 */
  color: #faad14; /* 黄色文字 */
  border-bottom: 1px solid #ffe58f;
}

/* 过渡动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
  max-height: 50px; /* 假设最大高度 */
  opacity: 1;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-bottom-width: 0;
}

.conversation-list {
  height: calc(100% - 50px);
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
}

/* 宽度 */
.conversation-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

/* 轨道 */
.conversation-list::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 4px;
}

/* 默认：thumb 隐藏 */
.conversation-list::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
  transition: background 0.2s;
}

/* hover 时显示 thumb */
.conversation-list:hover::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

/* hover 且指向 thumb 时更深 */
.conversation-list:hover::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px 4px;
  border-bottom: 2px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #dde0e0;
}

.conversation-item.pinned {
  background-color: #faf0e6;
}

.conversation-item.active.pinned {
  background-color: #dde0e0;
}

.conversation-item.pinned:hover {
  background-color: #f1f3f3;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.remark {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;

  white-space: nowrap; /* 不换行 */
  overflow: hidden; /* 隐藏溢出部分 */
  text-overflow: ellipsis; /* 显示省略号 */
  max-width: 100%;
}

.message-info {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
  flex: 1;
  min-width: 0;
}

.unread-count {
  background-color: #ff4d4f;
  color: #fff;
  border-radius: 8px;
  padding: 2px 6px;
  margin-right: 8px;
  font-size: 10px;
  flex-shrink: 0;
}

.unread-count.muted-unread {
  background-color: #e5e5e5; /* 浅灰色背景 */
  color: #999999; /* 深灰色文字 */
}

.latest-message {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  flex-shrink: 1;
}

.right-section {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: 12px;
  margin-right: 0px;
  flex-shrink: 0;
}

.time {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  min-width: 40px;
  text-align: right;
}

.do-not-disturb svg {
  color: #999;
}
</style>
