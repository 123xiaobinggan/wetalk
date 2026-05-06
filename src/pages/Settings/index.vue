<template>
  <div class="wrap">
    <h2 class="title">设置中心</h2>

    <div class="card">
      <!-- 头像 -->
      <div class="row">
        <div class="label">头像</div>
        <div class="value avatar">
          <img v-if="form.avatar" :src="form.avatar" />
          <div v-else class="placeholder">无</div>
          <button @click="onPickAvatar">更换</button>
        </div>
      </div>
      <!-- 账户 -->
      <div class="row">
        <div class="label">账号</div>
        <input class="input" v-model="form.accountId" placeholder="账号ID/邮箱等" />
      </div>
      <!-- 用户名 输入框 -->
      <div class="row">
        <div class="label">用户名</div>
        <input class="input" v-model="form.username" placeholder="请输入用户名" />
      </div>

      <!-- 原密码 -->
      <div class="row">
        <div class="label">原密码</div>
        <input class="input" v-model="form.password" placeholder="请输入原密码" />
      </div>
      <!-- 新密码 -->
      <div class="row">
        <div class="label">新密码</div>
        <input class="input" v-model="form.newPassword" placeholder="请输入新密码" />
      </div>
      <!-- 确认密码 -->
      <div class="row">
        <div class="label">确认密码</div>
        <input class="input" v-model="form.confirmPassword" placeholder="请输入确认密码" />
      </div>

      <!-- 性别选择框 -->
      <div class="row">
        <div class="label">性别</div>
        <select class="input" v-model="sexUI">
          <option :value="-1" disabled>请选择性别</option>
          <option :value="1">男</option>
          <option :value="0">女</option>
        </select>
      </div>

      <!-- 地区选择 -->
      <div class="row">
        <div for="region" class="label">地区</div>
        <div class="region-input-wrapper">
          <input
            id="region"
            :value="inputText"
            class="input"
            readonly
            placeholder="请选择地区"
            @click="openPopup('province')"
          />
          <!-- 弹窗 -->
          <div v-show="showPopup" ref="popupContainer" class="region-popup" @click.stop>
            <!-- 省份选择 -->
            <ul v-if="currentLevel === 'province'">
              <li
                v-for="province in provinces"
                :key="province.code"
                @click="selectProvince(province)"
              >
                {{ province.name }}
              </li>
            </ul>

            <!-- 城市选择 -->
            <ul v-else-if="currentLevel === 'city'">
              <li v-for="city in cities" :key="city.code" @click="selectCity(city)">
                {{ city.name }}
              </li>
            </ul>

            <!-- 区域选择 -->
            <ul v-else-if="currentLevel === 'area'">
              <li v-for="area in areas" :key="area.code" @click="selectArea(area)">
                {{ area.name }}
              </li>
            </ul>
          </div>
        </div>
      </div>
      <!-- 手机号 -->
      <div class="row">
        <div class="label">手机号</div>
        <input class="input" v-model="form.phone" placeholder="手机号" />
      </div>

      <!-- 个性签名 -->
      <div class="row">
        <div class="label">个性签名</div>
        <textarea
          class="input"
          rows="2"
          v-model="form.personalSignature"
          placeholder="写点什么...(15字以内)"
        />
      </div>
      <!-- 来电铃声 -->
      <div class="row">
        <div class="label">来电铃声</div>
        <div class="value ringtone">
          <span class="path">{{ form.ringtone || '未设置' }}</span>
          <button @click="onPickRingtone">选择</button>
          <button :disabled="!form.ringtone" @click="onPreviewRingtone">试听</button>
        </div>
      </div>
    </div>

    <div class="footer">
      <button class="btn logout-btn" @click="logout">退出登录</button>
      <button class="btn save-btn" @click="onSave">保存</button>
    </div>

    <audio ref="audioRef" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch, nextTick, onBeforeUnmount, computed } from 'vue'
import regionData from '../../assets/data/regions.json'
import request from '/src/api/request.js'
import { confirm } from '/src/utils/confirm.js'
import { useUserStore } from '../../stores/userStore.js'
import { useToastStore } from '../../stores/toastStore.js'
import imageCompression from 'browser-image-compression'

defineOptions({ name: 'Settings' })

const userStore = useUserStore()
const toastStore = useToastStore()

