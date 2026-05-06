<template>
  <div class="detail-panel">
    <PrivateDetailPanel
      v-if="!convType"
      :user="user"
      :default-avatar="defaultAvatar"
      :muted="settings.muted"
      :pinned="settings.pinned"
      @update:muted="(val) => emit('update-setting', { key: 'muted', value: val })"
      @update:pinned="(val) => emit('update-setting', { key: 'pinned', value: val })"
      @search-chat-history="emit('search-chat-history')"
      @clear-chat-history="emit('clear-chat-history')"
      @delete-contact="emit('delete-contact')"
      @add-friend="emit('add-friend')"
    />

    <GroupDetailPanel
      v-else
      :group="group"
      :group-members="groupMembers"
      :default-avatar="defaultAvatar"
      :muted="settings.muted"
      :pinned="settings.pinned"
      :myNickname="myNickname"
      :groupRemark="groupRemark"
      @update:muted="(val) => emit('update-setting', { key: 'muted', value: val })"
      @update:pinned="(val) => emit('update-setting', { key: 'pinned', value: val })"
      @save-field="(val) => emit('save-group-field', val)"
      @search-chat-history="emit('search-chat-history')"
      @change-group-avatar="emit('change-group-avatar')"
      @clear-chat-history="emit('clear-chat-history')"
      @quit-group="emit('quit-group')"
      @add-member="emit('add-member')"
      @remove-member="(val) => emit('remove-member', val)"
      @add-admin="(val) => emit('add-admin', val)"
      @remove-admin="(val) => emit('remove-admin', val)"
      @transfer-owner="(val) => emit('transfer-owner', val)"
      @disband-group="emit('disband-group')"
      @silence-all="emit('silence-all')"
      @unsilence-all="emit('unsilence-all')"
      @silence-members="(val) => emit('silence-members', val)"
      @unsilence-members="(val) => emit('unsilence-members', val)"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import PrivateDetailPanel from './PrivateDetailPanel.vue'
import GroupDetailPanel from './GroupDetailPanel.vue'

defineOptions({ name: 'TalkDetailPanel' })

const props = defineProps({
  convType: {
    type: Boolean,
    required: true,
  },
  user: {
    type: Object,
    default: () => ({}),
  },
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
  settings: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits([
  'update-setting',
  'save-group-field',
  'search-chat-history',
  'change-group-avatar',
  'clear-chat-history',
  'delete-contact',
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

watch(
  () => props.group,
  (val) => {
    console.log('TalkDetailPanel group changed', val)
  },
  { immediate: true, deep: true },
)

onMounted(() => {
  console.log('TalkDetailPanel mounted', props)
})
</script>

<style scoped>
.detail-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 320px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #e9e9e9;
  overflow-y: auto;
  box-sizing: border-box;
  z-index: 20;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.06);
}
</style>
