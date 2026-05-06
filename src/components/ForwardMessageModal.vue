<template>
  <div v-if="visible" class="forward-mask" @click="handleCancel">
    <div class="forward-dialog" @click.stop>
      <div class="dialog-header">
        <span class="dialog-title">转发消息</span>
        <button class="close-btn" type="button" @click="handleCancel">×</button>
      </div>

      <div class="dialog-body">
        <!-- 左侧：会话 / 好友 -->
        <div class="panel left-panel">
          <div class="search-box">
            <input
              v-model.trim="keyword"
              type="text"
              class="search-input"
              placeholder="搜索最近会话或好友"
            />
          </div>

          <div class="tab-bar">
            <button
              type="button"
              class="tab-btn"
              :class="{ active: activeTab === 'conversation' }"
              @click="activeTab = 'conversation'"
            >
              最近会话
            </button>

            <button
              type="button"
              class="tab-btn"
              :class="{ active: activeTab === 'friend' }"
              @click="activeTab = 'friend'"
            >
              好友
            </button>
          </div>

          <div class="target-list">
            <template v-if="activeTab === 'conversation'">
              <div
                v-for="target in filteredConversationTargets"
                :key="target.key"
                class="target-row"
                @click="toggleSelect(target)"
              >
                <div class="checkbox" :class="{ checked: isSelected(target.key) }">
                  <span v-if="isSelected(target.key)" class="checkmark">✔</span>
                </div>

                <img :src="target.avatar" alt="avatar" class="avatar" />

                <div class="target-info">
                  <div class="target-name">
                    {{ target.title }}
                  </div>

                  <div class="target-desc">
                    {{ target.desc || '最近会话' }}
                  </div>
                </div>
              </div>

              <div v-if="filteredConversationTargets.length === 0" class="empty-text">
                未找到相关会话
              </div>
            </template>

            <template v-else>
              <div
                v-for="target in filteredFriendTargets"
                :key="target.key"
                class="target-row"
                @click="toggleSelect(target)"
              >
                <div class="checkbox" :class="{ checked: isSelected(target.key) }">
                  <span v-if="isSelected(target.key)" class="checkmark">✔</span>
                </div>

                <img :src="target.avatar" alt="avatar" class="avatar" />

                <div class="target-info">
                  <div class="target-name">
                    {{ target.title }}
                  </div>

                  <div class="target-desc">
                    {{ target.desc || '好友' }}
                  </div>
                </div>
              </div>

              <div v-if="filteredFriendTargets.length === 0" class="empty-text">未找到相关好友</div>
            </template>
          </div>
        </div>

        <!-- 右侧：已选择 -->
        <div class="panel right-panel">
          <div class="selected-header">已选择 {{ selectedTargets.length }} 项</div>

          <div class="selected-list">
            <div v-for="target in selectedTargets" :key="target.key" class="selected-row">
              <div class="selected-user">
                <img :src="target.avatar" alt="avatar" class="avatar" />

                <div class="selected-info">
                  <div class="selected-name">
                    {{ target.title }}
                  </div>

                  <div class="selected-type">
                    {{ target.type === 'conversation' ? '会话' : '好友' }}
                  </div>
                </div>
              </div>

              <button type="button" class="remove-btn" @click="removeSelected(target.key)">
                ×
              </button>
            </div>

            <div v-if="selectedTargets.length === 0" class="empty-text">暂未选择转发对象</div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <button type="button" class="footer-btn cancel-btn" @click="handleCancel">取消</button>

        <button
          type="button"
          class="footer-btn confirm-btn"
          :disabled="selectedTargets.length === 0"
          @click="handleConfirm"
        >
          确定
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

defineOptions({
  name: 'ForwardMessageModal',
})

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },

  conversations: {
    type: Array,
    default: () => [],
  },

  friends: {
    type: Array,
    default: () => [],
  },

  defaultAvatar: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close', 'confirm'])

const keyword = ref('')
const activeTab = ref('conversation')
const selectedTargets = ref([])

const conversationTargets = computed(() => {
  return (props.conversations || [])
    .filter((conv) => conv && conv.convId)
    .map((conv) => {
      const title = getConversationTitle(conv)
      const avatar = normalizeUrl(conv.avatar || conv.groupAvatar || props.defaultAvatar)

      return {
        key: `conversation_${conv.convId}`,
        type: 'conversation',

        convId: conv.convId,
        convType: conv.convType,
        peerId: conv.peerId,
        groupId: conv.groupId,
        sessionId: conv.sessionId,

        title,
        avatar,
        desc: conv.lastMsgBrief || '',
        raw: conv,
      }
    })
})

