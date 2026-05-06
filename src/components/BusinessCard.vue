<!-- src/components/BusinessCard.vue -->
<template>
  <div class="mask" @click="businessCardStore.hideCard">
    <div class="business-card" :style="cardStyle" @click.stop="businessCardStore.closeMenu">
      <!-- 上部分：头像 + 备注/用户名 + AccountId + 地区 -->
      <div class="header-section">
        <img
          :src="businessCardStore.userInfo.avatar"
          alt="Avatar"
          class="avatar"
          @click.stop="previewAvatar"
        />
        <div class="info">
          <div class="remark-or-username">
            <template v-if="businessCardStore.isEditingRemark">
              <input
                ref="remarkInputRef"
                v-model.trim="businessCardStore.remarkDraft"
                class="remark-input"
                type="text"
                maxlength="20"
                @click.stop
                @keydown.enter.prevent="handleSaveRemark"
                @keydown.esc.prevent="businessCardStore.cancelRemark"
                @blur="handleSaveRemark"
              />
            </template>

            <template v-else>
              <span>{{
                businessCardStore.userInfo.remark || businessCardStore.userInfo.username
              }}</span>
            </template>

            <img
              :src="getImageUrl(businessCardStore.getSexIcon())"
              alt="Gender"
              class="gender-icon"
            />
          </div>
          <div class="username" v-show="businessCardStore.isOther">
            {{ businessCardStore.userInfo.username }}
          </div>
          <div class="account-id">ID: {{ businessCardStore.userInfo.accountId }}</div>
          <div class="region">地区: {{ businessCardStore.userInfo.areaName }}</div>
        </div>
        <!-- 设置按钮 -->
        <div
          class="settings"
          v-show="businessCardStore.isOther"
          @click.stop="businessCardStore.toggleMenu"
        >
          <span class="settings-icon">...</span>
          <!-- 下拉菜单 -->
          <div class="dropdown-menu" v-show="businessCardStore.isMenuVisible" @click.stop>
            <div
              class="menu-item"
              v-if="!businessCardStore.isNotFriendOrDeleted"
              @click.stop="startEditRemark"
            >
              设置备注
            </div>
            <div
              class="menu-item"
              v-if="!businessCardStore.isNotFriendOrDeleted"
              @click.stop="businessCardStore.setPermissions"
            >
              设置权限
            </div>
            <div
              class="menu-item"
              v-if="!businessCardStore.isNotFriendOrDeleted"
              @click.stop="businessCardStore.recommendToFriend"
            >
              把Ta推荐给朋友
            </div>
            <div
              v-if="!businessCardStore.isBlocked"
              class="menu-item danger"
              @click.stop="businessCardStore.blockFriend"
            >
              加入黑名单
            </div>
            <div v-else class="menu-item danger" @click.stop="businessCardStore.unBlockFriend">
              移出黑名单
            </div>
            <div
              class="menu-item danger"
              v-if="!businessCardStore.isNotFriendOrDeleted"
              @click.stop="businessCardStore.deleteFriend"
            >
              删除好友
            </div>
          </div>
        </div>
      </div>

      <!-- 中间部分：朋友圈 -->
      <div class="moments-section" @click="businessCardStore.showMoments">
        <div class="moments-header">
          <span>朋友圈</span>
          <img src="../assets/BusinessCard/arrow-right.png" alt="Arrow" class="arrow-icon" />
        </div>
        <div class="moments-preview">
          <img
            v-for="(moment, index) in businessCardStore.userInfo.moments.slice(0, 4)"
            :key="index"
            :src="moment.image"
            alt="Moment"
            class="moment-image"
          />
        </div>
      </div>

      <!-- 底部信息：共同群聊数、个性签名、添加来源 -->
      <div class="footer-info">
        <div class="common-groups" v-if="businessCardStore.userInfo.commonGroups">
          共同群聊：{{ businessCardStore.commonGroups }}
        </div>
        <div class="signature" v-if="businessCardStore.userInfo.personalSignature">
          个性签名：{{ businessCardStore.userInfo.personalSignature }}
        </div>
        <div class="source" v-if="businessCardStore.userInfo.source">
          添加来源：{{ businessCardStore.userInfo.source }}
        </div>
      </div>

      <!-- 最底层：操作按钮 -->
      <div class="actions" v-if="businessCardStore.isOther">
        <template v-if="businessCardStore.isNotFriendOrDeleted">
          <div class="action-item single-action" @click="businessCardStore.addFriend">
            <img src="../assets/BusinessCard/addFriend.png" alt="Add Friend" class="action-icon" />
            <span>添加好友</span>
          </div>
        </template>

        <template v-else>
          <div class="action-item" @click="businessCardStore.sendMessage(router)">
            <img src="../assets/BusinessCard/message.png" alt="Message" class="action-icon" />
            <span>发消息</span>
          </div>

          <div class="action-item" @click="businessCardStore.audioCall">
            <img src="../assets/BusinessCard/audio.png" alt="Voice" class="action-icon" />
            <span>语音聊天</span>
          </div>

          <div class="action-item" @click="businessCardStore.videoCall">
            <img src="../assets/BusinessCard/video.png" alt="Video" class="action-icon" />
            <span>视频聊天</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 权限设置对话框 -->
    <div v-if="businessCardStore.isPermissionsDialogVisible" class="permissions-dialog" @click.stop>
      <div class="permissions-title">朋友圈状态</div>

      <label class="permission-option">
        <input v-model="businessCardStore.permissionsDraft.hideMyMoments" type="checkbox" />
        <span>不允许对方查看我的朋友圈</span>
      </label>

      <label class="permission-option">
        <input v-model="businessCardStore.permissionsDraft.hideFriendMoments" type="checkbox" />
        <span>不看对方朋友圈</span>
      </label>

      <div class="permissions-actions">
        <button class="permissions-btn cancel" @click="businessCardStore.cancelPermissions">
          取消
        </button>
        <button class="permissions-btn confirm" @click="businessCardStore.confirmPermissions">
          确定
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
defineOptions({
  name: 'BusinessCard',
})

