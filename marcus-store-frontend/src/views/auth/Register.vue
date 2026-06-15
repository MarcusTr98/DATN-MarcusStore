<template>
  <div class="auth-page">
    <div class="left-panel">
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
            <div class="icon">🛡</div>
            <span>Bảo mật tuyệt đối - Tin cậy mọi giao dịch</span>
          </div>
          <div class="feature-item">
            <div class="icon">🕒</div>
            <span>Giao hàng siêu tốc - Toàn quốc trong 24h</span>
          </div>
          <div class="feature-item">
            <div class="icon">♡</div>
            <span>Nơi khách hàng gửi trọn niềm tin</span>
          </div>
          <div class="feature-item">
            <div class="icon">📞</div>
            <span>Hỗ trợ 24/7 - Luôn sẵn sàng phục vụ</span>
          </div>
          <div class="feature-item">
            <div class="icon">🔁</div>
            <span>Đổi trả dễ dàng - Cam kết hài lòng</span>
          </div>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="auth-card">
        <div class="tabs">
          <router-link to="/auth/login" class="tab-link">Đăng nhập</router-link>
          <button class="active">Đăng ký</button>
        </div>

        <div class="title">
          <h2>Tạo tài khoản mới</h2>
          <p>Tham gia MarcusStore - Mua sắm thả ga!</p>
        </div>

        <div class="social-login">
          <button class="social-btn social-fb">Facebook</button>
          <button class="social-btn social-gg">Google</button>
        </div>

        <div class="divider"><span>hoặc điền thông tin bên dưới</span></div>

        <form @submit.prevent="handleRegister">
          <div class="grid-form">
            <div class="form-group">
              <label>HỌ VÀ TÊN</label>
              <input type="text" v-model="registerForm.fullName" placeholder="Nguyễn Văn A"  />
            </div>
            <div class="form-group">
              <label>TÊN ĐĂNG NHẬP</label>
              <input type="text" v-model="registerForm.username" placeholder="nguyenvana123"  />
            </div>
            <div class="form-group">
              <label>EMAIL</label>
              <input type="email" v-model="registerForm.email" placeholder="email@example.com"  />
            </div>
            <div class="form-group">
              <label>SỐ ĐIỆN THOẠI</label>
              <input type="text" v-model="registerForm.phone" placeholder="0901234567"  />
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
                  placeholder="Nhập lại mật khẩu"  />
                <span class="eye" @click="showRegisterConfirm = !showRegisterConfirm">{{
                  showRegisterConfirm ? '🙈' : '👁'
                  }}</span>
              </div>
            </div>
          </div>

          <div class="agree">
            <input type="checkbox" v-model="registerForm.agree"  />
            <span>Tôi đồng ý với   <a href="#" @click.prevent="showTermsModal = true">
    Điều khoản dịch vụ
  </a>

  và

  <a href="#" @click.prevent="showPrivacyModal = true">
    Chính sách bảo mật
  </a>
