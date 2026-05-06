<template>
  <div v-if="visible" class="modal-mask" @click="handleMaskClick">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <span class="title">添加好友</span>
        <button class="close-btn" @click="handleCancel">×</button>
      </div>

      <div class="modal-body">
        <div class="user-info">
          <img class="avatar" :src="user?.avatar || defaultAvatar" alt="avatar" />
          <div class="user-meta">
            <div class="username">{{ user?.username || '未知用户' }}</div>
            <div class="account">账号：{{ user?.accountId || user?.userId || '--' }}</div>
          </div>
        </div>

        <div class="form-item">
          <label>申请语句</label>
          <textarea
            v-model="form.requestMsg"
            class="textarea"
            maxlength="100"
            placeholder="请输入申请语句"
          />
          <div class="count">{{ form.requestMsg.length }}/100</div>
        </div>

        <div class="form-item">
          <label>备注</label>
          <input v-model="form.remark" class="input" maxlength="30" placeholder="请输入备注名" />
        </div>

        <div class="form-item">
          <label>权限设置</label>
          <div class="switch-group">
            <label class="checkbox-row">
              <input type="checkbox" v-model="form.hideMyMoments" />
              <span>不允许对方查看我的朋友圈</span>
            </label>

            <label class="checkbox-row">
              <input type="checkbox" v-model="form.hideFriendMoments" />
              <span>不看对方朋友圈</span>
            </label>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-cancel" @click="handleCancel">取消</button>
        <button class="btn btn-confirm" @click="handleSubmit">发送申请</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { useUserStore } from '/src/stores/userStore.js'
defineOptions({
  name: 'AddFriendModal',
})
const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  user: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['close', 'submit', 'error'])

const userStore = useUserStore()

const defaultAvatar = ''

const form = reactive({
  requestMsg: '',
  remark: '',
  hideMyMoments: false,
  hideFriendMoments: false,
})

const initForm = () => {
  form.requestMsg = `你好，我是${userStore.userInfo.username}`.trim() || '你好，我想添加你为好友'
  form.remark = ''
  form.hideMyMoments = false
  form.hideFriendMoments = false
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      initForm()
    }
  },
)

const handleMaskClick = () => {
  handleCancel()
}

const handleCancel = () => {
  emit('close')
}

const handleSubmit = () => {
  const payload = {
    requesteeUserId: props.user?.userId,
    requestMsg: form.requestMsg.trim(),
    remark: form.remark.trim(),
    hideMyMoments: form.hideMyMoments,
    hideFriendMoments: form.hideFriendMoments,
  }

  if (!payload.requesteeUserId) {
    emit('error', '目标用户不存在')
    return
  }

  if (!payload.requestMsg) {
    emit('error', '申请语句不能为空')
    return
  }

  emit('submit', payload)
}
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 24px;
  box-sizing: border-box;
}

.modal-container {
  width: 460px;
  max-width: 100%;
  max-height: 90vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}

.modal-header {
  height: 56px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  border: none;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 18px;
  overflow-y: auto;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f3f3f3;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  background: #f2f2f2;
}

.user-meta {
  margin-left: 12px;
}

.username {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.account {
  margin-top: 4px;
  font-size: 13px;
  color: #888;
}

.form-item {
  margin-bottom: 18px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.textarea {
  min-height: 88px;
  resize: none;
}

.input:focus,
.textarea:focus {
  border-color: #409eff;
}

.count {
  margin-top: 6px;
  text-align: right;
  font-size: 12px;
  color: #999;
}

.switch-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #444;
}

.modal-footer {
  height: 64px;
  padding: 0 18px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.btn {
  min-width: 88px;
  height: 36px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.btn-cancel {
  background: #f3f4f6;
  color: #333;
}

.btn-confirm {
  background: #409eff;
  color: #fff;
}
</style>
