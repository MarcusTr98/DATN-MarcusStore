<template>
  <div class="pd-buy">
    <!-- Chọn số lượng -->
    <div class="pd-buy__qty">
      <span class="pd-buy__label">Số lượng:</span>
      <div class="pd-buy__qty-control">
        <button
          type="button"
          class="pd-buy__qty-btn"
          :disabled="quantity <= 1 || !canBuy"
          @click="changeQty(-1)"
        >
          −
        </button>
        <input
          v-model.number="quantity"
          type="number"
          class="pd-buy__qty-input"
          :min="1"
          :max="maxStock"
          :disabled="!canBuy"
          @change="normalize"
        />
        <button
          type="button"
          class="pd-buy__qty-btn"
          :disabled="quantity >= maxStock || !canBuy"
          @click="changeQty(1)"
        >
          +
        </button>
      </div>
      <span v-if="maxStock > 0" class="pd-buy__stock-hint">
        (Còn {{ maxStock }})
      </span>
    </div>

    <!-- Nút hành động -->
    <div class="pd-buy__buttons">
      <button
        type="button"
        class="pd-buy__btn pd-buy__btn--cart"
        :disabled="!canBuy || isLoading"
        @click="$emit('add-to-cart', quantity)"
      >
        <i class="ti ti-shopping-cart-plus" aria-hidden="true" />
        <span>{{ isAddingToCart ? 'Đang thêm...' : 'Thêm giỏ hàng' }}</span>
      </button>

      <button
        type="button"
        class="pd-buy__btn pd-buy__btn--buy"
        :disabled="!canBuy || isLoading"
        @click="$emit('buy-now', quantity)"
      >
        <i class="ti ti-bolt" aria-hidden="true" />
        <span>{{ isBuying ? 'Đang xử lý...' : 'Mua ngay' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  maxStock: { type: Number, default: 0 },
  inStock: { type: Boolean, default: false },
  isLoading: { type: Boolean, default: false },
  isAddingToCart: { type: Boolean, default: false },
  isBuying: { type: Boolean, default: false },
})
defineEmits(['add-to-cart', 'buy-now'])

const quantity = ref(1)
const canBuy = computed(() => props.inStock && props.maxStock > 0)

// Reset qty khi hết hàng hoặc đổi SKU
watch(
  () => [props.inStock, props.maxStock],
  ([s, m]) => {
    if (!s) quantity.value = 1
    else if (quantity.value > m) quantity.value = m || 1
  },
)

function changeQty(delta) {
  const next = quantity.value + delta
  if (next < 1) return
  if (props.maxStock && next > props.maxStock) return
  quantity.value = next
}
function normalize() {
  let v = Number(quantity.value) || 1
  if (v < 1) v = 1
  if (props.maxStock && v > props.maxStock) v = props.maxStock
  quantity.value = v
}
</script>

<style scoped>
.pd-buy {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pd-buy__qty {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}
.pd-buy__label {
  color: #555;
  font-weight: 500;
}
.pd-buy__qty-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.pd-buy__qty-btn {
  width: 36px;
  height: 36px;
  background: #fff;
  border: none;
  font-size: 18px;
  color: #555;
  cursor: pointer;
  transition: background 0.15s ease;
}
.pd-buy__qty-btn:hover:not(:disabled) {
  background: #fafafa;
  color: #e11d1d;
}
.pd-buy__qty-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
}
.pd-buy__qty-input {
  width: 56px;
  height: 36px;
  border: none;
  border-left: 1px solid #ddd;
  border-right: 1px solid #ddd;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  outline: none;
  -moz-appearance: textfield;
}
.pd-buy__qty-input::-webkit-outer-spin-button,
.pd-buy__qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.pd-buy__qty-input:disabled {
  background: #fafafa;
  color: #ccc;
}
.pd-buy__stock-hint {
  color: #888;
  font-size: 13px;
}

.pd-buy__buttons {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 10px;
}

.pd-buy__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.pd-buy__btn i {
  font-size: 18px;
}
.pd-buy__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pd-buy__btn--cart {
  background: #fff;
  color: #e11d1d;
  border: 1.5px solid #e11d1d;
}
.pd-buy__btn--cart:hover:not(:disabled) {
  background: #fff5f5;
}

.pd-buy__btn--buy {
  background: #e11d1d;
  color: #fff;
}
.pd-buy__btn--buy:hover:not(:disabled) {
  background: #c0392b;
}
</style>
