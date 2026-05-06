<template>
  <div class="home-container">
    <Sidebar />

    <RouterView v-slot="{ Component }">
      <KeepAlive>
        <component :is="Component" />
      </KeepAlive>
    </RouterView>

    <Teleport to="body">
      <BusinessCard v-if="businessCardStore.visible" />
      <ImagePreview v-if="imagePreviewStore.visible" />
      <PlusMenu v-if="plusMenuStore.visible" />
      <BuildGroupModal v-if="buildGroupModalStore.visible" />
      <AddFriendModal
        :visible="addFriendModalStore.visible"
        :user="addFriendModalStore.user"
        @close="addFriendModalStore.close"
        @submit="addFriendModalStore.submit"
      />
    </Teleport>
  </div>
</template>

<script setup lang="js">
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../../components/Sidebar.vue'
import BusinessCard from '../../components/BusinessCard.vue'
import ImagePreview from '../../components/ImagePreview.vue'
import PlusMenu from '../../components/PlusMenu.vue'
import BuildGroupModal from '../../components/BuildGroupModal.vue'
import AddFriendModal from '../../components/AddFriendModal.vue'

import { useUserStore } from '../../stores/userStore.js'
import { useBusinessCardStore } from '../../stores/businessCardStore.js'
import { useImagePreviewStore } from '../../stores/imagePreviewStore.js'
import { useConversationStore } from '../../stores/conversationStore.js'
import { usePlusMenuStore } from '../../stores/plusMenuStore.js'
import { useBuildGroupModalStore } from '../../stores/buildGroupModalStore.js'
import { useAddFriendModalStore } from '../../stores/addFriendModalStore.js'

defineOptions({
  name: 'HomePage',
})

const businessCardStore = useBusinessCardStore()
const imagePreviewStore = useImagePreviewStore()
const userStore = useUserStore()
const conversationStore = useConversationStore()
const plusMenuStore = usePlusMenuStore()
const buildGroupModalStore = useBuildGroupModalStore()
const addFriendModalStore = useAddFriendModalStore()

onMounted(async () => {
  const auth = await window.electronAPI.authGet()
  console.log('auth', auth.userInfo)
  if (auth && auth.userInfo) {
    userStore.setUserInfo(auth.userInfo)
  }
  if (auth && auth.token) {
    userStore.setToken(auth.token)
  }
  conversationStore.init(userStore.userInfo)
})
</script>

<style scoped>
@import url('index.css');
</style>
