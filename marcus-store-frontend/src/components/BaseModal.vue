<template>
  <Teleport to="body">
    <div v-if="show || visible" class="modal-overlay" @click.self="close">
      <div class="modal-box">
        <div class="modal-icon">
          <span v-if="type === 'success'">✅</span>
          <span v-else-if="type === 'error'">❌</span>
          <span v-else-if="type === 'confirm'">⚠️</span>
          <span v-else>ℹ️</span>
        </div>

        <h3 class="modal-title">{{ title }}</h3>

        <p class="modal-message" v-html="message"></p>

        <div v-if="type !== 'confirm'" class="modal-actions">
          <button class="modal-btn" :class="type" @click="close">Đóng</button>
        </div>

        <div v-else class="modal-actions dual-btns">
          <button class="modal-btn cancel-btn" @click="close">Hủy</button>
          <button class="modal-btn confirm-btn" @click="confirmAction">Đồng ý</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
const emit = defineEmits(['close', 'confirm'])

defineProps({
  show: { type: Boolean, default: false }, // Dành cho trang Checkout
  visible: { type: Boolean, default: false }, // Dành cho form Đổi mật khẩu
  type: { type: String, default: 'error' }, // 'success', 'error', 'confirm'
  title: { type: String, default: 'Thông báo' },
  message: { type: String, default: '' },
})

const close = () => emit('close')
const confirmAction = () => {
  emit('confirm')
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
  backdrop-filter: blur(2px);
}

.modal-box {
  background: white;
  border-radius: 20px;
  padding: 32px 28px;
  width: 380px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: popUp 0.25s ease;
}

.modal-icon {
  font-size: 52px;
  margin-bottom: 12px;
  line-height: 1;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #111;
}

.modal-message {
  font-size: 14px;
  color: #4b5563;
  margin-bottom: 24px;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: center;
}

.dual-btns {
  gap: 12px;
  width: 100%;
}

.modal-btn {
  flex: 1;
  height: 44px;
  width: 100%;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-btn.success {
  background: linear-gradient(135deg, #6fcf97, #27ae60);
  color: white;
}
.modal-btn.success:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(39, 174, 96, 0.3);
}

.modal-btn.error,
.confirm-btn {
  background: #d70018; /* Đỏ thương hiệu */
  color: white;
}
.modal-btn.error:hover,
.confirm-btn:hover {
  background: #b8001a;
  transform: translateY(-2px);
}

.cancel-btn {
  background: #f3f4f6;
  color: #4b5563;
}
.cancel-btn:hover {
  background: #e5e7eb;
  color: #111;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes popUp {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
