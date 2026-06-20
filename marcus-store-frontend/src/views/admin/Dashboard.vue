<template>
  <section class="dashboard-page">
    <div class="dashboard-shell">
      <header class="dashboard-heading">
        <div>
          <p class="eyebrow">Bảng điều khiển kinh doanh</p>
          <h1>Màn hình quản trị</h1>
          <span>Theo dõi doanh thu, đơn hàng, tồn kho và khách hàng trong một màn hình.</span>
        </div>

        <div class="time-filter">
          <button
            v-for="item in timeFilters"
            :key="item.value"
            type="button"
            :class="{ active: selectedTime === item.value }"
            @click="selectedTime = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </header>

      <section class="kpi-grid">
        <article
          v-for="item in kpiOverview"
          :key="item.key"
          class="kpi-card"
          :class="{ warning: item.type === 'warning' }"
        >
          <div class="kpi-top">
            <span class="kpi-icon">
              <i :class="item.icon"></i>
            </span>
            <small>{{ item.growth }}</small>
          </div>
          <div>
            <p>{{ item.title }}</p>
            <strong>{{ item.value }}</strong>
            <span>{{ item.note }}</span>
          </div>
        </article>
      </section>

      <section class="dashboard-grid">
        <div class="chart-grid">

          <!-- Chart 1: Xu hướng doanh thu - Line -->
          <article class="dashboard-card">
            <div class="card-header">
              <div>
                <h2>Xu hướng doanh thu</h2>
                <p>Theo ngày trong kỳ</p>
              </div>
              <span>Line chart</span>
            </div>
            <div class="chart-frame">
              <canvas ref="revenueChartRef"></canvas>
            </div>
          </article>

          <!-- Chart 2: Đơn hàng phát sinh - Bar -->
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

          <!-- Chart 3: Doanh thu theo thương hiệu - Doughnut -->
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

          <!-- Chart 4: So sánh doanh thu - Bar grouped -->
          <article class="dashboard-card">
            <div class="card-header">
              <div>
                <h2>So sánh doanh thu</h2>
                <p>Kỳ này so với kỳ trước</p>
              </div>
              <span>Bar chart</span>
            </div>
            <div class="chart-frame">
              <canvas ref="compareChartRef"></canvas>
            </div>
          </article>

        </div>

        <aside class="dashboard-card alert-card">
          <div class="card-header">
            <div>
              <h2>Cảnh báo cần chú ý</h2>
              <p>Các vấn đề cần xử lý nhanh</p>
            </div>
            <span class="danger-pill">{{ inventoryAlerts.length }} mới</span>
          </div>

          <div class="alert-list">
            <button
              v-for="alert in inventoryAlerts"
              :key="alert.id"
              type="button"
              class="alert-item"
              :class="alert.tone"
            >
              <span></span>
              <div>
                <strong>{{ alert.title }}</strong>
                <p>{{ alert.description }}</p>
                <small>{{ alert.actionLabel }}</small>
              </div>
            </button>
          </div>
        </aside>
      </section>

      <section class="dashboard-card data-card">
        <div class="data-header">
          <div>
            <h2>Dữ liệu chi tiết</h2>
            <p>Lọc nhanh dữ liệu bán hàng, tồn kho và khách hàng</p>
          </div>
          <button type="button" class="reset-btn" @click="resetFilters">Đặt lại bộ lọc</button>
        </div>

        <div class="filter-grid">
          <div class="search-box">
            <i class="bi bi-search"></i>
            <input v-model.trim="filters.search" type="text" placeholder="Tìm mã đơn / tên khách..." />
          </div>

          <select v-model="filters.status">
            <option value="all">Tất cả trạng thái</option>
            <option v-for="status in statusOptions" :key="status.value" :value="status.value">
              {{ status.label }}
            </option>
          </select>

          <select v-model="filters.category" v-show="categoryOptions.length > 0">
            <option value="all">Tất cả danh mục / thương hiệu</option>
            <option v-for="category in categoryOptions" :key="category" :value="category">{{ category }}</option>
          </select>

          <select v-model="filters.date">
            <option value="month">Tháng này</option>
            <option value="today">Hôm nay</option>
            <option value="week">7 ngày gần nhất</option>
            <option value="quarter">Quý này</option>
            <option value="year">Năm nay</option>
          </select>
        </div>

        <div class="tab-list">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            :class="{ active: currentTab === tab.value }"
            @click="currentTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th
                  v-for="column in activeTable.columns"
                  :key="column.key"
                  :class="alignClass(column.align)"
                >
                  {{ column.label }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredRows.length === 0">
                <td :colspan="activeTable.columns.length" class="empty-cell">
                  <strong>Không có dữ liệu phù hợp</strong>
                  <span>Hãy thử đổi từ khóa tìm kiếm hoặc đặt lại bộ lọc.</span>
                </td>
              </tr>
              <tr v-for="row in filteredRows" v-else :key="row.id">
                <td
                  v-for="column in activeTable.columns"
                  :key="`${row.id}-${column.key}`"
                  :class="alignClass(column.align)"
                >
                  <span v-if="column.type === 'money'" class="money">{{ formatCurrency(row[column.key]) }}</span>
                  <span v-else-if="column.type === 'percent'" class="cell-main">{{ row[column.key] }}%</span>
                  <span v-else-if="column.type === 'status'" class="status-badge" :class="statusClass(row[column.key])">
                    {{ statusLabel(row[column.key]) }}
                  </span>
                  <span v-else class="cell-main">{{ row[column.key] }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import statisticsApi from '@/api/statisticsApi'
import '@/assets/css/Dashboard.css'

Chart.register(...registerables)

const revenueChartRef = ref(null)
const orderChartRef = ref(null)
const brandChartRef = ref(null)
const compareChartRef = ref(null)

let revenueChart = null
let orderChart = null
let brandChart = null
let compareChart = null

//State 
const selectedTime = ref('month')
const currentTab = ref('recentOrders')
const loading = ref(true)
const errorMsg = ref('')

const filters = reactive({
  search: '',
  status: 'all',
  category: 'all',
  date: 'month',
})

watch(selectedTime, (val) => {
  filters.date = val
  fetchDashboardData(val)
})
onMounted(() => fetchDashboardData(selectedTime.value))

watch(() => filters.date, (val) => {
  selectedTime.value = val
})

watch(currentTab, () => {
  filters.status = 'all'
  filters.category = 'all'
})

const timeFilters = [
  { label: 'Hôm nay', value: 'today' },
  { label: 'Tuần', value: 'week' },
  { label: 'Tháng', value: 'month' },
  { label: 'Năm', value: 'year' },
]

const tabs = [
  { label: 'Đơn hàng gần nhất', value: 'recentOrders' },
  { label: 'Top sản phẩm bán chạy', value: 'topProducts' },
  { label: 'Sản phẩm sắp hết kho', value: 'lowStock' },
  { label: 'Khách hàng mua nhiều nhất', value: 'topCustomers' },
]

//Data từ API
const dailyStats = ref([])
const monthlyStats = ref([])
const weekdayStats = ref([])
const brandStats = ref([])
const recentOrdersData = ref([])
const topProductsData = ref([])
const lowStockData = ref([])
const topCustomersData = ref([])
const compareData = ref({ current: [], previous: [], currentLabel: '', previousLabel: '' })

//Chart colors
const DONUT_COLORS = ['#ff4d8d', '#6366f1', '#22c55e', '#f59e0b', '#06b6d4', '#a855f7']

//KPI
const todayStr = new Date().toISOString().split('T')[0]

const todayRevenue = computed(() => {
  const todayData = dailyStats.value.find((item) => item.reportDate === todayStr)
  return todayData?.totalRevenue || 0
})

const todayOrders = computed(() => {
  const todayData = dailyStats.value.find((item) => item.reportDate === todayStr)
  return todayData?.totalOrders || 0
})

const kpiOverview = computed(() => {
  const monthRevenue = monthlyStats.value[0]?.totalRevenue || 0
  const monthOrders = monthlyStats.value[0]?.totalOrders || 0
  const monthProductsSold = monthlyStats.value[0]?.totalProductsSold || 0
  const pendingCount = recentOrdersData.value.filter((o) => o.status === 'PENDING').length

  return [
    {
      key: 'revenue',
      title: 'Doanh thu',
      value: formatCurrency(monthRevenue),
      growth: '',
      note: 'Tổng doanh thu tháng này',
      icon: 'bi bi-currency-dollar',
      type: 'normal',
    },
    {
      key: 'orders',
      title: 'Tổng đơn hàng',
      value: String(monthOrders),
      growth: '',
      note: 'Đơn hàng đã ghi nhận trong tháng',
      icon: 'bi bi-bag-check',
      type: 'normal',
    },
    {
      key: 'soldProducts',
      title: 'Sản phẩm đã bán',
      value: String(monthProductsSold),
      growth: '',
      note: 'Số lượng SKU đã bán trong tháng',
      icon: 'bi bi-box-seam',
      type: 'normal',
    },
    {
      key: 'pendingOrders',
      title: 'Đơn mới chưa xử lý',
      value: String(pendingCount),
      growth: 'Ưu tiên',
      note: 'Ưu tiên duyệt để tránh trễ SLA',
      icon: 'bi bi-exclamation-triangle',
      type: 'warning',
    },
    {
      key: 'todayRevenue',
      title: 'Doanh thu hôm nay',
      value: formatCurrency(todayRevenue.value),
      growth: '',
      note: `${todayOrders.value} đơn hàng hôm nay`,
      icon: 'bi bi-graph-up-arrow',
      type: 'normal',
    },
    {
      key: 'lowStock',
      title: 'SP sắp/ hết hàng',
      value: String(lowStockData.value.length),
      growth: '',
      note: 'Cần nhập thêm hàng',
      icon: 'bi bi-people',
      type: lowStockData.value.length > 0 ? 'warning' : 'normal',
    },
  ]
})

const inventoryAlerts = computed(() =>
  lowStockData.value.slice(0, 3).map((item, idx) => ({
    id: idx + 1,
    tone: item.status === 'Hết hàng' ? 'pink' : 'orange',
    title:
      item.status === 'Hết hàng'
        ? `Hết hàng: ${item.productName} (${item.skuCode})`
        : `Sắp hết hàng: ${item.productName} chỉ còn ${item.stockQuantity} sản phẩm`,
    description: item.brand,
    actionLabel: 'Xem tồn kho',
  })),
)

//Table
const tableConfig = computed(() => ({
  recentOrders: {
    columns: [
      { label: 'Mã đơn', key: 'orderCode', align: 'left' },
      { label: 'Khách hàng', key: 'customerName', align: 'left' },
      { label: 'Thanh toán', key: 'paymentMethod', align: 'left' },
      { label: 'Trạng thái', key: 'orderStatus', align: 'center', type: 'status' },
      { label: 'Tổng tiền', key: 'totalAmount', align: 'right', type: 'money' },
      { label: 'Thời gian', key: 'createdAt', align: 'left' },
    ],
    data: recentOrdersData.value,
  },
  topProducts: {
    columns: [
      { label: 'Sản phẩm', key: 'productName', align: 'left' },
      { label: 'Đã bán', key: 'totalSold', align: 'center' },
      { label: 'Doanh thu', key: 'revenue', align: 'right', type: 'money' },
    ],
    data: topProductsData.value,
  },
  lowStock: {
    columns: [
      { label: 'SKU', key: 'skuCode', align: 'left' },
      { label: 'Sản phẩm', key: 'productName', align: 'left' },
      { label: 'Thương hiệu', key: 'brand', align: 'left' },
      { label: 'Tồn kho', key: 'stockQuantity', align: 'center' },
      { label: 'Trạng thái', key: 'status', align: 'center', type: 'status' },
    ],
    data: lowStockData.value,
  },
  topCustomers: {
    columns: [
      { label: 'Khách hàng', key: 'customerName', align: 'left' },
      { label: 'Email', key: 'email', align: 'left' },
      { label: 'Số đơn', key: 'totalOrders', align: 'center' },
      { label: 'Tổng chi tiêu', key: 'totalSpent', align: 'right', type: 'money' },
      { label: 'Đóng góp', key: 'contributionPercent', align: 'right', type: 'percent' },
    ],
    data: topCustomersData.value,
  },
}))

const statusOptions = computed(() => {
  if (currentTab.value === 'recentOrders') {
    return [
      { value: 'PAID',      label: 'Đã thanh toán' },
      { value: 'UNPAID',    label: 'Chưa thanh toán' },
      { value: 'PENDING',   label: 'Chờ xử lý' },
      { value: 'CONFIRMED', label: 'Đã xác nhận' },
      { value: 'SHIPPING',  label: 'Đang giao hàng' },
      { value: 'COMPLETED', label: 'Hoàn thành' },
      { value: 'CANCELLED', label: 'Đã hủy' },
      { value: 'PROCESSING', label: 'Đang xử lý' },
    ]
  }
  if (currentTab.value === 'lowStock') {
    return [
      { value: 'Hết hàng',     label: 'Hết hàng' },
      { value: 'Sắp hết hàng', label: 'Sắp hết hàng' },
    ]
  }
  return []
})

const categoryOptions = computed(() => {
  if (currentTab.value === 'recentOrders' || currentTab.value === 'lowStock') {
    return brandStats.value.map((b) => b.brand)
  }
  return []
})

const activeTable = computed(() => tableConfig.value[currentTab.value])

const filteredRows = computed(() => {
  const searchValue = filters.search.toLowerCase()
  const now = new Date()

  const parseItemDate = (dateStr) => {
    if (!dateStr) return null
    const match = dateStr.match(/(\d{2})\/(\d{2})\/(\d{4})/)
    if (match) return new Date(`${match[3]}-${match[2]}-${match[1]}`)
    return new Date(dateStr)
  }

  const getStartDate = () => {
    const d = new Date()
    if (filters.date === 'today') { d.setHours(0, 0, 0, 0); return d }
    if (filters.date === 'week') { d.setDate(d.getDate() - 7); return d }
    if (filters.date === 'month') return new Date(d.getFullYear(), d.getMonth(), 1)
    if (filters.date === 'quarter') return new Date(d.getFullYear(), Math.floor(d.getMonth() / 3) * 3, 1)
    if (filters.date === 'year') return new Date(d.getFullYear(), 0, 1)
    return null
  }

  const startDate = getStartDate()

  return activeTable.value.data.filter((item) => {
    const searchableText = Object.values(item).join(' ').toLowerCase()
    const matchSearch = !searchValue || searchableText.includes(searchValue)
    const matchStatus = filters.status === 'all' || item.status === filters.status || item.orderStatus === filters.status
    const matchCategory = filters.category === 'all' || item.brand === filters.category

    let matchDate = true
    if (startDate) {
      const dateStr = item.createdAt || item.reportDate || item.date
      const itemDate = parseItemDate(dateStr)
      if (itemDate) matchDate = itemDate >= startDate && itemDate <= now
    }

    return matchSearch && matchStatus && matchCategory && matchDate
  })
})

const brandRevenue = computed(() =>
  brandStats.value.map((item, idx) => ({
    label: item.brand || 'Khác',
    value: item.percentage,
    color: DONUT_COLORS[idx % DONUT_COLORS.length],
  })),
)

const chartDefaults = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
  },
}

