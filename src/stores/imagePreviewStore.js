import { defineStore } from 'pinia'
import { useToastStore } from './toastStore.js'

export const useImagePreviewStore = defineStore('imagePreview', {
  state: () => ({
    visible: false,
    src: '',
    alt: '',
    scale: 1,
    offset: { x: 0, y: 0 },
    rotate: 0,
    flipX: false,
    flipY: false,
    dragging: false,
    dragStart: { x: 0, y: 0 },
    offsetStart: { x: 0, y: 0 },
  }),

  actions: {
    open({ src, alt = '' }) {
      this.visible = true
      this.src = src
      this.alt = alt
      this.resetView()
    },

    close() {
      this.visible = false
      this.dragging = false
    },

    resetView() {
      this.scale = 1
      this.offset = { x: 0, y: 0 }
      this.dragging = false
    },

    zoom(delta, center = null) {
      // delta: +1/-1 or wheel delta
      const old = this.scale
      const next = Math.min(4, Math.max(0.3, old + delta))
      this.scale = next

      // 简单实现：缩放不围绕鼠标点，先够用（要围绕鼠标我也能给你升级）
      if (center) {
        // 可扩展
      }
    },

    rotateRight() {
      this.rotate = (this.rotate + 90) % 360
    },

    rotateLeft() {
      this.rotate = (this.rotate + 270) % 360
    },

    toggleFlipX() {
      this.flipX = !this.flipX
    },

    toggleFlipY() {
      this.flipY = !this.flipY
    },

    startDrag(e) {
      this.dragging = true
      this.dragStart = { x: e.clientX, y: e.clientY }
      this.offsetStart = { ...this.offset }
    },

    onDrag(e) {
      if (!this.dragging) return
      const dx = e.clientX - this.dragStart.x
      const dy = e.clientY - this.dragStart.y
      this.offset = { x: this.offsetStart.x + dx, y: this.offsetStart.y + dy }
    },

    endDrag() {
      this.dragging = false
    },

    async downloadImage() {
      const toast = useToastStore()
      try {
        const res = await window.electronAPI.saveImageFromUrl(this.src)
        console.log('res',res)
        if (res?.ok) {
          toast.success('下载成功')
        } else if (res?.error === 'CANCELED') {
          // 用户取消不提示也行
        } else {
          toast.error('下载失败')
          console.error(res)
        }
      } catch (e) {
        toast.error('下载失败')
        console.error(e)
      }
    },

    async copyImage() {
      const toast = useToastStore()
      try {
        const res = await window.electronAPI.copyImageFromUrl(this.src)
        console.log('res',res)
        if (res?.ok) {
          toast.success('已复制')
        } else {
          toast.error('复制失败')
          console.error(res)
        }
      } catch (e) {
        toast.error('复制失败')
        console.error(e)
      }
    }
  }
})