const friendTargets = computed(() => {
  return (props.friends || [])
    .filter((friend) => friend)
    .map((friend) => {
      const friendUserId = friend.friendUserId || friend.userId || friend.memberUserId

      const title = friend.remark || friend.username || `用户${friendUserId}`

      const avatar = normalizeUrl(friend.avatar || props.defaultAvatar)

      return {
        key: `friend_${friendUserId}`,
        type: 'friend',

        friendUserId,
        title,
        avatar,
        desc: friend.accountId || '',
        raw: friend,
      }
    })
    .filter((target) => target.friendUserId)
})

const filteredConversationTargets = computed(() => {
  return filterTargets(conversationTargets.value)
})

const filteredFriendTargets = computed(() => {
  return filterTargets(friendTargets.value)
})

function filterTargets(list) {
  const kw = keyword.value.trim().toLowerCase()

  if (!kw) return list

  return list.filter((target) => {
    const title = String(target.title || '').toLowerCase()
    const desc = String(target.desc || '').toLowerCase()

    return title.includes(kw) || desc.includes(kw)
  })
}

function getConversationTitle(conv) {
  if (conv.groupName) {
    return conv.groupRemark || conv.groupName || `群聊${conv.groupId || conv.convId}`
  }

  return conv.remark || conv.username || `用户${conv.peerId || conv.convId}`
}

function normalizeUrl(url) {
  if (!url) return props.defaultAvatar

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

function isSelected(key) {
  return selectedTargets.value.some((target) => target.key === key)
}

function toggleSelect(target) {
  const index = selectedTargets.value.findIndex((item) => item.key === target.key)

  if (index >= 0) {
    selectedTargets.value.splice(index, 1)
    return
  }

  selectedTargets.value.push(target)
}

function removeSelected(key) {
  selectedTargets.value = selectedTargets.value.filter((target) => target.key !== key)
}

function resetDialog() {
  keyword.value = ''
  activeTab.value = 'conversation'
  selectedTargets.value = []
}

function handleCancel() {
  resetDialog()
  emit('close')
}

function handleConfirm() {
  const result = selectedTargets.value.map((target) => {
    if (target.type === 'conversation') {
      return {
        type: 'conversation',
        convId: target.convId,
        convType: target.convType,
        sessionId: target.sessionId,
        peerId: target.peerId,
      }
    }

    return {
      type: 'friend',
      friendUserId: target.friendUserId,
      convType: false
    }
  })

  emit('confirm', result)
  resetDialog()
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      resetDialog()
    }
  },
)
</script>

<style scoped>
.forward-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.forward-dialog {
  width: 760px;
  height: 520px;
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

.panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.left-panel {
  width: 52%;
  border-right: 1px solid #f0f0f0;
}

.right-panel {
  width: 48%;
}

.search-box {
  padding: 16px;
  border-bottom: 1px solid #f6f6f6;
}

.search-input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  font-size: 14px;
  color: #333;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #22c55e;
}

.tab-bar {
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #f6f6f6;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-btn {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 999px;
  background: #f3f4f6;
  color: #555;
  font-size: 13px;
  cursor: pointer;
}

.tab-btn.active {
  background: #22c55e;
  color: #fff;
}

.target-list,
.selected-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.target-row,
.selected-row {
  height: 62px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.target-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.target-row:hover {
  background: #f8faf8;
}

.checkbox {
  width: 18px;
  height: 18px;
  margin-right: 12px;
  border: 1px solid #cfcfcf;
  border-radius: 4px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-sizing: border-box;
}

.checkbox.checked {
  background: #22c55e;
  border-color: #22c55e;
}

.checkmark {
  font-size: 12px;
  color: #fff;
  line-height: 1;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.target-info,
.selected-info {
  flex: 1;
  min-width: 0;
  margin-left: 12px;
}

.target-name,
.selected-name {
  font-size: 14px;
  color: #222;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.target-desc,
.selected-type {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-header {
  height: 54px;
  padding: 0 16px;
  border-bottom: 1px solid #f6f6f6;
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.selected-user {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.remove-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: #b5b5b5;
  font-size: 20px;
  cursor: pointer;
  line-height: 1;
  flex-shrink: 0;
}

.remove-btn:hover {
  color: #666;
}

.empty-text {
  padding: 24px 16px;
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
  gap: 12px;
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

.confirm-btn {
  border: none;
  background: #22c55e;
  color: #fff;
}

.confirm-btn:hover {
  background: #16a34a;
}

.confirm-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
