const {
  app,
  BrowserWindow,
  ipcMain,
  net,
  nativeImage,
  dialog,
  clipboard,
  shell,
} = require('electron')
const path = require('path')
const url = require('url')
const fs = require('fs')
const http = require('http')
const https = require('https')
const axios = require('axios')
const ElectronStore = require('electron-store')
const Store = ElectronStore && ElectronStore.default ? ElectronStore.default : ElectronStore
const WsClient = require('./webSocket/wsClient.js')

const store = new Store({ name: 'mytalk' })
const baseUrl = 'http://120.48.156.237:8080/'

// ====== 窗口 ======
let enterWindow
let mainWindow
let settingsWindow = null
let addFriendsWindow = null
let audioCallWindow = null

// 通话信息
let audioCallPayload = null
let audioCallForceClosing = false
let audioCallCloseTimer = null
// 创建登录窗口
function createEnterWindow() {
  enterWindow = new BrowserWindow({
    width: 375,
    height: 500,
    autoHideMenuBar: true,
    icon: getAppIcon(),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
    },
  })

  loadPage(enterWindow, '/')
}

// 创建主窗口
function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1050,
    height: 700,
    minWidth: 1050,
    minHeight: 450,
    autoHideMenuBar: true,
    icon: getAppIcon(),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
    },
  })
  loadPage(mainWindow, '/home')

  mainWindow.on('closed', () => {
    mainWindow = null
  })
  return mainWindow
}

// 创建设置窗口
function createSettingsWindow(parentWindow) {
  if (settingsWindow && !settingsWindow.isDestroyed()) {
    settingsWindow.focus()
    return
  }

  settingsWindow = new BrowserWindow({
    width: 520,
    height: 800,
    resizable: false,
    minimizable: false,
    maximizable: false,
    autoHideMenuBar: true,
    icon: getAppIcon(),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
    },
  })

  loadPage(settingsWindow, '/settings')

  settingsWindow.on('closed', () => {
    settingsWindow = null
  })
}

//创建加朋友窗口
function createAddFriendsWindow(parentWindow) {
  if (addFriendsWindow && !addFriendsWindow.isDestroyed()) {
    addFriendsWindow.focus()
    return
  }

  addFriendsWindow = new BrowserWindow({
    width: 350,
    height: 475,
    useContentSize: true,
    resizable: false,
    minimizable: false,
    maximizable: false,
    autoHideMenuBar: true,
    icon: getAppIcon(),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
    },
  })

  loadPage(addFriendsWindow, '/addFriends')

  addFriendsWindow.on('closed', () => {
    addFriendsWindow = null
  })
}

// 创建语音通话窗口
function createAudioCallWindow() {
  audioCallForceClosing = false

  audioCallWindow = new BrowserWindow({
    width: 360,
    height: 560,
    resizable: false,
    minimizable: true,
    maximizable: false,
    autoHideMenuBar: true,
    title: '语音通话',
    backgroundColor: '#ffffff',
    frame: false,
    icon: getAppIcon(),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
    },
  })

  loadPage(audioCallWindow, '/audioCall')

  audioCallWindow.on('close', (e) => {
    if (audioCallForceClosing) {
      return
    }

    e.preventDefault()

    console.log('audioCallWindow close intercepted')

    if (audioCallCloseTimer) {
      return
    }

    // 通知语音窗口：你要被关闭了，先做挂断/清理
    audioCallWindow.webContents.send('call:audio-window-close-request')

    // 兜底：如果渲染进程没有响应，2 秒后强制关闭
    audioCallCloseTimer = setTimeout(() => {
      audioCallForceClosing = true

      if (audioCallWindow && !audioCallWindow.isDestroyed()) {
        audioCallWindow.close()
      }
    }, 2000)
  })

  audioCallWindow.on('closed', () => {
    if (audioCallCloseTimer) {
      clearTimeout(audioCallCloseTimer)
      audioCallCloseTimer = null
    }

    audioCallWindow = null
    audioCallPayload = null
    callPayload = null
    audioCallForceClosing = false
  })
}

