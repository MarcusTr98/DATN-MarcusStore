<template>
  <div class="otp-page">
    <div class="otp-card">

      <div class="otp-header">
        <div class="icon">📩</div>
        <h2>Xác thực OTP</h2>
        <p>
          Mã đã được gửi tới
          <span>{{ email }}</span>
        </p>
      </div>

      <input
        v-model="otp"
        maxlength="6"
        class="otp-input"
        placeholder="••••••"
      />

      <button class="otp-btn" @click="verifyOtp">
        Xác nhận
      </button>
    </div>
     <BaseModal
      :visible="modal.visible"
      :type="modal.type"
      :title="modal.title"
      :message="modal.message"
      @close="onModalClose"
    />
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue' 
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'

const route = useRoute()
const router = useRouter()
const otp = ref('')
const email = route.query.email
const type = route.query.type
const loading = ref(false)
const failCount = ref(0)
const MAX_ATTEMPTS = 3

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

    if (type === 'forgot-password') {

      router.push('/auth/reset-password')

    } else {

      router.push('/auth/login')

    }

  } else if (failCount.value >= MAX_ATTEMPTS) {

    if (type === 'forgot-password') {

      router.push('/auth/forgot-password')

    } else {

      router.push('/auth/register')

    }

  }
}
const verifyOtp = async () => {
  if (loading.value) return

  loading.value = true

  try {

    if (type === 'forgot-password') {

      await api.post('/auth/verify-otp', {
        email,
        otp: otp.value,
      })
     sessionStorage.setItem('allowResetPassword', 'true')
      showModal(
        'success',
        'Thành công',
        'OTP hợp lệ. Vui lòng đặt lại mật khẩu.'
      )

    } else {

      await api.post('/auth/register/verify', {
        email,
        otp: otp.value,
      })

      showModal(
        'success',
        'Thành công',
        'Tài khoản đã được xác thực. Bạn có thể đăng nhập ngay!'
      )
    }

  } catch (e) {

    failCount.value++

    const msg =
      e.response?.data?.message ||
      'OTP không hợp lệ!'

    if (failCount.value >= MAX_ATTEMPTS) {

      showModal(
        'error',
        'Sai quá nhiều lần',
        'Bạn đã nhập sai OTP quá 3 lần.'
      )

    } else {

      showModal(
        'error',
        'Xác thực thất bại',
        `${msg} (Còn ${
          MAX_ATTEMPTS - failCount.value
        } lần thử)`
      )
    }

  } finally {
    loading.value = false
  }
}
onMounted(() => {
  if (!email) {

    if (type === 'forgot-password') {
      router.replace('/auth/forgot-password')
    } else {
      router.replace('/auth/register')
    }

  }
})
</script>
<style scoped>
.otp-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #ffe4ec, #fff0f6);
  font-family: 'Inter', sans-serif;
}

.otp-card {
  width: 420px;
  background: white;
  padding: 36px;
  border-radius: 22px;
  box-shadow: 0 25px 70px rgba(255, 77, 148, 0.25);
  text-align: center;
  animation: pop 0.25s ease;
}

.otp-header .icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.otp-header h2 {
  color: #ff4d94;
  margin-bottom: 6px;
}

.otp-header p {
  color: #777;
  font-size: 14px;
}

.otp-header span {
  color: #ff4d94;
  font-weight: 600;
}

.otp-input {
  width: 100%;
  height: 60px;
  margin-top: 20px;
  border-radius: 14px;
  border: 2px solid #ffd4e4;
  text-align: center;
  font-size: 24px;
  letter-spacing: 10px;
  font-weight: 700;
  outline: none;
  transition: 0.2s;
}

.otp-input:focus {
  border-color: #ff4d94;
  box-shadow: 0 0 0 4px rgba(255, 77, 148, 0.15);
}

.otp-btn {
  width: 100%;
  height: 50px;
  margin-top: 18px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff76ae, #ff4d94);
  color: white;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
}

.otp-btn:hover {
  transform: translateY(-2px);
}



@keyframes pop {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>