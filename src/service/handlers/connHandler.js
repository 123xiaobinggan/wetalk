import { useConversationStore } from '/src/stores/conversationStore.js'

export function connHandler(message) {
  switch (message.event) {
    default:
      console.log('unknown message type: ' + message.type)
      break
  }
}
