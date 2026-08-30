<template>
  <Teleport to="body">
    <Transition name="v-modal-fade">
      <div
        v-if="visible"
        class="v-modal-overlay"
        :class="{ active: visible }"
        @click.self="handleClose"
      >
        <div class="v-modal-card">
          <div class="v-modal-header">
            <div class="v2-header-text">
              <h3>{{ title }}</h3>
              <p class="v2-subtitle">{{ subtitle }}</p>
            </div>
            <button class="close-btn" type="button" @click="handleClose">
              &times;
            </button>
          </div>

          <div class="v-modal-body v2-modal-body">
            <div v-if="isLoading" class="v2-voucher-loading">
              <i class="fas fa-spinner fa-spin"></i> Đang tải voucher...
            </div>

            <div v-else class="v2-voucher-list">
              <div
                v-for="voucher in activeVouchers"
                :key="voucher.id"
                class="v2-voucher-card"
                :class="{ selected: selectedId === voucher.id }"
                @click="selectVoucher(voucher.id)"
              >
                <div class="v2-badge-best" v-if="voucher.isBest">Tốt Nhất</div>

                <div class="v2-voucher-left">
                  <div class="v2-icon-box" :class="voucher.iconClass">
                    <i :class="voucher.icon" aria-hidden="true"></i>
                  </div>
                </div>

                <div class="v2-voucher-info">
                  <span class="v2-code">ID voucher: {{ voucher.voucherCode }}</span>
                  <span class="v2-title">{{ voucher.title }}</span>
                  <span class="v2-min-order">Đơn tối thiểu {{ formatCurrency(voucher.minOrder) }}</span>
                  <span class="v2-expiry" :class="{ urgent: voucher.expiryUrgent }">
                    {{ voucher.expiryLabel }}
                  </span>
                </div>

                <div class="v2-voucher-action" @click.stop>
                  <input
                    type="radio"
                    name="voucher-radio"
                    :checked="selectedId === voucher.id"
                    @change="selectVoucher(voucher.id)"
                  />
                </div>
              </div>

              <div
                v-for="voucher in disabledVouchers"
                :key="voucher.id"
                class="v2-voucher-card v2-voucher-card--disabled"
              >
                <div class="v2-voucher-left">
                  <div class="v2-icon-box" :class="voucher.iconClass">
                    <i :class="voucher.icon" aria-hidden="true"></i>
                  </div>
                </div>

                <div class="v2-voucher-info">
                  <span class="v2-code">ID voucher: {{ voucher.voucherCode }}</span>
                  <span class="v2-title">{{ voucher.title }}</span>
                  <span class="v2-min-order">Đơn tối thiểu {{ formatCurrency(voucher.minOrder) }}</span>
                  <span class="v2-expiry" :class="{ urgent: voucher.expiryUrgent }">
                    {{ voucher.expiryLabel }}
                  </span>
                </div>

                <div class="v2-voucher-action v2-voucher-action--disabled">
                  <input type="radio" name="voucher-radio" disabled />
                  <span class="v2-disabled-reason">{{ voucher.disabledReason }}</span>
                </div>
              </div>

              <div v-if="activeVouchers.length === 0 && disabledVouchers.length === 0" class="v2-voucher-empty">
                <i class="fas fa-ticket" aria-hidden="true"></i>
                <p>Không có voucher khả dụng</p>
              </div>
            </div>
          </div>

          <div class="v2-footer-bar">
            <button
              class="v-btn v-btn-primary v2-confirm-btn"
              type="button"
              @click="handleConfirm"
            >
              ĐỒNG Ý
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  vouchers: {
    type: Array,
    default: () => [],
  },
  cartTotal: {
    type: Number,
    default: 0,
  },
  title: {
    type: String,
    default: 'Chọn 1 Voucher Áp Dụng',
  },
  subtitle: {
    type: String,
    default: 'Hệ thống tự động chọn mã có giá trị giảm cao nhất cho bạn',
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  preSelectedId: {
    type: [Number, String, null],
    default: null,
  },
})

const emit = defineEmits(['close', 'select', 'confirm'])

const selectedId = ref(null)

// Sync preSelectedId when props change
watch(
  () => props.preSelectedId,
  (newVal) => {
    selectedId.value = newVal
  },
  { immediate: true }
)

// Reset selected when modal opens
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      selectedId.value = props.preSelectedId
    }
  }
)

