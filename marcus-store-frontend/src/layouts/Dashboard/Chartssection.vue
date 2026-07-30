<template>
  <section class="dashboard-grid">

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Xu hướng doanh thu</h2>
          <p>{{ compareData.currentLabel }} so với {{ compareData.previousLabel }}</p>
        </div>
        <span>Biểu đồ đường</span>
      </div>
      <div class="chart-frame">
        <canvas ref="revenueChartRef"></canvas>
      </div>
    </article>

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Số đơn hoàn thành</h2>
          <p>{{ orderChartSubtitle }}</p>
        </div>
        <span>Biểu đồ cột</span>
      </div>
      <div class="chart-frame">
        <canvas ref="orderChartRef"></canvas>
      </div>
    </article>

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Doanh thu theo thương hiệu</h2>
          <p>Tỷ lệ phần trăm và doanh thu theo từng thương hiệu</p>
        </div>
        <span>Biểu đồ tròn</span>
      </div>
      <div class="donut-wrap">
        <div class="chart-frame donut-canvas">
          <canvas ref="brandChartRef"></canvas>
        </div>
        <div class="legend-list">
          <span v-for="item in brandRevenue" :key="item.label">
            <i :style="{ backgroundColor: item.color }"></i>
            <span class="legend-text">
              <b>{{ item.label }}</b>
              <small>{{ item.value }}% · {{ formatShortCurrency(item.revenue) }}</small>
            </span>
          </span>
        </div>
      </div>
    </article>

    <article class="dashboard-card">
      <div class="card-header">
        <div>
          <h2>Phương thức thanh toán &amp; Trạng thái đơn hàng</h2>
          <p>Tỷ lệ phần trăm theo phương thức thanh toán và trạng thái đơn</p>
        </div>
        <span>Biểu đồ tròn</span>
      </div>
      <div class="payment-wrap">
        <div class="payment-half">
          <p class="chart-sub">Phương thức thanh toán</p>
          <div class="chart-frame donut-canvas">
            <canvas ref="methodChartRef"></canvas>
          </div>
          <div class="legend-list compact">
            <span v-for="item in methodSlices" :key="item.method">
              <i :style="{ backgroundColor: item.color }"></i>
              <span class="legend-text">
                <b>{{ item.method }}</b>
                <small>{{ item.percentage }}% ({{ item.totalOrders }} đơn)</small>
              </span>
            </span>
          </div>
        </div>
        <div class="payment-half">
          <p class="chart-sub">Trạng thái đơn hàng</p>
          <div class="chart-frame donut-canvas">
            <canvas ref="statusChartRef"></canvas>
          </div>
          <div class="legend-list compact">
            <span v-for="item in statusSlices" :key="item.status">
              <i :style="{ backgroundColor: item.color }"></i>
              <span class="legend-text">
                <b>{{ statusLabel(item.status) }}</b>
                <small>{{ item.percentage }}% ({{ item.totalOrders }} đơn)</small>
              </span>
            </span>
          </div>
        </div>
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
  orderStats:    { type: Array,  default: () => [] },
  paymentStats:  { type: Object, default: () => ({ byMethod: [], byStatus: [] }) },
})

// refs
const revenueChartRef = ref(null)
const orderChartRef   = ref(null)
const brandChartRef   = ref(null)
const methodChartRef  = ref(null)
const statusChartRef  = ref(null)

let revenueChart = null
let orderChart   = null
let brandChart   = null
let methodChart  = null
let statusChart  = null

// colors
const DONUT_COLORS  = ['#2563eb', '#16a34a', '#0891b2', '#f59e0b', '#7c3aed', '#0d9488', '#dc2626', '#db2777']
const STATUS_COLORS = {
  COMPLETED:  '#16a34a',
  PAID:       '#16a34a',
  PENDING:    '#f59e0b',
  PROCESSING: '#0891b2',
  CONFIRMED:  '#2563eb',
  SHIPPING:   '#7c3aed',
  CANCELLED:  '#dc2626',
  UNPAID:     '#9ca3af',
}
const METHOD_COLORS = {
  VNPay:   '#2563eb',
  VNPAY:   '#2563eb',
  COD:     '#f59e0b',
  MOMO:    '#db2777',
  ZALOPAY: '#0891b2',
  BankTransfer: '#0d9488',
}
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

