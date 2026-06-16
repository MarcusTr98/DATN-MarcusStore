<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h2>Quên mật khẩu</h2>
        <p>
          Nhập email đã đăng ký tài khoản MarcusStore để nhận mã OTP xác thực.
        </p>
      </div>

      <form @submit.prevent="handleForgotPassword">
        <div class="form-group">
          <label>Email</label>
          <input
            v-model="email"
            type="email"
            placeholder="Nhập email của bạn"
          />
        </div>

        <button
          type="submit"
          class="btn-submit"
          :disabled="loading"
        >
          {{ loading ? 'Đang gửi OTP...' : 'Gửi mã OTP' }}
        </button>
      </form>

      <div class="back-login">
        <RouterLink to="/auth/login">
          Quay lại đăng nhập
        </RouterLink>
      </div>
    </div>
  </div>
      <BaseModal
      :visible="modal.visible"
      :type="modal.type"
      :title="modal.title"
      :message="modal.message"
      @close="onModalClose"
    />
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPasswordApi } from '@/api/authApi'
import BaseModal from '@/components/BaseModal.vue' 

const router = useRouter()
const email = ref('')
const loading = ref(false)

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

const onModalClose = () => {
  modal.visible = false
  // Chỉ chuyển trang khi thành công
  if (modal.type === 'success') {
    router.push({
      path: '/auth/verify-otp',
      query: { email: email.value, type: 'forgot-password' }
    })
  }
}

const handleForgotPassword = async () => {
    if (!email.value.trim()) {
    showModal('error', 'Lỗi', 'Vui lòng nhập email')
    return
  }
  try {
    loading.value = true
    await forgotPasswordApi(email.value)
    sessionStorage.setItem('resetEmail', email.value)
    showModal('success', 'Thành công', 'Mã OTP đã được gửi tới email của bạn')
  } catch (error) {
    showModal(
      'error',
      'Gửi OTP thất bại',
      error.response?.data?.message || 'Không thể gửi OTP, vui lòng thử lại'
    )
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: #f5f7fb;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 480px;
  background: white;
  border-radius: 20px;
  padding: 36px;
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.08);
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.auth-header h2 {
  font-size: 28px;
  margin-bottom: 10px;
  color: #222;
}

.auth-header p {
  color: #666;
  line-height: 1.6;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
}

.form-group input {
  width: 100%;
  height: 50px;
  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 0 15px;
  outline: none;
  transition: 0.2s;
}

.form-group input:focus {
  border-color: #2563eb;
}

.btn-submit {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  background: #2563eb;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit:hover {
  opacity: 0.95;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.back-login {
  margin-top: 20px;
  text-align: center;
}

.back-login a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}
</style>