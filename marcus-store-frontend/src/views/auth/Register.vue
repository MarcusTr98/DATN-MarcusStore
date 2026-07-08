<template>
  <div class="auth-page">
    <div class="left-panel" @mousemove="handleParallax" @mouseleave="resetParallax">
      <div class="deco-circle circle-1" ref="circle1"></div>
      <div class="deco-circle circle-2" ref="circle2"></div>
      <div class="content">
        <div class="brand">
          <div class="menu-icon">☰</div>
          <div>
            <h1>MarcusStore</h1>
            <p>Chào mừng bạn đến với MarcusStore!</p>
          </div>
        </div>
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
          <router-link
            to="/auth/login"
            class="tab-link"
            @mouseenter="moveIndicator"
            @mouseleave="resetIndicator"
          >Đăng nhập</router-link>
          <button
            class="active"
            ref="tabActive"
            @mouseenter="moveIndicator"
            @mouseleave="resetIndicator"
          >Đăng ký</button>
        </div>

        <div class="title">
          <h2>Tạo tài khoản mới</h2>
          <p>Tham gia MarcusStore - Mua sắm thả ga!</p>
        </div>
        <div class="social-login">
          <button type="button" class="social-btn social-gg" @click="registerGoogle">
            <span class="social-icon">
              <svg width="20" height="20" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.99.66-2.25 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.85A10.98 10.98 0 0 0 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.05H2.18a11 11 0 0 0 0 9.9l3.66-2.85z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.05l3.66 2.85C6.71 7.3 9.14 5.38 12 5.38z"/>
              </svg>
            </span>
            Đăng ký bằng Google
          </button>
        </div>

        <div class="divider"><span>hoặc điền thông tin bên dưới</span></div>

        <form @submit.prevent="handleRegister">
          <div class="grid-form">
            <div class="form-group">
              <label>HỌ VÀ TÊN</label>
              <input type="text" v-model="registerForm.fullName" placeholder="Nguyễn Văn A" />
            </div>
            <div class="form-group">
              <label>TÊN ĐĂNG NHẬP</label>
              <input type="text" v-model="registerForm.username" placeholder="nguyenvana123" />
            </div>
            <div class="form-group">
              <label>EMAIL</label>
              <input type="email" v-model="registerForm.email" placeholder="email@example.com" />
            </div>
            <div class="form-group">
              <label>SỐ ĐIỆN THOẠI</label>
              <input type="text" v-model="registerForm.phone" placeholder="0901234567" />
            </div>
            <div class="form-group">
              <label>MẬT KHẨU</label>
              <div class="password-input">
                <input :type="showRegisterPassword ? 'text' : 'password'" v-model="registerForm.password"
                  placeholder="Tối thiểu 6 ký tự" />
                <span class="eye" @click="showRegisterPassword = !showRegisterPassword">{{
                  showRegisterPassword ? '🙈' : '👁'
                }}</span>
              </div>
            </div>
            <div class="form-group">
              <label>XÁC NHẬN MẬT KHẨU</label>
              <div class="password-input">
                <input :type="showRegisterConfirm ? 'text' : 'password'" v-model="registerForm.confirmPassword"
                  placeholder="Nhập lại mật khẩu" />
                <span class="eye" @click="showRegisterConfirm = !showRegisterConfirm">{{
                  showRegisterConfirm ? '🙈' : '👁'
                }}</span>
              </div>
            </div>
          </div>

          <div class="agree">
            <input type="checkbox" v-model="registerForm.agree" />
            <span>Tôi đồng ý với <a href="#" @click.prevent="showTermsModal = true">
                Điều khoản dịch vụ
              </a>

              và

              <a href="#" @click.prevent="showPrivacyModal = true">
                Chính sách bảo mật
              </a>
            </span>
          </div>

          <button class="main-btn" type="submit" :disabled="loading" @click="createRipple">
            {{ loading ? 'Đang xử lý...' : 'Đăng ký ngay' }}
          </button>
        </form>

        <div class="bottom-link">
          Đã có tài khoản? <router-link to="/auth/login">Đăng nhập</router-link>
        </div>
      </div>
    </div>
  </div>
  <BaseModal :visible="modal.visible" :type="modal.type" :title="modal.title" :message="modal.message"
    @close="modal.visible = false" />
  <TermsOfServiceModal :show="showTermsModal" @close="showTermsModal = false" />

  <PrivacyPolicyModal :show="showPrivacyModal" @close="showPrivacyModal = false" />
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import PrivacyPolicyModal from '@/components/PrivacyPolicyModal.vue'
import TermsOfServiceModal from '@/components/TermsOfServiceModal.vue'
import '@/assets/css/Register.css'
const showTermsModal = ref(false)
const showPrivacyModal = ref(false)
const router = useRouter()
const showRegisterPassword = ref(false)
const showRegisterConfirm = ref(false)
const loading = ref(false)

const registerGoogle = () => {
  window.location.href =
    "http://localhost:8080/oauth2/authorization/google";
}

const registerForm = reactive({
  fullName: '',
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  agree: false,
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
})

const handleRegister = async () => {
  try {
    if (!registerForm.fullName.trim()) {
      triggerShake()
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập họ và tên.')
      return
    }

    if (!registerForm.username.trim()) {
      triggerShake()
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập tên đăng nhập.')
      return
    }

    if (!registerForm.email.trim()) {
      triggerShake()
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập email.')
      return
    }

    if (!registerForm.phone.trim()) {
      triggerShake()
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập số điện thoại.')
      return
    }

    if (!registerForm.password.trim()) {
      triggerShake()
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập mật khẩu.')
      return
    }
    if (!registerForm.agree) {
      triggerShake()
      showModal('error', 'Chưa đồng ý', 'Vui lòng đồng ý với Điều khoản dịch vụ và Chính sách bảo mật.')
      return
    }

    loading.value = true

    const payload = {
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email,
      fullName: registerForm.fullName,
      phoneNumber: registerForm.phone,
    }

    const response = await api.post('/auth/register/request', payload)

    if (response?.data?.success === false) {
      triggerShake()
      showModal(
        'error',
        'Đăng ký thất bại',
        response.data.message || 'Không thể đăng ký tài khoản.'
      )
      return
    }

    showModal(
      'success',
      'Đăng ký thành công',
      'Mã OTP đã được gửi đến email của bạn.'
    )

    setTimeout(() => {
      router.push({
        path: '/auth/verify-otp',
        query: {
          email: registerForm.email,
        },
      })
    }, 1500)
  } catch (error) {
    triggerShake()
    const status = error?.response?.status
    const message = error?.response?.data?.message

    if (status === 400) {
      showModal(
        'error',
        'Thông tin không hợp lệ',
        message || 'Dữ liệu đăng ký không hợp lệ.'
      )
    } else if (status === 409) {
      showModal(
        'error',
        'Tài khoản đã tồn tại',
        message || 'Tên đăng nhập hoặc email đã được sử dụng.'
      )
    } else if (status === 500) {
      showModal(
        'error',
        'Lỗi hệ thống',
        message || 'Máy chủ đang gặp sự cố.'
      )
    } else {
      showModal(
        'error',
        'Đăng ký thất bại',
        message || 'Không thể kết nối đến máy chủ.'
      )
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>

</style>