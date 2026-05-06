import { chatHandler } from './handlers/chatHandler.js'
import { callHandler } from './handlers/callHandler.js'
import { friendHandler } from './handlers/friendHandler.js'
import { groupHandler } from './handlers/groupHandler.js'
import { systemHandler } from './handlers/systemHandler.js'
import { connHandler } from './handlers/connHandler.js'

export function dispatchWsMessage(data) {
  // 处理消息
  switch (data.type) {
    case 'chat':
      chatHandler(data)
      break
    case 'friend':
      friendHandler(data)
      break
    case 'group':
      groupHandler(data)
      break
    case 'call':
      callHandler(data)
      break
    case 'system':
      systemHandler(data)
      break
    case 'conn':
      connHandler(data)
      break
    default:
      console.log('unknown message type', data)
      break
  }
}
