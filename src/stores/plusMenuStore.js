import { defineStore } from 'pinia'
import { useBuildGroupModalStore } from './buildGroupModalStore.js'

export const usePlusMenuStore = defineStore('plusMenu', {
  state: () => ({
    visible: false,
    x: 0,
    y: 0
  }),

  actions: {
    showMenu(x, y){
        this.visible = true
        this.x = x
        this.y = y
    },
    hideMenu(){
        this.visible = false
    },
    handleBuildGroup(){
      console.log('handleGroupChat')
      const buildGroupModalStore = useBuildGroupModalStore()
      buildGroupModalStore.showModal()
      this.hideMenu()
    },
    async handleAddFriend(){
      console.log('handleAddFriend')
      await window.electronAPI.openAddFriendsWindow()
      this.hideMenu()
    }
  }
})