const gridColor = 'rgba(255, 77, 141, 0.1)'
const tickColor = '#9ca3af'

function destroyChart(chart) {
  if (chart) { chart.destroy() }
  return null
}

function buildRevenueChart() {
  revenueChart = destroyChart(revenueChart)
  if (!revenueChartRef.value || !dailyStats.value.length) return

  const sorted = [...dailyStats.value].reverse()
  const labels = sorted.map((d) => formatShortDate(d.reportDate))
  const data = sorted.map((d) => d.totalRevenue)

  revenueChart = new Chart(revenueChartRef.value, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        data,
        borderColor: '#ff4d8d',
        backgroundColor: 'rgba(255, 77, 141, 0.12)',
        borderWidth: 3,
        pointBackgroundColor: '#ff4d8d',
        pointRadius: 4,
        tension: 0.4,
        fill: true,
      }],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 11 } },
          grid: { color: gridColor },
        },
        y: {
          ticks: {
            color: tickColor,
            font: { size: 11 },
            callback: (val) => formatShortCurrency(val),
          },
          grid: { color: gridColor },
        },
      },
      plugins: {
        ...chartDefaults.plugins,
        tooltip: {
          callbacks: {
            label: (ctx) => ` ${formatCurrency(ctx.parsed.y)}`,
          },
        },
      },
    },
  })
}

