<template>
  <view class="group-detail-page">
    <GroupDetail
      :groupId="groupInfo.groupId"
      :groupAvatar="groupInfo.groupAvatar"
      :groupName="groupInfo.groupName"
      :groupRemark="groupInfo.groupRemark"
      @enter="handleEnterGroup"
    />
  </view>
</template>

<script setup>
import GroupDetail from '/src/components/GroupDetail.vue'
import { useGroupDetail } from './index.js'
defineOptions({
  name: 'GroupDetail',
})
const { groupInfo, handleEnterGroup } = useGroupDetail()
</script>

<style>
@import url('./index.css');
</style>
