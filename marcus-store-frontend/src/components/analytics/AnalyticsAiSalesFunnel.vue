<template>
  <section v-if="data" class="ai-sales-funnel analysis-source-host">
    <header>
      <div>
        <span>Hiệu quả Marcus AI</span>
        <h2>Từ tư vấn đến thanh toán thành công</h2>
      </div>
      <AnalysisSourceBadge source="algorithm" />
    </header>
    <div class="ai-sales-funnel__steps">
      <article v-for="step in steps" :key="step.label">
        <strong>{{ format(step.value) }}</strong>
        <span>{{ step.label }}</span>
        <small>{{ step.rate }}</small>
      </article>
    </div>
    <p>
      Funnel chỉ nối các sự kiện phát sinh sau lần hỏi AI đầu tiên trong cùng anonymous journey;
      không lưu nội dung chat hoặc danh tính khách hàng.
    </p>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import AnalysisSourceBadge from './AnalysisSourceBadge.vue'

const props = defineProps({ data: { type: Object, default: null } })
const steps = computed(() => [
  { label: 'Hỏi AI', value: props.data?.questionSessions, rate: 'Điểm bắt đầu' },
  { label: 'AI phản hồi', value: props.data?.responseSessions, rate: `${props.data?.responseRate || 0}% lượt hỏi` },
  { label: 'Hữu ích', value: props.data?.helpfulSessions, rate: `${props.data?.helpfulRate || 0}% phản hồi` },
  { label: 'Click sản phẩm', value: props.data?.clickSessions, rate: `${props.data?.clickRate || 0}% phiên hỏi` },
  { label: 'Checkout', value: props.data?.checkoutSessions, rate: `${props.data?.checkoutRate || 0}% sau click` },
  { label: 'Tạo đơn', value: props.data?.orderSessions, rate: `${props.data?.orderRate || 0}% Checkout` },
  { label: 'Đã thanh toán', value: props.data?.paidSessions, rate: `${props.data?.paidRate || 0}% đơn` },
])
const format = (value) => Number(value || 0).toLocaleString('vi-VN')
</script>

<style scoped>
.ai-sales-funnel{position:relative;margin-top:18px;padding:20px;border:1px solid #c8b5fb;border-radius:18px;background:linear-gradient(145deg,#fff 0%,#faf7ff 100%)}.ai-sales-funnel header{display:flex;justify-content:space-between;padding-right:110px}.ai-sales-funnel header span{color:#6d28d9;font-size:11px;font-weight:800;text-transform:uppercase}.ai-sales-funnel h2{margin:4px 0 14px;font-size:20px}.ai-sales-funnel__steps{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:8px}.ai-sales-funnel article{position:relative;padding:13px 10px;border:1px solid #e6dcff;border-radius:13px;background:#fff}.ai-sales-funnel article:not(:last-child)::after{position:absolute;z-index:2;top:50%;right:-8px;color:#8b5cf6;content:'›';font-size:18px;transform:translateY(-50%)}.ai-sales-funnel strong,.ai-sales-funnel span,.ai-sales-funnel small{display:block}.ai-sales-funnel strong{color:#5b21b6;font-size:18px}.ai-sales-funnel article span{margin-top:3px;color:#1e293b;font-size:11px}.ai-sales-funnel small{margin-top:5px;color:#64748b;font-size:9px}.ai-sales-funnel>p{margin:12px 0 0;color:#64748b;font-size:11px}@media(max-width:1100px){.ai-sales-funnel__steps{grid-template-columns:repeat(4,1fr)}}@media(max-width:700px){.ai-sales-funnel__steps{grid-template-columns:repeat(2,1fr)}}
</style>
