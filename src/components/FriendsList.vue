<template>
  <div class="friend-list">
    <!-- 搜索 -->
    <div class="search-bar">
      <div class="search-wrap">
        <span class="search-icon">🔍</span>
        <input
          class="search-input"
          v-model="searchKeyword"
          placeholder="输入"
          @input="onSearchInput"
          @keydown.enter="onSearchConfirm"
        />
        <button v-if="searchKeyword" class="clear-btn" type="button" @click="clearSearch">×</button>
      </div>
    </div>

    <!-- 内容滚动 -->
    <div class="content" ref="contentRef">
      <!-- 好友申请 -->
      <div class="section">
        <div class="section-header" @click="toggle('friendRequests')">
          <span class="chev" :class="{ open: open.friendRequests }">›</span>
          <span class="section-title">新的朋友</span>
          <span v-if="Object.keys(filteredFriendRequests).length" class="count">{{
            Object.keys(filteredFriendRequests).length
          }}</span>
        </div>

        <div class="collapse" :style="collapseStyle.friendRequests">
          <div class="section-body" ref="bodyFriendRequests">
            <div
              v-for="item in filteredFriendRequests"
              :key="item.friendRequestId"
              class="card"
              :class="{
                selected:
                  selected.type === 'friendRequests' && selected.id === item.friendRequestId,
              }"
              @click="clickFriendRequest(item)"
            >
              <img class="avatar" :src="item.avatar" alt="" />
              <div class="info">
                <div class="title">{{ item.remark ?? '' }}</div>
                <div class="sub">{{ item.requestMsg }}</div>
              </div>

              <div class="action-area">
                <span v-if="item.status == 2" class="status-text"> 已同意 </span>

                <span v-else class="status-text"> 等待验证 </span>
              </div>
            </div>

            <div v-if="filteredFriendRequests.length === 0" class="empty">暂无记录</div>
          </div>
        </div>
      </div>

      <!-- 群聊 -->
      <div class="section">
        <div class="section-header" @click="toggle('groupChat')">
          <span class="chev" :class="{ open: open.groupChat }">›</span>
          <span class="section-title">群聊</span>
          <span v-if="Object.keys(filteredgroups).length" class="count">{{
            Object.keys(filteredgroups).length
          }}</span>
        </div>

        <div class="collapse" :style="collapseStyle.groupChat">
          <div class="section-body" ref="bodyGroupChat">
            <div
              v-for="g in filteredgroups"
              :key="g.id"
              class="card"
              :class="{ selected: selected.type === 'group' && selected.id === g.groupId }"
              @click="clickGroup(g)"
            >
              <img class="avatar" :src="g.groupAvatar" alt="" />
              <div class="info">
                <div class="title">{{ g?.groupRemark || g?.groupName }}</div>
              </div>
            </div>

            <div v-if="filteredgroups.length === 0" class="empty">暂无群聊</div>
          </div>
        </div>
      </div>

      <!-- 联系人 -->
      <div class="section">
        <div class="section-header" @click="toggle('friends')">
          <span class="chev" :class="{ open: open.friends }">›</span>
          <span class="section-title">联系人</span>
          <span v-if="Object.keys(filteredFriends).length" class="count">{{
            Object.keys(filteredFriends).length
          }}</span>
        </div>

        <div class="collapse" :style="collapseStyle.friends">
          <div class="section-body" ref="bodyFriends">
            <div v-for="group in groupedFriends" :key="group.letter" class="group">
              <div class="group-title">{{ group.letter }}</div>

              <div
                v-for="u in group.list"
                :key="u.friendUserId"
                class="card"
                :class="{ selected: selected.type === 'contact' && selected.id === u.friendUserId }"
                @click="clickContact(u)"
              >
                <img class="avatar" :src="u.avatar" alt="" />
                <div class="info">
                  <div class="title">{{ u?.remark || u.username }}</div>
                </div>
              </div>
            </div>

            <div v-if="filteredFriends.length === 0" class="empty">暂无联系人</div>
          </div>
        </div>
      </div>

      <div style="height: 12px"></div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import {
  computed,
  ref,
  reactive,
  nextTick,
  onMounted,
  watch,
  onActivated,
  onDeactivated,
} from 'vue'

defineOptions({ name: 'FriendList' })

const props = defineProps({
  friendRequests: { type: Object, default: () => [] },
  groups: { type: Object, default: () => [] },
  friends: { type: Object, default: () => [] },
})

