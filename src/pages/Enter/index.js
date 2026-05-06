import { ref, watch, onMounted, nextTick } from 'vue'
import { useUserStore } from '../../stores/userStore.js'
import { useToastStore } from '../../stores/toastStore.js'
import request from '/src/api/request.js'
import jsonwebtoken from 'jsonwebtoken'
import regionData from '../../assets/data/regions.json'

export default function useEnter() {
  // 表单数据
  const formData = ref({
    accountId: '',
    username: '',
    password: '',
    sex: '',
    areaName: '',
    areaCode: '',
    captcha: '',
    isLogin: true,
  })

  const provinces = ref(regionData)
  const cities = ref([])
  const areas = ref([])

  const showPopup = ref(false)
  const currentLevel = ref('province')
  const popupContainer = ref(null)

  const inputText = ref('请选择省份')

  const captchaCanvas = ref(null)
  const captchaCode = ref('')
  const captchaText = ref('')

  const windowSize = ref({
    width: window.innerWidth,
    height: window.innerHeight,
  })

  onMounted(() => {
    drawCaptcha()
  })

  function openPopup(level) {
    currentLevel.value = level
    if (showPopup.value == true) {
      closePopup()
    } else {
      showPopup.value = true
      if (popupContainer.value) {
        nextTick(() => {
          popupContainer.value.scrollTop = 0
        })
      }
    }
  }

  function closePopup() {
    showPopup.value = false
  }

  function selectProvince(provinceName) {
    const selectedProvince = provinces.value.find((p) => p.name === provinceName)
    cities.value = selectedProvince ? selectedProvince.children : []
    inputText.value = provinceName
    currentLevel.value = 'city'
    nextTick(() => {
      popupContainer.value.scrollTop = 0
    })
  }

  function selectCity(cityName) {
    const selectedCity = cities.value.find((c) => c.name === cityName)
    areas.value = selectedCity ? selectedCity.children : []
    inputText.value = `${inputText.value}/${cityName}`
    currentLevel.value = 'area'
    nextTick(() => {
      popupContainer.value.scrollTop = 0
    })
  }

  function selectArea(areaName, areaCode) {
    inputText.value = `${inputText.value}/${areaName}`
    formData.value.areaName = inputText.value
    formData.value.areaCode = areaCode
    closePopup()
  }

  function generateCaptcha() {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    let result = ''
    for (let i = 0; i < 4; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return result
  }

  function drawCaptcha() {
    const canvas = captchaCanvas.value
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    const width = canvas.width
    const height = canvas.height

    // 清空画布
    ctx.clearRect(0, 0, width, height)

    // 设置背景色
    ctx.fillStyle = '#f0f0f0'
    ctx.fillRect(0, 0, width, height)

    // 设置字体样式
    ctx.font = '20px Arial'
    ctx.textBaseline = 'middle'

    // 绘制验证码文字
    const text = generateCaptcha()
    captchaText.value = text
    for (let i = 0; i < text.length; i++) {
      ctx.fillStyle = `rgb(${Math.floor(Math.random() * 150)}, ${Math.floor(Math.random() * 150)}, ${Math.floor(Math.random() * 150)})`
      ctx.fillText(text[i], 15 + i * 20, height / 2)
    }

    // 添加干扰线
    for (let i = 0; i < 5; i++) {
      ctx.strokeStyle = `rgb(${Math.floor(Math.random() * 255)}, ${Math.floor(Math.random() * 255)}, ${Math.floor(Math.random() * 255)})`
      ctx.beginPath()
      ctx.moveTo(Math.random() * width, Math.random() * height)
      ctx.lineTo(Math.random() * width, Math.random() * height)
      ctx.stroke()
    }

    // 添加噪点
    for (let i = 0; i < 30; i++) {
      ctx.fillStyle = `rgb(${Math.floor(Math.random() * 255)}, ${Math.floor(Math.random() * 255)}, ${Math.floor(Math.random() * 255)})`
      ctx.beginPath()
      ctx.arc(Math.random() * width, Math.random() * height, 1, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // 刷新验证码（模拟）
  function refreshCaptcha() {
    drawCaptcha()
  }

  // 处理登录逻辑
  async function handleEnter() {
    console.log('Enter信息:', formData.value)
    const toastStore = useToastStore()
    if (!formData.value.captcha) {
      toastStore.error('请填写验证码')
      return
    }

    if (formData.value.captcha !== captchaText.value) {
      toastStore.error('验证码错误')
      refreshCaptcha()
      return
    }

    if (!formData.value.accountId || !formData.value.password) {
      toastStore.error('请填写账号和密码')
    }

    if (formData.value.isLogin == false) {
      if (
        !formData.value.username ||
        !formData.value.sex ||
        !formData.value.areaName ||
        !formData.value.areaCode
      ) {
        toastStore.error('请填写用户信息')
        return
      }
    }

    refreshCaptcha()
    const userStore = useUserStore()
    try {
      toastStore.loading('登录中...')
      const res = await request.post('enter', formData.value)
      console.log('handleEnter: ', res)
      toastStore.hide()
      if (res.code === 0) {
        await window.electronAPI.authSet({ userInfo: res.data.user, token: res.data.token })
        userStore.setUserInfo(res.data)
        await window.electronAPI.enter_success()
      } else {
        toastStore.error(res.msg)
      }
    } catch (err) {
      console.log(err)
      toastStore.error('登录失败')
    }
  }

  // 跳转到注册页面（模拟）
  function changeEnterStatus() {
    formData.value.isLogin = !formData.value.isLogin
    if (formData.value.isLogin) {
      windowSize.width = 375
      windowSize.height = 500
    } else {
      windowSize.width = 375
      windowSize.height = 725
    }
    console.log(window.electronAPI)
    window.electronAPI?.window_status(windowSize.width, windowSize.height)
    nextTick(() => {
      refreshCaptcha()
    })
  }

  return {
    formData,
    provinces,
    cities,
    areas,
    showPopup,
    currentLevel,
    inputText,
    openPopup,
    closePopup,
    selectProvince,
    selectCity,
    selectArea,
    refreshCaptcha,
    handleEnter,
    changeEnterStatus,
    captchaCanvas,
    windowSize,
    popupContainer,
  }
}
