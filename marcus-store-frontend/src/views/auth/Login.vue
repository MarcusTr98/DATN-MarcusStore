<template>
  <div class="auth-page">
    <div class="left-panel" @mousemove="handleParallax" @mouseleave="resetParallax">
      <div class="deco-circle circle-1" ref="circle1"></div>
      <div class="deco-circle circle-2" ref="circle2"></div>
      <div class="content">
        <router-link to="/" class="brand brand-logo-link">
          <div class="logo-icon-box">
            <i class="fas fa-mobile-alt"></i>
          </div>
          <div>
            <h1>MarcusStore</h1>
            <p>Chào mừng bạn đến với MarcusStore!</p>
          </div>
        </router-link>
        <div class="features">
          <div class="feature-item">
            <div class="icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2 4 5v6c0 5.25 3.5 9.74 8 11 4.5-1.26 8-5.75 8-11V5l-8-3z"/>
              </svg>
            </div>
            <span>Bảo mật tuyệt đối - Tin cậy mọi giao dịch</span>
          </div>
          <div class="feature-item">
            <div class="icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="9"/>
                <path d="M12 7v5l3 3"/>
              </svg>
            </div>
            <span>Giao hàng siêu tốc - Toàn quốc trong 24h</span>
          </div>
          <div class="feature-item">
            <div class="icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="white" stroke="none">
                <path d="M12 21s-7.5-4.6-10-9.2C.4 8.8 2 5 6 5c2 0 3.5 1.2 4.5 2.8C11.5 6.2 13 5 15 5c4 0 5.6 3.8 4 6.8-2.5 4.6-10 9.2-10 9.2z"/>
              </svg>
            </div>
            <span>Nơi khách hàng gửi trọn niềm tin</span>
          </div>
          <div class="feature-item">
            <div class="icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4 2h3a2 2 0 0 1 2 1.7c.1 1 .3 2 .7 2.9a2 2 0 0 1-.4 2.1L8.1 10a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.4 1.9.6 2.9.7a2 2 0 0 1 1.6 2z"/>
              </svg>
            </div>
            <span>Hỗ trợ 24/7 - Luôn sẵn sàng phục vụ</span>
          </div>
          <div class="feature-item">
            <div class="icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 2 21 6l-4 4"/>
                <path d="M3 12v-2a4 4 0 0 1 4-4h14"/>
                <path d="M7 22 3 18l4-4"/>
                <path d="M21 12v2a4 4 0 0 1-4 4H3"/>
              </svg>
            </div>
            <span>Đổi trả dễ dàng - Cam kết hài lòng</span>
          </div>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="auth-card" :class="{ shake: isShaking }">
        <div class="tabs" ref="tabsRef">
          <div class="tab-indicator" :style="indicatorStyle"></div>
          <button
            class="active"
            ref="tabActive"
            @mouseenter="moveIndicator"
            @mouseleave="resetIndicator"
          >Đăng nhập</button>
          <router-link
            to="/auth/register"
            class="tab-link"
            @mouseenter="moveIndicator"
            @mouseleave="resetIndicator"
          >Đăng ký</router-link>
        </div>

        <div class="title">
          <h2>Chào mừng trở lại!</h2>
          <p>Đăng nhập để tiếp tục mua sắm nhé</p>
        </div>

        <div class="social-login">
          <button class="social-btn social-gg" @click="loginGoogle">
            <span class="social-icon">
              <svg width="20" height="20" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.99.66-2.25 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.85A10.98 10.98 0 0 0 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.05H2.18a11 11 0 0 0 0 9.9l3.66-2.85z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.05l3.66 2.85C6.71 7.3 9.14 5.38 12 5.38z"/>
              </svg>
            </span>
            Google
          </button>
        </div>

        <div class="divider"><span>hoặc đăng nhập bằng tài khoản</span></div>
        <div v-if="errorMessage" class="error-box">
          {{ errorMessage }}
        </div>
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label>TÊN ĐĂNG NHẬP</label>
            <input type="text" v-model="loginForm.username" placeholder="Nhập tên đăng nhập của bạn" />
          </div>

          <div class="form-group">
            <div class="password-header">
              <label>MẬT KHẨU</label>
              <router-link to="/auth/forgot-password">QUÊN MẬT KHẨU?</router-link>
            </div>
            <div class="password-input">
              <input :type="showLoginPassword ? 'text' : 'password'" v-model="loginForm.password"
                placeholder="Nhập mật khẩu của bạn" />
              <span class="eye" @click="showLoginPassword = !showLoginPassword">{{
                showLoginPassword ? '🙈' : '👁'
                }}</span>
            </div>
          </div>

          <div class="remember">
            <input type="checkbox" v-model="loginForm.rememberMe" />
            <span>Nhớ tài khoản</span>
          </div>

          <button type="submit" class="main-btn" @click="createRipple">Đăng nhập</button>
        </form>

        <div class="bottom-link">
          <router-link to="/" class="back-home-btn">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5"/>
              <path d="M12 19 5 12l7-7"/>
            </svg>
            Về trang chủ
          </router-link>
          <span class="bottom-link-divider">|</span>
          <span class="bottom-link-text">Chưa có tài khoản? <router-link to="/auth/register">Đăng ký ngay</router-link></span>
        </div>
      </div>
    </div>
  </div>
  <BaseModal :visible="modal.visible" :type="modal.type" :title="modal.title" :message="modal.message"
    @close="modal.visible = false" />
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi } from '@/api/authApi'
import BaseModal from '@/components/BaseModal.vue'
import '@/assets/css/Login.css'
const router = useRouter()

