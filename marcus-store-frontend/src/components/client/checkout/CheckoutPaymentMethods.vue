<template>
  <div class="payment-options" role="radiogroup" aria-label="Phương thức thanh toán">
    <label class="payment-option" :class="{ 'payment-option--active': model === 'COD' }">
      <input v-model="model" type="radio" value="COD" class="payment-option__radio" />
      <span class="payment-option__icon payment-option__icon--cod">
        <i class="fas fa-hand-holding-dollar"></i>
      </span>
      <span class="payment-option__body">
        <span class="payment-option__name">
          {{ isStorePickup ? 'Thanh toán tại cửa hàng' : 'Thanh toán khi nhận hàng (COD)' }}
        </span>
        <span class="payment-option__desc">
          {{
            isStorePickup
              ? 'Thanh toán khi đến nhận sản phẩm tại Marcus Store'
              : 'Thanh toán cho nhân viên giao hàng sau khi nhận kiện hàng'
          }}
        </span>
        <span class="payment-option__meta">
          <i class="fas fa-circle-check"></i>
          {{ isStorePickup ? 'Không phát sinh phí thanh toán' : 'Đơn giản, không cần trả trước' }}
        </span>
      </span>
      <span class="payment-option__check"><i class="fas fa-check-circle"></i></span>
    </label>

    <label class="payment-option" :class="{ 'payment-option--active': model === 'VNPAY' }">
      <input v-model="model" type="radio" value="VNPAY" class="payment-option__radio" />
      <span class="payment-option__icon payment-option__icon--vnpay">
        <i class="fas fa-shield-halved"></i>
      </span>
      <span class="payment-option__body">
        <span class="payment-option__heading">
          <span class="payment-option__name">Cổng thanh toán VNPAY</span>
          <span class="payment-option__badge">An toàn</span>
        </span>
        <span class="payment-option__desc">
          Thanh toán trên website VNPAY bằng ứng dụng ngân hàng, thẻ nội địa hoặc thẻ quốc tế
        </span>
        <span class="payment-option__meta">
          <i class="fas fa-bolt"></i>
          Trạng thái đơn được cập nhật sau khi VNPAY trả kết quả
        </span>
      </span>
      <span class="payment-option__check"><i class="fas fa-check-circle"></i></span>
    </label>

    <div class="payment-security-note">
      <i class="fas fa-lock"></i>
      <span>Marcus Store không lưu thông tin thẻ hoặc tài khoản ngân hàng của bạn.</span>
    </div>
  </div>
</template>

<script setup>
// Marcus thêm: tách UI thanh toán để Checkout.vue gọn và không lẫn logic VNPAY nguyên bản.
defineProps({
  isStorePickup: {
    type: Boolean,
    default: false,
  },
})

const model = defineModel({
  type: String,
  required: true,
})
</script>
