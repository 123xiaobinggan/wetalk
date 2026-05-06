<template>
  <div class="panel-root">
    <div class="detail-section">
      <div class="group-search-box">
        <span class="group-search-icon">🔍</span>
        <input v-model="keyword" type="text" class="group-search-input" placeholder="搜索群成员" />
      </div>
    </div>

    <div class="detail-section members-section">
      <div class="members-grid">
        <div v-for="member in displayMembers" :key="member.memberUserId" class="member-item">
          <img
            :src="member.avatar || defaultAvatar"
            class="member-avatar"
            @click="businessCardStore.showCard(userStore.getUser(member.memberUserId), $event)"
          />
          <div class="member-name">
            {{ member?.remark || member.myNickname || member.username || '未命名' }}
          </div>
        </div>

        <div class="member-item add-member-item" @click="emit('add-member')">
          <div class="member-add-square">+</div>
          <div class="member-name">添加</div>
        </div>
        <div v-if="hasPrivilege" class="member-item add-member-item" @click="emit('remove-member')">
          <div class="member-add-square">-</div>
          <div class="member-name">移除</div>
        </div>
      </div>

      <div
        v-if="filteredMembers.length > collapsedMemberCount"
        class="expand-toggle"
        @click="toggleExpandMembers"
      >
        <span>{{ membersExpanded ? '收起' : '查看更多' }}</span>
        <span class="expand-arrow">{{ membersExpanded ? '▲' : '▼' }}</span>
      </div>
    </div>

    <div class="detail-divider"></div>

    <!-- groupName -->
    <EditableInfoItem
      label="群聊名称"
      :value="localInfo.groupName"
      placeholder="未设置"
      @save="(val) => saveField('groupName', val)"
      :readonly="!hasPrivilege"
    />
    <!-- announcement -->
    <EditableInfoItem
      label="群公告"
      :value="localInfo.announcement"
      placeholder="暂无公告"
      @save="(val) => saveField('announcement', val)"
      :readonly="!hasPrivilege"
    />
    <!-- groupRemark -->
    <EditableInfoItem
      label="群聊备注"
      :value="localInfo.groupRemark"
      placeholder="未设置"
      @save="(val) => saveField('groupRemark', val)"
    />
    <!-- myNickname -->
    <EditableInfoItem
      label="我在本群的昵称"
      :value="localInfo.myNickname"
      placeholder="未设置"
      @save="(val) => saveField('myNickname', val)"
    />

    <div class="detail-divider"></div>
    <!-- 查找聊天内容 -->
    <div class="detail-row clickable-row" @click="emit('search-chat-history')">
      <span>查找聊天内容</span>
      <span class="arrow">›</span>
    </div>

    <div class="detail-divider"></div>

    <!-- muted -->
    <div class="detail-row">
      <span>消息免打扰</span>
      <SwitchToggle v-model="localMuted" />
    </div>

    <!-- pinned -->
    <div class="detail-row">
      <span>置顶聊天</span>
      <SwitchToggle v-model="localPinned" />
    </div>

    <div class="detail-divider"></div>

    <template v-if="hasPrivilege">
      <div class="detail-row clickable-row" @click="openSilenceMenu">
        <span>禁言管理</span>
        <span class="arrow">›</span>
      </div>
    </template>

    <template v-if="isOwner">
      <div class="detail-row clickable-row" @click="openMemberPicker('addAdmin')">
        <span>添加管理员</span>
        <span class="arrow">›</span>
      </div>

      <div class="detail-row clickable-row" @click="openMemberPicker('removeAdmin')">
        <span>移除管理员</span>
        <span class="arrow">›</span>
      </div>

      <div class="detail-row clickable-row" @click="openMemberPicker('transferOwner')">
        <span>转让群主</span>
        <span class="arrow">›</span>
      </div>
    </template>

    <div class="detail-divider" v-if="isOwner"></div>

    <div class="change-avatar-row" @click="emit('change-group-avatar')">更换群头像</div>

    <div class="danger-row" @click="emit('clear-chat-history')">清空聊天记录</div>

    <div class="detail-divider"></div>

    <div class="danger-row" @click="emit('quit-group')">退出群聊</div>

    <div v-if="isOwner" class="danger-row" @click="emit('disband-group')">解散群聊</div>

    <div v-if="silenceMenuVisible" class="silence-menu-mask" @click.self="closeSilenceMenu">
      <div class="silence-menu">
        <div class="silence-menu-header">
          <span>禁言管理</span>
          <button class="silence-close-btn" type="button" @click="closeSilenceMenu">×</button>
        </div>

        <div v-if="props.group?.status == 0" class="silence-menu-item" @click="silenceAll">
          全员禁言
        </div>

        <div v-if="props.group?.status == 2" class="silence-menu-item" @click="unsilenceAll">
          解除全员禁言
        </div>

        <div class="silence-menu-item" @click="openSilenceMemberPicker">选择成员禁言</div>
        <div class="silence-menu-item" @click="openUnSilenceMemberPicker">解除成员禁言</div>
      </div>
    </div>

    <MemberPickerModal
      :visible="pickerVisible"
      :title="pickerTitle"
      :empty-text="pickerEmptyText"
      :members="pickerMembers"
      :default-avatar="defaultAvatar"
      :picker-multiple="pickerMode !== 'transferOwner'"
      :confirm-on-single="pickerMode === 'transferOwner'"
      @close="closeMemberPicker"
      @select="selectPickerMember"
      @confirm="confirmPickerMembers"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import EditableInfoItem from './EditableInfoItem.vue'