const form = reactive({
  avatar: '',
  username: '',
  sex: -1, // -1 未选；1男 0女
  password: '',
  newPassword: '',
  confirmPassword: '',
  areaName: '',
  areaCode: '',
  phone: '',
  accountId: '',
  personalSignature: '',
  ringtone: '',
})

/* 地区选择相关 */
const provinces = ref(regionData)
const cities = ref([])
const areas = ref([])

const avatarFile = ref(null) // File | null
const avatarPreviewUrl = ref('') // string

const showPopup = ref(false)
const currentLevel = ref('province') // province | city | area
const popupContainer = ref(null)

const selectedProvince = ref(null)
const selectedCity = ref(null)
const selectedArea = ref(null)

/** 展示用文本 */
const inputText = ref('请选择地区')

/* 限制签名长度 */
watch(
  () => form.personalSignature,
  () => {
    if (form.personalSignature && form.personalSignature.length > 15) {
      form.personalSignature = form.personalSignature.slice(0, 15)
    }
  },
)

const sexUI = computed({
  get() {
    return form.sex
  },
  set(v) {
    form.sex = v
  },
})

const audioRef = ref(null)
const saving = ref(false)
/* 关闭弹窗 */
function closePopup() {
  showPopup.value = false
}

/* 点击外部关闭 */
function onDocClick(e) {
  if (!showPopup.value) return
  const el = popupContainer.value
  // 弹窗容器外点击关闭
  if (el && !el.contains(e.target)) {
    closePopup()
  }
}

onMounted(async () => {
  await userStore.init()
  const u = userStore.userInfo || {}

  Object.assign(form, u)
  console.log('u, form', u, form)

  if (form.areaName) inputText.value = form.areaName
  document.addEventListener('click', onDocClick, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick, true)
})

/* 打开地区弹窗 */
async function openPopup(level = 'province') {
  currentLevel.value = level
  showPopup.value = !showPopup.value

  if (showPopup.value) {
    await nextTick()
    if (popupContainer.value) popupContainer.value.scrollTop = 0
  }
}

/* 根据选择实时生成显示文本 */
function updateRegionText() {
  const p = selectedProvince.value?.name
  const c = selectedCity.value?.name
  const a = selectedArea.value?.name

  const parts = [p, c, a].filter(Boolean)
  inputText.value = parts.length ? parts.join('/') : '请选择地区'
}

/* 选择省 */
function selectProvince(province) {
  selectedProvince.value = province
  selectedCity.value = null
  selectedArea.value = null

  cities.value = province?.children || []
  areas.value = []

  currentLevel.value = 'city'
  updateRegionText()

  nextTick(() => {
    if (popupContainer.value) popupContainer.value.scrollTop = 0
  })
}

/* 选择市 */
function selectCity(city) {
  selectedCity.value = city
  selectedArea.value = null

  areas.value = city?.children || []
  currentLevel.value = 'area'
  updateRegionText()

  nextTick(() => {
    if (popupContainer.value) popupContainer.value.scrollTop = 0
  })
}

/* 选择区 */
function selectArea(area) {
  selectedArea.value = area
  updateRegionText()

  form.areaName = inputText.value
  form.areaCode = area?.code || ''

  closePopup()
}