// computed
const brandRevenue = computed(() =>
  props.brandStats.map((item, idx) => ({
    label:   item.brand || 'Khac',
    value:   item.percentage,
    revenue: item.revenue,
    color:   DONUT_COLORS[idx % DONUT_COLORS.length],
  }))
)

const methodSlices = computed(() =>
  (props.paymentStats?.byMethod ?? []).map((item, idx) => ({
    ...item,
    color: METHOD_COLORS[item.method] ?? DONUT_COLORS[idx % DONUT_COLORS.length],
  }))
)

const statusSlices = computed(() =>
  (props.paymentStats?.byStatus ?? []).map((item) => ({
    ...item,
    color: STATUS_COLORS[item.status] ?? '#9ca3af',
  }))
)

const orderChartSubtitle = computed(() => {
  switch (props.selectedTime) {
    case 'today':     return 'So don hoan thanh trong hom nay'
    case 'yesterday': return 'So don hoan thanh trong hom qua'
    case '7days':     return 'So don hoan thanh trong 7 ngay qua'
    case '30days':    return 'So don hoan thanh trong 30 ngay qua'
    case 'week':      return 'So don hoan thanh theo tung ngay trong tuan'
    case 'year':      return 'So don hoan thanh theo tung thang trong nam'
    default:          return 'So don hoan thanh theo tung ngay trong thang'
  }
})

// helpers
function destroyChart(c) { if (c) c.destroy(); return null }

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

function formatShortCurrency(value) {
  if (!value) return '0d'
  const n = Number(value)
  if (n >= 1_000_000_000) return (n / 1_000_000_000).toFixed(1) + ' ty'
  if (n >= 1_000_000)     return (n / 1_000_000).toFixed(0) + ' trieu'
  if (n >= 1_000)         return (n / 1_000).toFixed(0) + 'k'
  return n + 'd'
}

function reportDateToLabel(reportDate) {
  if (!reportDate) return ''
  const str = reportDate instanceof Date
    ? reportDate.toISOString().slice(0, 10)
    : String(reportDate)
  const parts = str.split('T')[0].split('-')
  if (parts.length === 3) return parts[2] + '/' + parts[1]
  return str
}

// FIX: findTodayIndex - yesterday highlight ngay hom qua
function findTodayIndex(labels, period) {
  const today = new Date()
  const dd = String(today.getDate()).padStart(2, '0')
  const mm = String(today.getMonth() + 1).padStart(2, '0')
  const todayShort = dd + '/' + mm

  if (period === 'yesterday') {
    const yest = new Date(today)
    yest.setDate(yest.getDate() - 1)
    const ydd = String(yest.getDate()).padStart(2, '0')
    const ymm = String(yest.getMonth() + 1).padStart(2, '0')
    return labels.indexOf(ydd + '/' + ymm)
  }
  if (period === 'week') {
    const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
    return labels.indexOf(days[today.getDay()])
  }
  if (['month', '7days', '30days', 'today'].includes(period)) {
    return labels.indexOf(todayShort)
  }
  if (period === 'year') return labels.indexOf('T' + (today.getMonth() + 1))
  return -1
}

const STATUS_LABELS = {
  PENDING: 'Cho xu ly', CONFIRMED: 'Da xac nhan', SHIPPING: 'Dang giao',
  COMPLETED: 'Hoan thanh', CANCELLED: 'Da huy', PROCESSING: 'Dang xu ly',
  PAID: 'Da thanh toan', UNPAID: 'Chua thanh toan',
}
function statusLabel(s) { return STATUS_LABELS[s] || s }