</span>
          </div>

          <button class="main-btn" type="submit" :disabled="loading">
            {{ loading ? 'Đang xử lý...' : 'Đăng ký ngay' }}
          </button>
        </form>

        <div class="bottom-link">
          Đã có tài khoản? <router-link to="/auth/login">Đăng nhập</router-link>
        </div>
      </div>
    </div>
  </div>
  <BaseModal 
  :visible="modal.visible" 
  :type="modal.type" :title="modal.title" 
  :message="modal.message"
  @close="modal.visible = false" />
  <TermsOfServiceModal 
  :show="showTermsModal" 
  @close="showTermsModal = false" />

  <PrivacyPolicyModal 
  :show="showPrivacyModal" 
  @close="showPrivacyModal = false" />
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import PrivacyPolicyModal from '@/components/PrivacyPolicyModal.vue'
import TermsOfServiceModal from '@/components/TermsOfServiceModal.vue'
const showTermsModal = ref(false)
const showPrivacyModal = ref(false)
const router = useRouter()
const showRegisterPassword = ref(false)
const showRegisterConfirm = ref(false)
const loading = ref(false)

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
const handleRegister = async () => {
  try {
    if (!registerForm.fullName.trim()) {
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập họ và tên.')
      return
    }

    if (!registerForm.username.trim()) {
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập tên đăng nhập.')
      return
    }

    if (!registerForm.email.trim()) {
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập email.')
      return
    }

    if (!registerForm.phone.trim()) {
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập số điện thoại.')
      return
    }

    if (!registerForm.password.trim()) {
      showModal('error', 'Thiếu thông tin', 'Vui lòng nhập mật khẩu.')
      return
    }
        if (!registerForm.agree) {
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
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

.auth-page {
  display: flex;
  min-height: 100vh;
  width: 100%;
  /* BẮT BUỘC: Ép component giãn hết chiều ngang hệ thống */
  background: #fff7fa;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Inter', sans-serif;
}

.auth-page {
  display: flex;
  min-height: 100vh;
  background: #fff7fa;
}

.left-panel {
  width: 35%;
  background: linear-gradient(135deg, #ff9ec2, #ff5d99);
  padding: 50px;
  display: flex;
  align-items: center;
  color: white;
  position: relative;
  overflow: hidden;
}

.left-panel::before {
  content: '';
  position: absolute;
  width: 450px;
  height: 450px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  top: -150px;
  left: -120px;
}

.left-panel::after {
  content: '';
  position: absolute;
  width: 350px;
  height: 350px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
  bottom: -120px;
  right: -100px;
}

.content {
  position: relative;
  z-index: 2;
}

.brand {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 70px;
}

.menu-icon {
  font-size: 28px;
}

.brand h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.brand p {
  opacity: 0.9;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.right-panel {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
}

.auth-card {
  width: 100%;
  max-width: 700px;
  background: white;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 10px 35px rgba(255, 105, 160, 0.12);
}

.tabs {
  display: flex;
  background: #fff0f6;
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 28px;
}

.tabs button,
.tab-link {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  color: #777;
  transition: 0.3s;
  text-align: center;
  text-decoration: none;
}

.tabs .active {
  background: white;
  color: #ff4d94;
  box-shadow: 0 4px 12px rgba(255, 105, 160, 0.15);
}

.title h2 {
  font-size: 32px;
  margin-bottom: 8px;
}

.title p {
  color: #777;
  margin-bottom: 24px;
}

.social-login {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.social-btn {
  flex: 1;
  height: 48px;
  border: 1px solid #ffd4e4;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  color: #273044;
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
  transition: 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.social-btn:hover {
  background: #fff3f8;
  color: #111827;
}

.social-icon {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 24px;
}

.social-icon svg {
  width: 24px;
  height: 24px;
  display: block;
}

.divider {
  position: relative;
  text-align: center;
  margin-bottom: 28px;
  color: #ff6ca7;
  font-size: 13px;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  width: 32%;
  height: 1px;
  background: #f2d8e3;
  top: 50%;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.grid-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.form-group {
  margin-bottom: 18px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #555;
}

input[type='text'],
input[type='email'],
input[type='password'] {
  width: 100%;
  height: 48px;
  border: 1px solid #f2d8e3;
  border-radius: 12px;
  padding: 0 16px;
  outline: none;
  transition: 0.3s;
}

input:focus {
  border-color: #ff6ca7;
  box-shadow: 0 0 0 4px rgba(255, 105, 160, 0.12);
}

.password-input {
  position: relative;
}

.eye {
  position: absolute;
  right: 16px;
  top: 13px;
  opacity: 0.5;
  cursor: pointer;
  user-select: none;
}

.agree {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 22px;
  font-size: 14px;
  color: #666;
}

.agree a {
  color: #ff4d94;
  text-decoration: none;
  font-weight: 600;
}

.main-btn {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff76ae, #ff4d94);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.3s;
  box-shadow: 0 8px 20px rgba(255, 105, 160, 0.2);
}

.main-btn:hover {
  transform: translateY(-2px);
}

.bottom-link {
  text-align: center;
  margin-top: 24px;
  color: #777;
}

.bottom-link a {
  color: #ff4d94;
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 992px) {
  .auth-page {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
  }

  .grid-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .social-login {
    flex-direction: column;
  }

  .auth-card {
    padding: 24px;
  }

  .title h2 {
    font-size: 26px;
  }
}

@keyframes popupShow {
  from {
    opacity: 0;
    transform: scale(0.85);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
