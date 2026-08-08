<template>
  <article class="analytics-panel analytics-forecast-chart analysis-source-host">
    <!-- Marcus thêm: dự báo hồi quy tuyến tính được ghi rõ là Thuật toán. -->
    <AnalysisSourceBadge source="algorithm" />
    <div class="analytics-panel__header">
      <div>
        <span class="analytics-panel__eyebrow">Dự báo thống kê</span>
        <h2>Ước tính doanh thu {{ forecast.future.length }} {{ forecast.unit }} tiếp theo</h2>
        <p>Dựa trên hồi quy xu hướng của các kỳ gần nhất.</p>
      </div>
      <div class="analytics-forecast-total">
        <small>Tổng dự báo</small>
        <strong>{{ formatMoney(forecast.forecastTotal) }}</strong>
        <span>Độ tin cậy {{ forecast.confidence.toLowerCase() }}</span>
      </div>
    </div>

    <apexchart height="300" type="line" :options="chartOptions" :series="series" />

    <div class="analytics-forecast-disclaimer">
      <i class="bi bi-info-circle"></i>
      Đây là ngoại suy thống kê, không phải cam kết doanh thu. Khuyến mãi, tồn kho và thị trường
      thực tế có thể làm kết quả thay đổi.
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import AnalysisSourceBadge from '@/components/analytics/AnalysisSourceBadge.vue'

const props = defineProps({
  forecast: { type: Object, required: true },
})

const money = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })

const series = computed(() => [
  {
    name: 'Dự báo trung tâm',
    data: props.forecast.future.map((point) => point.predictedSales),
  },
  {
    name: 'Biên trên',
    data: props.forecast.future.map((point) => point.upperBound),
  },
  {
    name: 'Biên dưới',
    data: props.forecast.future.map((point) => point.lowerBound),
  },
])

const chartOptions = computed(() => ({
  chart: { toolbar: { show: false }, zoom: { enabled: false }, fontFamily: 'inherit' },
  colors: ['#7047c7', '#b8a2e8', '#b8a2e8'],
  dataLabels: { enabled: false },
  grid: { borderColor: '#e8eef5', strokeDashArray: 4 },
  legend: { position: 'top', horizontalAlign: 'right', fontSize: '13px' },
  markers: { size: [4, 0, 0] },
  stroke: { curve: 'smooth', width: [3, 1.5, 1.5], dashArray: [0, 5, 5] },
  xaxis: {
    categories: props.forecast.future.map((point) => formatDate(point.label)),
    axisBorder: { show: false },
    axisTicks: { show: false },
  },
  yaxis: {
    min: 0,
    title: { text: 'Doanh thu dự báo (VND)' },
    labels: {
      formatter: (value) => `${money.format(value)} ₫`,
      style: { fontSize: '13px' },
    },
  },
  tooltip: {
    shared: true,
    intersect: false,
    y: { formatter: (value) => `${money.format(value)} VND` },
  },
}))

function formatMoney(value) {
  return `${money.format(Number(value || 0))} VND`
}

function formatDate(value) {
  const [year, month, day] = value.split('-')
  return props.forecast.unit === 'tháng' ? `T${Number(month)}/${year}` : `${day}/${month}`
}
</script>
