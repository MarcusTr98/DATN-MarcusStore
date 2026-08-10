<template>
  <section v-if="data" class="ai-sales-funnel analysis-source-host">
    <header>
      <div>
        <span>Chuyển đổi từ tư vấn AI</span>
        <h2>Hành trình khách hàng sau khi nhận tư vấn</h2>
      </div>
      <AnalysisSourceBadge
        source="algorithm"
        detail="Tỷ lệ chuyển bước = số phiên đạt bước sau / số phiên ở bước trước × 100%. Các bước được nối bằng cùng mã phiên ẩn danh."
      />
    </header>
    <div class="ai-sales-funnel__steps">
      <article v-for="step in steps" :key="step.label">
        <strong>{{ format(step.value) }}</strong>
        <span>{{ step.label }}</span>
        <small>{{ step.rate }}</small>
      </article>
    </div>
    <p>
      Các bước được nối bằng mã phiên ẩn danh sau lần hỏi AI đầu tiên; hệ thống không lưu nội dung
      trò chuyện hoặc danh tính khách hàng.
    </p>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import AnalysisSourceBadge from './AnalysisSourceBadge.vue'

const props = defineProps({ data: { type: Object, default: null } })
const steps = computed(() => [
  { label: 'Khách hỏi AI', value: props.data?.questionSessions, rate: 'Bắt đầu hành trình' },
  {
    label: 'Nhận được tư vấn',
    value: props.data?.responseSessions,
    rate: `${props.data?.responseRate || 0}% lượt hỏi`,
  },
  {
    label: 'Đánh giá hữu ích',
    value: props.data?.helpfulSessions,
    rate: `${props.data?.helpfulRate || 0}% phản hồi`,
  },
  {
    label: 'Xem sản phẩm gợi ý',
    value: props.data?.clickSessions,
    rate: `${props.data?.clickRate || 0}% phiên hỏi`,
  },
  {
    label: 'Vào Checkout',
    value: props.data?.checkoutSessions,
    rate: `${props.data?.checkoutRate || 0}% sau khi xem`,
  },
  {
    label: 'Đặt hàng',
    value: props.data?.orderSessions,
    rate: `${props.data?.orderRate || 0}% Checkout`,
  },
  {
    label: 'Thanh toán thành công',
    value: props.data?.paidSessions,
    rate: `${props.data?.paidRate || 0}% đơn đã tạo`,
  },
])
const format = (value) => Number(value || 0).toLocaleString('vi-VN')
</script>

<style scoped>
.ai-sales-funnel {
  position: relative;
  margin-top: 18px;
  padding: 20px;
  border: 1px solid #c8b5fb;
  border-radius: 18px;
  background: linear-gradient(145deg, #fff 0%, #faf7ff 100%);
}
.ai-sales-funnel header {
  display: flex;
  justify-content: space-between;
  padding-right: 110px;
}
.ai-sales-funnel header span {
  color: #6d28d9;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}
.ai-sales-funnel h2 {
  margin: 4px 0 14px;
  font-size: 20px;
}
.ai-sales-funnel__steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.ai-sales-funnel article {
  position: relative;
  min-height: 94px;
  padding: 15px 13px;
  border: 1px solid #e6dcff;
  border-radius: 13px;
  background: #fff;
}
.ai-sales-funnel strong,
.ai-sales-funnel span,
.ai-sales-funnel small {
  display: block;
}
.ai-sales-funnel strong {
  color: #5b21b6;
  font-size: 20px;
}
.ai-sales-funnel article span {
  margin-top: 5px;
  color: #1e293b;
  font-size: 12px;
  font-weight: 700;
}
.ai-sales-funnel small {
  margin-top: 6px;
  color: #64748b;
  font-size: 10px;
}
.ai-sales-funnel > p {
  margin: 12px 0 0;
  color: #64748b;
  font-size: 11px;
}
@media (max-width: 900px) {
  .ai-sales-funnel__steps {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 540px) {
  .ai-sales-funnel__steps {
    grid-template-columns: 1fr;
  }
}
</style>
