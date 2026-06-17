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
          <article class="dashboard-card">
            <div class="card-header">
              <div>
                <h2>Xu hướng doanh thu</h2>
                <p>Theo ngày / tuần / tháng</p>
              </div>
              <span>Line chart</span>
            </div>
            <div class="chart-frame">
              <svg viewBox="0 0 520 260" role="img" aria-label="Biểu đồ xu hướng doanh thu">
                <g class="grid-lines">
                  <line v-for="y in [42, 92, 142, 192, 232]" :key="y" x1="32" :y1="y" x2="500" :y2="y" />
                </g>
                <polyline class="area-line" :points="revenueLinePoints" />
                <polygon class="area-fill" :points="`32,232 ${revenueLinePoints} 500,232`" />
                <circle
                  v-for="point in revenuePoints"
                  :key="`${point.x}-${point.y}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="5"
                />
                <text v-for="label in monthLabels" :key="label.text" :x="label.x" y="252">{{ label.text }}</text>
              </svg>
            </div>
          </article>

          <article class="dashboard-card">
            <div class="card-header">
              <div>
                <h2>Đơn hàng phát sinh</h2>
                <p>Số lượng đơn theo thời gian</p>
              </div>
              <span>Bar chart</span>
            </div>
            <div class="chart-frame">
              <svg viewBox="0 0 520 260" role="img" aria-label="Biểu đồ đơn hàng phát sinh">
                <g class="grid-lines">
                  <line v-for="y in [42, 92, 142, 192, 232]" :key="y" x1="32" :y1="y" x2="500" :y2="y" />
                </g>
                <g v-for="bar in orderBars" :key="bar.label">
                  <rect :x="bar.x" :y="bar.y" width="42" :height="bar.height" rx="12" />
                  <text :x="bar.x + 21" y="252">{{ bar.label }}</text>
                </g>
              </svg>
            </div>
          </article>

          <article class="dashboard-card">
            <div class="card-header">
              <div>
                <h2>Doanh thu theo thương hiệu</h2>
                <p>Apple, Samsung, Xiaomi, Oppo</p>
              </div>
              <span>Doughnut</span>
            </div>
            <div class="donut-wrap">
              <div class="donut-chart">
                <strong>100%</strong>
                <span>Doanh thu</span>
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
                <h2>So sánh doanh thu</h2>
                <p>Kỳ này so với kỳ trước</p>
              </div>
              <span>Area chart</span>
            </div>
            <div class="chart-frame">
              <svg viewBox="0 0 520 260" role="img" aria-label="Biểu đồ so sánh doanh thu">
                <g class="grid-lines">
                  <line v-for="y in [42, 92, 142, 192, 232]" :key="y" x1="32" :y1="y" x2="500" :y2="y" />
                </g>
                <polyline class="compare-line muted" points="42,190 180,166 318,142 468,112" />
                <polyline class="compare-line" points="42,176 180,138 318,120 468,74" />
                <polygon class="area-fill" points="42,232 42,176 180,138 318,120 468,74 468,232" />
                <text x="42" y="252">Tuan 1</text>
                <text x="172" y="252">Tuan 2</text>
                <text x="310" y="252">Tuan 3</text>
                <text x="450" y="252">Tuan 4</text>
              </svg>
            </div>
          </article>
        </div>

        <aside class="dashboard-card alert-card">
          <div class="card-header">
            <div>
                <h2>Cảnh báo cần chú ý</h2>
                <p>Các vấn đề cần xử lý nhanh</p>
            </div>
            <span class="danger-pill">3 mới</span>
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
            <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
          </select>

          <select v-model="filters.category">
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
                  <span v-else-if="column.type === 'status'" class="status-badge" :class="statusClass(row[column.key])">
                    {{ row[column.key] }}
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
import { computed, reactive, ref } from 'vue'

const selectedTime = ref('month')
const currentTab = ref('recentOrders')

