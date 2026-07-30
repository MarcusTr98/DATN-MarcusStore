<template>
  <article class="analytics-panel analytics-sales-chart">
    <div class="analytics-panel__header">
      <div>
        <span class="analytics-panel__eyebrow">Xu hướng theo {{ monthly ? 'tháng' : 'ngày' }}</span>
        <h2>Doanh thu đã thu và lượng đơn hoàn tất</h2>
      </div>
      <div class="analytics-chart-legend">
        <span><i class="analytics-chart-legend__sales"></i>Doanh thu đã thu</span>
        <span><i class="analytics-chart-legend__orders"></i>Đơn hàng</span>
      </div>
    </div>

    <apexchart
      v-if="trend.length"
      height="360"
      type="line"
      :options="chartOptions"
      :series="series"
    />
    <div v-else class="analytics-empty">
      <i class="bi bi-graph-up"></i>
      <p>Chưa có doanh thu hoàn tất trong khoảng này.</p>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  trend: { type: Array, required: true },
  monthly: { type: Boolean, default: false },
})

const moneyFormatter = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })

const series = computed(() => [
  {
    name: 'Doanh thu đã thu',
    type: 'area',
    data: props.trend.map((point) => Number(point.completedSales || 0)),
  },
  {
    name: 'Đơn hoàn tất',
    type: 'line',
    data: props.trend.map((point) => Number(point.completedOrders || 0)),
  },
])

const chartOptions = computed(() => ({
  chart: {
    toolbar: { show: false },
    zoom: { enabled: false },
    fontFamily: 'inherit',
  },
  colors: ['#1769ca', '#13a471'],
  dataLabels: { enabled: false },
  fill: {
    type: ['gradient', 'solid'],
    gradient: {
      shadeIntensity: 0.2,
      opacityFrom: 0.28,
      opacityTo: 0.03,
      stops: [0, 95],
    },
  },
  grid: {
    borderColor: '#e8eef5',
    strokeDashArray: 4,
    padding: { left: 8, right: 10 },
  },
  legend: { show: false },
  markers: { size: props.trend.length <= 31 ? 3 : 0, hover: { sizeOffset: 3 } },
  stroke: { curve: 'smooth', width: [3, 2.5] },
  xaxis: {
    categories: props.trend.map((point) => formatLabel(point.label)),
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      rotate: 0,
      hideOverlappingLabels: true,
      style: { colors: '#718096', fontSize: '13px' },
    },
  },
  yaxis: [
    {
      title: { text: 'Doanh thu (VND)', style: { color: '#64748b', fontWeight: 600 } },
      labels: {
        formatter: (value) => `${moneyFormatter.format(value)} ₫`,
        style: { colors: '#718096', fontSize: '13px' },
      },
    },
    {
      opposite: true,
      min: 0,
      forceNiceScale: true,
      title: { text: 'Đơn hàng', style: { color: '#64748b', fontWeight: 600 } },
      labels: {
        formatter: (value) => Math.round(value).toLocaleString('vi-VN'),
        style: { colors: '#718096', fontSize: '13px' },
      },
    },
  ],
  tooltip: {
    shared: true,
    intersect: false,
    y: [
      { formatter: (value) => `${moneyFormatter.format(value)} VND` },
      { formatter: (value) => `${Math.round(value).toLocaleString('vi-VN')} đơn` },
    ],
  },
}))

function formatLabel(value) {
  if (!value) return ''
  if (props.monthly) {
    const [year, month] = value.split('-')
    return `T${Number(month)}/${year}`
  }
  const [, month, day] = value.split('-')
  return `${day}/${month}`
}
</script>