const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`

// Computed voucher list
const processedVouchers = computed(() => {
  const total = props.cartTotal
  return props.vouchers.map((v) => {
    const discountType = (v.discountType || '').toUpperCase()
    const isAmount = discountType === 'AMOUNT'
    const isPercent = discountType === 'PERCENT'
    const isFreeship = discountType === 'FREESHIP'

    let title = ''
    let discountValue = 0
    let discountPercent = 0

    if (isAmount) {
      title = `Giảm ${formatCurrency(v.discountValue)} toàn sàn`
      discountValue = v.discountValue
    } else if (isPercent) {
      title = `Giảm ${v.discountValue}% toàn sàn`
      discountPercent = v.discountValue
    } else if (isFreeship) {
      title = `Miễn phí vận chuyển ${formatCurrency(v.discountValue)}`
      discountValue = v.discountValue
    } else {
      title = `Giảm ${formatCurrency(v.discountValue)} toàn sàn`
      discountValue = v.discountValue
    }

    const active = (v.status === 'ACTIVE' || v.status === 'SCHEDULED') && !v.isUsed && total >= (v.minOrderValue || 0)
    let disabledReason = ''
    if (v.status !== 'ACTIVE' && v.status !== 'SCHEDULED') disabledReason = 'Voucher không còn hoạt động'
    else if (v.isUsed) disabledReason = 'Voucher đã được sử dụng'
    else if (total < (v.minOrderValue || 0)) {
      const needed = (v.minOrderValue || 0) - total
      disabledReason = `Chưa đủ điều kiện: Mua thêm ${formatCurrency(needed)}`
    }

    return {
      id: v.voucherId,
      voucherCode: v.voucherCode,
      title,
      discountType,
      minOrder: v.minOrderValue,
      discountValue,
      discountPercent,
      maxDiscountAmount: v.maxDiscountAmount || 0,
      expiryLabel: `Hạn dùng đến: ${new Date(v.endDate).toLocaleDateString('vi-VN')}`,
      expiryUrgent: false,
      icon: isFreeship ? 'fas fa-truck' : 'fas fa-tag',
      iconClass: isFreeship
        ? 'v2-icon-box--freeship'
        : isPercent
          ? 'v2-icon-box--percent'
          : 'v2-icon-box--amount',
      active,
      disabledReason,
      isBest: false,
    }
  })
})

// Estimate discount value for sorting
function estimateDiscount(voucher) {
  if (!voucher.active) return 0
  const total = props.cartTotal
  if (voucher.discountType === 'AMOUNT') {
    return Math.min(voucher.discountValue, total)
  }
  if (voucher.discountType === 'PERCENT') {
    const raw = (voucher.discountPercent / 100) * total
    return Math.min(raw, voucher.maxDiscountAmount > 0 ? voucher.maxDiscountAmount : raw)
  }
  if (voucher.discountType === 'FREESHIP') return voucher.discountValue
  return 0
}

const activeVouchers = computed(() => {
  const list = processedVouchers.value
    .filter((v) => v.active)
    .map((v) => ({ ...v, _discount: estimateDiscount(v) }))
    .sort((a, b) => b._discount - a._discount)
  if (list.length > 0) list[0].isBest = true
  return list.map(({ _discount, ...rest }) => rest)
})

const disabledVouchers = computed(() => processedVouchers.value.filter((v) => !v.active))

function selectVoucher(id) {
  selectedId.value = selectedId.value === id ? null : id
  emit('select', selectedId.value)
}

function handleClose() {
  emit('close')
}

function handleConfirm() {
  const picked = processedVouchers.value.find((v) => v.id === selectedId.value)
  emit('confirm', picked)
  emit('close')
}
</script>

<style scoped>
.v-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20px;
}

.v-modal-overlay.active {
  display: flex;
}

.v-modal-card {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 600px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.v-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 20px 0;
}

.v2-header-text h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

.v2-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #374151;
}

.v-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.v2-voucher-loading {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
  font-size: 14px;
}

.v2-voucher-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.v2-voucher-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.v2-voucher-card:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.v2-voucher-card.selected {
  border-color: #e11d1d;
  background: #fef2f2;
}

.v2-badge-best {
  position: absolute;
  top: -8px;
  left: 12px;
  background: #e11d1d;
  color: white;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.v2-voucher-left {
  flex-shrink: 0;
}

.v2-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #fef2f2;
  color: #e11d1d;
}

.v2-icon-box--freeship {
  background: #ecfdf5;
  color: #10b981;
}

.v2-icon-box--percent {
  background: #fffbeb;
  color: #f59e0b;
}

.v2-icon-box--amount {
  background: #eff6ff;
  color: #3b82f6;
}

.v2-voucher-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.v2-code {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
}

.v2-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.v2-min-order,
.v2-expiry {
  font-size: 12px;
  color: #6b7280;
}

.v2-expiry.urgent {
  color: #dc2626;
}

.v2-voucher-action input[type='radio'] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #e11d1d;
}

.v2-voucher-action--disabled {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.v2-disabled-reason {
  font-size: 10px;
  color: #ef4444;
  text-align: center;
  max-width: 60px;
}

.v2-voucher-card--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.v2-voucher-card--disabled:hover {
  border-color: #e5e7eb;
  background: white;
}

.v2-voucher-empty {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
}

.v2-voucher-empty i {
  font-size: 40px;
  margin-bottom: 12px;
  display: block;
}

.v2-voucher-empty p {
  margin: 0;
  font-size: 14px;
}

.v2-footer-bar {
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}

.v-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.v-btn-primary {
  background: #e11d1d;
  color: white;
  width: 100%;
}

.v-btn-primary:hover {
  background: #c0392b;
}

.v2-confirm-btn {
  width: 100%;
}

/* Transitions */
.v-modal-fade-enter-active,
.v-modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.v-modal-fade-enter-active .v-modal-card,
.v-modal-fade-leave-active .v-modal-card {
  transition: transform 0.25s ease;
}

.v-modal-fade-enter-from,
.v-modal-fade-leave-to {
  opacity: 0;
}

.v-modal-fade-enter-from .v-modal-card,
.v-modal-fade-leave-to .v-modal-card {
  transform: scale(0.95);
}
</style>
