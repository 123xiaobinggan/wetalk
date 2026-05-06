import { defineStore } from 'pinia'

export const useToastStore = defineStore('toast', {
  state: () => ({
    visible: false,
    message: '',
    type: 'success', // success | error | info
    timer: null,
    duration: 1500,
  }),

  actions: {
    show(message, type = 'info', duration = 1500) {
      this.message = message
      this.type = type
      this.visible = true
      this.duration = duration

      if (this.timer) clearTimeout(this.timer)
      this.timer = setTimeout(() => {
        this.visible = false
        this.timer = null
      }, duration)
    },

    success(message, duration) {
      this.show(message, 'success', duration ?? this.duration)
    },

    loading(message, duration) {
      this.show(message, 'loading', duration ?? this.duration)
    },

    error(message, duration) {
      this.show(message, 'error', duration ?? this.duration)
    },

    info(message, duration) {
      this.show(message, 'info', duration ?? this.duration)
    },

    hide() {
      this.visible = false
      if (this.timer) clearTimeout(this.timer)
      this.timer = null
    }
  }
})