const emit = defineEmits(['click-friendRequest', 'click-group', 'click-contact'])

const route = useRoute()

const selected = reactive({
  type: '',
  id: null,
})

/** 搜索 */
const searchKeyword = ref('')
const keyword = ref('')
const searchTimer = ref(null)

const contentRef = ref(null)
// 用 sessionStorage 做“路由级记忆”，刷新页面也能保留
const scrollKey = computed(() => `scrollTop:${route.fullPath}`)

function onSearchInput() {
  clearTimeout(searchTimer.value)
  searchTimer.value = setTimeout(() => {
    keyword.value = searchKeyword.value
    measureAll()
  }, 200)
}
function onSearchConfirm() {
  keyword.value = searchKeyword.value
  measureAll()
}
function clearSearch() {
  searchKeyword.value = ''
  keyword.value = ''
  measureAll()
}

function selectItem(type, id) {
  selected.type = type
  selected.id = id
}

const kw = computed(() => (keyword.value || '').trim().toLowerCase())

/** 过滤 */
const filteredFriendRequests = computed(() => {
  console.log('filteredFriendRequests', props.friendRequests)
  if (!kw.value) return props.friendRequests

  // 将对象转为数组，过滤，再转回对象
  return Object.fromEntries(
    Object.entries(props.friendRequests).filter(([id, request]) => {
      const a = (request.remark || '').toLowerCase()
      const b = (request.requestMsg || '').toLowerCase()
      const searchTerm = kw.value.toLowerCase()
      return a.includes(searchTerm) || b.includes(searchTerm)
    }),
  )
})

const filteredgroups = computed(() => {
  if (!kw.value) return props.groups
  return props.groups.entries().filter(([id, g]) => {
    const a = (g.name || '').toLowerCase()
    const searchTerm = kw.value.toLowerCase()
    return a.includes(searchTerm)
  })
})

const filteredFriends = computed(() => {
  if (!kw.value) return props.friends
  return props.friends.entries().filter(([id, u]) => {
    const a = (u.remark || '').toLowerCase()
    const searchTerm = kw.value.toLowerCase()
    return a.includes(searchTerm)
  })
})

/** 联系人分组 */
const groupedFriends = computed(() => {
  const map = new Map()
  filteredFriends.value.forEach((u) => {
    const remark = (u.remark || '').trim()
    let letter = '#'
    if (remark) {
      const c = remark[0].toUpperCase()
      letter = /[A-Z]/.test(c) ? c : '#'
    }
    if (!map.has(letter)) map.set(letter, [])
    map.get(letter).push(u)
  })

  const groups = Array.from(map.entries()).map(([letter, arr]) => {
    arr.sort((a, b) => (a.remark || '').localeCompare(b.remark || ''))
    return { letter, list: arr }
  })

  groups.sort((g1, g2) => {
    if (g1.letter === '#') return 1
    if (g2.letter === '#') return -1
    return g1.letter.localeCompare(g2.letter)
  })

  return groups
})

/** 折叠 */
const open = reactive({
  friendRequests: false,
  groupChat: false,
  friends: false,
})

const bodyFriendRequests = ref(null)
const bodyGroupChat = ref(null)
const bodyFriends = ref(null)

const heights = reactive({
  friendRequests: 0,
  groupChat: 0,
  friends: 0,
})

const collapseStyle = computed(() => ({
  friendRequests: { maxHeight: open.friendRequests ? `${heights.friendRequests}px` : '0px' },
  groupChat: { maxHeight: open.groupChat ? `${heights.groupChat}px` : '0px' },
  friends: { maxHeight: open.friends ? `${heights.friends}px` : '0px' },
}))

function toggle(key) {
  open[key] = !open[key]
  if (open[key]) nextTick(() => measureOne(key))
}

function measureOne(key) {
  const el =
    key === 'friendRequests'
      ? bodyFriendRequests.value
      : key === 'groupChat'
        ? bodyGroupChat.value
        : bodyFriends.value

  if (!el) return
  heights[key] = el.scrollHeight || 0
}

function measureAll() {
  nextTick(() => {
    heights.friendRequests = bodyFriendRequests.value?.scrollHeight || 0
    heights.groupChat = bodyGroupChat.value?.scrollHeight || 0
    heights.friends = bodyFriends.value?.scrollHeight || 0
  })
}