// ====== WebSocket ======
function createWs() {
  wsUrl = 'ws://120.48.156.237:8080/ws'
  options = {
    headers: {
      Authorization: `Bearer ${store.get('auth', null).token}`,
    },
  }
  WsClient.setBrowserWindow(BrowserWindow)
  WsClient.connect(wsUrl, options)
}

function disconnectWs() {
  console.log('disconnectWs')
  WsClient.disconnect()
}

// 发送消息
ipcMain.handle('ws:send', async (event, payload) => {
  console.log('ws:send', payload)
  WsClient.send(payload)
})

// ====== Auth IPC（主进程权威）======
ipcMain.handle('renderer:ready', () => {
  console.log('renderer:ready')
  if (mainWindow) {
    createWs()
  }
  if (audioCallPayload && audioCallWindow) {
    audioCallWindow.webContents.send('call:payload', audioCallPayload)
  }
})

ipcMain.handle('auth:set', async (_, payload) => {
  // payload: { token, userInfo }
  if (store.get('auth', null)) {
    if (!payload.token && store.get('auth', null)?.token) {
      payload.token = store.get('auth', null).token
    }
    if (!payload.userInfo && store.get('auth', null)?.userInfo) {
      payload.userInfo = store.get('auth', null)?.userInfo
    }
  }
  store.set('auth', payload)
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send('auth:update')
  }
  console.log('auth:set', payload)
  return { ok: true }
})

ipcMain.handle('auth:get', async () => {
  return store.get('auth', null)
})

ipcMain.handle('auth:clear', async () => {
  store.delete('auth')
  const allWindows = BrowserWindow.getAllWindows()
  allWindows.forEach((win) => win.close())
  createEnterWindow()
  disconnectWs()
  return { ok: true }
})

