<template>
  <div class="friend-detail-page">
    <FriendDetail
      v-if="friend?.friendUserId"
      :friend="friend"
      @send-message="handleSendMessage"
      @audio-call="handleAudioCall"
      @video-call="handleVideoCall"
      @update-remark="handleUpdateRemark"
      @update-permissions="handleUpdatePermissions"
      @block-friend="handleBlockFriend"
      @delete-friend="handleDeleteFriend"
    />
  </div>
</template>

<script setup>
import FriendDetail from '/src/components/FriendDetail.vue'
import useFriendDetail from './index'
defineOptions({
  name: 'FriendDetail',
})

const {
  friend,
  handleSendMessage,
  handleAudioCall,
  handleVideoCall,
  handleUpdateRemark,
  handleUpdatePermissions,
  handleBlockFriend,
  handleDeleteFriend,
} = useFriendDetail()
</script>

<style scoped>
@import url('index.css');
</style>
