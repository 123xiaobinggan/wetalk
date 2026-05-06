<template>
  <FriendsList
    :friendRequests="friendRequests"
    :groups="groups"
    :friends="friends"
    @click-friendRequest="clickFriendRequest"
    @click-group="clickGroup"
    @click-contact="clickContact"
    @handleAccept="handleAccept"
  />
  <router-view />
</template>

<script setup lang="js">
import { ref } from 'vue'
import FriendsList from '../../../components/FriendsList.vue'
import useFriendsList from './index.js'
defineOptions({
  name: 'FriendsList',
})

const {
  friendRequests,
  groups,
  friends,
  clickFriendRequest,
  clickGroup,
  clickContact,
  handleAccept,
} = useFriendsList()
</script>

<style scoped>
.talk-window {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: calc(100% - 25% - 100% / 15);
  background-color: hsl(0, 0%, 96%);
}

/* 顶部区域 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.nickname {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.more-options {
  font-size: 20px;
  cursor: pointer;
  color: #999;
}

.profile-panel {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
  color: #666;
}

.divider {
  height: 1px;
  background-color: #e0e0e0;
}

/* 聊天记录区域 */
.chat-history {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #fafafa;
}

.message {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  max-width: 70%;
}

.message.sent {
  margin-left: auto;
  /* text-align: right; */
  flex-direction: row-reverse;
}

.message.received {
  margin-right: auto;
  /* text-align: left; */
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 10px;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
}

.message-content {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
}

.message.sent .message-content {
  background-color: #1890ff;
  color: #fff;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.message-time.sent {
  text-align: right;
}

.message-time.received {
  text-align: left;
}

/* 底部功能区 */
.footer {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
}

.icon-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left-icons,
.right-icons {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.icon-btn {
  font-size: 18px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

.input-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.message-input {
  flex: 1;
  padding: 0px 12px;
  border: none;
  outline: none;
  font-size: 14px;
  resize: none;
  min-height: 175px;
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.5;
}

.send-btn {
  padding: 8px 16px;
  background-color: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  height: 32px;
  align-self: flex-end;
}

/* 全局滚动条样式 - assets/main.css */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  transition: all 0.2s;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

::-webkit-scrollbar-corner {
  background: transparent;
}

/* 暗色主题适配 */
.dark ::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
}

.dark ::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style>