// 保存：弹出保存对话框 -> 写入文件
ipcMain.handle('image:saveFromUrl', async (event, url) => {
  if (!url) return { ok: false, error: 'EMPTY_URL' }

  // 1) 下载
  const { buffer, contentType, statusCode } = await fetchBufferByNet(url)
  if (statusCode && statusCode >= 400) {
    return { ok: false, error: `HTTP_${statusCode}` }
  }

  // 2) 推断默认文件名/扩展名
  const nameFromUrl = safeFileNameFromUrl(url)
  const extFromCT = extFromContentType(contentType)
  const hasExt = path.extname(nameFromUrl)
  const defaultName = hasExt ? nameFromUrl : nameFromUrl + (extFromCT || '.png')

  // 3) 选择保存路径
  const result = await dialog.showSaveDialog({
    title: '保存图片',
    defaultPath: defaultName,
    filters: [
      { name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp', 'gif', 'bmp'] },
      { name: 'All Files', extensions: ['*'] },
    ],
  })

  if (result.canceled || !result.filePath) {
    return { ok: false, error: 'CANCELED' }
  }

  // 4) 写入
  await fs.promises.writeFile(result.filePath, buffer)
  return { ok: true, filePath: result.filePath }
})

// 复制：下载 -> nativeImage -> clipboard
ipcMain.handle('image:copyFromUrl', async (event, url) => {
  if (!url) return { ok: false, error: 'EMPTY_URL' }

  const { buffer, statusCode } = await fetchBufferByNet(url)
  if (statusCode && statusCode >= 400) {
    return { ok: false, error: `HTTP_${statusCode}` }
  }

  const img = nativeImage.createFromBuffer(buffer)
  if (img.isEmpty()) {
    return { ok: false, error: 'INVALID_IMAGE' }
  }

  clipboard.writeImage(img)
  return { ok: true }
})

// 窗口：打开设置
ipcMain.handle('win:openSettings', () => {
  const parent = mainWindow // 你主窗口变量
  createSettingsWindow(parent)
})

// 窗口：打开朋友添加窗口
ipcMain.handle('win:openAddFriendsWindow', () => {
  const parent = mainWindow // 你主窗口变量
  createAddFriendsWindow(parent)
})

// 设置：选择头像
ipcMain.handle('settings:pickAvatar', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog({
    title: '选择头像',
    properties: ['openFile'],
    filters: [{ name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp'] }],
  })

  if (canceled || !filePaths?.length) return null

  const filePath = filePaths[0]
  const buffer = fs.readFileSync(filePath)

  return {
    buffer,
    filename: path.basename(filePath),
  }
})

// 文件：下载并显示
ipcMain.handle('file:downloadAndReveal', async (event, payload = {}) => {
  const { url, fileName } = payload

  try {
    if (!url) {
      return {
        success: false,
        error: '文件地址为空',
      }
    }

    if (url.startsWith('blob:') || url.startsWith('data:')) {
      return {
        success: false,
        error: '临时文件无法下载，请等待上传完成',
      }
    }

    const targetPath = await downloadToDownloads(url, fileName)

    shell.showItemInFolder(targetPath)

    return {
      success: true,
      filePath: targetPath,
    }
  } catch (e) {
    console.error('file:downloadAndReveal error', e)

    return {
      success: false,
      error: e.message,
    }
  }
})

// 发起语音通话
ipcMain.handle('call:startAudio', async (_event, payload) => {
  audioCallPayload = payload

  if (audioCallWindow && !audioCallWindow.isDestroyed()) {
    audioCallWindow.focus()
    console.log('不能同时多个通话')
    return { code: 1, msg: '不能同时多个通话' }
  }

  createAudioCallWindow()
  audioCallPayload = payload
  console.log('call:startAudio', payload)
  return { code: 0, msg: '成功' }
})

// 结束
ipcMain.handle('call:closeAudioWindow', async () => {
  audioCallForceClosing = true

  if (audioCallCloseTimer) {
    clearTimeout(audioCallCloseTimer)
    audioCallCloseTimer = null
  }

  if (audioCallWindow && !audioCallWindow.isDestroyed()) {
    audioCallWindow.close()
  }
})

// 窗口状态
ipcMain.on('window_status', (event, { width, height }) => {
  if (enterWindow) {
    enterWindow.setResizable(true)
    enterWindow.setSize(width, height)
    enterWindow.setResizable(false)
  }
})

// 登录成功：由渲染进程调用
ipcMain.on('enter_success', async () => {
  if (enterWindow) {
    enterWindow.close()
    enterWindow = null
  }
  createMainWindow()
})

// 发送好友申请
ipcMain.on('friend:sendFriendRequest', (event, friendRequest) => {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send('friend:friendRequestSend', friendRequest)
  }
})

app.on('ready', async () => {
  // 启动时判断是否已登录（持久化）
  const auth = store.get('auth', null)
  console.log('auth:', auth)
  if (auth && auth.token && auth.userInfo) {
    try {
      const res = await axios.get(baseUrl + 'api/autoEnter', {
        headers: { Authorization: `Bearer ${auth.token}` },
      })
      console.log('res:', res.data)
      if (res.data.code === 0) {
        const payload = {
          userInfo: res.data.data.user,
          token: res.data.data.token ?? auth.token,
        }
        store.set('auth', payload)
        createMainWindow()
      } else {
        createEnterWindow()
      }
    } catch (e) {
      console.log('e:', e.message)
      createEnterWindow()
    }
  } else {
    createEnterWindow()
  }
})

app.on('window-all-closed', () => {
  disconnectWs()

  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    const auth = store.get('auth', null)
    auth.token || auth.userInfo ? createMainWindow() : createEnterWindow()
  }
})

