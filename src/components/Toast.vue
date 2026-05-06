<template>
  <Teleport to="body">
    <div v-show="toast.visible" class="toast-wrap" @click="toast.hide">
      <div class="toast" :class="toast.type">
        {{ toast.message }}
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { useToastStore } from '@/stores/toastStore'
defineOptions({
  name: 'Toast',
})
const toast = useToastStore()
</script>

<style scoped>
.toast-wrap {
  position: fixed;
  inset: 0;
  z-index: 20000;
  pointer-events: none;
}

.toast {
  position: fixed;
  left: 50%;
  top: 18px;
  transform: translateX(-50%);
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.78);
  color: #fff;
  box-shadow: 0 10px 30px rgba(0,0,0,.25);
  pointer-events: auto;
  user-select: none;
}

/* 不指定颜色也能用；这里用轻微差异做区分 */
.toast.success { }
.toast.error { }
.toast.info { }
</style>