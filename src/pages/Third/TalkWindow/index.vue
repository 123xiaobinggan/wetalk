<template>
  <TalkWindow
    v-if="peerId"
    :peerId="peerId"
    :convType="false"
    :me="userStore.userInfo.userId"
    :messages="messages"
    :unreadCnt="unreadCnt"
    v-model="draft"
    :profileVisible="showProfilePanel"
    :historyLoading="historyLoading"
    :hasMoreHistory="hasMoreHistory"
    @toggle-profile="onToggleProfile"
    @sendText="handleSendText"
    @sendAudio="handleSendMedia"
    @sendImage="handleSendMedia"
    @sendVideo="handleSendMedia"
    @sendFile="handleSendMedia"
    @call="startAudioCall"
    @video-call="startVideoCall"
    :pendingFiles="pendingFiles"
    :quoteMessage="quoteMessage"
    @add-pending-file="addPendingFile"
    @remove-pending-file="removePendingFile"
    @clear-pending-files="clearPendingFiles"
    @set-quote-message="setQuoteMessage"
    @clear-quote-message="clearQuoteMessage"
    @recall-message="recallMessage"
    @delete-message="deleteMessage"
    @forward-message="openForwardModal"
    @update-setting="updateConversation"
    @save-group-field="saveGroupField"
    @search-chat-history="searchChatHistory"
    @clear-chat-history="clearChatHistory"
    @delete-contact="deleteFriend"
    @quit-group="quitGroup"
    @add-friend="addFriend"
    @add-member="addMember"
    @mark-conversation-read="markConversationRead"
    @load-more-history="loadMoreHistory"
    ref="chatHistoryRef"
  />
  <TalkWindow
    v-if="groupId"
    :groupId="groupId"
    :convType="true"
    :me="userStore.userInfo.userId"
    :messages="messages"
    :unreadCnt="unreadCnt"
    v-model="draft"
    :profileVisible="showProfilePanel"
    :historyLoading="historyLoading"
    :hasMoreHistory="hasMoreHistory"
    @toggle-profile="onToggleProfile"
    @sendText="handleSendText"
    @sendAudio="handleSendMedia"
    @sendImage="handleSendMedia"
    @sendVideo="handleSendMedia"
    @sendFile="handleSendMedia"
    @call="startAudioCall"
    @video-call="startVideoCall"
    :pendingFiles="pendingFiles"
    :quoteMessage="quoteMessage"
    @add-pending-file="addPendingFile"
    @remove-pending-file="removePendingFile"
    @clear-pending-files="clearPendingFiles"
    @set-quote-message="setQuoteMessage"
    @clear-quote-message="clearQuoteMessage"
    @recall-message="recallMessage"
    @delete-message="deleteMessage"
    @forward-message="openForwardModal"
    @update-setting="updateConversation"
    @save-group-field="saveGroupField"
    @search-chat-history="searchChatHistory"
    @change-group-avatar="changeGroupAvatar"
    @clear-chat-history="clearChatHistory"
    @delete-contact="deleteFriend"
    @quit-group="quitGroup"
    @add-friend="addFriend"
    @add-member="addMember"
    @remove-member="removeMember"
    @disband-group="disbandGroup"
    @transfer-group-owner="transferGroupOwner"
    @update-group-member-role="updateGroupMembersRole"
    @mark-conversation-read="markConversationRead"
    @load-more-history="loadMoreHistory"
    @silence-all="silenceAll"
    @unsilence-all="unsilenceAll"
    @silence-members="silenceMembers"
    @unsilence-members="unsilenceMembers"
    ref="chatHistoryRef"
  />
  <ForwardMessageModal
    :visible="forwardModalVisible"
    :conversations="forwardConversations"
    :friends="forwardFriends"
    @close="closeForwardModal"
    @confirm="handleForwardConfirm"
  />
  <GroupFriendManageModal
    :visible="groupFriendModalVisible"
    :mode="membersManageMode"
    :candidates="candidates"
    @close="groupFriendModalVisible = false"
    @confirm="handleMembersManage"
  />
  <ChatSearchModal
    ref="chatSearchModalRef"
    :visible="chatSearchVisible"
    :messages="messages"
    :convType="Boolean(groupId)"
    :peerId="peerId"
    :groupId="groupId"
    :loading="chatSearchLoading"
    :hasMore="chatSearchHasMore"
    @close="chatSearchVisible = false"
    @locate="locateSearchMessage"
    @load-more="loadMoreChatSearchMessages"
  />
</template>

<script setup lang="js">
import { ref, computed } from 'vue'
defineOptions({
  name: 'TalkWindow',
})
import TalkWindow from '../../../components/TalkWindow.vue'
import GroupFriendManageModal from '../../../components/GroupFriendManageModal.vue'
import ForwardMessageModal from '../../../components/ForwardMessageModal.vue'
import ChatSearchModal from '../../../components/ChatSearchModal.vue'
import useTalkWindow from './index.js'
import { useUserStore } from '/src/stores/userStore.js'
const userStore = useUserStore()
const chatSearchModalRef = ref(null)
const {
  peerId,
  groupId,
  draft,
  messages,
  unreadCnt,
  showProfilePanel,
  chatHistoryRef,
  historyLoading,
  hasMoreHistory,

  groupFriendModalVisible,
  membersManageMode,
  candidates,

  handleSendText,
  handleSendMedia,

  onToggleProfile,
  startAudioCall,
  startVideoCall,
  pendingFiles,
  quoteMessage,
  addPendingFile,
  removePendingFile,
  clearPendingFiles,
  setQuoteMessage,
  clearQuoteMessage,
  recallMessage,
  deleteMessage,
  
  forwardModalVisible,
  forwardConversations,
  forwardFriends,
  openForwardModal,
  closeForwardModal,

  handleForwardConfirm,
  updateConversation,
  saveGroupField,
  searchChatHistory,
  changeGroupAvatar,
  clearChatHistory,
  deleteFriend,
  quitGroup,
  addFriend,
  addMember,
  removeMember,
  disbandGroup,
  transferGroupOwner,
  updateGroupMembersRole,
  silenceAll,
  unsilenceAll,
  silenceMembers,
  unsilenceMembers,
  markConversationRead,
  loadMoreHistory,
  handleMembersManage,
  chatSearchVisible,
  chatSearchLoading,
  chatSearchHasMore,
  locateSearchMessage,
  loadMoreChatSearchMessages,
} = useTalkWindow()
</script>

<style scoped>
@import url('index.css');
</style>