import { useUserStore } from '../stores/userStore.js'
import { useBusinessCardStore } from '../stores/businessCardStore.js'
import { useImagePreviewStore } from '../stores/imagePreviewStore.js'
import { useRouter } from 'vue-router'

const businessCardStore = useBusinessCardStore()
const imagePreviewStore = useImagePreviewStore()
const userStore = useUserStore()
const router = useRouter()

const remarkInputRef = ref(null)

const cardStyle = computed(() => ({
  top: businessCardStore.position.y + 'px',
  left: businessCardStore.position.x + 'px',
}))

const startEditRemark = async () => {
  businessCardStore.setRemark()
  await nextTick()
  remarkInputRef.value?.focus()
  remarkInputRef.value?.select()
}

const handleSaveRemark = async () => {
  if (!businessCardStore.isEditingRemark) return
  await businessCardStore.saveRemark()
}

const previewAvatar = () => {
  imagePreviewStore.open({
    src: businessCardStore.userInfo.avatar,
    alt: businessCardStore.userInfo.username,
  })
}

const getImageUrl = (name) => {
  return new URL(`${name}`, import.meta.url).href
}
</script>
<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.business-card {
  position: absolute;
  width: 215px;
  padding: 20px;
  border-radius: 10px;
  background: white;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

/* 上部分样式 */
.header-section {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

.info {
  flex: 1;
  min-width: 0;
}

.remark-or-username {
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
  flex-direction: row;
  min-width: 0;
  width: 100%;
}

.remark-input {
  flex: 1;
  width: 0;
  max-width: 100%;
  min-width: 0;
  height: 28px;
  padding: 0 8px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  outline: none;
  box-sizing: border-box;
}

.remark-input:focus {
  border-color: #29e069;
}

.username {
  font-size: 14px;
  color: #666;
}

.account-id,
.region {
  font-size: 12px;
  color: #999;
}

/* 设置按钮样式 */
.settings {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 20px;
  cursor: pointer;
  z-index: 10;
  transition: color 0.2s ease;
  user-select: none;
}

.settings-icon:hover {
  color: #9c9e9d;
}

.dropdown-menu {
  position: absolute;
  top: 30px;
  right: 0;
  padding: 5px;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 5px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 150px;
  z-index: 100;
}

.menu-item {
  padding: 10px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 5px;
  transition: background-color 0.2s ease;
}

.menu-item:hover {
  background-color: #29e069;
  color: white;
}

.menu-item.danger:hover {
  color: white;
  background-color: #ff4d4f;
}

/* 中间部分样式 */
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

.arrow-icon {
  width: 12px;
  height: 12px;
  visibility: hidden;
}

.moments-header:hover .arrow-icon {
  visibility: visible;
}

.moments-preview {
  display: flex;
  gap: 5px;
}

.moment-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 5px;
}

/* 底部信息样式 */
.footer-info {
  margin-bottom: 20px;
  font-size: 12px;
  color: #666;
}

.common-groups,
.signature,
.source {
  margin-bottom: 5px;
}

/* 操作按钮样式 */
.actions {
  display: flex;
  justify-content: space-around;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.action-item:hover {
  transform: scale(1.1);
}

.action-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 5px;
}

.action-item span {
  font-size: 12px;
  color: #333;
}

.gender-icon {
  width: 16px;
  height: 16px;
  margin-left: 5px;
  vertical-align: middle;
}

.permissions-dialog {
  position: absolute;
  top: 90px;
  left: 50%;
  transform: translateX(-50%);
  width: 230px;
  padding: 16px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
  z-index: 300;
  box-sizing: border-box;
}

.permissions-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.permission-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
  margin-bottom: 12px;
  cursor: pointer;
  user-select: none;
}

.permission-option input {
  width: 16px;
  height: 16px;
  margin: 0;
}

.permissions-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.permissions-btn {
  min-width: 64px;
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.permissions-btn.cancel {
  background: #f2f3f5;
  color: #333;
}

.permissions-btn.confirm {
  background: #29e069;
  color: #fff;
}
</style>
