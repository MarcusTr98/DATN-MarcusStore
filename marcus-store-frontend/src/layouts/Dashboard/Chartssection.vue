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
          <h2>Đơn hàng thành công</h2>
          <p>{{ orderChartSubtitle }}</p>
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
  selectedTime: { type: String, default: 'month' },
  compareData:  { type: Object, default: () => ({ current: [], previous: [], currentLabel: '', previousLabel: '' }) },
  weekdayStats: { type: Array,  default: () => [] },
  brandStats:   { type: Array,  default: () => [] },
  newUsersData: { type: Array,  default: () => [] },
  orderStats:   { type: Array,  default: () => [] },
})

const revenueChartRef = ref(null)
const orderChartRef   = ref(null)
const brandChartRef   = ref(null)
const newUserChartRef = ref(null)

let revenueChart = null
let orderChart   = null
let brandChart   = null
let newUserChart = null

const DONUT_COLORS      = ['#2563eb', '#16a34a', '#0891b2', '#f59e0b', '#7c3aed', '#0d9488']
const gridColor         = 'rgba(37, 99, 235, 0.08)'
const tickColor         = '#4b5563'
const COLOR_BLUE        = '#2563eb'
const COLOR_BLUE_LIGHT  = 'rgba(37, 99, 235, 0.12)'
const COLOR_PREV        = '#9ca3af'
const COLOR_GREEN_LIGHT = 'rgba(22, 163, 74, 0.8)'

const chartDefaults = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
}

const brandRevenue = computed(() =>
  props.brandStats.map((item, idx) => ({
    label: item.brand || 'Khác',
    value: item.percentage,
    color: DONUT_COLORS[idx % DONUT_COLORS.length],
  })),
)

const orderChartSubtitle = computed(() => {
  switch (props.selectedTime) {
    case 'today':
    case 'yesterday': return 'Số đơn trong ngày'
    case '7days':     return 'Số đơn 7 ngày qua'
    case '30days':    return 'Số đơn 30 ngày qua'
    case 'week':      return 'Số đơn theo thứ trong tuần'
    case 'year':      return 'Số đơn theo tháng trong năm'
    default:          return 'Số đơn theo ngày trong tháng'
  }
})

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

function reportDateToLabel(reportDate) {
  if (!reportDate) return ''
  const parts = String(reportDate).split('-')
  if (parts.length === 3) return `${parts[2]}/${parts[1]}`
  return reportDate
}

function findTodayIndex(labels, period) {
  const today = new Date()
  const dd = String(today.getDate()).padStart(2, '0')
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const todayShort = `${dd}/${mm}`

  if (period === 'week') {
    const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
    return labels.indexOf(days[today.getDay()])
  }
  if (['month', '7days', '30days', 'today', 'yesterday'].includes(period)) {
    return labels.indexOf(todayShort)
  }
  if (period === 'year') {
    return labels.indexOf(`T${today.getMonth() + 1}`)
  }
  return -1
}

function getNewUserCount(d) {
  return d.totalNewUsers ?? d.newUsers ?? d.count ?? d.total ?? 0
}

