import { useConversationStore } from '/src/stores/conversationStore.js'

export function chatHandler(message) {
  switch (message.event) {
    case 'receive':
      handleReceiveMessage(message.data)
      break
    case 'recall':
      handleRecallMessage(message.data)
      break
    case 'ack':
      handleAckMessage(message)
      break
    case 'failed':
      handleFailedMessage(message)
      break
    default:
      break
  }
}

function handleReceiveMessage(data) {
  const conversationStore = useConversationStore()
  conversationStore.receiveMessage(data)
}

function handleAckMessage(message) {
  console.log('handleAck:' + message)
  const conversationStore = useConversationStore()
  conversationStore.sendMsgSuccess({ clientMsgId: message.clientMsgId, data: message.data })
}

function handleFailedMessage(message) {
  console.log('handleFailed:' + message)
  const conversationStore = useConversationStore()
  conversationStore.sendMsgFailed({ clientMsgId: message.clientMsgId, msg: message.msg })
}

function handleRecallMessage(data) {
  const conversationStore = useConversationStore()
  conversationStore.messageRecall(data.message)
}
