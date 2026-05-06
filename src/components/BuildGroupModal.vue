<template>
  <div v-if="buildGroupModalStore.visible" class="group-chat-mask" @click="handleCancel">
    <div class="group-chat-dialog" @click.stop>
      <div class="dialog-header">
        <span class="dialog-title">发起群聊</span>
        <button class="close-btn" @click="handleCancel">×</button>
      </div>

      <div class="dialog-body">
        <!-- 左侧 -->
        <div class="panel left-panel">
          <div class="search-box">
            <input v-model.trim="keyword" type="text" class="search-input" placeholder="搜索好友" />
          </div>

          <div class="friend-list">
            <div
              v-for="friend in filteredFriends"
              :key="friend.userId"
              class="friend-row"
              @click="toggleSelect(friend)"
            >
              <div class="checkbox" :class="{ checked: isSelected(friend.userId) }">
                <span v-if="isSelected(friend.userId)" class="checkmark">✔</span>
              </div>

              <img :src="friend.avatar" alt="avatar" class="avatar" />

              <span class="friend-name">
                {{ friend.remark }}
              </span>
            </div>

            <div v-if="filteredFriends.length === 0" class="empty-text">未找到相关好友</div>
          </div>
        </div>

        <!-- 右侧 -->
        <div class="panel right-panel">
          <div class="selected-header">已选择 {{ selectedFriends.length }} 人</div>

          <div class="selected-list">
            <div v-for="friend in selectedFriends" :key="friend.userId" class="selected-row">
              <div class="selected-user">
                <img :src="friend.avatar" alt="avatar" class="avatar" />
                <span class="friend-name">
                  {{ friend.remark || friend.username }}
                </span>
              </div>

              <button class="remove-btn" @click="removeSelected(friend.userId)">×</button>
            </div>

            <div v-if="selectedFriends.length === 0" class="empty-text">暂未选择好友</div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <button class="footer-btn cancel-btn" @click="handleCancel">取消</button>
        <button class="footer-btn confirm-btn" @click="handleComplete">完成</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useBuildGroupModalStore } from '/src/stores/buildGroupModalStore'
defineOptions({
  name: 'BuildGroupModal',
})
const buildGroupModalStore = useBuildGroupModalStore()

const keyword = ref('')
const selectedIds = ref([])

const filteredFriends = computed(() => {
  const list = buildGroupModalStore.gettersFriends || []
  const kw = keyword.value.trim().toLowerCase()

  if (!kw) {
    return list
  }

  return list.filter((friend) => {
    const remark = (friend.remark || '').toLowerCase()
    const username = (friend.username || '').toLowerCase()
    const accountId = (friend.accountId || '').toLowerCase()

    return remark.includes(kw) || username.includes(kw) || accountId.includes(kw)
  })
})

const selectedFriends = computed(() => {
  const idSet = new Set(selectedIds.value)
  return (buildGroupModalStore.gettersFriends || []).filter((friend) => idSet.has(friend.userId))
})

const isSelected = (userId) => {
  return selectedIds.value.includes(userId)
}

const toggleSelect = (friend) => {
  const userId = friend.userId
  const index = selectedIds.value.indexOf(userId)

  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(userId)
  }
}

const removeSelected = (userId) => {
  selectedIds.value = selectedIds.value.filter((id) => id !== userId)
}

const resetDialog = () => {
  keyword.value = ''
  selectedIds.value = []
}

const handleCancel = () => {
  buildGroupModalStore.cancel()
  resetDialog()
}

const handleComplete = () => {
  buildGroupModalStore.complete(selectedIds.value)
  resetDialog()
}
</script>

<style scoped>
.group-chat-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.group-chat-dialog {
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

.friend-list,
.selected-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.friend-row,
.selected-row {
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.friend-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.friend-row:hover {
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
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.friend-name {
  margin-left: 12px;
  font-size: 14px;
  color: #222;
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
</style>