const filters = reactive({
  search: '',
  status: 'all',
  category: 'all',
  date: 'month',
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

const kpiOverview = [
  {
    key: 'revenue',
    title: 'Doanh thu',
    value: '12.15B',
    growth: '+12.4%',
    note: 'Tổng doanh thu tháng này',
    icon: 'bi bi-currency-dollar',
    type: 'normal',
  },
  {
    key: 'orders',
    title: 'Tổng đơn hàng',
    value: '1,486',
    growth: '+8.2%',
    note: 'Đơn hàng đã ghi nhận',
    icon: 'bi bi-bag-check',
    type: 'normal',
  },
  {
    key: 'soldProducts',
    title: 'Sản phẩm đã bán',
    value: '2,943',
    growth: '+10.6%',
    note: 'Số lượng SKU đã bán',
    icon: 'bi bi-box-seam',
    type: 'normal',
  },
  {
    key: 'pendingOrders',
    title: 'Đơn mới chưa xử lý',
    value: '27',
    growth: 'Ưu tiên',
    note: 'Ưu tiên duyệt để tránh trễ SLA',
    icon: 'bi bi-exclamation-triangle',
    type: 'warning',
  },
  {
    key: 'newCustomers',
    title: 'Khách hàng mới',
    value: '392',
    growth: '+5.1%',
    note: 'Tài khoản mới phát sinh',
    icon: 'bi bi-people',
    type: 'normal',
  },
  {
    key: 'grossGrowth',
    title: 'Tăng trưởng gộp',
    value: '14.8%',
    growth: '+14.8%',
    note: 'So với kỳ trước',
    icon: 'bi bi-graph-up-arrow',
    type: 'normal',
  },
]

const recentOrdersData = [
  {
    id: 'order-9482',
    orderCode: '#MS-9482',
    customerName: 'Nguyễn Minh Anh',
    phone: '0982 112 345',
    paymentMethod: 'Chuyển khoản',
    status: 'Chờ xác nhận',
    quantity: 2,
    totalAmount: 52980000,
    category: 'Apple',
    createdAt: '17/06/2026 14:10',
  },
  {
    id: 'order-9479',
    orderCode: '#MS-9479',
    customerName: 'Trần Quốc Huy',
    phone: '0918 667 889',
    paymentMethod: 'COD',
    status: 'Hoàn thành',
    quantity: 1,
    totalAmount: 31990000,
    category: 'Samsung',
    createdAt: '17/06/2026 13:32',
  },
  {
    id: 'order-9474',
    orderCode: '#MS-9474',
    customerName: 'Lê Khánh Linh',
    phone: '0977 445 221',
    paymentMethod: 'Ví điện tử',
    status: 'Đang giao',
    quantity: 3,
    totalAmount: 18470000,
    category: 'Xiaomi',
    createdAt: '17/06/2026 11:48',
  },
  {
    id: 'order-9468',
    orderCode: '#MS-9468',
    customerName: 'Phạm Gia Bảo',
    phone: '0903 882 001',
    paymentMethod: 'COD',
    status: 'Hoàn thành',
    quantity: 4,
    totalAmount: 7650000,
    category: 'Phụ kiện',
    createdAt: '16/06/2026 21:05',
  },
  {
    id: 'order-9459',
    orderCode: '#MS-9459',
    customerName: 'Đặng Thu Hà',
    phone: '0966 123 909',
    paymentMethod: 'Chuyển khoản',
    status: 'Chờ xác nhận',
    quantity: 1,
    totalAmount: 28990000,
    category: 'Apple',
    createdAt: '16/06/2026 18:27',
  },
]

const inventoryAlerts = [
  {
    id: 1,
    tone: 'orange',
    title: 'Có 27 đơn hàng mới đang chờ duyệt xử lý!',
    description: 'Bấm để lọc nhanh các đơn chờ xử lý.',
    actionLabel: 'Lọc đơn chờ xử lý',
  },
  {
    id: 2,
    tone: 'pink',
    title: 'Cảnh báo: SKU IP15PM-256 chỉ còn tồn 2 sản phẩm trong kho!',
    description: 'iPhone 15 Pro Max 256GB.',
    actionLabel: 'Xem tồn kho',
  },
  {
    id: 3,
    tone: 'blue',
    title: 'Đơn hàng #MS-9482 vừa yêu cầu hoàn hàng/đổi trả.',
    description: 'Lý do: Lỗi màn hình.',
    actionLabel: 'Xem yêu cầu',
  },
]

const topProductsData = [
  {
    id: 'product-ip15pm',
    productName: 'iPhone 15 Pro Max 256GB',
    sku: 'IP15PM-256',
    category: 'Apple',
    soldQuantity: 428,
    revenue: 12835720000,
    growthPercent: '+18.6%',
    roi: '32.4%',
    status: 'Đang bán',
  },
  {
    id: 'product-s24u',
    productName: 'Samsung Galaxy S24 Ultra',
    sku: 'SS-S24U-512',
    category: 'Samsung',
    soldQuantity: 316,
    revenue: 9170840000,
    growthPercent: '+11.2%',
    roi: '28.1%',
    status: 'Đang bán',
  },
  {
    id: 'product-xm14u',
    productName: 'Xiaomi 14 Ultra 512GB',
    sku: 'XM14U-512',
    category: 'Xiaomi',
    soldQuantity: 204,
    revenue: 4896000000,
    growthPercent: '+9.7%',
    roi: '21.5%',
    status: 'Đang bán',
  },
  {
    id: 'product-app2',
    productName: 'AirPods Pro Gen 2 USB-C',
    sku: 'APP2-USBC',
    category: 'Phụ kiện',
    soldQuantity: 192,
    revenue: 1113600000,
    growthPercent: '+6.8%',
    roi: '19.8%',
    status: 'Đang bán',
  },
]

const lowStockData = [
  {
    id: 'stock-ip15pm',
    sku: 'IP15PM-256',
    productName: 'iPhone 15 Pro Max 256GB',
    category: 'Apple',
    stockQuantity: 2,
    status: 'Sắp hết hàng',
    estimatedRevenue: 67980000,
    note: 'Cần nhập thêm trong 24 giờ',
  },
  {
    id: 'stock-s24u',
    sku: 'SS-S24U-512',
    productName: 'Samsung Galaxy S24 Ultra',
    category: 'Samsung',
    stockQuantity: 0,
    status: 'Hết hàng',
    estimatedRevenue: 0,
    note: 'Đang chờ nhà cung cấp xác nhận',
  },
  {
    id: 'stock-xm14u',
    sku: 'XM14U-512',
    productName: 'Xiaomi 14 Ultra 512GB',
    category: 'Xiaomi',
    stockQuantity: 4,
    status: 'Sắp hết hàng',
    estimatedRevenue: 95960000,
    note: 'Nhu cầu tăng nhanh trong tuần',
  },
  {
    id: 'stock-app2',
    sku: 'APP2-USBC',
    productName: 'AirPods Pro Gen 2 USB-C',
    category: 'Phụ kiện',
    stockQuantity: 5,
    status: 'Sắp hết hàng',
    estimatedRevenue: 29000000,
    note: 'Nên nhập thêm trước Flash Sale',
  },
]

const topCustomersData = [
  {
    id: 'customer-minhanh',
    customerName: 'Nguyễn Minh Anh',
    contact: 'minhanh@example.com',
    category: 'Apple',
    orders: 18,
    totalSpent: 382500000,
    contributionPercent: '8.4%',
    status: 'VIP',
  },
  {
    id: 'customer-quochuy',
    customerName: 'Trần Quốc Huy',
    contact: 'quochuy@example.com',
    category: 'Samsung',
    orders: 15,
    totalSpent: 294100000,
    contributionPercent: '6.1%',
    status: 'VIP',
  },
  {
    id: 'customer-khanhlinh',
    customerName: 'Lê Khánh Linh',
    contact: 'khanhlinh@example.com',
    category: 'Xiaomi',
    orders: 12,
    totalSpent: 188750000,
    contributionPercent: '4.2%',
    status: 'Tiềm năng',
  },
  {
    id: 'customer-giabao',
    customerName: 'Phạm Gia Bảo',
    contact: 'giabao@example.com',
    category: 'Phụ kiện',
    orders: 10,
    totalSpent: 153000000,
    contributionPercent: '3.6%',
    status: 'Tiềm năng',
  },
]

const tableConfig = {
  recentOrders: {
    columns: [
      { label: 'Mã đơn', key: 'orderCode', align: 'left' },
      { label: 'Khách hàng', key: 'customerName', align: 'left' },
      { label: 'Thanh toán', key: 'paymentMethod', align: 'left' },
      { label: 'Trạng thái', key: 'status', align: 'center', type: 'status' },
      { label: 'SL', key: 'quantity', align: 'center' },
      { label: 'Tổng tiền', key: 'totalAmount', align: 'right', type: 'money' },
      { label: 'Thời gian', key: 'createdAt', align: 'left' },
    ],
    data: recentOrdersData,
  },
  topProducts: {
    columns: [
      { label: 'Sản phẩm', key: 'productName', align: 'left' },
      { label: 'SKU', key: 'sku', align: 'left' },
      { label: 'Thương hiệu', key: 'category', align: 'left' },
      { label: 'Đã bán', key: 'soldQuantity', align: 'center' },
      { label: 'Doanh thu', key: 'revenue', align: 'right', type: 'money' },
      { label: 'Tăng trưởng', key: 'growthPercent', align: 'right' },
      { label: 'ROI', key: 'roi', align: 'right' },
      { label: 'Trạng thái', key: 'status', align: 'center', type: 'status' },
    ],
    data: topProductsData,
  },
  lowStock: {
    columns: [
      { label: 'SKU', key: 'sku', align: 'left' },
      { label: 'Sản phẩm', key: 'productName', align: 'left' },
      { label: 'Thương hiệu', key: 'category', align: 'left' },
      { label: 'Tồn kho', key: 'stockQuantity', align: 'center' },
      { label: 'Trạng thái', key: 'status', align: 'center', type: 'status' },
      { label: 'Giá trị dự kiến', key: 'estimatedRevenue', align: 'right', type: 'money' },
      { label: 'Ghi chú', key: 'note', align: 'left' },
    ],
    data: lowStockData,
  },
  topCustomers: {
    columns: [
      { label: 'Khách hàng', key: 'customerName', align: 'left' },
      { label: 'Liên hệ', key: 'contact', align: 'left' },
      { label: 'Nhóm quan tâm', key: 'category', align: 'left' },
      { label: 'Số đơn', key: 'orders', align: 'center' },
      { label: 'Tổng chi tiêu', key: 'totalSpent', align: 'right', type: 'money' },
      { label: 'Đóng góp', key: 'contributionPercent', align: 'right' },
      { label: 'Trạng thái', key: 'status', align: 'center', type: 'status' },
    ],
    data: topCustomersData,
  },
}

const statusOptions = ['Hoàn thành', 'Chờ xác nhận', 'Đang giao', 'Sắp hết hàng', 'Hết hàng', 'VIP']
const categoryOptions = ['Apple', 'Samsung', 'Xiaomi', 'Phụ kiện']

const activeTable = computed(() => tableConfig[currentTab.value])

const filteredRows = computed(() => {
  const searchValue = filters.search.toLowerCase()

  return activeTable.value.data.filter((item) => {
    const searchableText = Object.values(item).join(' ').toLowerCase()
    const matchSearch = !searchValue || searchableText.includes(searchValue)
    const matchStatus = filters.status === 'all' || item.status === filters.status
    const matchCategory = filters.category === 'all' || item.category === filters.category

    return matchSearch && matchStatus && matchCategory
  })
})

const revenueValues = [7.2, 8.1, 8.8, 9.4, 10.1, 9.7, 10.9, 11.4, 11.1, 11.7, 12.0, 12.15]
const maxRevenue = Math.max(...revenueValues)
const minRevenue = Math.min(...revenueValues)

const revenuePoints = computed(() =>
  revenueValues.map((value, index) => ({
    x: 42 + index * 39,
    y: 222 - ((value - minRevenue) / (maxRevenue - minRevenue)) * 160,
  })),
)

const revenueLinePoints = computed(() => revenuePoints.value.map((point) => `${point.x},${point.y}`).join(' '))

const monthLabels = computed(() =>
  ['T1', 'T3', 'T5', 'T7', 'T9', 'T11'].map((text, index) => ({
    text,
    x: 42 + index * 78,
  })),
)

const orderVolume = [
  { label: 'T2', value: 182 },
  { label: 'T3', value: 221 },
  { label: 'T4', value: 198 },
  { label: 'T5', value: 246 },
  { label: 'T6', value: 288 },
  { label: 'T7', value: 224 },
  { label: 'CN', value: 127 },
]

const orderBars = computed(() => {
  const max = Math.max(...orderVolume.map((item) => item.value))
  return orderVolume.map((item, index) => {
    const height = (item.value / max) * 170
    return {
      label: item.label,
      x: 54 + index * 66,
      y: 232 - height,
      height,
    }
  })
})

const brandRevenue = [
  { label: 'Apple', value: 48, color: '#ff4d8d' },
  { label: 'Samsung', value: 24, color: '#6366f1' },
  { label: 'Xiaomi', value: 14, color: '#22c55e' },
  { label: 'Oppo', value: 8, color: '#f59e0b' },
  { label: 'Phu kien', value: 6, color: '#06b6d4' },
]

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value || 0)
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
    success: ['Hoàn thành', 'Đang bán'].includes(status),
    warning: ['Chờ xác nhận', 'Sắp hết hàng'].includes(status),
    info: status === 'Đang giao',
    danger: status === 'Hết hàng',
    vip: status === 'VIP',
    potential: status === 'Tiềm năng',
  }
}

