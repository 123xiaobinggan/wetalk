import axios from 'axios'
import { useUserStore } from '../stores/userStore.js'
import { useToastStore } from '../stores/toastStore.js'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 20000,
  maxRedirects: 0,
})

// ====== 请求拦截器：智能处理 Headers ======
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token || localStorage.getItem('token')

    // 1. 添加 Authorization（始终添加，除非自定义 headers 中明确禁止）
    if (token && config.headers.Authorization !== false) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 2. 判断是否已有自定义 Content-Type
    const hasCustomContentType = config.headers['Content-Type']
    const isFormData =
      config.data instanceof FormData ||
      Object.prototype.toString.call(config.data) === '[object FormData]'
    if (hasCustomContentType) {
      // 用户已自定义 Content-Type，直接使用
      // 例如：'multipart/form-data'、'application/x-www-form-urlencoded' 等
    } else if (isFormData) {
      // 3. FormData：删除 Content-Type，让浏览器自动设置 multipart/form-data + boundary
      delete config.headers['Content-Type']
    } else {
      // 4. 默认：设置为 application/json
      config.headers['Content-Type'] = 'application/json'
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// ====== 响应拦截器：统一处理错误 ======
request.interceptors.response.use(
  (response) => {
    // console.log('response', response)
    return response.data
  },
  (error) => {
    const toastStore = useToastStore()

    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 401:
          toastStore.error('登录已过期，请重新登录')
          break
        case 403:
          toastStore.error('无权限访问')
          break
        case 404:
          toastStore.error('请求资源不存在')
          break
        case 500:
          toastStore.error('服务器错误')
          break
        default:
          toastStore.error(data?.msg || '请求失败')
      }
    } else if (error.request) {
      toastStore.error('网络连接失败')
    } else {
      toastStore.error(error.message)
    }
    console.log('error', error)
    return Promise.reject(error)
  },
)

export default request