import SwitchToggle from './SwitchToggle.vue'
import MemberPickerModal from './MemberPickerModal.vue'
import { useUserStore } from '../stores/userStore.js'
import { useGroupStore } from '../stores/groupStore.js'
import { useBusinessCardStore } from '../stores/businessCardStore.js'

defineOptions({ name: 'GroupDetailPanel' })

const props = defineProps({
  group: {
    type: Object,
    default: () => ({}),
  },
  groupMembers: {
    type: Array,
    default: () => [],
  },
  myNickname: {
    type: String,
    default: '',
  },
  groupRemark: {
    type: String,
    default: '',
  },
  defaultAvatar: {
    type: String,
    default: '',
  },
  muted: {
    type: Boolean,
    default: false,
  },
  pinned: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:muted',
  'update:pinned',
  'save-field',
  'search-chat-history',
  'change-group-avatar',
  'clear-chat-history',
  'quit-group',
  'add-member',
  'remove-member',
  'add-admin',
  'remove-admin',
  'transfer-owner',
  'disband-group',
  'silence-all',
  'unsilence-all',
  'silence-members',
  'unsilence-members',
])

onMounted(() => {
  console.log('GroupDetailmounted', props)
})

const userStore = useUserStore()
const groupStore = useGroupStore()
const businessCardStore = useBusinessCardStore()

const keyword = ref('')
const membersExpanded = ref(false)
const collapsedMemberCount = 15

const localMuted = ref(props.muted)
const localPinned = ref(props.pinned)

watch(
  () => props.muted,
  (val) => {
    localMuted.value = val
  },
)

watch(
  () => props.pinned,
  (val) => {
    localPinned.value = val
  },
)

watch(localMuted, (val) => emit('update:muted', val))
watch(localPinned, (val) => emit('update:pinned', val))

const groupMembers = () => {
  const members = props.groupMembers || []
  return members.map((member) => {
    return groupStore.getGroupMemberProfile(member.groupId, member.memberUserId).value
  })
}

const filteredMembers = computed(() => {
  const k = keyword.value.trim()
  if (!k) return groupMembers() || []

  return groupMembers().filter((member) => {
    const name = member.myNickname || member.username || ''
    return name.includes(k)
  })
})

const displayMembers = computed(() => {
  if (membersExpanded.value) return filteredMembers.value
  return filteredMembers.value.slice(0, collapsedMemberCount)
})

function toggleExpandMembers() {
  membersExpanded.value = !membersExpanded.value
}

const silenceMenuVisible = ref(false)

function openSilenceMenu() {
  silenceMenuVisible.value = true
}

function closeSilenceMenu() {
  silenceMenuVisible.value = false
}

function silenceAll() {
  emit('silence-all', {
    groupId: props.group?.groupId,
  })

  closeSilenceMenu()
}

function unsilenceAll() {
  emit('unsilence-all', {
    groupId: props.group?.groupId,
  })

  closeSilenceMenu()
}

function openSilenceMemberPicker() {
  closeSilenceMenu()
  openMemberPicker('silenceMember')
}

function openUnSilenceMemberPicker() {
  closeSilenceMenu()
  openMemberPicker('unsilenceMember')
}

const OWNER_ROLE = 2
const ADMIN_ROLE = 1
const NORMAL_ROLE = 0

const pickerVisible = ref(false)
const pickerMode = ref('')

const pickerTitle = computed(() => {
  if (pickerMode.value === 'addAdmin') return '添加管理员'
  if (pickerMode.value === 'removeAdmin') return '移除管理员'
  if (pickerMode.value === 'transferOwner') return '转让群主'
  if (pickerMode.value === 'silenceMember') return '选择禁言成员'
  if (pickerMode.value === 'unsilenceMember') return '选择解除禁言成员'
  return '选择成员'
})

const pickerEmptyText = computed(() => {
  if (pickerMode.value === 'addAdmin') return '暂无可添加为管理员的普通成员'
  if (pickerMode.value === 'removeAdmin') return '暂无可移除的管理员'
  if (pickerMode.value === 'silenceMember') return '暂无可禁言的成员'
  if (pickerMode.value === 'unsilenceMember') return '暂无已禁言的成员'
  if (pickerMode.value === 'transferOwner') return '暂无可转让的成员'
  return '暂无成员'
})

