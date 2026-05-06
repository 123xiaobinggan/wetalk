import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { initListener } from './service/listener.js'
import { useUserStore } from './stores/userStore.js'
import { useConversationStore } from './stores/conversationStore.js'


import App from './App.vue'
import router from './router'

import './assets/main.css'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
app.use(router)

async function bootstrap() {
  const userStore = useUserStore()
  await userStore.init()
  initListener()
  window.electronAPI.rendererReady()
  app.mount('#app')
}

bootstrap()
