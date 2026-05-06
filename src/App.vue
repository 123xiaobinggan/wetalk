<script setup>
import { onMounted } from 'vue'
import Toast from './components/Toast.vue'

onMounted(async () => {
  console.log('app mounted')
})
</script>

<template>
  <div class="app-route">
    <router-view />
    <Toast />
  </div>
</template>

<style scoped>
.app-route {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 全局滚动条样式 - assets/main.css */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  transition: all 0.2s;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

::-webkit-scrollbar-corner {
  background: transparent;
}

/* 暗色主题适配 */
.dark ::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
}

.dark ::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style>
