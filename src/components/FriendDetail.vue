<template>
  <div class="friend-detail-window">
    <!-- 顶部：头像与信息 -->
    <div class="header-section">
      <div class="avatar-container">
        <img :src="userInfo.avatar" alt="Avatar" class="avatar" @click="previewAvatar" />
      </div>
      <div class="info-container">
        <div class="top-row">
          <!-- 备注/用户名编辑区 -->
          <div class="name-area">
            <template v-if="isEditingRemark">
              <input
                ref="remarkInputRef"
                v-model.trim="remarkDraft"
                class="remark-input"
                type="text"
                maxlength="20"
                @blur="saveRemark"
                @keydown.enter.prevent="saveRemark"
                @keydown.esc.prevent="cancelEditRemark"
              />
            </template>
            <template v-else>
              <span class="remark-name">{{ userInfo.remark || userInfo.username }}</span>
            </template>

            <!-- 性别图标 -->
            <img
              v-if="userInfo.gender"
              :src="getSexIcon(userInfo.gender)"
              class="gender-icon"
              alt="gender"
            />
          </div>

          <!-- 设置按钮 (...) -->
          <div class="settings-btn" @click.stop="toggleMenu">
            <span>...</span>
            <!-- 下拉菜单 -->
            <transition name="fade">
              <div v-if="isMenuVisible" class="dropdown-menu" @click.stop>
                <div class="menu-item" @click="startEditRemark">设置备注</div>
                <div class="menu-item" @click="openPermissionsDialog">设置好友权限</div>
                <div class="menu-item danger" @click="handleBlock">加入黑名单</div>
                <div class="menu-item danger" @click="handleDelete">删除联系人</div>
              </div>
            </transition>
          </div>
        </div>

        <div class="sub-info">
          <div>{{ userInfo.username }}</div>
          <div class="account-id">ID: {{ userInfo.accountId }}</div>
          <div class="area">{{ userInfo.areaName || '未知地区' }}</div>
        </div>
      </div>
    </div>

    <!-- 中部：个性签名 -->
    <div class="signature-section">
      <div class="section-title">个性签名</div>
      <div class="signature-content">{{ userInfo.personalSignature }}</div>
    </div>

    <!-- 下部：朋友圈预览 (最近3张) -->
    <div class="moments-section">
      <div class="moments-header">
        <div class="section-title">朋友圈</div>
        <img src="../assets/BusinessCard/arrow-right.png" alt="Arrow" class="arrow-icon" />
      </div>
      <div class="moments-grid">
        <img
          v-for="(img, index) in moments.slice(0, 3)"
          :key="index"
          :src="img"
          class="moment-img"
          @click="previewMoment(img)"
        />
      </div>
    </div>

    <!-- 底部：操作按钮 -->
    <div class="action-bar">
      <button class="action-btn primary" @click="emitAction('message')">
        <img src="/src/assets/BusinessCard/message.png" class="icon" /> 发消息
      </button>
      <button class="action-btn secondary" @click="emitAction('audio')">
        <img src="/src/assets/BusinessCard/audio.png" class="icon" /> 语音聊天
      </button>
      <button class="action-btn secondary" @click="emitAction('video')">
        <img src="/src/assets/BusinessCard/video.png" class="icon" /> 视频聊天
      </button>
    </div>

    <!-- 权限设置弹窗 -->
    <div
      v-if="isPermissionsDialogVisible"
      class="permissions-mask"
      @click.self="closePermissionsDialog"
    >
      <div class="permissions-box">
        <div class="perm-title">好友权限</div>
        <label class="perm-item">
          <input type="checkbox" v-model="permDraft.hideMyMoments" />
          <span>不让Ta看我的朋友圈</span>
        </label>
        <label class="perm-item">
          <input type="checkbox" v-model="permDraft.hideFriendMoments" />
          <span>不看Ta的朋友圈</span>
        </label>
        <div class="perm-actions">
          <button @click="closePermissionsDialog">取消</button>
          <button class="confirm" @click="savePermissions">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="js">
import { ref, computed, nextTick } from 'vue'
import { useUserStore } from '../stores/userStore.js'
import { useImagePreviewStore } from '../stores/imagePreviewStore.js'

