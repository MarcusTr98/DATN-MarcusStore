<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="loadingStore.isLoading" class="global-spinner-overlay">
        <div class="spinner-content-box">
          <div class="spinner-box">
            <div class="spinner-ring"></div>
            <div class="spinner-logo">
              <i class="fas fa-mobile-alt"></i>
            </div>
          </div>

          <div class="spinner-text-wrap">
            <p class="spinner-title">Marcus Store</p>
            <p class="spinner-subtitle">
              Đang xử lý dữ liệu<span>.</span><span>.</span><span>.</span>
            </p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useLoadingStore } from '@/stores/useLoadingStore'

const loadingStore = useLoadingStore()
</script>

<style scoped>
.global-spinner-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner-content-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.spinner-box {
  position: relative;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner-ring {
  position: absolute;
  inset: 0;
  border: 4px solid #f8d7da;
  border-top-color: #d70018;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

.spinner-logo {
  color: #d70018;
  font-size: 20px;
  animation: pulse 1.5s ease-in-out infinite;
}

/* Cụm Text */
.spinner-text-wrap {
  text-align: center;
}

.spinner-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 800;
  color: #1a202c;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.spinner-subtitle {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #d70018;
}

/* Hiệu ứng chớp nháy cho 3 dấu chấm */
.spinner-subtitle span {
  animation: blink 1.4s infinite both;
}
.spinner-subtitle span:nth-child(2) {
  animation-delay: 0.2s;
}
.spinner-subtitle span:nth-child(3) {
  animation-delay: 0.4s;
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

/* Hiệu ứng nháy dấu chấm */
@keyframes blink {
  0% {
    opacity: 0.2;
  }
  20% {
    opacity: 1;
  }
  100% {
    opacity: 0.2;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