// chart builders
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

  const todayIdx       = findTodayIndex(labels, period)
  const pointRadiusCur = labels.map((_, i) => i === todayIdx ? 7 : 4)
  const pointStyleCur  = labels.map((_, i) => i === todayIdx ? 'star' : 'circle')
  const isSinglePoint  = labels.length <= 1

  revenueChart = new Chart(revenueChartRef.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: props.compareData.previousLabel || 'Ky truoc',
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
          label: props.compareData.currentLabel || 'Ky nay',
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
          offset: isSinglePoint,
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
          display: true, position: 'top',
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
              if (period === 'week') return source.sublabel ? (source.label + ' (' + source.sublabel + ')') : source.label
              return source.sublabel ?? source.label ?? items[0].label
            },
            label: (ctx) => ' ' + ctx.dataset.label + ': ' + formatCurrency(ctx.parsed.y),
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
  let labels = [], data = []

  if (period === 'week') {
    const ORDER = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
    const map   = {}
    props.weekdayStats.forEach(d => {
      const key = d.dayLabel.replace('Thu ', 'T').replace('Chu nhat', 'CN')
      map[key] = d.totalOrders
    })
    labels = ORDER
    data   = ORDER.map(day => map[day] ?? 0)
  } else if (period === 'year') {
    const monthMap = {}
    for (let m = 1; m <= 12; m++) monthMap['T' + m] = 0
    props.orderStats.forEach(d => {
      if (d.reportDate) {
        const str   = d.reportDate instanceof Date ? d.reportDate.toISOString().slice(0, 10) : String(d.reportDate)
        const month = parseInt(str.split('T')[0].split('-')[1])
        monthMap['T' + month] += d.totalOrders ?? 0
      }
    })
    labels = Object.keys(monthMap)
    data   = Object.values(monthMap)
  } else {
    labels = props.orderStats.map(d => reportDateToLabel(d.reportDate))
    data   = props.orderStats.map(d => d.totalOrders ?? 0)
  }

  const todayIdx = findTodayIndex(labels, period)

  const todayTagPlugin = {
    id: 'todayTag',
    afterDatasetsDraw(chart) {
      if (todayIdx < 0) return
      const { ctx } = chart
      const meta = chart.getDatasetMeta(0)
      const bar  = meta.data[todayIdx]
      if (!bar) return
      const x       = bar.x
      const barTop  = bar.y
      const text    = 'Hom nay'
      const fontSize = 10
      const padX    = 7
      const padY    = 4
      const arrowH  = 5
      const gap     = 4
      ctx.save()
      ctx.font = '700 ' + fontSize + 'px Inter, sans-serif'
      const textW      = ctx.measureText(text).width
      const boxW       = textW + padX * 2
      const boxH       = fontSize + padY * 2
      const pillBottom = barTop - gap - arrowH
      const pillTop    = pillBottom - boxH
      ctx.beginPath()
      ctx.roundRect(x - boxW / 2, pillTop, boxW, boxH, 4)
      ctx.fillStyle = '#2563eb'
      ctx.fill()
      ctx.beginPath()
      ctx.moveTo(x - 5, pillBottom)
      ctx.lineTo(x + 5, pillBottom)
      ctx.lineTo(x,     pillBottom + arrowH)
      ctx.closePath()
      ctx.fillStyle = '#2563eb'
      ctx.fill()
      ctx.fillStyle    = '#fff'
      ctx.textAlign    = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(text, x, pillTop + boxH / 2)
      ctx.restore()
    },
  }

  orderChart = new Chart(orderChartRef.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: COLOR_GREEN_LIGHT,
        borderRadius: 10,
        borderSkipped: false,
        barThickness: labels.length <= 2 ? 60 : undefined,
        maxBarThickness: 80,
      }],
    },
    options: {
      ...chartDefaults,
      layout: { padding: { top: 36 } },
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, maxRotation: 45, minRotation: 0, autoSkip: true, maxTicksLimit: 31 },
          grid: { display: false },
        },
        y: {
          min: 0,
          ticks: { color: tickColor, font: { size: 11, weight: '600' }, stepSize: 1, precision: 0 },
          grid: { color: gridColor },
          title: { display: true, text: 'So don', color: tickColor, font: { size: 11, weight: '600' } },
        },
      },
      plugins: {
        ...chartDefaults.plugins,
        tooltip: { callbacks: { label: (ctx) => ' ' + ctx.parsed.y + ' don' } },
      },
    },
    plugins: [todayTagPlugin],
  })
}

