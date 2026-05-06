<template>
  <FriendRequestDetail :friendRequest="friendRequest" @accept="handleAccept" @block="handleBlock" />
</template>

<script setup>
defineOptions({
  name: 'FriendRequestDetail',
})
import FriendRequestDetail from '/src/components/FriendRequestDetail.vue'
import useFriendRequestDetail from './index.js'

const { friendRequest, handleAccept, handleBlock } = useFriendRequestDetail()
</script>

<style scoped>
@import url('./index.css');
</style>
