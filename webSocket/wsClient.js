// services/WsClient.js
const WebSocket = require('ws')
const { buildWsMessage } = require('./protocol.js')

class WsClient {
  constructor() {
    this.ws = null
    this.browserWindowClass = null
    this.options = {}
    this.heartbeatInterval = null
    this.reconnectInterval = null
    this.url = null
    this.isConnected = false
    this.manualClose = false
    this.pendingMessages = []
    this.maxPendingMessages = 100
  }

  connect(url, options = {}) {
    if (
      this.ws &&
      (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)
    ) {
      return
    }

    this.url = url
    this.options = options

    try {
      this.ws = new WebSocket(this.url, this.options)

      this.ws.on('open', () => {
        console.log('WebSocket 连接成功')
        this.isConnected = true
        this.manualClose = false
        this.startHeartbeat()
        this.stopReconnect()
        this.flushPendingMessages()

        this.notifyRenderer('ws:status', { status: 'connected' })
      })

      this.ws.on('message', (rawData) => {
        this.handleMessage(rawData)
      })

      this.ws.on('close', () => {
        console.log('WebSocket 连接断开')

        this.isConnected = false
        this.stopHeartbeat()
        this.ws = null

        this.notifyRenderer('ws:status', { status: "disconnected" })

        if (!this.manualClose) {
          this.startReconnect()
        }
      })

      this.ws.on('error', (error) => {
        console.error('WebSocket 错误:', error)
        this.notifyRenderer('ws:error', {
          message: error?.message || 'WebSocket error',
        })
      })
    } catch (error) {
      console.error('WebSocket 连接失败:', error)
      this.isConnected = false
      this.startReconnect()
    }
  }

  setBrowserWindow(BrowserWindow) {
    this.browserWindowClass = BrowserWindow
  }

  handleMessage(rawData) {
    try {
      const text = Buffer.isBuffer(rawData) ? rawData.toString('utf-8') : String(rawData)

      const message = JSON.parse(text)
      console.log('WebSocket 收到消息:', text)

      if (message.type === 'conn' && message.event === 'pong') {
        return
      }

      this.notifyRenderer('ws:receive', message)
    } catch (error) {
      console.error('WebSocket 消息解析失败:', error)
      this.notifyRenderer('ws:error', {
        message: 'WebSocket 消息解析失败',
      })
    }
  }

  notifyRenderer(channel, data) {
    console.log('通知 renderer:', channel, data)
    try {
      if (!this.browserWindowClass) {
        console.warn('BrowserWindow 未注入，无法通知 renderer:', channel)
        return
      }

      const windows = this.browserWindowClass.getAllWindows()

      for (const win of windows) {
        if (!win || win.isDestroyed()) continue
        if (!win.webContents || win.webContents.isDestroyed()) continue

        win.webContents.send(channel, data)
      }
    } catch (error) {
      console.error('notifyRenderer 失败:', error, channel, data)
    }
  }

  send(payload) {
    if (!payload || typeof payload !== 'object') {
      console.warn('发送失败：payload 必须是对象')
      return false
    }

    if (!this.isConnected || !this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket 未连接，消息缓存')
      this.cacheMessage(payload)
      return false
    }

    try {
      const message = buildWsMessage(payload)
      this.ws.send(JSON.stringify(message))
      return true
    } catch (error) {
      console.error('发送消息失败:', error, payload)
      this.cacheMessage(payload)
      return false
    }
  }

  startHeartbeat() {
    this.stopHeartbeat()

    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected && this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.send({
          type: 'conn',
          event: 'ping',
          data: null,
        })
      }
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  startReconnect() {
    if (this.reconnectInterval || !this.url || this.manualClose) return
    this.notifyRenderer('ws:status', { status: 'reconnecting' })
    this.reconnectInterval = setInterval(() => {
      console.log('尝试重连 WebSocket...')
      this.connect(this.url, this.options)
    }, 5000)
  }

  stopReconnect() {
    if (this.reconnectInterval) {
      clearInterval(this.reconnectInterval)
      this.reconnectInterval = null
    }
  }

  cacheMessage(payload) {
    if (this.pendingMessages.length >= this.maxPendingMessages) {
      this.pendingMessages.shift()
    }
    this.pendingMessages.push(payload)
    console.log('缓存消息:', payload)
  }

  flushPendingMessages() {
    if (!this.pendingMessages.length) return

    const queue = [...this.pendingMessages]
    this.pendingMessages = []

    for (const payload of queue) {
      const ok = this.send(payload)
      if (!ok) {
        this.cacheMessage(payload)
      }
    }
  }

  disconnect() {
    this.manualClose = true
    this.stopHeartbeat()
    this.stopReconnect()

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }

    this.isConnected = false
    this.notifyRenderer('ws:status', { status: 'disconnected' })
  }
}

module.exports = new WsClient()