watch(
  () => [
    filteredFriendRequests.value.length,
    filteredgroups.value.length,
    filteredFriends.value.length,
  ],
  () => measureAll(),
)

onMounted(() => {
  measureAll()
  console.log('props.groups', props.groups)
})

/** emits */
function clickFriendRequest(item) {
  selectItem('friendRequests', item.friendRequestId, item)
  emit('click-friendRequest', item)
}
function clickGroup(group) {
  selectItem('group', group.groupId, group)
  emit('click-group', group)
}
function clickContact(friend) {
  selectItem('contact', friend.friendUserId, friend)
  emit('click-contact', friend)
}

function onContentScroll() {
  const el = contentRef.value
  if (!el) return
  sessionStorage.setItem(scrollKey.value, String(el.scrollTop))
}

function restoreScroll() {
  nextTick(() => {
    const el = contentRef.value
    if (!el) return
    const saved = Number(sessionStorage.getItem(scrollKey.value) || '0')
    el.scrollTop = saved
  })
}

// 组件被 KeepAlive 缓存时：切回该路由触发
onActivated(() => {
  restoreScroll()
})

// 切走时也存一次（保险）
onDeactivated(() => {
  onContentScroll()
})

// 首次进入也恢复一次（比如刷新后进来）
onMounted(() => {
  restoreScroll()
})
</script>

<style scoped>
.friend-list {
  width: calc(100% / 4);
  height: 100vh;
  box-sizing: border-box;
  padding: 14px;
  background: #f6f7f9;
  border-right: 1px solid #e9e9e9;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 搜索 */
.search-bar {
  padding-bottom: 12px;
}

.search-wrap {
  position: relative;
  height: 40px;
  background: #fff;
  border: 1px solid #e7e7e7;
  border-radius: 10px;
  display: flex;
  align-items: center;
  padding: 0 10px;
}

.search-icon {
  position: absolute;
  left: 10px;
  font-size: 14px;
  opacity: 0.65;
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 40px;
  padding-left: 26px;
  padding-right: 28px;
  font-size: 14px;
  color: #222;
  border: none;
  outline: none;
  background: transparent;
}

.clear-btn {
  position: absolute;
  right: 8px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #f0f0f0;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  border: none;
  cursor: pointer;
}

.clear-btn:hover {
  background: #e8e8e8;
}

/* 内容滚动 */
.content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
  scrollbar-gutter: stable;
}

/* 宽度 */
.content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

/* 轨道 */
.content::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 4px;
}

/* 默认：thumb 隐藏 */
.content::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
  transition: background 0.2s;
}

/* hover 时显示 thumb */
.content:hover::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

/* hover 且指向 thumb 时更深 */
.content:hover::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

/* section */
.section {
  /* background: #fff; */
  /* border: 1px solid #eeeeee; */
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 12px;
}

/* header */
.section-header {
  height: 44px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  user-select: none;
  cursor: pointer;
}

.section-header:hover {
  background: #f6f6f6;
}

.chev {
  width: 18px;
  text-align: center;
  color: #8b8b8b;
  font-size: 20px;
  line-height: 1;
  transform: rotate(0deg);
  transition: transform 0.18s ease;
}

.chev.open {
  transform: rotate(90deg);
}

.section-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}

.count {
  min-width: 22px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: #2f7cf6;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* collapse */
.collapse {
  overflow: hidden;
  transition: max-height 0.22s ease;
}

.section-body {
  padding: 6px 12px 10px;
}

/* card */
.card {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 8px;
  border-radius: 10px;
  cursor: pointer;
}

.card + .card {
  margin-top: 2px;
}

.card:hover {
  background: #ededed;
}

.card.selected {
  background: #e6e6e6;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #eee;
  flex-shrink: 0;
  object-fit: cover;
}

.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-size: 14px;
  color: #1f1f1f;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub {
  font-size: 12px;
  color: #7a7a7a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-area {
  margin-left: auto;
  flex-shrink: 0;
}

.accept-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  background: #07c160;
  color: #fff;
  cursor: pointer;
}

.status-text {
  font-size: 12px;
  color: #999;
}

/* group */
.group-title {
  margin: 10px -12px 6px;
  padding: 6px 12px;
  background: #f5f6f8;
  color: #6b7cff;
  font-size: 12px;
  font-weight: 700;
}

.empty {
  padding: 14px 8px 8px;
  color: #aaa;
  font-size: 13px;
  text-align: center;
}
</style>
