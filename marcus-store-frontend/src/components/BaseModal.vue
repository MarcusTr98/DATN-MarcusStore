<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="modal-overlay" @click.self="close">
        <div class="modal-box">
          <button class="close-btn" @click="close">
            <i class="fa-solid fa-xmark"></i>
          </button>

          <div class="modal-icon" :class="type">
            <i v-if="type === 'success'" class="fa-solid fa-circle-check"></i>
            <i v-else-if="type === 'error'" class="fa-solid fa-circle-xmark"></i>
            <i v-else-if="type === 'confirm'" class="fa-solid fa-circle-exclamation"></i>
            <i v-else class="fa-solid fa-circle-info"></i>
          </div>

          <h3 class="modal-title">{{ title }}</h3>

          <div class="modal-body-custom">
            <slot>
              <p class="modal-message">{{ message }}</p>
            </slot>
          </div>

          <div class="modal-actions" :class="{ 'single-btn': type !== 'confirm' }">
            <button v-if="type === 'confirm'" class="btn-cancel" @click="close">Hủy bỏ</button>
            <button class="btn-confirm" :class="type" @click="confirmAction">
              {{ type === 'confirm' ? 'Xác nhận' : 'Đóng' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  visible: Boolean,
  type: { type: String, default: 'info' }, // success, error, confirm, info
  title: { type: String, default: 'Thông báo' },
  message: { type: String, default: '' },
})

const emit = defineEmits(['close', 'confirm'])

const close = () => emit('close')
const confirmAction = () => {
  if (props.type === 'confirm') {
    emit('confirm')
  } else {
    emit('close')
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-box {
  background: white;
  border-radius: 16px;
  padding: 32px 28px 24px;
  width: min(90vw, 420px);
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  position: relative;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  transition: 0.2s;
}

.close-btn:hover {
  color: #333;
}

.modal-icon {
  font-size: 52px;
  margin-bottom: 16px;
}

.modal-icon.success {
  color: #0ea5e9;
}
.modal-icon.error {
  color: #ef4444;
}
.modal-icon.confirm {
  color: #f59e0b;
}
.modal-icon.info {
  color: #3b82f6;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
  color: #1e293b;
}

.modal-body-custom {
  margin-bottom: 24px;
}

.modal-message {
  font-size: 15px;
  color: #475569;
  line-height: 1.5;
  white-space: pre-wrap; /* Giữ nguyên xuống dòng nếu dùng String */
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-actions.single-btn {
  justify-content: center;
}

.modal-actions button {
  flex: 1;
  height: 44px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cancel {
  background: #f1f5f9;
  color: #475569;
}

.btn-cancel:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-confirm {
  color: white;
}

.btn-confirm.success {
  background: #0ea5e9;
}
.btn-confirm.success:hover {
  background: #0284c7;
}

.btn-confirm.error {
  background: #ef4444;
}
.btn-confirm.error:hover {
  background: #dc2626;
}

.btn-confirm.confirm {
  background: #f59e0b;
}
.btn-confirm.confirm:hover {
  background: #d97706;
}

.btn-confirm.info {
  background: #3b82f6;
}
.btn-confirm.info:hover {
  background: #2563eb;
}

/* Transitions */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
