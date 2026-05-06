<template>
  <div class="panel-root">
    <div class="detail-section detail-top-user-section">
      <div class="top-user-grid">
        <div class="simple-user-card">
          <img
            :src="avatar"
            class="simple-user-avatar"
            @click="businessCard.showCard(user, $event)"
          />
          <div class="simple-user-name">{{ displayName }}</div>
        </div>
      </div>
    </div>

    <div class="detail-divider"></div>

    <div class="detail-row clickable-row" @click="emit('search-chat-history')">
      <span>查找聊天记录</span>
      <span class="arrow">›</span>
    </div>

    <div class="detail-divider"></div>

    <div class="detail-row">
      <span>消息免打扰</span>
      <SwitchToggle v-model="localMuted" />
    </div>

    <div class="detail-row">
      <span>置顶聊天</span>
      <SwitchToggle v-model="localPinned" />
    </div>

    <div class="detail-divider"></div>

    <div class="danger-row" @click="emit('clear-chat-history')">清空聊天记录</div>

    <div class="detail-divider"></div>

    <div class="danger-row" @click="emit('delete-contact')">删除联系人</div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import SwitchToggle from './SwitchToggle.vue'
import { useBusinessCardStore } from '/src/stores/businessCardStore.js'

defineOptions({ name: 'PrivateDetailPanel' })

const props = defineProps({
  user: {
    type: Object,
    default: () => ({}),
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
  'search-chat-history',
  'clear-chat-history',
  'delete-contact',
  'add-friend',
])

const businessCard = useBusinessCardStore()

onMounted(() => {
  console.log('PrivateDetailPanel mounted', props.muted, props.pinned)
})

const localMuted = computed({
  get: () => props.muted,
  set: (val) => emit('update:muted', val),
})

const localPinned = computed({
  get: () => props.pinned,
  set: (val) => emit('update:pinned', val),
})

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

const avatar = computed(() => props.user?.avatar || props.defaultAvatar)
const displayName = computed(() => props.user?.remark || props.user?.username || '用户')
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

.detail-top-user-section {
  padding-top: 20px;
  padding-bottom: 20px;
}

.top-user-grid {
  display: flex;
  gap: 22px;
}

.simple-user-card {
  width: 56px;
  text-align: center;
}

.simple-user-avatar,
.add-square {
  width: 56px;
  height: 56px;
  border-radius: 10px;
}

.simple-user-avatar {
  object-fit: cover;
  display: block;
}

.add-square {
  border: 1px dashed #b8b8b8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #8c8c8c;
  background: #fff;
  box-sizing: border-box;
}

.simple-user-name {
  margin-top: 8px;
  font-size: 12px;
  color: #333;
  line-height: 1.4;
  word-break: break-all;
}

.add-card {
  cursor: pointer;
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
</style>
