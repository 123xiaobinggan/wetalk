<template>
  <div v-if="visible" class="member-picker-mask" @click.self="emit('close')">
    <div class="member-picker">
      <div class="member-picker-header">
        <span>{{ title }}</span>
        <button class="picker-close-btn" type="button" @click="emit('close')">×</button>
      </div>

      <div v-if="members.length === 0" class="member-picker-empty">
        {{ emptyText }}
      </div>

      <div v-else class="member-picker-list">
        <div
          v-for="member in members"
          :key="member.memberUserId"
          class="member-picker-item"
          :class="{
            selected: isSelected(member.memberUserId),
            locked: isLocked(member.memberUserId),
          }"
          @click="onClickMember(member)"
        >
          <div v-if="showCheck" class="picker-check">
            <span v-if="isSelected(member.memberUserId)">✓</span>
          </div>

          <img :src="member.avatar || defaultAvatar" class="picker-avatar" />

          <div class="picker-info">
            <div class="picker-name">
              {{ member.remark || member.myNickname || member.username || '未命名' }}
            </div>
            <div class="picker-sub">
              {{ getRoleText(member.role) }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="showFooter" class="member-picker-footer">
        <button class="picker-cancel-btn" type="button" @click="emit('close')">取消</button>
        <button
          class="picker-confirm-btn"
          type="button"
          :disabled="selectedMembers.length === 0"
          @click="confirmSelect"
        >
          确定{{ selectedMembers.length > 0 ? `(${selectedMembers.length})` : '' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '选择成员',
  },
  emptyText: {
    type: String,
    default: '暂无可选成员',
  },
  members: {
    type: Array,
    default: () => [],
  },
  pickerMultiple: {
    type: Boolean,
    default: true,
  },
  confirmOnSingle: {
    type: Boolean,
    default: false,
  },
  defaultAvatar: {
    type: String,
    default: '',
  },
})
const emit = defineEmits(['close', 'select', 'confirm'])

const selectedIds = ref([])
function getRoleText(role) {
  if (role === 2) return '群主'
  if (role === 1) return '管理员'
  return '普通成员'
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      selectedIds.value = []
    }
  },
)

const showFooter = computed(() => {
  return props.pickerMultiple || props.confirmOnSingle
})

const showCheck = computed(() => {
  return props.pickerMultiple || props.confirmOnSingle
})

const selectedMembers = computed(() => {
  const idSet = new Set(selectedIds.value.map(String))
  return props.members.filter((member) => idSet.has(String(member.memberUserId)))
})

function isSelected(memberUserId) {
  return selectedIds.value.map(String).includes(String(memberUserId))
}

function isLocked(memberUserId) {
  if (props.pickerMultiple) return false
  if (!props.confirmOnSingle) return false
  if (selectedIds.value.length === 0) return false

  return !isSelected(memberUserId)
}

function onClickMember(member) {
  if (isLocked(member.memberUserId)) return

  const id = String(member.memberUserId)

  // 多选
  if (props.pickerMultiple) {
    const index = selectedIds.value.map(String).indexOf(id)

    if (index === -1) {
      selectedIds.value.push(member.memberUserId)
    } else {
      selectedIds.value.splice(index, 1)
    }

    return
  }

  // 单选 + 立即返回
  if (!props.confirmOnSingle) {
    emit('select', member)
    return
  }

  // 单选 + 确认模式
  if (isSelected(member.memberUserId)) {
    selectedIds.value = []
    return
  }

  if (selectedIds.value.length > 0) {
    return
  }

  selectedIds.value = [member.memberUserId]
}

function confirmSelect() {
  if (selectedIds.value.length === 0) return

  if (props.pickerMultiple) {
    emit('confirm', selectedIds.value)
    return
  }

  emit('select', selectedIds.value[0])
}
</script>

<style scoped>
.member-picker-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.member-picker {
  width: 320px;
  max-height: 520px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
}

.member-picker-header {
  height: 48px;
  padding: 0 14px 0 16px;
  border-bottom: 1px solid #ededed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  color: #222;
}

.picker-close-btn {
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
}

.picker-close-btn:hover {
  color: #333;
}

.member-picker-empty {
  padding: 28px 16px;
  font-size: 13px;
  color: #999;
  text-align: center;
}

.member-picker-list {
  overflow-y: auto;
  max-height: 472px;
}

.member-picker-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
}

.member-picker-item:hover {
  background: #f7f7f7;
}

.picker-avatar {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  flex: none;
}

.picker-info {
  min-width: 0;
  flex: 1;
}

.picker-name {
  font-size: 14px;
  color: #222;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picker-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #999;
}

.member-picker-footer {
  height: 56px;
  padding: 0 14px;
  border-top: 1px solid #ededed;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  flex: none;
}

.picker-cancel-btn,
.picker-confirm-btn {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.picker-cancel-btn {
  background: #f2f2f2;
  color: #333;
}

.picker-confirm-btn {
  background: #1890ff;
  color: #fff;
}

.picker-confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.member-picker-item.locked {
  opacity: 0.35;
  cursor: not-allowed;
}

.member-picker-item.locked:hover {
  background: transparent;
}

.member-picker-item.selected {
  background: #f0f8ff;
}

.member-picker-item.selected .picker-check {
  border-color: #1890ff;
  background: #e6f4ff;
}

.picker-check {
  width: 18px;
  height: 18px;
  border: 1px solid #cfcfcf;
  border-radius: 50%;
  color: #1890ff;
  font-size: 13px;
  line-height: 18px;
  text-align: center;
  flex: none;
  box-sizing: border-box;
}
</style>