function resetFilters() {
  filters.search = ''
  filters.status = 'all'
  filters.category = 'all'
  filters.date = 'month'
}
</script>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 74px);
  background: #fff7fb;
  color: #1f2937;
  padding: 28px;
}

.dashboard-shell {
  max-width: 1580px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

.dashboard-heading,
.dashboard-card,
.kpi-card {
  background: #fff;
  border: 1px solid #ffe0ec;
  box-shadow: 0 12px 32px rgba(255, 77, 141, 0.09);
}

.dashboard-heading {
  border-radius: 24px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f0528f;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.dashboard-heading h1,
.dashboard-card h2 {
  margin: 0;
  color: #111827;
  font-weight: 900;
}

.dashboard-heading h1 {
  font-size: 34px;
}

.dashboard-heading span,
.dashboard-card p,
.kpi-card p,
.kpi-card span {
  color: #6b7280;
}

.time-filter {
  display: inline-flex;
  gap: 8px;
  padding: 8px;
  border: 1px solid #ffe0ec;
  border-radius: 18px;
  background: #fff;
}

.time-filter button,
.tab-list button,
.reset-btn {
  border: 0;
  cursor: pointer;
  font-weight: 800;
  transition: 0.2s ease;
}

.time-filter button {
  padding: 11px 18px;
  border-radius: 13px;
  color: #6b7280;
  background: transparent;
}

.time-filter button.active,
.tab-list button.active {
  background: #ff4d8d;
  color: #fff;
  box-shadow: 0 10px 22px rgba(255, 77, 141, 0.22);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  min-height: 170px;
  border-radius: 22px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.kpi-card.warning {
  background: #fff7ed;
  border-color: #fed7aa;
}

.kpi-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kpi-card .kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: #fff2f7;
  color: #ff4d8d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-top: 0;
  flex: none;
}

.kpi-card .kpi-icon i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1em;
  height: 1em;
  line-height: 1;
}

.warning .kpi-icon {
  background: #ffedd5;
  color: #c2410c;
}

.kpi-top small {
  padding: 5px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  font-weight: 900;
}

.warning .kpi-top small {
  background: #ffedd5;
  color: #c2410c;
}

.kpi-card p {
  margin: 0;
  font-size: 13px;
  font-weight: 800;
}

.kpi-card strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 30px;
  line-height: 1.1;
  font-weight: 900;
}

