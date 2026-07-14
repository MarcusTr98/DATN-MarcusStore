<template>
  <Teleport to="body">
    <Transition name="cfsm-fade">
      <div
        v-if="visible"
        class="cfsm-overlay"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cfsm-title"
        @click.self="handleClose"
      >
        <div class="cfsm-card">
          <div class="cfsm-icon">
            <svg
              width="56"
              height="56"
              viewBox="0 0 48 48"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <path
                d="M22.314 5.286L4.286 35.286C3.428 36.8 4.514 38.628 6.228 38.628H41.772C43.486 38.628 44.572 36.8 43.714 35.286L25.686 5.286C24.828 3.772 23.172 3.772 22.314 5.286Z"
                stroke="#E11D1D"
                stroke-width="2.5"
                stroke-linejoin="round"
                fill="none"
              />
              <path
                d="M24 17V25"
                stroke="#E11D1D"
                stroke-width="2.8"
                stroke-linecap="round"
              />
              <circle cx="24" cy="31" r="1.8" fill="#E11D1D" />
            </svg>
          </div>

          <h3 id="cfsm-title" class="cfsm-title">Thông báo</h3>

          <p class="cfsm-message">
            Xin lỗi quý khách, Flash Sale này đã được admin hủy. Vui lòng chờ đợi chương
            trình lần sau nhé!
          </p>

          <button class="cfsm-btn" type="button" @click="handleConfirm">
            Đồng ý
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'confirm'])

// Click ngoài overlay đóng modal — không có nút Hủy vì đây là thông báo 1 chiều.
function handleClose() {
  emit('close')
}

// Bấm "Đồng ý" → parent xử lý tiếp (thường là reload trang).
function handleConfirm() {
  emit('confirm')
}
</script>

<style scoped>
.cfsm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 16px;
}

.cfsm-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px 24px;
  max-width: 420px;
  width: 100%;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: cfsm-pop 0.25s ease;
}

.cfsm-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.cfsm-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 12px;
}

.cfsm-message {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin: 0 0 24px;
}

.cfsm-btn {
  background: #E11D1D;
  color: white;
  border: none;
  padding: 12px 36px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
  min-width: 120px;
}

.cfsm-btn:hover {
  background: #c81818;
}

.cfsm-btn:active {
  background: #b11515;
}

/* Animations */
.cfsm-fade-enter-active,
.cfsm-fade-leave-active {
  transition: opacity 0.25s ease;
}

.cfsm-fade-enter-from,
.cfsm-fade-leave-to {
  opacity: 0;
}

.cfsm-fade-enter-active .cfsm-card,
.cfsm-fade-leave-active .cfsm-card {
  transition: transform 0.25s ease;
}

.cfsm-fade-enter-from .cfsm-card,
.cfsm-fade-leave-to .cfsm-card {
  transform: scale(0.95);
}

@keyframes cfsm-pop {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>