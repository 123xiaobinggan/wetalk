<!-- src/components/Sidebar.vue -->
<template>
  <div class="sidebar">
    <!-- 用户头像 -->
    <div class="avatar-section" @click="businessCardStore.showCard(userStore.userInfo, $event)">
      <img :src="userStore.userInfo.avatar" alt="User Avatar" class="avatar" />
    </div>

    <!-- 功能图标区域 -->
    <div class="menu-section">
      <div
        class="menu-item"
        v-for="item in menuItems"
        :key="item.name"
        @click="navigate(item.route)"
      >
        <div class="icon-wrapper">
          <img :src="getIcon(item)" :alt="item.name" class="icon" />

          <!-- 未读角标 -->
          <div v-if="item.unread && item.unread > 0" class="badge">
            {{ formatUnread(item.unread) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 底部设置图标 -->
    <div class="settings-section" @click="openSettings()">
      <img :src="settingIcon" alt="Settings" class="icon" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore.js'
import { useConversationStore } from '../stores/conversationStore.js'
import { useBusinessCardStore } from '../stores/businessCardStore.js'
import { useFriendStore } from '../stores/friendStore.js'

import talkIcon from '../assets/sidebar/talk.png'
import talkActiveIcon from '../assets/sidebar/talk_active.png'
import friendIcon from '../assets/sidebar/friend.png'
import friendActiveIcon from '../assets/sidebar/friend_active.png'
import favoritesIcon from '../assets/sidebar/favorites.png'
import favoritesActiveIcon from '../assets/sidebar/favorites_active.png'
import momentsIcon from '../assets/sidebar/moments.png'
import momentsActiveIcon from '../assets/sidebar/moments_active.png'
import settingIcon from '../assets/sidebar/setting.png'

defineOptions({
  name: 'Sidebar',
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const conversationStore = useConversationStore()
const friendStore = useFriendStore()
const businessCardStore = useBusinessCardStore()

const menuItems = computed(() => [
  {
    name: '聊天',
    icon: talkIcon,
    activeIcon: talkActiveIcon,
    route: '/home/talk',
    unread: conversationStore.unread,
  },
  {
    name: '好友',
    icon: friendIcon,
    activeIcon: friendActiveIcon,
    route: '/home/friends',
    unread: friendStore.unread,
  },
  {
    name: '收藏',
    icon: favoritesIcon,
    activeIcon: favoritesActiveIcon,
    route: '/home/favorites',
    unread: 0,
  },
  {
    name: '朋友圈',
    icon: momentsIcon,
    activeIcon: momentsActiveIcon,
    route: '/home/moments',
    unread: 0,
  },
])

const getIcon = (item) => {
  return route.path.includes(item.route) ? item.activeIcon : item.icon
}

const navigate = (routePath) => {
  if (routePath === '/home/moments') {
    openMoments()
    return
  }
  if (routePath) {
    router.replace(routePath)
  }
}

const openMoments = () => {
  // window.electronAPI?.openMoments()
}

const openSettings = () => {
  window.electronAPI.openSettings()
}

const formatUnread = (count) => {
  if (count > 99) return '99+'
  return count
}
</script>

<style scoped>
.sidebar {
  width: calc(100% / 15);
  flex-shrink: 0;
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  box-sizing: border-box;
  margin-left: 0px;
}

.avatar-section {
  margin-bottom: 30px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.menu-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 25px;
  width: 100%;
  align-items: center;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  width: 65%;
  padding-top: 5px;
  border-radius: 5px;
  transition: transform 0.2s ease;
}

.menu-item:hover {
  transform: scale(1.1);
  background-color: #e8e7e7;
}

.icon-wrapper {
  position: relative;
  display: inline-block;
}

.icon {
  height: 30px;
  margin-bottom: 5px;
  display: block;
}

.badge {
  position: absolute;
  top: -6px;
  right: -8px;

  min-width: 18px;
  height: 18px;
  padding: 0 4px;

  display: flex;
  align-items: center;
  justify-content: center;

  background-color: #ff3b30;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  border-radius: 999px;
  box-sizing: border-box;
  border: 2px solid #f5f5f5;
  white-space: nowrap;
}

.settings-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.settings-section:hover {
  transform: scale(1.1);
}
</style>
