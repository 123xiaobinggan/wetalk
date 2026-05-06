<template>
  <RecentConversations
    :wsStatus="wsStatus"
    :conversationsList="conversationsList"
    :activeConversationId="activeConversationId"
    @selectConversation="selectConversation"
    @handlePlusClick="handlePlusClick"
    @togglePin="togglePin"
    @toggleMute="toggleMute"
    @markAsRead="markAsRead"
    @deleteConversation="deleteConversation"
  />
  <router-view />
</template>

<script setup lang="js">
import RecentConversations from '../../../components/RecentConversations.vue'
import useRecentConversation from './index.js'
defineOptions({
  name: 'RecentConversation',
})
const {
  wsStatus,
  conversationsList,
  activeConversationId,
  handlePlusClick,
  selectConversation,
  togglePin,
  toggleMute,
  markAsRead,
  deleteConversation,
} = useRecentConversation()
</script>

<style>
@import url('index.css');
</style>