.kpi-card span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 700;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(280px, 1fr);
  gap: 24px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.dashboard-card {
  border-radius: 24px;
  padding: 22px;
}

.card-header,
.data-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.dashboard-card h2 {
  font-size: 20px;
}

.dashboard-card p {
  margin: 4px 0 0;
  font-size: 13px;
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

.danger-pill {
  background: #ef4444 !important;
  color: #fff !important;
}

.chart-frame {
  height: 290px;
}

.chart-frame svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.grid-lines line {
  stroke: rgba(255, 77, 141, 0.14);
  stroke-width: 1;
}

.area-line,
.compare-line {
  fill: none;
  stroke: #ff4d8d;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.compare-line.muted {
  stroke: #9ca3af;
  stroke-dasharray: 8 7;
}

.area-fill {
  fill: rgba(255, 77, 141, 0.13);
}

.chart-frame circle,
.chart-frame rect {
  fill: #ff4d8d;
}

.chart-frame text {
  fill: #6b7280;
  font-size: 12px;
  font-weight: 800;
  text-anchor: middle;
}

.donut-wrap {
  min-height: 290px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  align-items: center;
  gap: 20px;
}

.donut-chart {
  width: 210px;
  height: 210px;
  border-radius: 50%;
  background: conic-gradient(#ff4d8d 0 48%, #6366f1 48% 72%, #22c55e 72% 86%, #f59e0b 86% 94%, #06b6d4 94% 100%);
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-chart::after {
  content: '';
  position: absolute;
  inset: 42px;
  border-radius: 50%;
  background: #fff;
}

.donut-chart strong,
.donut-chart span {
  position: relative;
  z-index: 1;
}

.donut-chart strong {
  color: #111827;
  font-size: 28px;
  font-weight: 900;
}

.donut-chart span {
  color: #6b7280;
  font-size: 12px;
  font-weight: 800;
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
}

.alert-card {
  height: fit-content;
  position: sticky;
  top: 20px;
}

.alert-list {
  display: grid;
  gap: 14px;
}

.alert-item {
  width: 100%;
  display: flex;
  gap: 12px;
  padding: 18px;
  border: 1px solid;
  border-radius: 22px;
  text-align: left;
  cursor: pointer;
  transition: 0.2s ease;
}

.alert-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(255, 77, 141, 0.12);
}

.alert-item > span {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  margin-top: 5px;
  flex: none;
}

.alert-item.orange {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.alert-item.pink {
  background: #fff2f7;
  border-color: #ffcfe0;
  color: #9d174d;
}

.alert-item.blue {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.alert-item.orange > span {
  background: #f97316;
}

.alert-item.pink > span {
  background: #ff4d8d;
}

.alert-item.blue > span {
  background: #3b82f6;
}

.alert-item strong {
  display: block;
  font-weight: 900;
  line-height: 1.35;
}

.alert-item p {
  margin: 8px 0 0;
  color: inherit;
  opacity: 0.85;
}

.alert-item small {
  display: inline-flex;
  margin-top: 12px;
  text-decoration: underline;
  text-underline-offset: 4px;
  font-weight: 900;
}

.data-card {
  display: grid;
  gap: 18px;
}

.data-header {
  margin-bottom: 0;
}

.reset-btn {
  padding: 12px 18px;
  border-radius: 16px;
  background: #fff2f7;
  color: #e11d65;
  border: 1px solid #ffcfe0;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.filter-grid input,
.filter-grid select {
  width: 100%;
  height: 52px;
  border-radius: 16px;
  border: 1px solid #ffe0ec;
  background: #fff;
  color: #374151;
  font-weight: 700;
  outline: none;
  padding: 0 16px;
}

.filter-grid input:focus,
.filter-grid select:focus {
  border-color: #f9a8c9;
  box-shadow: 0 0 0 4px #fff2f7;
}

.search-box {
  position: relative;
}

.search-box i {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #f0528f;
}

.search-box input {
  padding-left: 44px;
}

.tab-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tab-list button {
  padding: 12px 16px;
  border-radius: 16px;
  background: #fff;
  color: #374151;
  border: 1px solid #ffe0ec;
}

.table-wrap {
  overflow: hidden;
  border: 1px solid #ffe0ec;
  border-radius: 22px;
  background: #fff;
}

table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  font-size: 14px;
}

thead {
  background: #fff2f7;
}

th,
td {
  padding: 15px 18px;
  border-bottom: 1px solid #fff0f6;
}

th {
  color: #374151;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

tbody tr:hover {
  background: rgba(255, 242, 247, 0.55);
}

.text-left {
  text-align: left;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.cell-main {
  color: #374151;
  font-weight: 800;
}

.money {
  color: #111827;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.status-badge {
  display: inline-flex;
  min-width: 108px;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.status-badge.success {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #d1fae5;
}

.status-badge.warning {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
}

.status-badge.info {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.status-badge.danger {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.status-badge.vip {
  background: #fff2f7;
  color: #be185d;
  border: 1px solid #ffcfe0;
}

.status-badge.potential {
  background: #eef2ff;
  color: #4338ca;
  border: 1px solid #c7d2fe;
}

.empty-cell {
  padding: 44px 18px;
  text-align: center;
}

.empty-cell strong,
.empty-cell span {
  display: block;
}

.empty-cell strong {
  color: #374151;
  font-size: 17px;
  font-weight: 900;
}

.empty-cell span {
  margin-top: 6px;
  color: #9ca3af;
  font-weight: 700;
}

@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .alert-card {
    position: static;
  }
}

@media (max-width: 992px) {
  .dashboard-page {
    padding: 18px;
  }

  .dashboard-heading,
  .data-header,
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .kpi-grid,
  .chart-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .donut-wrap {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .table-wrap {
    overflow-x: auto;
  }
}
</style>