function buildOrderChart() {
  orderChart = destroyChart(orderChart)
  if (!orderChartRef.value || !weekdayStats.value.length) return

  const labels = weekdayStats.value.map((d) =>
    d.dayLabel.replace('Thứ ', 'T').replace('Chủ nhật', 'CN'),
  )
  const data = weekdayStats.value.map((d) => d.totalOrders)

  orderChart = new Chart(orderChartRef.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: 'rgba(255, 77, 141, 0.8)',
        borderRadius: 10,
        borderSkipped: false,
      }],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 11 } },
          grid: { display: false },
        },
        y: {
          ticks: { color: tickColor, font: { size: 11 }, stepSize: 1 },
          grid: { color: gridColor },
          title: { display: true, text: 'Số đơn', color: tickColor, font: { size: 11 } },
        },
      },
      plugins: {
        ...chartDefaults.plugins,
        tooltip: {
          callbacks: {
            label: (ctx) => ` ${ctx.parsed.y} đơn`,
          },
        },
      },
    },
  })
}

// ====== Chart 3: Doanh thu theo thương hiệu (Doughnut) ======
function buildBrandChart() {
  brandChart = destroyChart(brandChart)
  if (!brandChartRef.value || !brandStats.value.length) return

  const labels = brandRevenue.value.map((b) => b.label)
  const data = brandRevenue.value.map((b) => b.value)
  const colors = brandRevenue.value.map((b) => b.color)

  brandChart = new Chart(brandChartRef.value, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: colors,
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
            label: (ctx) => ` ${ctx.label}: ${ctx.parsed}%`,
          },
        },
      },
    },
  })
}

