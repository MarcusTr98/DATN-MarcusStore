<template>
  <div v-if="!emailVerified" class="verify-banner">

    <div class="banner-left">
      <i class="bi bi-envelope-exclamation-fill banner-icon"></i>
      <div>
        <p class="banner-title">Email chưa được xác thực</p>
        <p class="banner-desc">
          Xác thực email để sử dụng đầy đủ tính năng như nhận voucher, đánh giá sản phẩm...
        </p>
      </div>
    </div>

<button
  class="banner-btn"
  @click="goToVerify"
>
  <i class="bi bi-shield-check"></i>
  Nhập mã OTP
</button>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  email: {
    type: String,
    required: true
  },
  emailVerified: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['error'])

const router  = useRouter()
const goToVerify = () => {
  router.push({
    path: '/auth/verify-otp',
    query: {
      email: props.email,
      type: 'verify-email'
    }
  })
}

</script>

<style scoped>
.verify-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-left: 4px solid #f59e0b;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.banner-icon {
  font-size: 24px;
  color: #f59e0b;
  flex-shrink: 0;
}

.banner-title {
  margin: 0 0 2px;
  font-weight: 700;
  font-size: 14px;
  color: #92400e;
}

.banner-desc {
  margin: 0;
  font-size: 13px;
  color: #b45309;
}

.banner-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: #f59e0b;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: 0.2s;
  flex-shrink: 0;
}

.banner-btn:hover:not(:disabled) {
  background: #d97706;
}

.banner-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>