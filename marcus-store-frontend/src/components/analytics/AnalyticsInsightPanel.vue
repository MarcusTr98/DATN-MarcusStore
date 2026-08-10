<template>
  <section
    class="analytics-insight-panel analysis-source-host"
    :class="`analytics-insight-panel--${analysis.status.key}`"
  >
    <!-- Marcus thêm: kết luận tại panel này do công thức so sánh KPI, không phải Gemini. -->
    <AnalysisSourceBadge source="algorithm" />
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
        <span v-if="forecast.backtest?.available">
          Kiểm thử {{ forecast.backtest.testedPeriods }} kỳ · sai số
          {{ formatNumber(forecast.backtest.mape) }}%
        </span>
      </div>
    </div>

    <div v-if="forecast?.anomalies?.length" class="analytics-anomaly-list">
      <strong><i class="bi bi-exclamation-diamond"></i> Kỳ dữ liệu bất thường</strong>
      <span v-for="point in forecast.anomalies" :key="point.label">
        {{ formatDate(point.label) }}: thực tế {{ formatMoney(point.actual) }}, lệch đáng kể khỏi xu hướng.
      </span>
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
import AnalysisSourceBadge from '@/components/analytics/AnalysisSourceBadge.vue'

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

function formatMoney(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')} VND`
}

function formatDate(value) {
  if (!value) return '—'
  const normalized = value.length === 7 ? `${value}-01` : value
  return new Intl.DateTimeFormat('vi-VN', { month: '2-digit', year: 'numeric', day: value.length === 7 ? undefined : '2-digit' })
    .format(new Date(`${normalized}T00:00:00`))
}
</script>

<style scoped>
.analytics-anomaly-list{display:flex;flex-wrap:wrap;gap:8px 16px;padding:12px 20px;border-top:1px solid rgba(219,39,119,.13);color:#53657d;font-size:13px}.analytics-anomaly-list strong{color:#b42352}.analytics-anomaly-list span{padding-left:12px;border-left:2px solid #f3a9bf}
</style>
