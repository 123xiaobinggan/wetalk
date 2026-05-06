<template>
  <div class="friend-request-detail">
    <!-- 上部分：用户基本信息 -->
    <div class="header-section">
      <img :src="friendRequest?.avatar" alt="Avatar" class="avatar" />
      <div class="info-container">
        <div class="account-row">
          <span class="account-id">{{ friendRequest?.accountId }}</span>
          <!-- 性别图标，假设 sex: 'male' | 'female' | 'other' -->
          <span v-if="friendRequest?.sex">
            <img :src="getSexIcon(friendRequest?.sex)" alt="性别图标" class="sex-icon" />
          </span>
        </div>
        <div class="username">{{ friendRequest?.username }}</div>
      </div>
    </div>

    <!-- 中间部分：请求信息 -->
    <div class="request-msg-section">
      <div class="label">请求信息：</div>
      <div class="msg-content">{{ friendRequest?.requestMsg || '无附加消息' }}</div>
    </div>

    <!-- 设置部分：备注与朋友圈权限 -->
    <div class="settings-section">
      <div class="form-item">
        <label>备注</label>
        <input type="text" v-model="remark" placeholder="请输入备注名" class="remark-input" />
      </div>

      <div class="form-item checkbox-group">
        <label>朋友圈状态</label>
        <div class="switch-group">
          <label class="checkbox-row">
            <input type="checkbox" v-model="settings.hideMyMoments" />
            <span>不允许对方查看我的朋友圈</span>
          </label>

          <label class="checkbox-row">
            <input type="checkbox" v-model="settings.hideFriendMoments" />
            <span>不看对方朋友圈</span>
          </label>
        </div>
      </div>
    </div>

    <!-- 底部按钮区 -->
    <div class="action-buttons">
      <button
        v-if="
          friendRequest.status !== 2 && friendRequest.requesterUserId !== userStore.userInfo.userId
        "
        class="btn btn-agree"
        @click="handleAccept"
      >
        同意
      </button>
      <button class="btn btn-block" @click="handleBlock">加入黑名单</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '/src/stores/userStore'

// 定义 Props
const props = defineProps({
  friendRequest: {
    type: Object,
    required: true,
    default: () => ({
      avatar: '',
      accountId: '',
      sex: '', // 'male', 'female'
      username: '',
      requestMsg: '',
    }),
  },
})

// 定义 Emits
const emit = defineEmits(['accept', 'block'])

const userStore = useUserStore()

// 本地状态
const remark = ref('')
const settings = reactive({
  hideMyMoments: false,
  hideFriendMoments: false,
})

// 辅助函数：获取性别图标符号
const getSexIcon = (sex) => {
  if (sex === 1) return '/src/assets/FriendRequestDetail/boy.png'
  if (sex === 0) return '/src/assets/FriendRequestDetail/girl.png'
  return ''
}

// 处理同意
const handleAccept = () => {
  emit('accept', {
    friendRequestId: props.friendRequest.friendRequestId,
    remark: remark.value,
    hideMyMoments: settings.hideMyMoments,
    hideFriendMoments: settings.hideFriendMoments,
  })
}

// 处理加入黑名单
const handleBlock = () => {
  // 通常加入黑名单前可能需要二次确认，这里直接触发事件
  emit('block', {
    requestId: props.friendRequest.id,
    accountId: props.friendRequest.accountId,
  })
}
</script>

<style scoped>
/* 如果 index.css 为空或需要基础样式补充，可在此处添加基础布局样式 */
.friend-request-detail {
  max-width: 400px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 头部区域 */
.header-section {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  background-color: #f0f0f0;
}

.info-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.account-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.sex-icon {
  width: 16px;
  height: 16px;
  margin-left: 5px;
  vertical-align: middle;
}

.username {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.signature {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}

/* 请求消息区域 */
.request-msg-section {
  background-color: #f9f9f9;
  padding: 12px;
  border-radius: 6px;
  font-size: 14px;
}

.request-msg-section .label {
  color: #888;
  margin-bottom: 4px;
  font-size: 12px;
}

.msg-content {
  color: #333;
  line-height: 1.5;
}

/* 设置区域 */
.settings-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.remark-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.remark-input:focus {
  border-color: #52c41a;
}

/* 按钮区域 */
.action-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.btn {
  width: 60%;
  padding: 12px;
  border-radius: 25px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  font-weight: 500;
}

.btn-agree {
  background-color: #52c41a; /* 绿色 */
  color: white;
}

.btn-agree:hover {
  background-color: #73d13d;
}

.btn-block {
  background-color: #fff; /* 白色背景 */
  color: #ff4d4f; /* 红色文字表示危险操作，或者用灰色 #666 */
  border: 1px solid #ff4d4f; /* 可选：加边框增强视觉 */
}

.btn-block:hover {
  background-color: #fff1f0;
}

.switch-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #444;
}
</style>