const showLoginPassword = ref(false)
const errorMessage = ref('')
const loading = ref(false)

const loginGoogle = () => {
  window.location.href =
    "http://localhost:8080/oauth2/authorization/google";
}

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
})
const modal = reactive({
  visible: false,
  type: 'error',
  title: '',
  message: '',
})
const showModal = (type, title, message) => {
  modal.type = type
  modal.title = title
  modal.message = message
  modal.visible = true
}

const OAUTH_ERROR_MESSAGES = {
  unsupported_provider: 'Nhà cung cấp đăng nhập không được hỗ trợ.',
  no_email: 'Tài khoản mạng xã hội của bạn không cung cấp email. Vui lòng dùng phương thức khác.',
  social_not_allowed: 'Tài khoản này không được phép đăng nhập bằng mạng xã hội.',
  account_disabled: 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.',
  email_not_verified: 'Email Google của bạn chưa được xác thực. Vui lòng xác thực email trước khi đăng nhập.',
}

// ---- Parallax ----
const circle1 = ref(null)
const circle2 = ref(null)

const handleParallax = (e) => {
  const { innerWidth, innerHeight } = window
  const x = (e.clientX / innerWidth - 0.5) * 20
  const y = (e.clientY / innerHeight - 0.5) * 20
  if (circle1.value) circle1.value.style.transform = `translate(${x}px, ${y}px)`
  if (circle2.value) circle2.value.style.transform = `translate(${-x}px, ${-y}px)`
}

const resetParallax = () => {
  if (circle1.value) circle1.value.style.transform = 'translate(0, 0)'
  if (circle2.value) circle2.value.style.transform = 'translate(0, 0)'
}

// ---- Tab indicator ----
const tabsRef = ref(null)
const tabActive = ref(null)
const indicatorStyle = ref({ left: '4px', width: 'calc(50% - 4px)' })

const setIndicatorToActive = () => {
  if (!tabActive.value) return
  indicatorStyle.value = {
    left: tabActive.value.offsetLeft + 'px',
    width: tabActive.value.offsetWidth + 'px',
  }
}

const moveIndicator = (e) => {
  const el = e.currentTarget
  indicatorStyle.value = {
    left: el.offsetLeft + 'px',
    width: el.offsetWidth + 'px',
  }
}

const resetIndicator = () => {
  setIndicatorToActive()
}