// 根据 Content-Type 推断扩展名
function extFromContentType(ct = '') {
  const t = ct.split(';')[0].trim().toLowerCase()
  if (t === 'image/png') return '.png'
  if (t === 'image/jpeg') return '.jpg'
  if (t === 'image/jpg') return '.jpg'
  if (t === 'image/webp') return '.webp'
  if (t === 'image/gif') return '.gif'
  if (t === 'image/bmp') return '.bmp'
  return ''
}

// 用 Electron net 下载（支持 http/https；不会受浏览器 CORS 限制）
function fetchBufferByNet(url) {
  return new Promise((resolve, reject) => {
    try {
      const request = net.request(url)

      request.on('response', (response) => {
        const chunks = []
        response.on('data', (chunk) => chunks.push(chunk))
        response.on('end', () => {
          const buffer = Buffer.concat(chunks)
          const contentType = response.headers['content-type']?.[0] || ''
          resolve({ buffer, contentType, statusCode: response.statusCode })
        })
      })

      request.on('error', (err) => reject(err))
      request.end()
    } catch (e) {
      reject(e)
    }
  })
}

function safeFileNameFromUrl(urlStr) {
  try {
    const u = new URL(urlStr)
    const base = path.basename(u.pathname) || 'image'
    // 去掉奇怪字符
    return base.replace(/[\\/:*?"<>|]+/g, '_')
  } catch {
    return 'image'
  }
}

function downloadToDownloads(url, fileName = '') {
  return new Promise((resolve, reject) => {
    const downloadDir = path.join(app.getPath('downloads'), 'WeTalk')

    fs.mkdirSync(downloadDir, {
      recursive: true,
    })

    const safeName = buildSafeDownloadName(url, fileName)
    const targetPath = getAvailableFilePath(downloadDir, safeName)

    const file = fs.createWriteStream(targetPath)
    const client = url.startsWith('https:') ? https : http

    const req = client.get(url, (res) => {
      // 简单处理 301 / 302 重定向
      if ([301, 302, 303, 307, 308].includes(res.statusCode)) {
        file.close()
        fs.unlink(targetPath, () => {})

        const location = res.headers.location
        if (!location) {
          reject(new Error(`下载重定向失败，状态码: ${res.statusCode}`))
          return
        }

        const redirectUrl = new URL(location, url).href
        downloadToDownloads(redirectUrl, fileName).then(resolve).catch(reject)
        return
      }

      if (res.statusCode !== 200) {
        file.close()
        fs.unlink(targetPath, () => {})
        reject(new Error(`下载失败，状态码: ${res.statusCode}`))
        return
      }

      res.pipe(file)

      file.on('finish', () => {
        file.close(() => resolve(targetPath))
      })
    })

    req.on('error', (err) => {
      file.close()
      fs.unlink(targetPath, () => {})
      reject(err)
    })
  })
}

function buildSafeDownloadName(url, fileName = '') {
  let name = fileName

  if (!name) {
    try {
      name = path.basename(new URL(url).pathname)
    } catch {
      name = ''
    }
  }

  if (!name) {
    name = `wetalk_file_${Date.now()}`
  }

  return sanitizeFileName(name)
}

function getAvailableFilePath(dir, fileName) {
  const ext = path.extname(fileName)
  const base = path.basename(fileName, ext)

  let targetPath = path.join(dir, fileName)
  let index = 1

  while (fs.existsSync(targetPath)) {
    targetPath = path.join(dir, `${base}(${index})${ext}`)
    index++
  }

  return targetPath
}

function sanitizeFileName(name) {
  return String(name).replace(/[\\/:*?"<>|]/g, '_')
}

function loadPage(win, route = '/') {
  const normalizedRoute = route.startsWith('/') ? route : `/${route}`

  if (!app.isPackaged) {
    win.loadURL(`http://localhost:5173/#${normalizedRoute}`)
  } else {
    win.loadFile(path.join(__dirname, 'dist/index.html'), {
      hash: normalizedRoute,
    })
  }
}

function getAppIcon() {
  return path.join(__dirname, 'build/wetalk.ico')
}
