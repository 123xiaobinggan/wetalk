const electron = require('electron')
const { contextBridge, ipcRenderer } = electron
const webUtils = electron.webUtils

contextBridge.exposeInMainWorld('electronAPI', {
  authSet: (payload) => ipcRenderer.invoke('auth:set', payload),
  authGet: () => ipcRenderer.invoke('auth:get'),
  authClear: () => ipcRenderer.invoke('auth:clear'),

  window_status: (width, height) => {
    console.log('window_status', width, height)
    ipcRenderer.send('window_status', { width, height })
  },

  enter_success: () => {
    ipcRenderer.send('enter_success')
  },

  sendFriendRequest: (payload) => ipcRenderer.send('friend:sendFriendRequest', payload),

  saveImageFromUrl: (url) => ipcRenderer.invoke('image:saveFromUrl', url),
  copyImageFromUrl: (url) => ipcRenderer.invoke('image:copyFromUrl', url),

  openSettings: () => ipcRenderer.invoke('win:openSettings'),

  openAddFriendsWindow: () => ipcRenderer.invoke('win:openAddFriendsWindow'),

  // 订阅用户信息更新（跨窗口同步）
  onAuthUpdated: (callback) => {
    const handler = (_event) => callback()
    ipcRenderer.on('auth:update', handler)

    // 返回一个取消订阅函数（避免内存泄漏）
    return () => ipcRenderer.removeListener('auth:update', handler)
  },

  // 好友申请发送:更新主窗口friendStore
  onFriendRequestSend: (callback) => {
    const handler = (_event, data) => callback(data)
    ipcRenderer.on('friend:friendRequestSend', handler)

    return () => ipcRenderer.removeListener('friend:friendRequestSend', handler)
  },

  // 本地铃声文件
  pickRingtone: () => ipcRenderer.invoke('settings:pickRingtone'),
  // 选本地头像文件
  pickAvatar: () => ipcRenderer.invoke('settings:pickAvatar'),

  downloadFileAndReveal: (payload) => ipcRenderer.invoke('file:downloadAndReveal', payload),

  getPathForFile: (file) => {
    try {
      if (webUtils?.getPathForFile) {
        return webUtils.getPathForFile(file) || ''
      }

      // 兼容旧版本 Electron
      return file?.path || ''
    } catch (e) {
      console.warn('getPathForFile failed:', e)
      return ''
    }
  },

  openMediaFile: (payload) => ipcRenderer.invoke('open-media-file', payload),

  startAudioCall: (payload) => ipcRenderer.invoke('call:startAudio', payload),
  closeAudioCallWindow: () => ipcRenderer.invoke('call:closeAudioWindow'),

  onAudioCallPayload: (callback) => {
    const handler = (_event, data) => callback(data)
    ipcRenderer.on('call:payload', handler)
    return () => ipcRenderer.removeListener('call:payload', handler)
  },

  onAudioCallWindowCloseRequest: (callback) => {
    const handler = () => callback()
    ipcRenderer.on('call:audio-window-close-request', handler)

    return () => {
      ipcRenderer.removeListener('call:audio-window-close-request', handler)
    }
  },

  // ws
  wsSend: (payload) => ipcRenderer.invoke('ws:send', payload),
  // 收到消息
  onWsReceive: (callback) => {
    const handler = (_event, data) => callback(data)
    ipcRenderer.on('ws:receive', handler)

    return () => ipcRenderer.removeListener('ws:receive', handler)
  },
  // ws状态
  onWsStatus: (callback) => {
    const handler = (_event, data) => callback(data)
    ipcRenderer.on('ws:status', handler)
    return () => ipcRenderer.removeListener('ws:status', handler)
  },
  rendererReady: () => ipcRenderer.invoke('renderer:ready'),
})
