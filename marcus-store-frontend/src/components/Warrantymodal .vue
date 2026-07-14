<template>
  <BaseModal
    :visible="visible"
    type="info"
    title="Chính sách bảo hành"
    @close="$emit('close')"
  >
    <div class="policy-content">
      <h4>Thời gian bảo hành</h4>
      <ul>
        <li>Sản phẩm được bảo hành <strong>12 tháng</strong> kể từ ngày mua hàng, theo hóa đơn hoặc phiếu bảo hành.</li>
        <li>Phụ kiện đi kèm (cáp, sạc, tai nghe...) bảo hành <strong>6 tháng</strong>.</li>
      </ul>
      <h4>Điều kiện bảo hành</h4>
      <ul>
        <li>Còn trong thời hạn bảo hành, có phiếu hoặc hóa đơn hợp lệ.</li>
        <li>Lỗi phát sinh do nhà sản xuất, không do tác động vật lý hoặc ngập nước.</li>
        <li>Tem, số serial còn nguyên vẹn, không bị tẩy xóa.</li>
      </ul>
      <h4>Trường hợp không được bảo hành</h4>
      <ul>
        <li>Sản phẩm hết thời hạn bảo hành.</li>
        <li>Hư hỏng do rơi vỡ, va đập, cháy nổ, thiên tai.</li>
        <li>Tự ý sửa chữa, can thiệp bởi bên thứ ba không được ủy quyền.</li>
      </ul>
      <h4>Quy trình bảo hành</h4>
      <ul>
        <li>Mang sản phẩm và phiếu bảo hành đến cửa hàng gần nhất hoặc gửi qua bưu điện.</li>
        <li>Thời gian xử lý trung bình 3–7 ngày làm việc.</li>
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