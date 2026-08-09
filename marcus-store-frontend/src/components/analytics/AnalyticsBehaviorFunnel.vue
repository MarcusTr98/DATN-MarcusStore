<template>
  <section v-if="data" class="behavior-funnel">
    <header><div><span>Hành vi ẩn danh</span><h2>Funnel các điểm chạm Marcus sở hữu</h2></div><AnalysisSourceBadge source="algorithm" /></header>
    <div class="behavior-funnel__steps">
      <article v-for="step in steps" :key="step.label"><strong>{{ format(step.value) }}</strong><span>{{ step.label }}</span><small>{{ step.rate }}</small></article>
    </div>
    <p>Không ghi ADD_TO_CART vì Cart là module thành viên. Funnel dùng UUID phiên, không lưu nội dung, IP, token hoặc userId.</p>
  </section>
</template>
<script setup>
import { computed } from 'vue'
import AnalysisSourceBadge from './AnalysisSourceBadge.vue'
const props = defineProps({ data: { type: Object, default: null } })
const steps = computed(() => [
  { label: 'Xem sản phẩm', value: props.data?.productViewSessions, rate: 'Điểm bắt đầu' },
  { label: 'Bắt đầu Checkout', value: props.data?.checkoutSessions, rate: `${props.data?.viewToCheckoutRate || 0}% từ lượt xem` },
  { label: 'Tạo đơn', value: props.data?.orderSessions, rate: `${props.data?.checkoutToOrderRate || 0}% từ Checkout` },
  { label: 'Thanh toán thành công', value: props.data?.paidSessions, rate: `${props.data?.orderToPaymentRate || 0}% từ tạo đơn` },
  { label: 'Hỏi AI → click SP', value: props.data?.aiProductClickSessions, rate: `${props.data?.aiClickRate || 0}% phiên hỏi AI` },
])
const format = (value) => Number(value || 0).toLocaleString('vi-VN')
</script>
<style scoped>
.behavior-funnel{margin-top:18px;padding:20px;border:1px solid #cfe1f7;border-radius:18px;background:#fff;position:relative}.behavior-funnel header{position:relative;display:flex;justify-content:space-between;padding-right:110px}.behavior-funnel header span{color:#1763b0;font-size:11px;font-weight:800;text-transform:uppercase}.behavior-funnel h2{margin:4px 0 14px;font-size:20px}.behavior-funnel__steps{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:9px}.behavior-funnel article{padding:13px;border-radius:13px;background:#f4f8fd;border:1px solid #dbe9f8}.behavior-funnel strong,.behavior-funnel span,.behavior-funnel small{display:block}.behavior-funnel strong{font-size:19px;color:#0f4f91}.behavior-funnel article span{margin-top:3px;color:#1e293b;font-size:12px;text-transform:none}.behavior-funnel small{margin-top:5px;color:#64748b;font-size:10px}.behavior-funnel>p{margin:12px 0 0;color:#64748b;font-size:11px}@media(max-width:900px){.behavior-funnel__steps{grid-template-columns:repeat(2,1fr)}}
</style>