const pickerMembers = computed(() => {
  const members = groupMembers()

  if (pickerMode.value === 'addAdmin') {
    return members.filter((member) => member.role === NORMAL_ROLE)
  }

  if (pickerMode.value === 'removeAdmin') {
    return members.filter((member) => member.role === ADMIN_ROLE)
  }

  if (pickerMode.value === 'transferOwner') {
    return members.filter((member) => member.memberUserId !== userStore.userInfo.userId)
  }

  if (pickerMode.value === 'silenceMember') {
    return members.filter((member) => {
      if (member.role === OWNER_ROLE || member.role === ADMIN_ROLE) return false
      if (member.silence) return false
      return true
    })
  }

  if (pickerMode.value === 'unsilenceMember') {
    return members.filter((member) => {
      return member.silence !== false
    })
  }

  return []
})

function openMemberPicker(mode) {
  pickerMode.value = mode
  pickerVisible.value = true
}

function closeMemberPicker() {
  pickerVisible.value = false
  pickerMode.value = ''
}

const currentMember = computed(() => {
  return groupMembers().find((member) => member.memberUserId === userStore.userInfo.userId)
})

const isOwner = computed(() => {
  return (
    props.group?.ownerUserId === userStore.userInfo.userId ||
    currentMember.value?.role === OWNER_ROLE
  )
})
function selectPickerMember(memberUserId) {
  if (pickerMode.value !== 'transferOwner') return

  emit('transfer-owner', { transfereeUserId: memberUserId })

  closeMemberPicker()
}
function confirmPickerMembers(memberUserIds) {
  if (pickerMode.value === 'addAdmin') {
    emit('add-admin', {
      memberUserIds,
      role: 1,
    })
  } else if (pickerMode.value === 'removeAdmin') {
    emit('remove-admin', {
      memberUserIds,
      role: 0,
    })
  } else if (pickerMode.value === 'silenceMember') {
    emit('silence-members', { memberUserIds })
  } else if (pickerMode.value === 'unsilenceMember') {
    emit('unsilence-members', { memberUserIds })
  }

  closeMemberPicker()
}

const hasPrivilege = computed(() => {
  return currentMember.value?.role === OWNER_ROLE || currentMember.value?.role === ADMIN_ROLE
})

const localInfo = ref({
  groupName: '',
  announcement: '',
  groupRemark: '',
  myNickname: '',
})

watch(
  () => props.group,
  (val) => {
    localInfo.value.groupName = val?.groupName || val?.name || 'xxx群'
    localInfo.value.announcement = val?.announcement || ''
  },
  { immediate: true, deep: true },
)

watch(
  () => props.myNickname,
  (val) => {
    localInfo.value.myNickname = val || ''
    console.log('undate myNickname', val)
  },
  { immediate: true, deep: true },
)

watch(
  () => props.groupRemark,
  (val) => {
    localInfo.value.groupRemark = val || ''
    console.log('undate groupRemark', val)
  },
  { immediate: true, deep: true },
)

function saveField(key, value) {
  localInfo.value[key] = value
  emit('save-field', { key, value })
  console.log('保存群设置', key, value)
}
</script>

<style scoped>
.panel-root {
  height: 100%;
  background: #fff;
}

.detail-section {
  padding: 16px;
}

.detail-divider {
  height: 1px;
  background: #ededed;
  margin: 0 16px;
}

.group-search-box {
  height: 36px;
  border-radius: 8px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  padding: 0 10px;
  gap: 8px;
}

.group-search-icon {
  font-size: 14px;
  color: #999;
}

.group-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #333;
}

.members-section {
  padding-top: 10px;
}

.members-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px 12px;
}

.member-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.member-avatar,
.member-add-square {
  width: 52px;
  height: 52px;
  border-radius: 10px;
}

.member-avatar {
  object-fit: cover;
  display: block;
}

.member-add-square {
  border: 1px dashed #b8b8b8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #8c8c8c;
  background: #fff;
  box-sizing: border-box;
}

.member-name {
  margin-top: 6px;
  font-size: 12px;
  color: #333;
  line-height: 1.4;
  width: 100%;
  text-align: center;
  word-break: break-all;
}

.add-member-item {
  cursor: pointer;
}

.expand-toggle {
  margin-top: 16px;
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.expand-toggle:hover {
  color: #222;
}

.expand-arrow {
  font-size: 12px;
}

.detail-row {
  min-height: 52px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #222;
  font-size: 14px;
  box-sizing: border-box;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: #fafafa;
}

.arrow {
  font-size: 20px;
  color: #999;
  line-height: 1;
}

.change-avatar-row {
  min-height: 52px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  font-size: 14px;
  box-sizing: border-box;
  cursor: pointer;
}

.change-avatar-row:hover {
  background: #d6f1d1;
}

.danger-row {
  min-height: 52px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  color: #e53935;
  font-size: 14px;
  cursor: pointer;
  box-sizing: border-box;
}

.danger-row:hover {
  background: #fff6f6;
}

.silence-menu-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1999;
}

.silence-menu {
  width: 300px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
}

.silence-menu-header {
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

.silence-close-btn {
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
}

.silence-close-btn:hover {
  color: #333;
}

.silence-menu-item {
  min-height: 50px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #222;
  cursor: pointer;
}

.silence-menu-item:hover {
  background: #fafafa;
}
</style>
