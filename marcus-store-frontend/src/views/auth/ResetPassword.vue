<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h2>Đặt lại mật khẩu</h2>
        <p>Vui lòng nhập mật khẩu mới cho tài khoản của bạn</p>
      </div>

      <form @submit.prevent="handleResetPassword">
        <div class="form-group">
          <label>Mật khẩu mới</label>
          <input
            v-model="form.newPassword"
            type="password"
            placeholder="Nhập mật khẩu mới"
        
          />
        </div>

        <div class="form-group">
          <label>Xác nhận mật khẩu</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Nhập lại mật khẩu mới"
           
          />
        </div>

        <button
          type="submit"
          class="btn-submit"
          :disabled="loading"
        >
          {{ loading ? 'Đang xử lý...' : 'Đổi mật khẩu' }}
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
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { resetPasswordApi } from '@/api/authApi'
import BaseModal from '@/components/BaseModal.vue'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
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

const onModalClose = () => {
  modal.visible = false
  if (modal.type === 'success') {
    sessionStorage.removeItem('resetEmail')
    sessionStorage.removeItem('allowResetPassword')
    router.push('/auth/login')
  }
}

const handleResetPassword = async () => {
 if (!form.newPassword.trim()) {
    showModal('error', 'Lỗi', 'Vui lòng nhập mật khẩu mới')
    return
  }

  if (!form.confirmPassword.trim()) {
    showModal('error', 'Lỗi', 'Vui lòng nhập xác nhận mật khẩu')
    return
  }

  if (form.newPassword.length < 6) {
    showModal('error', 'Lỗi', 'Mật khẩu phải có ít nhất 6 ký tự')
    return
  }

  if (form.newPassword !== form.confirmPassword) {
    showModal('error', 'Lỗi', 'Mật khẩu xác nhận không khớp')
    return
  }
  try {
    const email = sessionStorage.getItem('resetEmail')

    if (!email) {
      showModal('error', 'Hết phiên', 'Phiên đặt lại mật khẩu đã hết hạn')
      return
    }

    loading.value = true

    await resetPasswordApi({
      email,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })

    showModal('success', 'Thành công', 'Đổi mật khẩu thành công! Vui lòng đăng nhập lại.')
  } catch (error) {
    showModal(
      'error',
      'Thất bại',
      error.response?.data?.message || 'Đổi mật khẩu thất bại, vui lòng thử lại'
    )
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!sessionStorage.getItem('allowResetPassword')) {
    router.replace('/auth/login')
  }
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: #f5f7fb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 460px;
  background: #fff;
  border-radius: 20px;
  padding: 35px;
  box-shadow: 0 10px 35px rgba(0, 0, 0, 0.08);
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
  color: #777;
  font-size: 14px;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.form-group input {
  width: 100%;
  height: 48px;
  border: 1px solid #ddd;
  border-radius: 10px;
  padding: 0 14px;
  outline: none;
  transition: 0.3s;
}

.form-group input:focus {
  border-color: #2563eb;
}

.btn-submit {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 10px;
  background: #2563eb;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.3s;
}

.btn-submit:hover {
  opacity: 0.9;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.back-login {
  text-align: center;
  margin-top: 18px;
}

.back-login a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}
</style>