/* 保存 */
async function onSave() {
  if (saving.value === true) return
  saving.value = true
  if (form.newPassword !== form.confirmPassword) {
    toastStore.error('密码不一致')
    return
  }
  if (form.accountId === '') {
    toastStore.error('账号不能为空')
    return
  }
  if (form.username === '') {
    toastStore.error('用户名不能为空')
    return
  }
  if (form.sex === '') {
    toastStore.error('性别不能为空')
    return
  }
  if (form.areaCode === '') {
    toastStore.error('地区不能为空')
    return
  }

  console.log('form', form, avatarFile.value)

  try {
    if (avatarFile.value) {
      const formData = new FormData()
      formData.append('avatars', avatarFile.value)
      formData.append('names', 'user:' + userStore.userInfo.userId)
      const res1 = await request.post('uploadAvatars', formData)
      console.log('上传结果:', res1)
      if (res1.code === 0) {
        form.avatar = res1.data.urls[0]
      } else {
        toastStore.error(res1.msg || '上传失败')
        return
      }
    }

    const payload = {
      userId: userStore.userInfo.userId,
      accountId: form.accountId,
      oldAccountId: userStore.userInfo.accountId,
      username: form.username,
      sex: form.sex,
      avatar: form.avatar,
      phone: form.phone,
      areaCode: form.areaCode,
      areaName: form.areaName,
      personalSignature: form.personalSignature,
      ringtone: form.ringtone,
    }

    if (form.newPassword) {
      payload.password = form.password
      payload.newPassword = form.newPassword
    }

    const res2 = await request.post('updateUserInfo', payload, {
      timeout: 20000,
    })

    console.log('res', res2)

    if (res2.code !== 0) {
      toastStore.error(res2.msg || '保存失败')
      return
    }

    const user = res2.data.user
    Object.assign(form, user)

    await window.electronAPI.authSet({ userInfo: user })
    userStore.setUserInfo(user)

    avatarFile.value = null
    if (avatarPreviewUrl.value) {
      URL.revokeObjectURL(avatarPreviewUrl.value)
      avatarPreviewUrl.value = ''
    }
    toastStore.success('保存成功')
  } catch (e) {
    console.error(e)
    toastStore.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function onPickAvatar() {
  const result = await window.electronAPI.pickAvatar()
  if (!result) return

  let { buffer, filename } = result
  let file = new File([new Uint8Array(buffer)], filename)

  // 压缩（>2MB）
  if (file.size > 2 * 1024 * 1024) {
    try {
      file = await imageCompression(file, {
        maxSizeMB: 2,
        maxWidthOrHeight: 1024, // 头像没必要 1920，1024 更够用
        useWebWorker: true,
      })
      // 压缩后 file.name 可能丢失，给它补回去（可选）
      if (!file.name) file = new File([file], filename, { type: file.type })
    } catch (e) {
      console.log('图像压缩失败:', e)
      toastStore.error('图像压缩失败，请重试')
      return
    }
  }

  // 记录待上传文件
  avatarFile.value = file

  // 本地预览：立刻把界面头像变成本地的
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value)
  avatarPreviewUrl.value = URL.createObjectURL(file)

  // UI 用这个显示（不影响后端 url）
  form.avatar = avatarPreviewUrl.value
}

/* 选择铃声 */
async function onPickRingtone() {
  const saved = await window.electronAPI.pickRingtone()
  if (saved) Object.assign(form, saved)
}

/* 试听 */
function onPreviewRingtone() {
  if (!form.ringtone) return
  const src = form.ringtone.startsWith('http') ? form.ringtone : `file://${form.ringtone}`
  audioRef.value.src = src
  audioRef.value.play()
}

async function logout() {
  const ok = await confirm('确定要退出登录吗？', {
    title: '提示',
    confirmText: '退出',
    cancelText: '取消',
    danger: true,
  })
  if (!ok) {
    return
  } else {
    window.electronAPI.authClear()
  }
}
</script>

<style scoped>
.wrap {
  padding: 16px;
}

.title {
  margin: 0 0 12px;
  font-size: 18px;
  align-items: center;
  justify-content: center;
}

.card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f3f3f3;
}

.row:last-child {
  border-bottom: none;
}

.label {
  width: 84px;
  color: #666;
  font-size: 14px;
}

.input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.value {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.placeholder {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px dashed #ccc;
  display: grid;
  place-items: center;
  color: #aaa;
  font-size: 12px;
}

.ringtone .path {
  flex: 1;
  font-size: 12px;
  color: #888;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.btn {
  padding: 8px 16px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
}

.save-btn {
  background-color: #4caf50;
  /* 绿色背景 */
  color: white;
}

.save-btn:hover {
  background-color: #45a049;
  /* 悬停时颜色变浅 */
}

.logout-btn {
  background-color: #ff4d4f;
  /* 红色背景 */
  color: #fff;
  /* 白色文字 */
  margin-right: 10px;
  /* 保存按钮之间留点间距 */
}

.logout-btn:hover {
  background-color: #ff7875;
  /* 悬停时颜色变浅 */
}

button {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.region-input-wrapper {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
}

.region-input-wrapper .input {
  flex: 1;
}

.region-popup {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  max-height: 200px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #ccc;
  border-top: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.region-popup ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.region-popup li {
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.region-popup li:hover {
  background: #f0f0f0;
}
</style>
