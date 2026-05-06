<!-- src/pages/AddFriends/index.vue -->
<template>
  <div>
    <div class="add-friends-page">
      <!-- 头部：搜索区域 -->
      <div class="search-section">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            class="search-input"
            placeholder="请输入账户名"
            v-model="searchQuery"
            @keyup.enter="handleSearch"
          />
        </div>
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>

      <!-- 搜索结果列表 -->
      <div class="search-results">
        <div v-for="user in searchResults" :key="user.userId" class="result-item">
          <!-- 头像 -->
          <img :src="user.avatar" :alt="user.username" class="avatar" />

          <!-- 用户信息 -->
          <div class="user-info">
            <div class="username-row">
              <span class="username">{{ user.username }}</span>
              <!-- 性别 icon -->
              <img :src="getSexIcon(user.sex)" :alt="user.sex" class="gender-icon" />
            </div>
            <div class="account-id">ID: {{ user.accountId }}</div>
          </div>

          <!-- 添加按钮 -->
          <button
            class="add-btn"
            v-show="!friendStore.isFriend(userId)"
            @click="clickAddFriend(user)"
          >
            添加
          </button>
        </div>

        <!-- 无结果提示 -->
        <div v-if="searchResults.length === 0 && hasSearched" class="no-results">
          未找到相关用户
        </div>

        <!-- 初始状态提示 -->
        <div v-if="!hasSearched" class="initial-hint">请输入账户名搜索好友</div>
      </div>
    </div>
    <AddFriendModal
      :visible="showAddFriendModal"
      :user="selectedUser"
      @close="showAddFriendModal = false"
      @submit="sendFriendRequest"
      @error="handleModalError"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '/src/api/request.js'
import { useUserStore } from '/src/stores/userStore.js'
import { useFriendStore } from '/src/stores/friendStore.js'
import { useToastStore } from '/src/stores/toastStore.js'
import AddFriendModal from '/src/components/AddFriendModal.vue'
defineOptions({
  name: 'AddFriends',
})

const userStore = useUserStore()
const friendStore = useFriendStore()
const toastStore = useToastStore()

// 本地状态管理
const searchQuery = ref('')
const searchResults = ref([])
const hasSearched = ref(false)
const isLoading = ref(false)

const showAddFriendModal = ref(false)
const selectedUser = ref(null)
// 获取性别图标
const getSexIcon = (sex) => {
  const iconMap = {
    1: '/src/assets/AddFriendsWindow/boy.png',
    0: '/src/assets/AddFriendsWindow/girl.png',
  }
  console.log('iconMap', sex, iconMap[sex])
  return iconMap[sex] || ''
}

// 搜索处理
const handleSearch = async () => {
  if (!searchQuery.value.trim()) return

  isLoading.value = true
  hasSearched.value = true

  try {
    const res = await request.post('searchFriends', {
      searchQuery: searchQuery.value,
      userId: userStore.userInfo.userId,
    })
    console.log('搜索结果:', res)
    searchResults.value = res.data.searchUsers
  } catch (error) {
    console.error('搜索失败:', error)
    searchResults.value = []
  } finally {
    isLoading.value = false
  }
}

const clickAddFriend = (user) => {
  console.log('点击添加好友:', user.userId, user.accountId)
  selectedUser.value = user
  showAddFriendModal.value = true
}

// 添加好友处理
const sendFriendRequest = async (payload) => {
  console.log('添加好友:', payload)
  payload.requesterUserId = userStore.userInfo.userId
  const res = await request.post('sendFriendRequest', payload)
  console.log('sendFriendRequest', res)
  if (res.code === 0) {
    toastStore.success(res.msg)
    payload.friendRequestId = res.data
    payload.status = 0
    window.electronAPI.sendFriendRequest(payload)
  } else {
    toastStore.error(res.msg)
  }
  showAddFriendModal.value = false
}

const handleModalError = (error) => {
  console.error('添加好友失败:', error)
  toastStore.error(error)
}
</script>

<style scoped>
.add-friends-page {
  width: 100%;
  height: 100vh;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}

/* 搜索区域 */
.search-section {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  gap: 10px;
  background-color: #fff;
}

.search-input-wrapper {
  flex: 1;
  max-width: 400px;
  display: flex;
  align-items: center;
  padding: 4px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #fff;
}

.search-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  margin-bottom: 5px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 20px;
  height: 20px;
}

.search-btn {
  padding: 8px 20px;
  background-color: #07c160;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.search-btn:hover {
  background-color: #06ad56;
}

.search-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 搜索结果列表 */
.search-results {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.result-item {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s;
}

.result-item:hover {
  background-color: #f9f9f9;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
}

.user-info {
  flex: 1;
}

.account-id {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.gender-icon {
  width: 16px;
  height: 16px;
}

.add-btn {
  padding: 6px 16px;
  background-color: #07c160;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.2s;
}

.add-btn:hover {
  background-color: #06ad56;
}

.no-results,
.initial-hint {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}
</style>