// ====== Chart 4: So sánh doanh thu (Bar grouped theo tuần) ======
function buildCompareChart() {
  compareChart = destroyChart(compareChart)
  if (!compareChartRef.value || !compareData.value.current?.length) return

  const labels = compareData.value.current.map((d) => d.label)
  const currentValues = compareData.value.current.map((d) => d.revenue)
  const previousValues = compareData.value.previous.map((d) => d.revenue)

  compareChart = new Chart(compareChartRef.value, {
    type: 'bar',
    data: {
      labels,
      datasets: [
        {
          label: compareData.value.previousLabel || 'Kỳ trước',
          data: previousValues,
          backgroundColor: 'rgba(156, 163, 175, 0.6)',
          borderRadius: 8,
          borderSkipped: false,
        },
        {
          label: compareData.value.currentLabel || 'Kỳ này',
          data: currentValues,
          backgroundColor: 'rgba(255, 77, 141, 0.85)',
          borderRadius: 8,
          borderSkipped: false,
        },
      ],
    },
    options: {
      ...chartDefaults,
      scales: {
        x: {
          ticks: { color: tickColor, font: { size: 11 } },
          grid: { display: false },
        },
        y: {
          ticks: {
            color: tickColor,
            font: { size: 11 },
            callback: (val) => formatShortCurrency(val),
          },
          grid: { color: gridColor },
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
          labels: { color: '#6b7280', font: { size: 11 }, boxWidth: 12 },
        },
        tooltip: {
          callbacks: {
            label: (ctx) => ` ${ctx.dataset.label}: ${formatCurrency(ctx.parsed.y)}`,
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
    buildCompareChart()
  })
}

// ====== Helpers ======
function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value || 0)
}

function formatShortCurrency(value) {
  if (value >= 1_000_000_000) return `${(value / 1_000_000_000).toFixed(1)}T`
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(0)}tr`
  if (value >= 1_000) return `${(value / 1_000).toFixed(0)}k`
  return String(value)
}

function formatShortDate(dateStr) {
  const d = new Date(dateStr)
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
}

function alignClass(align) {
  return {
    'text-center': align === 'center',
    'text-right': align === 'right',
    'text-left': align !== 'center' && align !== 'right',
  }
}

function statusClass(status) {
  return {
    success: ['COMPLETED', 'PAID'].includes(status),
    warning: ['PENDING', 'PROCESSING', 'Sắp hết hàng'].includes(status),
    info:    ['SHIPPING', 'CONFIRMED'].includes(status),
    danger:  ['CANCELLED', 'Hết hàng', 'UNPAID'].includes(status),
  }
}

const statusLabels = {
  PENDING:    'Chờ xử lý',
  CONFIRMED:  'Đã xác nhận',
  SHIPPING:   'Đang giao hàng',
  COMPLETED:  'Hoàn thành',
  CANCELLED:  'Đã hủy',
  PROCESSING: 'Đang xử lý',
  PAID:       'Đã thanh toán',
  UNPAID:     'Chưa thanh toán',
  'Hết hàng':     'Hết hàng',
  'Sắp hết hàng': 'Sắp hết hàng',
}

function statusLabel(status) {
  return statusLabels[status] || status
}

function resetFilters() {
  filters.search = ''
  filters.status = 'all'
  filters.category = 'all'
  filters.date = 'month'
}

// ====== API ======
async function fetchDashboardData(period = 'month') {
  loading.value = true
  errorMsg.value = ''
  try {
    const [
      dailyRes, monthlyRes, weekdayRes, brandRes,
      recentOrdersRes, topProductsRes, lowStockRes, topCustomersRes,
      compareRes,
    ] = await Promise.all([
      statisticsApi.getRevenueByDay(period),
      statisticsApi.getRevenueByMonth(),
      statisticsApi.getOrdersByWeekday(period),
      statisticsApi.getRevenueByBrand(period),
      statisticsApi.getRecentOrders(10, period),
      statisticsApi.getTopProducts(10, period),
      statisticsApi.getLowStockProducts(),
      statisticsApi.getTopCustomers(10, period),
      statisticsApi.getRevenueCompare(period),
    ])

    dailyStats.value = dailyRes.data.data
    monthlyStats.value = monthlyRes.data.data
    weekdayStats.value = weekdayRes.data.data
    brandStats.value = brandRes.data.data
    recentOrdersData.value = recentOrdersRes.data.data
    topProductsData.value = topProductsRes.data.data
    lowStockData.value = lowStockRes.data.data
    topCustomersData.value = topCustomersRes.data.data
    compareData.value = compareRes.data.data

    buildAllCharts()
  } catch (err) {
    errorMsg.value = err.response?.data?.message || 'Không thể tải dữ liệu thống kê'
  } finally {
    loading.value = false
  }
}
</script>