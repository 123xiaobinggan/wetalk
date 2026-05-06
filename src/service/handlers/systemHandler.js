import { useToastStore } from '/src/stores/toastStore.js'

export function systemHandler(message) {
  switch (message.event) {
    case 'error':
      handleError(message.data)
      break
    case 'notice':
      handleNotice(message.data)
      break
    case 'online':
      handleOnline(message.data)
      break
    case 'offline':
      handleOffline(message.data)
      break
  }
}

function handleError(data) {
  console.log('handleError', data)
  const toastStore = useToastStore()
  toastStore.error(data.msg)
}

function handleNotice(data) {
  console.log('handleNotice', data)
}

function handleOnline(data) {
  console.log('handleOnline', data)
}

function handleOffline(data) {
  console.log('handleOffline', data)
}