// ---- Ripple ----
const createRipple = (e) => {
  const button = e.currentTarget
  const circle = document.createElement('span')
  const diameter = Math.max(button.clientWidth, button.clientHeight)
  const rect = button.getBoundingClientRect()

  circle.style.width = circle.style.height = `${diameter}px`
  circle.style.left = `${e.clientX - rect.left - diameter / 2}px`
  circle.style.top = `${e.clientY - rect.top - diameter / 2}px`
  circle.classList.add('ripple')

  const oldRipple = button.querySelector('.ripple')
  if (oldRipple) oldRipple.remove()

  button.appendChild(circle)
  setTimeout(() => circle.remove(), 600)
}

// ---- Shake khi lỗi ----
const isShaking = ref(false)
const triggerShake = () => {
  isShaking.value = true
  setTimeout(() => (isShaking.value = false), 450)
}

onMounted(() => {
  nextTick(() => setIndicatorToActive())

  const params = new URLSearchParams(window.location.search)
  const error = params.get('error')
  const notice = params.get('notice')

  if (error) {
    const message = OAUTH_ERROR_MESSAGES[error] || 'Đăng nhập bằng mạng xã hội thất bại. Vui lòng thử lại.'
    showModal('error', 'Đăng nhập thất bại', message)
    triggerShake()
  } else if (notice === 'account_linked') {
    showModal(
      'success',
      'Đã liên kết tài khoản',
      'Email này đã có tài khoản trên hệ thống. Chúng tôi đã tự động liên kết đăng nhập mạng xã hội vào tài khoản đó.'
    )
  }

  if (error || notice) {
    const url = new URL(window.location.href)
    url.searchParams.delete('error')
    url.searchParams.delete('notice')
    window.history.replaceState({}, '', url.toString())
  }
})

const handleLogin = async () => {
  errorMessage.value = ''

  if (!loginForm.username.trim()) {
    triggerShake()
    showModal('error', 'Thiếu thông tin', 'Vui lòng nhập tên đăng nhập.')
    return
  }

  if (!loginForm.password.trim()) {
    triggerShake()
    showModal('error', 'Thiếu thông tin', 'Vui lòng nhập mật khẩu.')
    return
  }

  try {
    loading.value = true

    const response = await loginApi({
      username: loginForm.username,
      password: loginForm.password,
    })

    const userData = response.data.data

    localStorage.setItem('ACCESS_TOKEN', userData.token)
    localStorage.setItem('USERNAME', userData.username)
    localStorage.setItem('USER_ROLE', JSON.stringify(userData.roles))
    localStorage.setItem('USER_PERMISSIONS', JSON.stringify(userData.permissions || []))

    window.dispatchEvent(new Event('auth-changed'))

    if (userData.roles.includes('ROLE_ADMIN')) {
      router.push('/admin/dashboard')
    } else if (userData.roles.includes('ROLE_STAFF')) {
      router.push('/admin/dashboard')
    } else {
      router.push('/')
    }
  } catch (error) {
    console.error(error)
    triggerShake()

    const status = error?.response?.status
    const message = error?.response?.data?.message

    if (status === 400) {
      showModal('error', 'Thông tin không hợp lệ', message || 'Tên đăng nhập hoặc mật khẩu không đúng định dạng.')
    } else if (status === 401) {
      showModal('error', 'Đăng nhập thất bại', message || 'Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.')
    } else if (status === 403) {
      showModal('error', 'Tài khoản bị khóa', message || 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.')
    } else if (status === 500) {
      showModal('error', 'Lỗi hệ thống', message || 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.')
    } else {
      showModal('error', 'Mất kết nối', message || 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.brand-logo-link {
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.logo-icon-box {
  width: 48px;
  height: 48px;
  background: #fff;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}

.logo-icon-box i {
  color: #d70018;
  font-size: 22px;
}

.brand-logo-link:hover .logo-icon-box {
  transform: scale(1.05);
}

.bottom-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.bottom-link-divider {
  color: #cbd5e1;
}

.back-home-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #64748b;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s ease;
}

.back-home-btn:hover {
  color: #d70018;
  text-decoration: underline;
}

.back-home-btn svg {
  transition: transform 0.2s ease;
}

.back-home-btn:hover svg {
  transform: translateX(-3px);
}
</style>