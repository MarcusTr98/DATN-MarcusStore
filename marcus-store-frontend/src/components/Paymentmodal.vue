<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Hướng dẫn thanh toán"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <h4>Phương thức thanh toán</h4>
      <ul>
        <li><strong>Thanh toán khi nhận hàng (COD)</strong> — áp dụng cho đơn hàng nội địa.</li>
        <li><strong>Chuyển khoản ngân hàng</strong> — thanh toán trước qua tài khoản ngân hàng của cửa hàng.</li>
        <li><strong>Ví điện tử</strong> — ZaloPay.</li>
        <li><strong>Thẻ tín dụng/ghi nợ</strong> — Visa, Mastercard, JCB.</li>
      </ul>
      <h4>Các bước thanh toán chuyển khoản</h4>
      <ul>
        <li>Đặt hàng và chọn phương thức chuyển khoản khi thanh toán.</li>
        <li>Chuyển khoản theo thông tin được hệ thống cung cấp, ghi rõ mã đơn hàng ở nội dung chuyển khoản.</li>
        <li>Đơn hàng được xử lý sau khi nhận được xác nhận thanh toán.</li>
      </ul>
      <h4>Lưu ý</h4>
      <ul>
        <li>Hóa đơn điện tử sẽ được gửi qua email sau khi đơn hàng hoàn tất.</li>
        <li>Mọi giao dịch được bảo mật theo tiêu chuẩn an toàn thông tin.</li>
      </ul>
    </div>
  </BaseModal>
</template>

<script setup>
import BaseModal from './BaseModal.vue'

defineProps({
  visible: { type: Boolean, default: false },
})
defineEmits(['close'])
</script>

<style scoped>
.policy-content {
  text-align: left;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  max-height: 55vh;
  overflow-y: auto;
  padding-right: 4px;
}

.policy-content h4 {
  color: #1e293b;
  font-size: 14.5px;
  font-weight: 700;
  margin: 16px 0 8px;
}

.policy-content h4:first-child {
  margin-top: 0;
}

.policy-content ul {
  margin: 0 0 10px;
  padding-left: 18px;
}

.policy-content li {
  margin-bottom: 6px;
}

.policy-content strong {
  color: #1e293b;
}
</style>