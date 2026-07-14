<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Liên hệ với chúng tôi"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <ul>
        <li><strong>Hotline:</strong> {{ hotline }}</li>
        <li><strong>Email:</strong> {{ email }}</li>
        <li><strong>Địa chỉ:</strong> {{ address }}</li>
        <li><strong>Giờ làm việc:</strong> {{ workingHours }}, tất cả các ngày trong tuần</li>
      </ul>
      <h4>Kênh hỗ trợ khác</h4>
      <ul>
        <li>Fanpage Facebook: phản hồi trong vòng 30 phút trong giờ làm việc.</li>
        <li>Zalo OA: hỗ trợ tư vấn nhanh và gửi hình ảnh sản phẩm lỗi.</li>
      </ul>
    </div>
  </BaseModal>
</template>

<script setup>
import BaseModal from './BaseModal.vue'

defineProps({
  visible: { type: Boolean, default: false },
  hotline: { type: String, default: '1800 6975' },
  email: { type: String, default: 'support@marcus.com' },
  address: { type: String, default: 'Đang cập nhật địa chỉ' },
  workingHours: { type: String, default: '08:00 – 21:00' },
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