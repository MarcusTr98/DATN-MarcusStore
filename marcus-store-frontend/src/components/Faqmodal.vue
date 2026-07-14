<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Câu hỏi thường gặp"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <h4>Làm sao để theo dõi đơn hàng?</h4>
      <ul>
        <li>Kiểm tra trạng thái đơn hàng qua email xác nhận hoặc liên hệ trực tiếp bộ phận hỗ trợ.</li>
      </ul>
      <h4>Tôi có thể hủy đơn hàng không?</h4>
      <ul>
        <li>Có thể hủy trước khi đơn hàng được giao cho đơn vị vận chuyển, bằng cách liên hệ ngay với chúng tôi.</li>
      </ul>
      <h4>Sản phẩm có được kiểm tra trước khi giao không?</h4>
      <ul>
        <li>Tất cả sản phẩm được kiểm tra kỹ trước khi đóng gói và xuất kho.</li>
      </ul>
      <h4>Tôi mua hàng nhưng chưa nhận được hóa đơn?</h4>
      <ul>
        <li>Vui lòng liên hệ bộ phận chăm sóc khách hàng để được gửi lại hóa đơn qua email.</li>
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