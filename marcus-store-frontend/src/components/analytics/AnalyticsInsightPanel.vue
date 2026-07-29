<template>
  <section
    class="analytics-insight-panel"
    :class="`analytics-insight-panel--${analysis.status.key}`"
  >
    <div class="analytics-insight-panel__summary">
      <span class="analytics-insight-panel__status-icon">
        <i :class="analysis.status.icon"></i>
      </span>
      <div>
        <span class="analytics-panel__eyebrow">Kết luận điều hành</span>
        <h2>{{ analysis.status.label }}</h2>
        <p>Kết luận dựa trên doanh thu, số đơn hoàn tất và tỷ lệ hủy khi so với kỳ liền trước.</p>
      </div>
      <div v-if="forecast" class="analytics-insight-panel__forecast">
        <small>Dự báo {{ forecast.future.length }} {{ forecast.unit }} tới</small>
        <strong :class="{ negative: forecast.changePercent < 0 }">
          {{ formatChange(forecast.changePercent) }}
        </strong>
        <span>Độ tin cậy: {{ forecast.confidence }}</span>
      </div>
    </div>

    <div class="analytics-insight-grid">
      <article
        v-for="insight in analysis.insights"
        :key="insight.title"
        :class="`analytics-insight analytics-insight--${insight.tone}`"
      >
        <i :class="insight.icon"></i>
        <div>
          <strong>{{ insight.title }}</strong>
          <p>{{ insight.text }}</p>
        </div>
      </article>
    </div>

    <div
      v-if="analysis.risingProduct || analysis.decliningProduct"
      class="analytics-product-signals"
    >
      <div v-if="analysis.risingProduct">
        <span class="up"><i class="bi bi-fire"></i> Đang tăng tốt</span>
        <strong>{{ analysis.risingProduct.productName }}</strong>
        <small>+{{ formatNumber(analysis.risingProduct.unitsChangePercent) }}% số lượng</small>
      </div>
      <div v-if="analysis.decliningProduct">
        <span class="down"><i class="bi bi-graph-down"></i> Cần theo dõi</span>
        <strong>{{ analysis.decliningProduct.productName }}</strong>
        <small>{{ formatNumber(analysis.decliningProduct.unitsChangePercent) }}% số lượng</small>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  analysis: { type: Object, required: true },
  forecast: { type: Object, default: null },
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })
}

function formatChange(value) {
  if (value === null || value === undefined) return 'Chưa đủ dữ liệu'
  const sign = value >= 0 ? '+' : ''
  return `${sign}${formatNumber(value)}%`
}
</script>
