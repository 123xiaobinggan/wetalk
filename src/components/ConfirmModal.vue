<template>
  <Teleport to="body">
    <div v-if="visible" class="overlay" @click.self="onCancel">
      <div class="modal">
        <div class="title">{{ title }}</div>

        <div class="content">
          <slot>{{ message }}</slot>
        </div>

        <div class="footer">
          <button class="btn cancel" @click="onCancel">{{ cancelText }}</button>
          <button class="btn confirm" :class="{ danger }" @click="onConfirm">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineOptions({
  name: 'ConfirmModal'
})
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '提示' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  danger: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'cancel'])

function onConfirm() {
  emit('confirm')
}
function onCancel() {
  emit('cancel')
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.modal {
  width: 340px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}
.title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
.content {
  font-size: 14px;
  color: #555;
  margin-bottom: 18px;
  line-height: 1.6;
}
.footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn {
  padding: 6px 18px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}
.cancel {
  background: #eee;
}
.confirm {
  background: #4caf50;
  color: #fff;
}
.confirm.danger {
  background: #e53935;
}
</style>