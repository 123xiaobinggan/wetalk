<template>
  <div v-if="visible" class="group-candidate-mask" @click="handleCancel">
    <div class="group-candidate-dialog" @click.stop>
      <div class="dialog-header">
        <span class="dialog-title">{{ modalTitle }}</span>
        <button class="close-btn" type="button" @click="handleCancel">×</button>
      </div>

      <div class="dialog-body">
        <!-- 左侧：可选列表 -->
        <div class="panel left-panel">
          <div class="search-box">
            <input
              v-model.trim="keyword"
              type="text"
              class="search-input"
              :placeholder="searchPlaceholder"
            />
          </div>

          <div class="candidate-list">
            <div
              v-for="candidate in filteredCandidates"
              :key="candidate.userId"
              class="candidate-row"
              :class="{ selected: isSelected(candidate.userId) }"
              @click="toggleSelect(candidate)"
            >
              <div class="checkbox" :class="{ checked: isSelected(candidate.userId) }">
                <span v-if="isSelected(candidate.userId)" class="checkmark">✔</span>
              </div>

              <img :src="candidate.avatar" alt="avatar" class="avatar" />

              <div class="candidate-info">
                <div class="candidate-name">
                  {{ getDisplayName(candidate) }}
                </div>
                <div v-if="getSubText(candidate)" class="candidate-sub">
                  {{ getSubText(candidate) }}
                </div>
              </div>
            </div>

            <div v-if="filteredCandidates.length === 0" class="empty-text">
              {{ emptyText }}
            </div>
          </div>
        </div>

        <!-- 右侧：已选择 -->
        <div class="panel right-panel">
          <div class="selected-header">已选择 {{ selectedCandidates.length }} 人</div>

          <div class="selected-list">
            <div
              v-for="candidate in selectedCandidates"
              :key="candidate.userId"
              class="selected-row"
            >
              <div class="selected-user">
                <img :src="candidate.avatar" alt="avatar" class="avatar" />

                <div class="candidate-info">
                  <div class="candidate-name">
                    {{ getDisplayName(candidate) }}
                  </div>
                  <div v-if="getSubText(candidate)" class="candidate-sub">
                    {{ getSubText(candidate) }}
                  </div>
                </div>
              </div>

              <button class="remove-btn" type="button" @click="removeSelected(candidate.userId)">
                ×
              </button>
            </div>

            <div v-if="selectedCandidates.length === 0" class="empty-text">暂未选择</div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <button class="footer-btn cancel-btn" type="button" @click="handleCancel">取消</button>

        <button
          class="footer-btn confirm-btn"
          type="button"
          :disabled="selectedCandidates.length === 0"
          @click="handleConfirm"
        >
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

defineOptions({ name: 'GroupFriendManageModal' })

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  mode: {
    type: String,
    default: 'add',
    validator: (value) => ['add', 'remove'].includes(value),
  },
  candidates: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['close', 'confirm'])

const keyword = ref('')
const selectedUserIds = ref([])

const modalTitle = computed(() => {
  return props.mode === 'remove' ? '移除群成员' : '添加好友'
})

const searchPlaceholder = computed(() => {
  return props.mode === 'remove' ? '搜索群成员' : '搜索好友'
})

const confirmText = computed(() => {
  return props.mode === 'remove' ? '移除' : '添加'
})

const emptyText = computed(() => {
  return props.mode === 'remove' ? '未找到相关群成员' : '未找到相关好友'
})

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      resetDialog()
    }
  },
)

const filteredCandidates = computed(() => {
  const list = props.candidates || []
  const kw = keyword.value.trim().toLowerCase()

  if (!kw) return list

  return list.filter((candidate) => {
    const remark = (candidate.remark || '').toLowerCase()
    const username = (candidate.username || '').toLowerCase()
    const accountId = (candidate.accountId || '').toLowerCase()
    const myNickname = (candidate.myNickname || '').toLowerCase()

    return (
      remark.includes(kw) ||
      username.includes(kw) ||
      accountId.includes(kw) ||
      myNickname.includes(kw)
    )
  })
})

const selectedCandidates = computed(() => {
  const idSet = new Set(selectedUserIds.value.map(String))

  return (props.candidates || []).filter((candidate) => {
    return idSet.has(String(candidate.userId))
  })
})

function getDisplayName(candidate) {
  return candidate.remark || candidate.MyNickname || candidate.username || '未命名'
}

function getSubText(candidate) {
  if (props.mode === 'remove') {
    if (candidate.role === 2) return '群主'
    if (candidate.role === 1) return '管理员'
    return '群成员'
  }

  return candidate.accountId ? `ID: ${candidate.accountId}` : ''
}

function isSelected(userId) {
  return selectedUserIds.value.map(String).includes(String(userId))
}

function toggleSelect(candidate) {
  const userId = candidate.userId
  if (!userId) return

  const id = String(userId)
  const index = selectedUserIds.value.map(String).indexOf(id)

  if (index > -1) {
    selectedUserIds.value.splice(index, 1)
  } else {
    selectedUserIds.value.push(userId)
  }
}

function removeSelected(userId) {
  selectedUserIds.value = selectedUserIds.value.filter((id) => {
    return String(id) !== String(userId)
  })
}

function resetDialog() {
  keyword.value = ''
  selectedUserIds.value = []
}

function handleCancel() {
  resetDialog()
  emit('close')
}

function handleConfirm() {
  emit('confirm', {
    selectedUserIds: selectedUserIds.value,
  })

  resetDialog()
}
</script>

<style scoped>
.group-candidate-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.group-candidate-dialog {
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

.candidate-list,
.selected-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.candidate-row,
.selected-row {
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.candidate-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.candidate-row:hover {
  background: #f8faf8;
}

.candidate-row.selected {
  background: #f0fdf4;
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

.candidate-info {
  flex: 1;
  min-width: 0;
  margin-left: 12px;
}

.candidate-name {
  font-size: 14px;
  color: #222;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #999;
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
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
