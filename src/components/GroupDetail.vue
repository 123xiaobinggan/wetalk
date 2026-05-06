<template>
  <view class="group-detail-card">
    <img class="group-avatar" :src="groupAvatar" />

    <view class="group-remark" v-if="groupRemark">
      {{ groupRemark || groupName || '群聊' }}
    </view>

    <view class="group-name"> 群名称: {{ groupName || '未命名群聊' }} </view>

    <button class="enter-btn" @click="enterGroup">进入群聊</button>
  </view>
</template>

<script setup>
import { onMounted } from 'vue'
defineOptions({
  name: 'GroupDetail',
})
const props = defineProps({
  groupId: {
    type: [String, Number],
    default: '',
  },
  groupAvatar: {
    type: String,
    default: '',
  },
  groupRemark: {
    type: String,
    default: '',
  },
  groupName: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['enter'])

onMounted(() => {
  console.log('GroupDetail', props)
})

const enterGroup = () => {
  emit('enter')
}
</script>

<style scoped>
.group-detail-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.group-avatar {
  width: 100px;
  height: 100px;
  border-radius: 24px;
  background-color: #f2f2f2;
  margin-bottom: 16px;
}

.group-remark {
  max-width: 560rpx;
  font-size: 38rpx;
  font-weight: 600;
  color: #222;
  text-align: center;
  margin-bottom: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-name {
  max-width: 560rpx;
  font-size: 28rpx;
  color: #777;
  text-align: center;
  margin-bottom: 32px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enter-btn {
  width: 125px;
  height: 40px;
  line-height: 35px;
  border-radius: 16px;
  border: none;
  background-color: #07c160;
  color: #fff;
  font-size: 16px;
}

.enter-btn:hover {
  background-color: #07b558;
}

.enter-btn::after {
  border: none;
}
</style>
