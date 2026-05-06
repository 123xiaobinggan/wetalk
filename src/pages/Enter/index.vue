<template>
  <div class="enter-container">
    <div class="enter-box">
      <h2 class="title" v-if="formData.isLogin">登录</h2>
      <h2 class="title" v-else>注册</h2>
      <form @submit.prevent="handleLogin" v-if="formData.isLogin">
        <!-- Account ID 输入框 -->
        <div class="input-group">
          <label for="accountId">账号</label>
          <input
            id="accountId"
            v-model="formData.accountId"
            type="text"
            placeholder="请输入账号"
            required
          />
        </div>

        <!-- Password 输入框 -->
        <div class="input-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <!-- 验证码 输入框 -->
        <div class="input-group captcha-group">
          <label for="captcha">验证码</label>
          <div class="captcha-wrapper">
            <input
              id="captcha"
              v-model="formData.captcha"
              type="text"
              placeholder="请输入验证码"
              required
            />
            <canvas ref="captchaCanvas" class="captcha-canvas" width="100" height="40"></canvas>
          </div>
          <div class="captcha-refresh-text" @click="refreshCaptcha">看不清？换一张</div>
        </div>

        <!-- 登录按钮 -->
        <button type="submit" class="login-button" @click="handleEnter">登录</button>
      </form>

      <form @submit.prevent="handleEnter" v-else>
        <!-- Account ID 输入框 -->
        <div class="input-group">
          <label for="accountId">账号</label>
          <input
            id="accountId"
            v-model="formData.accountId"
            type="text"
            placeholder="请输入账号"
            required
          />
        </div>

        <!-- username 输入框 -->
        <div class="input-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>

        <!-- Password 输入框 -->
        <div class="input-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <!-- sex 选择框 -->
        <div class="input-group">
          <label for="sex">性别</label>
          <select id="sex" v-model="formData.sex">
            <option value="" disabled selected>请输入性别</option>
            <option value="1">男</option>
            <option value="0">女</option>
          </select>
        </div>

        <!-- 地区选择 -->
        <div class="input-group">
          <label for="region">地区</label>
          <div class="region-input-wrapper">
            <input
              id="region"
              :value="inputText"
              readonly
              placeholder="请选择地区"
              @click="openPopup('province')"
            />
            <!-- 弹窗 -->
            <div v-show="showPopup" ref="popupContainer" class="region-popup">
              <!-- 省份选择 -->
              <ul v-if="currentLevel === 'province'">
                <li
                  v-for="province in provinces"
                  :key="province.code"
                  @click="selectProvince(province.name)"
                >
                  {{ province.name }}
                </li>
              </ul>

              <!-- 城市选择 -->
              <ul v-else-if="currentLevel === 'city'">
                <li v-for="city in cities" :key="city.code" @click="selectCity(city.name)">
                  {{ city.name }}
                </li>
              </ul>

              <!-- 区域选择 -->
              <ul v-else-if="currentLevel === 'area'">
                <li
                  v-for="area in areas"
                  :key="area.code"
                  @click="selectArea(area.name, area.code)"
                >
                  {{ area.name }}
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 验证码 输入框 -->
        <div class="input-group captcha-group">
          <label for="captcha">验证码</label>
          <div class="captcha-wrapper">
            <input
              id="captcha"
              v-model="formData.captcha"
              type="text"
              placeholder="请输入验证码"
              required
            />
            <canvas ref="captchaCanvas" class="captcha-canvas" width="100" height="40"></canvas>
          </div>
          <div class="captcha-refresh-text" @click="refreshCaptcha">看不清？换一张</div>
        </div>

        <button type="button" class="login-button" @click="handleEnter">注册</button>
      </form>

      <!-- 去注册链接 -->
      <div class="register-link" v-if="formData.isLogin">
        <span>还没有账号？</span>
        <a href="#" @click="changeEnterStatus">去注册</a>
      </div>

      <!-- 去登录链接 -->
      <div class="register-link" v-else>
        <span>已有账号？</span>
        <a href="#" @click="changeEnterStatus">去登录</a>
      </div>
    </div>
  </div>
</template>



<script setup lang="js">
import { ref } from 'vue'
defineOptions({
  name: 'EnterPage',
})
import useEnter from './index.js'
const {
  formData,
  provinces,
  cities,
  areas,
  showPopup,
  currentLevel,
  inputText,
  selectProvince,
  selectCity,
  selectArea,
  openPopup,
  closePopup,
  handleEnter,
  refreshCaptcha,
  changeEnterStatus, // 切换登录/注册状态,
  captchaCanvas,
  popupContainer,
} = useEnter()
</script>

<style scoped>
@import url('./index.css');
</style>
