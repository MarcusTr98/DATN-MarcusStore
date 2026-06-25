<template>
  <section class="dashboard-grid">

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Xu hướng doanh thu</h2>
          <p>{{ compareData.currentLabel }} so với {{ compareData.previousLabel }}</p>
        </div>
        <span>Line chart</span>
      </div>
      <div class="chart-frame">
        <canvas ref="revenueChartRef"></canvas>
      </div>
    </article>

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Đơn hàng phát sinh</h2>
          <p>Số lượng đơn theo thứ trong tuần</p>
        </div>
        <span>Bar chart</span>
      </div>
      <div class="chart-frame">
        <canvas ref="orderChartRef"></canvas>
      </div>
    </article>

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Doanh thu theo thương hiệu</h2>
          <p>Tỷ lệ % theo thương hiệu</p>
        </div>
        <span>Doughnut</span>
      </div>
      <div class="donut-wrap">
        <div class="chart-frame donut-canvas">
          <canvas ref="brandChartRef"></canvas>
        </div>
        <div class="legend-list">
          <span v-for="item in brandRevenue" :key="item.label">
            <i :style="{ backgroundColor: item.color }"></i>
            {{ item.label }} {{ item.value }}%
          </span>
        </div>
      </div>
    </article>

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Tài khoản mới đăng ký</h2>
          <p>Số lượng theo ngày trong kỳ</p>
        </div>
        <span>Line chart</span>
      </div>
      <div class="chart-frame">
        <canvas ref="newUserChartRef"></canvas>
      </div>
    </article>

  </section>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const props = defineProps({
  selectedTime:  { type: String, default: 'month' },
  compareData:   { type: Object, default: () => ({ current: [], previous: [], currentLabel: '', previousLabel: '' }) },
  weekdayStats:  { type: Array,  default: () => [] },
  brandStats:    { type: Array,  default: () => [] },
  newUsersData:  { type: Array,  default: () => [] },
})

// ── canvas refs ──────────────────────────────────────────────
const revenueChartRef = ref(null)
const orderChartRef   = ref(null)
const brandChartRef   = ref(null)
const newUserChartRef = ref(null)

let revenueChart = null
let orderChart   = null
let brandChart   = null
let newUserChart = null

// ── constants ────────────────────────────────────────────────
const DONUT_COLORS = ['#2563eb', '#16a34a', '#0891b2', '#f59e0b', '#7c3aed', '#0d9488']
const gridColor    = 'rgba(37, 99, 235, 0.08)'
const tickColor    = '#9ca3af'
const COLOR_BLUE       = '#2563eb'
const COLOR_BLUE_LIGHT = 'rgba(37, 99, 235, 0.12)'
const COLOR_TEAL       = '#0891b2'
const COLOR_PREV       = '#9ca3af'

const chartDefaults = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
}

// ── computed ─────────────────────────────────────────────────
const brandRevenue = computed(() =>
  props.brandStats.map((item, idx) => ({
    label: item.brand || 'Khác',
    value: item.percentage,
    color: DONUT_COLORS[idx % DONUT_COLORS.length],
  })),
)

// ── helpers ──────────────────────────────────────────────────
function destroyChart(c) { if (c) c.destroy(); return null }

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

function formatShortCurrency(value) {
  if (value >= 1_000_000_000) return `${(value / 1_000_000_000).toFixed(1)}T`
  if (value >= 1_000_000)     return `${(value / 1_000_000).toFixed(0)}tr`
  if (value >= 1_000)         return `${(value / 1_000).toFixed(0)}k`
  return String(value)
}

function formatShortDate(dateStr) {
  const d = new Date(dateStr)
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
}

