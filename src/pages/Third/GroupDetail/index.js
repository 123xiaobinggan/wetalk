import { reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGroupStore } from '/src/stores/groupStore'
import { useConversationStore } from '/src/stores/conversationStore'
import { useUserStore } from '/src/stores/userStore'
export function useGroupDetail() {
  const groupStore = useGroupStore()
  const conversationStore = useConversationStore()
  const userStore = useUserStore()
  const router = useRouter()

  const group = computed(() => groupStore.activeGroup())
  const conversation = computed(() => {
    return conversationStore.getConversations.filter((c) => c.groupId === group.value.groupId)[0]
  })

  watch(
    () => group.value,
    () => {
      if (group.value) {
        console.log('group', group.value)
      }
    },
  )

  const groupInfo = computed(() => ({
    groupId: group.value.groupId,
    groupAvatar: group.value.groupAvatar,
    groupRemark: conversation.value?.groupRemark,
    groupName: group.value.groupName,
  }))

  const handleEnterGroup = async () => {
    await conversationStore.getConversation(true, null, group.value.groupId)
    var convId = null
    for (const key of Object.keys(conversationStore.conversationsMap)) {
      const conv = conversationStore.conversationsMap[key]
      if (conv.convType === true && conv.groupId === group.value.groupId) {
        convId = conv.convId
        break
      }
    }
    if (convId == null) return
    conversationStore.openConversation(convId)
    router.push('/home/talk/recentConversation/talkwindow')
  }

  return {
    groupInfo,
    handleEnterGroup,
  }
}
