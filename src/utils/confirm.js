import { createApp, h, reactive } from 'vue'
import ConfirmModal from '../components/ConfirmModal.vue'

export function confirm(message, options = {}) {
  return new Promise((resolve) => {
    const state = reactive({
      visible: true,
      title: options.title ?? '提示',
      message: message ?? options.message ?? '',
      confirmText: options.confirmText ?? '确定',
      cancelText: options.cancelText ?? '取消',
      danger: options.danger ?? false,
    })

    const mountEl = document.createElement('div')
    document.body.appendChild(mountEl)

    const app = createApp({
      render() {
        return h(ConfirmModal, {
          visible: state.visible,
          title: state.title,
          message: state.message,
          confirmText: state.confirmText,
          cancelText: state.cancelText,
          danger: state.danger,
          onConfirm: () => {
            cleanup()
            resolve(true)
          },
          onCancel: () => {
            cleanup()
            resolve(false)
          },
        })
      },
    })

    function cleanup() {
      state.visible = false
      // 给个很短的时间让 Teleport v-if 结束也行，不要也行
      app.unmount()
      mountEl.remove()
    }

    app.mount(mountEl)
  })
}