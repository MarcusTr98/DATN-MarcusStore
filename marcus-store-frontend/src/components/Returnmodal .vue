<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Chính sách đổi trả"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <h4>Thời gian đổi trả</h4>
      <ul>
        <li>Đổi trả trong vòng <strong>30 ngày</strong> kể từ ngày nhận hàng.</li>
      </ul>
      <h4>Điều kiện đổi trả</h4>
      <ul>
        <li>Sản phẩm còn nguyên tem, nhãn mác, chưa qua sử dụng.</li>
        <li>Còn đầy đủ hộp, phụ kiện, quà tặng kèm theo (nếu có).</li>
        <li>Có hóa đơn mua hàng hoặc xác nhận đơn hàng.</li>
      </ul>
      <h4>Các trường hợp được đổi trả</h4>
      <ul>
        <li>Sản phẩm bị lỗi do nhà sản xuất.</li>
        <li>Giao sai mẫu mã, màu sắc, thiếu số lượng so với đơn đặt hàng.</li>
        <li>Sản phẩm bị hư hỏng trong quá trình vận chuyển.</li>
      </ul>
      <h4>Hình thức xử lý</h4>
      <ul>
        <li>Đổi sản phẩm mới cùng loại hoặc hoàn tiền 100% theo yêu cầu khách hàng.</li>
        <li>Miễn phí đổi trả, không cần lý do.</li>
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