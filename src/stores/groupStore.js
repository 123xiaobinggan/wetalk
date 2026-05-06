import { defineStore } from 'pinia'
import { computed } from 'vue'
import { useUserStore } from './userStore'
import { useConversationStore } from './conversationStore'
import { useFriendStore } from './friendStore'
import { useToastStore } from './toastStore'
import request from '/src/api/request.js'

export const useGroupStore = defineStore('group', {
  state: () => ({
    groups: {},
    // 存放群成员数据 {groupId:[{},{}]}
    groupMembers: {},
    fetchingGroupMembers: {},
    activeGroupId: null,
  }),

  getters: {
    getGroups(state) {
      return state.groups
    },
    getGroup(state) {
      return (groupId) => state.groups[groupId]
    },
    activeGroup(state) {
      return () => state.groups[state.activeGroupId]
    },
    getGroupMemberProfile(state) {
      const userStore = useUserStore()
      const friendStore = useFriendStore()
      return (groupId, memberUserId) => {
        return computed(() => {
          if (!groupId || !memberUserId) {
            return null
          }
          console.log('getGroupMemberProfile', groupId, memberUserId)
          if (!(groupId in state.groupMembers) || !state.groupMembers[groupId]) {
            this.fetchGroupMembers(groupId)
            return { ...(userStore.getUser(memberUserId) || {}) }
          }
          const member = state.groupMembers[groupId]?.find(
            (member) => member.memberUserId === memberUserId,
          )
          let friendship = {}
          if (friendStore.isFriend(memberUserId)) {
            friendship = friendStore.gettersFriendship(memberUserId)
          }
          return {
            ...(userStore.getUser(memberUserId) || {}),
            ...(member || {}),
            ...friendship,
          }
        })
      }
    },
    // 获取特定群的群成员
    getGroupMembers(state) {
      return (groupId) => {
        if (!groupId) {
          return []
        }
        if (state.groupMembers[groupId]) {
          return state.groupMembers[groupId].sort(
            (a, b) => b.role - a.role || a.joinSeq - b.joinSeq,
          )
        } else {
          this.fetchGroupMembers(groupId)
          return []
        }
      }
    },
  },

  actions: {
    async init() {
      await this.fetchGroups()
    },

    async fetchGroups() {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchGroups', { userId: userStore.userInfo.userId })
        console.log('fetchGroups ', res)
        if (res.code === 0) {
          res.data.groups.forEach((group) => {
            this.groups[group.groupId] = group
          })
        } else {
          toastStore.error('获取群聊失败')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('获取群聊失败')
      }
    },
    async fetchGroupMembers(groupId) {
      if (groupId in this.fetchingGroupMembers) {
        return
      }
      this.fetchingGroupMembers[groupId] = true
      const toastStore = useToastStore()
      try {
        const res = await request.post('fetchGroupMembers', { groupId })
        console.log('fetchGroupMembers ', res)
        if (res.code === 0) {
          this.groupMembers[groupId] = []
          this.groupMembers[groupId] = res.data.groupMembers
        } else {
          toastStore.error('获取群成员失败')
        }
      } catch (e) {
        toastStore.error('获取群成员失败')
        console.log(e)
      } finally {
        delete this.fetchingGroupMembers[groupId]
      }
    },

    openGroup(groupId) {
      this.activeGroupId = groupId
    },

    upsertGroup(group) {
      this.groups[group.groupId] = group
    },

    async quitGroup(groupId) {
      if (!this.groups[groupId]) {
        return
      }
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('quitGroup', {
          groupId,
          userIds: [userStore.userInfo.userId],
        })
        console.log(res)
        if (res.code !== 0) {
          toastStore.error(res.msg)
          return
        } else {
          toastStore.success('退出群聊成功')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('退出群聊失败')
      }
    },
    // 只有群主有权限
    async setGroupMembersRole(groupMembers) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      const ownerUserId = userStore.userInfo.userId
      try {
        const res = await request.post('setGroupMembersRole', {
          groupMembers,
          ownerUserId,
        })
        console.log(res)
        if (res.code !== 0) {
          toastStore.error(res.msg)
        } else {
          toastStore.success('更新成功')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('更新群成员角色失败')
      }
    },

    async transferGroupOwner(groupId, transfereeUserId) {
      const userStore = useUserStore()
      const toastStore = useToastStore()
      try {
        const res = await request.post('transferGroupOwner', {
          groupId,
          transfereeUserId,
          ownerUserId: userStore.userInfo.userId,
        })
        console.log(res)
        if (res.code !== 0) {
          toastStore.error(res.msg)
        } else {
          toastStore.success('转让群主成功')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('转让群主失败')
      }
    },
    async updateGroup(group) {
      const toastStore = useToastStore()
      const userStore = useUserStore()
      if (group.groupId in this.groups) {
        const groupMember = this.groupMembers[group.groupId]?.find(
          (member) => member.memberUserId === userStore.userInfo.userId,
        )
        if (groupMember.role === 0) {
          toastStore.error('只有群主有权限更新群信息')
          return
        }
      }
      try {
        const res = await request.post('updateGroup', {
          group,
          updaterUserId: userStore.userInfo.userId,
        })
        console.log(res)
        if (res.code !== 0) {
          toastStore.error(res.msg)
        } else {
          toastStore.success('更新成功')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('更新群信息失败')
      }
    },
    async updateGroupMembers(groupMembers) {
      const toastStore = useToastStore()
      try {
        const res = await request.post('updateGroupMembers', { groupMembers })
        console.log(res)
        if (res.code !== 0) {
          toastStore.error(res.msg)
        } else {
          toastStore.success('更新成功')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('更新群成员信息失败')
      }
    },

    async disbandGroup(groupId) {
      const toastStore = useToastStore()
      const userStore = useUserStore()
      try {
        const res = await request.post('disbandGroup', {
          groupId,
          ownerUserId: userStore.userInfo.userId,
        })
        console.log(res)
        if (res.code !== 0) {
          toastStore.error(res.msg)
        } else {
          toastStore.success('解散群聊成功')
        }
      } catch (e) {
        console.log(e)
        toastStore.error('解散群聊失败')
      }
    },

    // 群聊全员禁言
    async setGroupSilenceAll(group) {
      const toastStore = useToastStore()
      const userStore = useUserStore()
      try {
        const res = await request.post('setGroupSilenceAll', {
          group,
          updaterUserId: userStore.userInfo.userId,
        })
        console.log('setGroupSilenceAll', res)
        if (res.code === 0) {
          toastStore.success('设置群全体禁言成功')
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('设置群全体禁言失败')
      }
    },

    // 群聊取消全员禁言
    async setGroupUnSilenceAll(group) {
      const toastStore = useToastStore()
      const userStore = useUserStore()
      try {
        const res = await request.post('setGroupUnSilenceAll', {
          group,
          updaterUserId: userStore.userInfo.userId,
        })
        console.log('setGroupUnSilenceAll', res)
        if (res.code === 0) {
          toastStore.success('取消群全体禁言成功')
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('取消群全体禁言失败')
      }
    },

    async addGroupMembers({ groupId, userIds }) {
      const toastStore = useToastStore()
      const userStore = useUserStore()
      console.log('addGroupMembers', userIds, groupId)
      try {
        const res = await request.post('joinGroup', {
          groupId,
          inviteeUserIds: userIds,
          inviterUserId: userStore.userInfo.userId,
        })
        if (res.code === 0) {
          toastStore.success('添加群成员成功')
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('添加群成员失败')
      }
    },

    async removeGroupMembers({ groupId, userIds }) {
      const toastStore = useToastStore()
      try {
        const res = await request.post('quitGroup', {
          groupId,
          userIds,
        })
        if (res.code === 0) {
          toastStore.success('移除群成员成功')
        } else {
          toastStore.error(res.msg)
        }
      } catch (e) {
        console.log(e)
        toastStore.error('移除群成员失败')
      }
    },

    // ws 处理
    groupMembersJoin({ group, groupMembers }) {
      this.groups[group.groupId] = group
      this.groupMembers[group.groupId].push(...groupMembers)
    },
    groupMembersQuit({ groupId, memberUserIds }) {
      this.groupMembers[groupId] = this.groupMembers[groupId].filter(
        (member) => !memberUserIds.some((id) => String(member.memberUserId) === String(id)),
      )
    },
    groupDisband({ groupId }) {
      this.groups[groupId].status = 1
    },
    groupUpdate(group) {
      this.groups[group.groupId] = { ...this.groups[group.groupId], ...group }
    },
    groupMembersUpdate(groupMembers) {
      groupMembers.forEach((groupMember) => {
        const memberList = this.groupMembers[groupMember.groupId]
        if (!memberList) return // 该群不存在，跳过

        const index = memberList.findIndex(
          (member) => member.memberUserId === groupMember.memberUserId,
        )

        if (index !== -1) {
          const updatedMember = {
            ...memberList[index],
            ...groupMember,
          }
          memberList.splice(index, 1, updatedMember)
        }
      })
    },
  },
})