function buildBrandChart() {
  brandChart = destroyChart(brandChart)
  if (!brandChartRef.value || !props.brandStats.length) return

  const validItems = brandRevenue.value.filter(b => Number(b.revenue) > 0)
  if (!validItems.length) return

  brandChart = new Chart(brandChartRef.value, {
    type: 'doughnut',
    data: {
      labels: validItems.map(b => b.label),
      datasets: [{
        data:            validItems.map(b => Number(b.revenue)),
        backgroundColor: validItems.map(b => b.color),
        borderWidth: 2,
        borderColor: '#fff',
      }],
    },
    options: {
      ...chartDefaults,
      cutout: '68%',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const item = validItems[ctx.dataIndex]
              return ' ' + ctx.label + ': ' + (item?.value ?? 0) + '% - ' + formatShortCurrency(item?.revenue)
            },
          },
        },
      },
    },
  })
}

function buildMethodChart() {
  methodChart = destroyChart(methodChart)
  if (!methodChartRef.value || !methodSlices.value.length) return

  methodChart = new Chart(methodChartRef.value, {
    type: 'doughnut',
    data: {
      labels: methodSlices.value.map(m => m.method),
      datasets: [{
        data:            methodSlices.value.map(m => m.totalOrders),
        backgroundColor: methodSlices.value.map(m => m.color),
        borderWidth: 2,
        borderColor: '#fff',
      }],
    },
    options: {
      ...chartDefaults,
      cutout: '65%',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const item = methodSlices.value[ctx.dataIndex]
              return ' ' + ctx.label + ': ' + item.percentage + '% - ' + formatShortCurrency(item.totalRevenue)
            },
          },
        },
      },
    },
  })
}

function buildStatusChart() {
  statusChart = destroyChart(statusChart)
  if (!statusChartRef.value || !statusSlices.value.length) return

  statusChart = new Chart(statusChartRef.value, {
    type: 'doughnut',
    data: {
      labels: statusSlices.value.map(s => statusLabel(s.status)),
      datasets: [{
        data:            statusSlices.value.map(s => s.totalOrders),
        backgroundColor: statusSlices.value.map(s => s.color),
        borderWidth: 2,
        borderColor: '#fff',
      }],
    },
    options: {
      ...chartDefaults,
      cutout: '65%',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const item = statusSlices.value[ctx.dataIndex]
              return ' ' + statusLabel(item.status) + ': ' + item.percentage + '% (' + item.totalOrders + ' don)'
            },
          },
        },
      },
    },
  })
}

function buildAllCharts() {
  nextTick(() => {
    buildRevenueChart()
    buildOrderChart()
    buildBrandChart()
    buildMethodChart()
    buildStatusChart()
  })
}

watch(
  () => [props.compareData, props.weekdayStats, props.orderStats, props.brandStats, props.paymentStats],
  () => buildAllCharts(),
  { deep: true }
)

onBeforeUnmount(() => {
  revenueChart = destroyChart(revenueChart)
  orderChart   = destroyChart(orderChart)
  brandChart   = destroyChart(brandChart)
  methodChart  = destroyChart(methodChart)
  statusChart  = destroyChart(statusChart)
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

.dashboard-card h2 { margin: 0; color: #111827; font-weight: 900; font-size: 20px; }
.dashboard-card p  { margin: 4px 0 0; font-size: 13px; color: #6b7280; }

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
  height: 340px;
  position: relative;
}

.chart-frame canvas {
  width: 100% !important;
  height: 100% !important;
}

.donut-wrap {
  min-height: 340px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  align-items: center;
  gap: 20px;
}

.donut-canvas { height: 300px !important; }

.legend-list { display: grid; gap: 10px; }

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

.legend-text { display: flex; flex-direction: column; gap: 1px; }
.legend-text b     { font-size: 13px; font-weight: 800; color: #111827; }
.legend-text small { font-size: 11px; color: #6b7280; font-weight: 700; }

.payment-wrap {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.payment-half { display: flex; flex-direction: column; gap: 12px; }

.chart-sub { margin: 0; font-size: 13px; font-weight: 900; color: #374151; text-align: center; }

.legend-list.compact { gap: 8px; }
.legend-list.compact span { font-size: 12px; }
.legend-list.compact .legend-text b { font-size: 12px; }

@media (max-width: 1400px) { .dashboard-grid { grid-template-columns: 1fr; } }

@media (max-width: 992px) {
  .card-header  { flex-direction: column; align-items: stretch; }
  .donut-wrap   { grid-template-columns: 1fr; justify-items: center; }
  .payment-wrap { grid-template-columns: 1fr; }
}
</style>