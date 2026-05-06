import { ref, computed } from 'vue'
import { useConversationStore } from '/src/stores/conversationStore.js'
import { usePlusMenuStore } from '/src/stores/plusMenuStore.js'
import { useWsStatusStore } from '/src/stores/wsStatusStore.js'
import { confirm } from '/src/utils/confirm.js'
export default function useRecentConversation() {
  const conversationStore = useConversationStore()
  const plusMenuStore = usePlusMenuStore()
  const wsStatusStore = useWsStatusStore()

  const activeConversationId = computed(() => {
    return conversationStore.activeConversationId
  })

  const wsStatus = computed(() => {
    return wsStatusStore.getStatus
  })

  const conversationsList = computed(() => {
    return conversationStore.getConversations
  })

  const handlePlusClick = ({ x, y }) => {
    console.log('handlePlusClick', x, y)
    plusMenuStore.showMenu(x, y)
  }

  const selectConversation = (convId) => {
    console.log('selectConversation', conversationsList.value.filter((c) => c.convId === convId)[0])
    conversationStore.openConversation(convId)
  }

  const togglePin = async ({ convId, pinned }) => {
    const conv = { convId, pinned }
    await conversationStore.updateConversation(conv)
  }

  const toggleMute = async ({ convId, muted }) => {
    const conv = { convId, muted }
    await conversationStore.updateConversation(conv)
  }

  const markAsRead = async ({ convId }) => {
    await conversationStore.markConversationRead(convId)
  }

  const deleteConversation = async ({ convId }) => {
    const conv = { convId, deleted: true }
    const sure = await confirm('确定要删除该会话吗？', {
      title: '删除会话',
      confirmText: '删除',
      cancelText: '取消',
      danger: true,
    })
    if (!sure) return
    await conversationStore.updateConversation(conv)
  }

  return {
    wsStatus,
    conversationsList,
    activeConversationId,
    handlePlusClick,
    selectConversation,
    togglePin,
    toggleMute,
    markAsRead,
    deleteConversation,
  }
}
