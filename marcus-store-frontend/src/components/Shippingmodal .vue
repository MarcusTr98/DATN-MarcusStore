<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Chính sách giao hàng"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <h4>Khu vực giao hàng</h4>
      <ul>
        <li>Giao hàng toàn quốc, bao gồm các tỉnh thành và khu vực hải đảo.</li>
      </ul>
      <h4>Thời gian giao hàng</h4>
      <ul>
        <li>Nội thành: <strong>1–2 ngày</strong> làm việc.</li>
        <li>Tỉnh thành khác: <strong>3–5 ngày</strong> làm việc.</li>
        <li>Khu vực vùng sâu, vùng xa: <strong>5–7 ngày</strong> làm việc.</li>
      </ul>
      <h4>Phí vận chuyển</h4>
      <ul>
        <li>Miễn phí giao hàng cho đơn hàng từ <strong>500.000đ</strong> trở lên.</li>
        <li>Đơn hàng dưới mức trên áp dụng phí vận chuyển theo bảng giá của đơn vị vận chuyển.</li>
      </ul>
      <h4>Kiểm tra hàng khi nhận</h4>
      <ul>
        <li>Vui lòng kiểm tra tình trạng sản phẩm trước khi thanh toán/ký nhận.</li>
        <li>Liên hệ ngay với chúng tôi nếu phát hiện sản phẩm bị hư hỏng hoặc không đúng đơn đặt hàng.</li>
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