function buildRevenueChart() {
  revenueChart = destroyChart(revenueChart)
  if (!revenueChartRef.value || !props.compareData.current?.length) return

  const period = props.selectedTime
  let labels, curValues, prevValues

  if (period === 'week') {
    const ORDER   = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
    const curMap  = Object.fromEntries(props.compareData.current.map(d => [d.label, d]))
    const prevMap = Object.fromEntries(props.compareData.previous.map(d => [d.label, d]))
    labels     = ORDER
    curValues  = ORDER.map(day => curMap[day]?.revenue  ?? 0)
    prevValues = ORDER.map(day => prevMap[day]?.revenue ?? 0)
  } else if (period === 'year') {
    labels     = props.compareData.current.map(d => d.label)
    curValues  = props.compareData.current.map(d => d.revenue)
    prevValues = props.compareData.previous.map(d => d.revenue)
  } else {
    const maxLen = Math.max(props.compareData.current.length, props.compareData.previous.length)
    labels     = props.compareData.current.map(d => d.sublabel ?? d.label)
    while (labels.length < maxLen) labels.push('')
    curValues  = props.compareData.current.map(d => d.revenue)
    prevValues = props.compareData.previous.map(d => d.revenue)
  }

  const todayIdx      = findTodayIndex(labels, period)
  const pointRadiusCur = labels.map((_, i) => i === todayIdx ? 7 : 4)
  const pointStyleCur  = labels.map((_, i) => i === todayIdx ? 'star' : 'circle')

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
          pointBackgroundColor: labels.map((_, i) => i === todayIdx ? '#f59e0b' : COLOR_BLUE),
          pointRadius: pointRadiusCur,
          pointStyle: pointStyleCur,
          tension: 0.4,
          fill: true,
        },
      ],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, maxRotation: 45, minRotation: 0, autoSkip: true, maxTicksLimit: 31 },
          grid: { color: gridColor },
        },
        y: {
          min: 0,
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, callback: (v) => formatShortCurrency(v) },
          grid: { color: gridColor },
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
          labels: { color: '#374151', font: { size: 11, weight: '700' }, boxWidth: 14 },
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
  if (!orderChartRef.value) return

  const period = props.selectedTime
  let labels = []
  let data   = []

  if (period === 'week') {
    const ORDER = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
    const map   = {}
    props.weekdayStats.forEach(d => {
      const key = d.dayLabel.replace('Thứ ', 'T').replace('Chủ nhật', 'CN')
      map[key] = d.totalOrders
    })
    labels = ORDER
    data   = ORDER.map(day => map[day] ?? 0)

  } else if (period === 'year') {
    const monthMap = {}
    for (let m = 1; m <= 12; m++) monthMap[`T${m}`] = 0
    props.orderStats.forEach(d => {
      if (d.reportDate) {
        const month = parseInt(String(d.reportDate).split('-')[1])
        monthMap[`T${month}`] += d.totalOrders ?? 0
      }
    })
    labels = Object.keys(monthMap)
    data   = Object.values(monthMap)

  } else {
    labels = props.orderStats.map(d => reportDateToLabel(d.reportDate))
    data   = props.orderStats.map(d => d.totalOrders ?? 0)
  }

  const todayIdx = findTodayIndex(labels, period)
  const bgColors = labels.map((_, i) => i === todayIdx ? '#f59e0b' : COLOR_GREEN_LIGHT)

  orderChart = new Chart(orderChartRef.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: bgColors,
        borderRadius: 10,
        borderSkipped: false,
        barThickness: labels.length <= 2 ? 60 : undefined,
        maxBarThickness: 80,
      }],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, maxRotation: 45, minRotation: 0, autoSkip: true, maxTicksLimit: 31 },
          grid: { display: false },
        },
        y: {
          min: 0,
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, stepSize: 1, precision: 0 },
          grid: { color: gridColor },
          title: { display: true, text: 'Số đơn', color: tickColor, font: { size: 11, weight: '600' } },
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
  let labels, data

  if (isYear) {
    labels = props.newUsersData.map(d => {
      const r = d.registerDate ?? ''
      // "T6/2026" → "T6"
      return r.includes('/') ? r.split('/')[0] : r
    })
    data = props.newUsersData.map(d => getNewUserCount(d))
  } else {
   
    labels = props.newUsersData.map(d => {
      const dateStr = d.registerDate ?? ''
      if (!dateStr) return ''
      const parts = String(dateStr).split('-')
      if (parts.length === 3) return `${parts[2]}/${parts[1]}`
      return dateStr
    })
    data = props.newUsersData.map(d => getNewUserCount(d))
  }

  const todayIdx = findTodayIndex(labels, props.selectedTime)

  newUserChart = new Chart(newUserChartRef.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Tài khoản mới',
        data,
        backgroundColor: labels.map((_, i) => i === todayIdx ? '#f59e0b' : 'rgba(8, 145, 178, 0.8)'),
        borderRadius: 10,
        borderSkipped: false,
        barThickness: labels.length <= 2 ? 60 : undefined,
        maxBarThickness: 80,
      }],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: {
            color: tickColor,
            font: { size: 10, weight: '600' },
            maxRotation: isYear ? 0 : 45,
            minRotation: 0,
            autoSkip: false,
            maxTicksLimit: isYear ? 12 : 31,
          },
          grid: { color: gridColor },
        },
        y: {
          min: 0,
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, stepSize: 1, precision: 0 },
          grid: { color: gridColor },
          title: { display: true, text: 'Tài khoản', color: tickColor, font: { size: 11, weight: '600' } },
        },
      },
      plugins: {
        ...chartDefaults.plugins,
        tooltip: { callbacks: { label: (ctx) => ` ${ctx.parsed.y} tài khoản` } },
      },
    },
  })
}

function buildAllCharts() {
  nextTick(() => {
    buildRevenueChart()
    buildOrderChart()
    buildBrandChart()
    buildNewUserChart()
  })
}

watch(
  () => [props.compareData, props.weekdayStats, props.orderStats, props.brandStats, props.newUsersData],
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