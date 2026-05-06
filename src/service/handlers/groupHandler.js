import { useConversationStore } from '/src/stores/conversationStore'
import { useGroupStore } from '/src/stores/groupStore'

export function groupHandler(message) {
  switch (message.event) {
    case 'create':
      handleCreate(message.data)
      break
    case 'join':
      handleJoin(message.data)
      break
    case 'quit':
      handleQuit(message.data)
      break
    case 'disband':
      handleDisband(message.data)
      break
    case 'group_update':
      handleGroupUpdate(message.data)
      break
    case 'members_update':
      handleMembersUpdate(message.data)
      break
    default:
      console.log('unknown event', message.event)
      break
  }
}

function handleCreate(data) {
  console.log('handleCreate', data)
  const conversationStore = useConversationStore()
  const groupStore = useGroupStore()
  conversationStore.upsertConversation(data.conversation)
  groupStore.upsertGroup(data.group)
}

function handleJoin(data) {
  console.log('handleJoin', data)
  const groupStore = useGroupStore()
  groupStore.groupMembersJoin({
    group: data.group,
    groupMembers: data.groupMembers,
  })
}

function handleQuit(data) {
  console.log('handleQuit', data)
  const groupStore = useGroupStore()
  groupStore.groupMembersQuit({
    groupId: data.groupId,
    memberUserIds: data.memberUserIds,
  })
}

function handleDisband(data) {
  console.log('handleDisband', data)
  const groupStore = useGroupStore()
  groupStore.groupDisband(data.groupId)
}

function handleGroupUpdate(data) {
  console.log('handleGroupUpdate', data)
  const groupStore = useGroupStore()
  groupStore.groupUpdate(data.group)
}

function handleMembersUpdate(data) {
  console.log('handleMemberUpdate', data)
  const groupStore = useGroupStore()
  groupStore.groupMembersUpdate(data.groupMembers)
}
