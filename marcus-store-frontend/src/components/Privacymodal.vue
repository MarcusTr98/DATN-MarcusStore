<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Bảo mật thông tin"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <h4>Mục đích thu thập thông tin</h4>
      <ul>
        <li>Xử lý đơn hàng, giao nhận và chăm sóc khách hàng.</li>
        <li>Gửi thông tin khuyến mãi, ưu đãi khi khách hàng đồng ý đăng ký nhận tin.</li>
      </ul>
      <h4>Phạm vi sử dụng thông tin</h4>
      <ul>
        <li>Thông tin cá nhân chỉ được sử dụng nội bộ, không chia sẻ cho bên thứ ba khi chưa có sự đồng ý của khách hàng.</li>
        <li>Trường hợp ngoại lệ: theo yêu cầu của cơ quan nhà nước có thẩm quyền.</li>
      </ul>
      <h4>Cam kết bảo mật</h4>
      <ul>
        <li>Hệ thống áp dụng các biện pháp kỹ thuật để bảo vệ dữ liệu khách hàng khỏi truy cập trái phép.</li>
        <li>Khách hàng có quyền yêu cầu chỉnh sửa hoặc xóa thông tin cá nhân bất kỳ lúc nào.</li>
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