import { useConversationStore } from '/src/stores/conversationStore.js'
import { useFriendStore } from '/src/stores/friendStore.js'
import { useUserStore } from '/src/stores/userStore.js'
import { useGroupStore } from '/src/stores/groupStore.js'
export async function init() {
  const userStore = useUserStore()
  const conversationStore = useConversationStore()
  const friendStore = useFriendStore()
  const groupStore = useGroupStore()
  console.log('init')
  await Promise.all([userStore.init(), conversationStore.init(), friendStore.init(), groupStore.init()])
}