const props = defineProps({
  // 接收好友用户对象
  friend: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits([
  'send-message',
  'audio-call',
  'video-call',
  'update-remark',
  'update-permissions',
  'block-friend',
  'delete-friend',
])

const userStore = useUserStore()
const imagePreviewStore = useImagePreviewStore()

// --- 状态管理 ---
const isEditingRemark = ref(false)
const remarkDraft = ref('')
const isMenuVisible = ref(false)
const isPermissionsDialogVisible = ref(false)

// 权限草稿
const permDraft = ref({
  hideMyMoments: false,
  hideFriendMoments: false,
})

const userInfo = computed(() => props.friend || {})
const moments = computed(() => props.friend?.moments || [])

const remarkInputRef = ref(null)

// 备注
const startEditRemark = () => {
  isMenuVisible.value = false
  remarkDraft.value = props.friend.remark || ''
  isEditingRemark.value = true
  nextTick(() => {
    remarkInputRef.value?.focus()
  })
}

const cancelEditRemark = () => {
  isEditingRemark.value = false
}

const saveRemark = async () => {
  if (remarkDraft.value !== props.friend.remark) {
    emit('update-remark', { friendUserId: props.friend.userId, remark: remarkDraft.value })
  }
  isEditingRemark.value = false
}

// 2. 菜单相关
const toggleMenu = () => {
  isMenuVisible.value = !isMenuVisible.value
}

// 3. 权限相关
const openPermissionsDialog = () => {
  isMenuVisible.value = false
  // 初始化草稿为当前值
  permDraft.value = {
    hideMyMoments: props.friend.hideMyMoments || false,
    hideFriendMoments: props.friend.hideFriendMoments || false,
  }
  isPermissionsDialogVisible.value = true
}

const closePermissionsDialog = () => {
  isPermissionsDialogVisible.value = false
}

const savePermissions = () => {
  emit('update-permissions', {
    friendUserId: props.friend.userId,
    ...permDraft.value,
  })
  closePermissionsDialog()
}

// 4. 危险操作
const handleBlock = () => {
  isMenuVisible.value = false

  emit('block-friend', props.friend.userId)
}

const handleDelete = () => {
  isMenuVisible.value = false
  emit('delete-friend', props.friend.userId)
}

// 5. 底部按钮动作
const emitAction = (type) => {
  if (type === 'message') emit('send-message', props.friend)
  if (type === 'audio') emit('audio-call', props.friend)
  if (type === 'video') emit('video-call', props.friend)
}

// 6. 图片预览
const previewAvatar = () => {
  imagePreviewStore.open({ src: props.friend.avatar })
}
const previewMoment = (src) => {
  imagePreviewStore.open({ src })
}

const getSexIcon = (gender) => {
  return gender === 1 ? '/src/assets/BusinessCard/boy.png' : '/src/assets/BusinessCard/girl.png'
}
</script>

<style scoped>
.friend-detail-window {
  width: 50%;
  height: 100vh;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Header Section */
.header-section {
  display: flex;
  gap: 16px;
}

.avatar-container {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
}

.info-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.name-area {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.remark-input {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 16px;
  width: 120px;
}

.gender-icon {
  width: 16px;
  height: 16px;
}

.settings-btn {
  position: relative;
  cursor: pointer;
  padding: 0 8px;
  font-size: 20px;
  color: #999;
  line-height: 1;
}

.settings-btn:hover {
  color: #333;
}

.dropdown-menu {
  position: absolute;
  top: 24px;
  right: 0;
  width: 140px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  overflow: hidden;
}

.menu-item {
  padding: 10px 12px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:hover {
  background: #f5f5f5;
}

.menu-item.danger {
  color: #ff4d4f;
}

.menu-item.danger:hover {
  background: #fff1f0;
}

.sub-info {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.account-id,
.area {
  color: #999;
}

/* Moments Section */
.moments-section {
  margin-bottom: 20px;
}

.moments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}
.moments-header:hover .arrow-icon {
  visibility: visible;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  align-items: center;
  justify-content: center;
}

.arrow-icon {
  width: 12px;
  height: 12px;
  visibility: hidden;
}

.moments-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.moment-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  background: #f0f0f0;
}

/* Signature Section */
.signature-section {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
}

.signature-content {
  font-size: 14px;
  color: #666;
  background: #f9f9f9;
  padding: 10px;
  border-radius: 6px;
  line-height: 1.5;
}

/* Action Bar */
.action-bar {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.action-btn {
  flex: 1;
  flex-direction: column;
  background: #f5f5f5;
  padding: 10px 0;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: opacity 0.2s;
}

.action-btn.primary:hover,
.action-btn.secondary:hover {
  opacity: 0.9;
  background: #eae8e8;
}

.action-btn.primary {
  color: #2ba1e0;
  margin-right: 8px;
}

.action-btn.secondary {
  background: #f5f5f5;
  color: #2ba1e0;
  margin-left: 4px;
}

.action-btn:first-child {
  margin-left: 0;
}
.action-btn:last-child {
  margin-right: 0;
}

.icon {
  width: 20px;
  height: 20px;
}

/* Permissions Dialog */
.permissions-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.permissions-box {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  width: 280px;
}

.perm-title {
  font-weight: 600;
  margin-bottom: 16px;
  text-align: center;
}

.perm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  cursor: pointer;
}

.perm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.perm-actions button {
  padding: 6px 16px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
}

.perm-actions button.confirm {
  background: #29e069;
  color: #fff;
  border: none;
}

/* Animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
