<template>
  <section class="analytics-panel cancellation-reasons">
    <header class="analytics-panel__header">
      <div>
        <span class="analytics-panel__eyebrow">Chất lượng đơn hàng</span>
        <h2>Lý do hủy đơn</h2>
        <p>Mỗi đơn được tính một lần theo thời điểm hủy; ghi chú đã được gom nhóm an toàn.</p>
      </div>
      <span class="cancellation-reasons__total">{{ total }} đơn hủy</span>
    </header>

    <div v-if="reasons.length" class="cancellation-reasons__list">
      <article v-for="item in reasons" :key="item.reason" class="cancellation-reason">
        <div class="cancellation-reason__top">
          <strong>{{ item.reason }}</strong>
          <span>{{ item.currentCount }} đơn · {{ formatPercent(item.sharePercent) }}</span>
        </div>
        <div class="cancellation-reason__track">
          <span :style="{ width: `${Math.max(item.sharePercent, 2)}%` }"></span>
        </div>
        <small :class="changeClass(item.changePercent)">
          {{ changeLabel(item.changePercent) }}
        </small>
      </article>
    </div>
    <div v-else class="cancellation-reasons__empty">
      <i class="bi bi-check2-circle"></i>
      Không có đơn hủy trong kỳ đang chọn.
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  reasons: { type: Array, default: () => [] },
})

const total = computed(() =>
  props.reasons.reduce((sum, item) => sum + Number(item.currentCount || 0), 0),
)

function formatPercent(value) {
  return `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`
}

function changeLabel(value) {
  if (value === null || value === undefined) return 'Kỳ trước chưa phát sinh'
  if (Math.abs(value) < 0.01) return 'Không đổi so với kỳ trước'
  return `${value > 0 ? 'Tăng' : 'Giảm'} ${Math.abs(value).toLocaleString('vi-VN', {
    maximumFractionDigits: 1,
  })}% so với kỳ trước`
}

function changeClass(value) {
  if (value === null || value === undefined || Math.abs(value) < 0.01) return 'is-neutral'
  return value > 0 ? 'is-negative' : 'is-positive'
}
</script>