// ── chart builders ───────────────────────────────────────────
function buildRevenueChart() {
  revenueChart = destroyChart(revenueChart)
  if (!revenueChartRef.value || !props.compareData.current?.length) return

  const period = props.selectedTime
  let labels, curValues, prevValues

  if (period === 'week') {
    const ORDER   = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
    const curMap  = Object.fromEntries(props.compareData.current.map(d => [d.label, d]))
    const prevMap = Object.fromEntries(props.compareData.previous.map(d => [d.label, d]))
    const active  = ORDER.filter(day => curMap[day] || prevMap[day])
    labels     = active
    curValues  = active.map(day => curMap[day]?.revenue  ?? 0)
    prevValues = active.map(day => prevMap[day]?.revenue ?? 0)
  } else if (period === 'year') {
    labels     = props.compareData.current.map(d => d.label)
    curValues  = props.compareData.current.map(d => d.revenue)
    prevValues = props.compareData.previous.map(d => d.revenue)
  } else {
    const maxLen = Math.max(props.compareData.current.length, props.compareData.previous.length)
    labels     = Array.from({ length: maxLen }, (_, i) => `Ngày ${i + 1}`)
    curValues  = props.compareData.current.map(d => d.revenue)
    prevValues = props.compareData.previous.map(d => d.revenue)
  }

  revenueChart = new Chart(revenueChartRef.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: props.compareData.previousLabel || 'Kỳ trước',
          data: prevValues,
          borderColor: COLOR_PREV,
backgroundColor: 'transparent',
          borderWidth: 2,
          borderDash: [6, 4],
          pointBackgroundColor: COLOR_PREV,
          pointRadius: 3,
          tension: 0.4,
          fill: false,
        },
        {
          label: props.compareData.currentLabel || 'Kỳ này',
          data: curValues,
          borderColor: COLOR_BLUE,
          backgroundColor: COLOR_BLUE_LIGHT,
          borderWidth: 3,
          pointBackgroundColor: COLOR_BLUE,
          pointRadius: 4,
          tension: 0.4,
          fill: true,
        },
      ],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 10 }, maxRotation: 45, minRotation: 0, autoSkip: true, maxTicksLimit: 31 },
          grid: { color: gridColor },
        },
        y: {
          ticks: { color: tickColor, font: { size: 11 }, callback: (v) => formatShortCurrency(v) },
          grid: { color: gridColor },
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
          labels: { color: '#6b7280', font: { size: 11 }, boxWidth: 14 },
        },
        tooltip: {
          callbacks: {
            title: (items) => {
              const idx    = items[0].dataIndex
              const isPrev = items[0].datasetIndex === 0
              const source = isPrev ? props.compareData.previous[idx] : props.compareData.current[idx]
              if (!source) return items[0].label
              if (period === 'year') return source.label ?? items[0].label
              if (period === 'week') return source.sublabel ? `${source.label} (${source.sublabel})` : source.label
              return source.sublabel ?? source.label ?? items[0].label
            },
            label: (ctx) => ` ${ctx.dataset.label}: ${formatCurrency(ctx.parsed.y)}`,
          },
        },
      },
    },
  })
}

function buildOrderChart() {
  orderChart = destroyChart(orderChart)
  if (!orderChartRef.value || !props.weekdayStats.length) return

  const labels = props.weekdayStats.map(d => d.dayLabel.replace('Thứ ', 'T').replace('Chủ nhật', 'CN'))
  const data   = props.weekdayStats.map(d => d.totalOrders)

  orderChart = new Chart(orderChartRef.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: 'rgba(22, 163, 74, 0.8)',
        borderRadius: 10,
        borderSkipped: false,
      }],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: { ticks: { color: tickColor, font: { size: 11 } }, grid: { display: false } },
        y: {
          ticks: { color: tickColor, font: { size: 11 }, stepSize: 1 },
          grid: { color: gridColor },
          title: { display: true, text: 'Số đơn', color: tickColor, font: { size: 11 } },
        },
      },
      plugins: {
        ...chartDefaults.plugins,
        tooltip: { callbacks: { label: (ctx) => ` ${ctx.parsed.y} đơn` } },
      },
    },
})
}

