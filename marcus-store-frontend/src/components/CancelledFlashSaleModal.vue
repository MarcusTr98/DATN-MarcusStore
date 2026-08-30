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

          <h3 id="cfsm-title" class="cfsm-title">Flash Sale đã bị admin hủy</h3>

          <p class="cfsm-message">
            Xin lỗi quý khách, chương trình Flash Sale đã được admin huỷ bỏ. Giá các sản phẩm
            dưới đây đã được <strong>tự động chuyển về giá gốc</strong>:
          </p>

          <!-- Danh sách sản phẩm bị revert giá -->
          <div v-if="revertedItems && revertedItems.length > 0" class="cfsm-list">
            <div
              v-for="item in revertedItems"
              :key="item.cartItemId"
              class="cfsm-list-item"
            >
              <div class="cfsm-list-item__name">
                {{ item.productName }}
                <small v-if="item.variantName"> · {{ item.variantName }}</small>
              </div>
              <div class="cfsm-list-item__prices">
                <span class="cfsm-list-item__old">
                  <s>{{ formatPrice(item.oldPrice) }}₫</s>
                  <span class="cfsm-list-item__fs-badge">⚡ {{ item.slotName }}</span>
                </span>
                <i class="fas fa-arrow-right cfsm-list-item__arrow"></i>
                <span class="cfsm-list-item__new">{{ formatPrice(item.newPrice) }}₫</span>
              </div>
            </div>
          </div>

          <p class="cfsm-message cfsm-message--secondary">
            Quý khách có thể <strong>tiếp tục thanh toán với giá gốc</strong>, hoặc
            <strong>xóa sản phẩm</strong> khỏi giỏ hàng nếu không còn nhu cầu.
          </p>

          <div class="cfsm-actions">
            <button class="cfsm-btn cfsm-btn--secondary" type="button" @click="handleRemove">
              <i class="fas fa-trash-alt me-1"></i> Xóa khỏi giỏ hàng
            </button>
            <button class="cfsm-btn" type="button" @click="handleConfirm">
              <i class="fas fa-shopping-cart me-1"></i> Tiếp tục với giá gốc
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  // Danh sách cart item bị revert giá (để hiển thị lý do giá đổi)
  revertedItems: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['close', 'confirm', 'remove'])

// Click ngoài overlay đóng modal — không có nút Hủy vì đây là thông báo 1 chiều.
function handleClose() {
  emit('close')
}

// Bấm "Tiếp tục với giá gốc" → parent xử lý tiếp (giữ SP trong giỏ, tiếp tục checkout).
function handleConfirm() {
  emit('confirm')
}

// Bấm "Xóa khỏi giỏ hàng" → parent xử lý xóa SP khỏi cart rồi reload.
function handleRemove() {
  emit('remove')
}

function formatPrice(value) {
  if (!value && value !== 0) return '0'
  return Number(value).toLocaleString('vi-VN')
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
  padding: 28px 24px 24px;
  max-width: 480px;
  width: 100%;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: cfsm-pop 0.25s ease;
  max-height: 90vh;
  overflow-y: auto;
}

.cfsm-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
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
  margin: 0 0 16px;
  text-align: left;
}

.cfsm-message--secondary {
  margin-top: 12px;
  margin-bottom: 20px;
  font-size: 13px;
  color: #64748b;
  background: #fef3f2;
  border-left: 3px solid #E11D1D;
  padding: 10px 12px;
  border-radius: 6px;
}

.cfsm-list {
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px;
  margin: 0 0 4px;
  text-align: left;
  max-height: 220px;
  overflow-y: auto;
}

.cfsm-list-item {
  padding: 10px 12px;
  border-bottom: 1px dashed #e2e8f0;
}
.cfsm-list-item:last-child {
  border-bottom: none;
}

.cfsm-list-item__name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}
.cfsm-list-item__name small {
  font-weight: 400;
  color: #64748b;
}

.cfsm-list-item__prices {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  flex-wrap: wrap;
}

.cfsm-list-item__old {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  text-decoration: line-through;
}

.cfsm-list-item__fs-badge {
  font-size: 11px;
  color: #f59e0b;
  background: #fffbeb;
  padding: 2px 6px;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
}

.cfsm-list-item__arrow {
  color: #cbd5e1;
  font-size: 11px;
}

.cfsm-list-item__new {
  font-weight: 700;
  color: #E11D1D;
  font-size: 14px;
}

.cfsm-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.cfsm-btn {
  background: #E11D1D;
  color: white;
  border: none;
  padding: 11px 18px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 130px;
}

.cfsm-btn:hover {
  background: #c81818;
  transform: translateY(-1px);
}

.cfsm-btn:active {
  background: #b11515;
}

.cfsm-btn--secondary {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.cfsm-btn--secondary:hover {
  background: #e2e8f0;
  color: #1e293b;
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
