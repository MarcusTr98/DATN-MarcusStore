<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="loadingStore.isLoading" class="global-spinner-overlay">
        <div class="spinner-box">
          <div class="spinner-ring"></div>
          <div class="spinner-logo">
            <i class="fas fa-mobile-alt"></i>
          </div>
        </div>
        <p class="spinner-text">Đang xử lý...</p>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useLoadingStore } from '@/stores/useLoadingStore'

const loadingStore = useLoadingStore()
</script>

<style scoped>
/* Lớp phủ toàn màn hình */
.global-spinner-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999; /* Đảm bảo đè lên tất cả mọi thứ, kể cả Modal */
  background: rgba(255, 255, 255, 0.85); /* Nền trắng mờ để vẫn thấy mờ mờ nội dung phía sau */
  backdrop-filter: blur(4px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* Khối chứa spinner */
.spinner-box {
  position: relative;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Vòng xoay màu đỏ CellphoneS */
.spinner-ring {
  position: absolute;
  inset: 0;
  border: 4px solid #f8d7da; /* Đỏ cực nhạt làm nền */
  border-top-color: #d70018; /* Đỏ CellphoneS chuẩn */
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

/* Icon ở giữa vòng xoay */
.spinner-logo {
  color: #d70018;
  font-size: 20px;
  animation: pulse 1.5s ease-in-out infinite;
}

/* Chữ báo trạng thái */
.spinner-text {
  margin-top: 16px;
  font-size: 14px;
  font-weight: 700;
  color: #d70018;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

/* Hiệu ứng xoay */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Hiệu ứng nhịp đập cho icon */
@keyframes pulse {
  0%,
  100% {
    transform: scale(0.9);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.1);
    opacity: 1;
  }
}

/* Transition mượt mà khi ẩn/hiện */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
