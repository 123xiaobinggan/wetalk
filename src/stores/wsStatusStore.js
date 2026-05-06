import { defineStore } from 'pinia'
import { init } from '/src/service/init.js'

export const useWsStatusStore = defineStore('ws', {
  state: () => ({
    status: 'connected',
  }),
  getters: {
    getStatus: (state) => state.status,
  },
  actions: {
    setStatus(status) {
      console.log('wsStatusStore', status)
      this.status = status
      if(status === "connected"){
        init()
      }
    },
  },
})