function buildBrandChart() {
  brandChart = destroyChart(brandChart)
  if (!brandChartRef.value || !props.brandStats.length) return

  brandChart = new Chart(brandChartRef.value, {
    type: 'doughnut',
    data: {
      labels: brandRevenue.value.map(b => b.label),
      datasets: [{
        data:            brandRevenue.value.map(b => b.value),
        backgroundColor: brandRevenue.value.map(b => b.color),
        borderWidth: 2,
        borderColor: '#fff',
      }],
    },
    options: {
      ...chartDefaults,
      cutout: '68%',
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (ctx) => ` ${ctx.label}: ${ctx.parsed}%` } },
      },
    },
  })
}

function buildNewUserChart() {
  newUserChart = destroyChart(newUserChart)
  if (!newUserChartRef.value || !props.newUsersData.length) return

  const isYear = props.selectedTime === 'year'
  const labels = props.newUsersData.map(d => isYear ? d.registerDate : formatShortDate(d.registerDate))
  const data   = props.newUsersData.map(d => d.totalNewUsers)

  newUserChart = new Chart(newUserChartRef.value, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: 'Tài khoản mới',
        data,
        borderColor: COLOR_TEAL,
        backgroundColor: 'rgba(8, 145, 178, 0.12)',
        borderWidth: 3,
        pointBackgroundColor: COLOR_TEAL,
        pointRadius: 4,
        tension: 0.4,
        fill: true,
      }],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: {
            color: tickColor,
            font: { size: 10 },
            maxRotation: isYear ? 0 : 45,
            minRotation: 0,
            autoSkip: false,
            maxTicksLimit: isYear ? 12 : 31,
          },
          grid: { color: gridColor },
        },
        y: {
          ticks: { color: tickColor, font: { size: 11 }, stepSize: 1 },
          grid: { color: gridColor },
          title: { display: true, text: 'Tài khoản', color: tickColor, font: { size: 11 } },
        },
      },
      plugins: {
        ...chartDefaults.plugins,
        tooltip: { callbacks: { label: (ctx) => ` ${ctx.parsed.y} tài khoản` } },
      },
    },
  })
}

// ── rebuild khi props thay đổi ───────────────────────────────
function buildAllCharts() {
  nextTick(() => {
    buildRevenueChart()
    buildOrderChart()
    buildBrandChart()
    buildNewUserChart()
  })
}

watch(
  () => [props.compareData, props.weekdayStats, props.brandStats, props.newUsersData],
  () => buildAllCharts(),
  { deep: true },
)

onBeforeUnmount(() => {
  revenueChart = destroyChart(revenueChart)
  orderChart   = destroyChart(orderChart)
  brandChart   = destroyChart(brandChart)
  newUserChart = destroyChart(newUserChart)
})
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  width: 100%;
}

.dashboard-card {
  background: #fff;
border: 1px solid #ffe0ec;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.06);
  width: 100%;
  min-width: 0;
  border-radius: 24px;
  padding: 22px;
  box-sizing: border-box;
}

.dashboard-card h2 {
  margin: 0;
  color: #111827;
  font-weight: 900;
  font-size: 20px;
}

.dashboard-card p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.card-header > span {
  padding: 5px 10px;
  border-radius: 999px;
  background: #fff2f7;
  color: #e11d65;
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.chart-frame {
  height: 400px;
  position: relative;
}

.chart-frame canvas {
  width: 100% !important;
  height: 100% !important;
}

.donut-wrap {
  min-height: 400px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  align-items: center;
  gap: 20px;
}

.donut-canvas {
  height: 340px !important;
}

.legend-list {
  display: grid;
  gap: 10px;
}

.legend-list span {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #374151;
  font-size: 13px;
  font-weight: 800;
}

.legend-list i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: none;
}

@media (max-width: 1400px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .donut-wrap {
    grid-template-columns: 1fr;
    justify-items: center;
  }
}
</style>