<template>
  <section class="dashboard-card data-card">
    <div class="data-header">
      <div>
        <h2>Dữ liệu chi tiết</h2>
        <p>Lọc nhanh dữ liệu bán hàng, tồn kho và khách hàng</p>
      </div>
      <button type="button" class="reset-btn" @click="resetFilters">Đặt lại bộ lọc</button>
    </div>

    <!-- Filter bar -->
    <div class="filter-grid">
      <div class="search-box">
        <i class="bi bi-search"></i>
        <input
          v-model.trim="filters.search"
          type="text"
          placeholder="Tìm mã đơn / tên khách..."
          @keyup.enter="applyFilters"
        />
      </div>
      <select v-model="filters.status" @change="applyFilters">
        <option value="">Tất cả trạng thái</option>
        <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <select v-if="categoryOptions.length > 0" v-model="filters.category" @change="applyFilters">
        <option value="">Tất cả danh mục / thương hiệu</option>
        <option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="filters.date" @change="applyFilters">
        <option value="week">Tuần này</option>
        <option value="month">Tháng này</option>
        <option value="quarter">Quý này</option>
        <option value="year">Năm nay</option>
      </select>
    </div>

    <!-- Tabs -->
    <div class="tab-list">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >{{ tab.label }}</button>
    </div>

    <!-- Table -->
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th v-for="col in activeColumns" :key="col.key" :class="alignClass(col.align)">
              {{ col.label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="tableRows.length === 0">
            <td :colspan="activeColumns.length" class="empty-cell">
              <strong>Không có dữ liệu phù hợp</strong>
              <span>Hãy thử đổi từ khóa tìm kiếm hoặc đặt lại bộ lọc.</span>
            </td>
          </tr>
          <tr
            v-for="row in tableRows"
            v-else
            :key="row.id ?? row.orderCode ?? row.skuCode ?? row.email"
          >
            <td v-for="col in activeColumns" :key="col.key" :class="alignClass(col.align)">
              <span v-if="col.type === 'money'"   class="money">{{ formatCurrency(row[col.key]) }}</span>
              <span v-else-if="col.type === 'percent'" class="cell-main">{{ row[col.key] }}%</span>
              <span v-else-if="col.type === 'status'" class="status-badge" :class="statusClass(row[col.key])">
{{ statusLabel(row[col.key]) }}
              </span>
              <span v-else class="cell-main">{{ row[col.key] }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import statisticsApi from '@/api/statisticsApi'

const props = defineProps({
  selectedTime:     { type: String, default: 'month' },
  customDate:       { type: String, default: '' },
  childCategories:  { type: Array,  default: () => [] },
})

// ── tabs & columns ───────────────────────────────────────────
const tabs = [
  { label: 'Đơn hàng gần nhất',         value: 'recentOrders' },
  { label: 'Top sản phẩm bán chạy',     value: 'topProducts'  },
  { label: 'Sản phẩm sắp hết kho',      value: 'lowStock'     },
  { label: 'Khách hàng mua nhiều nhất', value: 'topCustomers' },
]

const columnMap = {
  recentOrders: [
    { label: 'Mã đơn',     key: 'orderCode',    align: 'left'   },
    { label: 'Khách hàng', key: 'customerName',  align: 'left'   },
    { label: 'Thanh toán', key: 'paymentMethod', align: 'left'   },
    { label: 'Trạng thái', key: 'orderStatus',   align: 'center', type: 'status' },
    { label: 'Tổng tiền',  key: 'totalAmount',   align: 'right',  type: 'money'  },
    { label: 'Thời gian',  key: 'createdAt',     align: 'left'   },
  ],
  topProducts: [
    { label: 'Sản phẩm',  key: 'productName', align: 'left'   },
    { label: 'Đã bán',    key: 'totalSold',   align: 'center' },
    { label: 'Doanh thu', key: 'revenue',     align: 'right',  type: 'money' },
  ],
  lowStock: [
    { label: 'SKU',          key: 'skuCode',      align: 'left'   },
    { label: 'Sản phẩm',    key: 'productName',  align: 'left'   },
    { label: 'Thương hiệu', key: 'brand',         align: 'left'   },
    { label: 'Tồn kho',     key: 'stockQuantity', align: 'center' },
    { label: 'Trạng thái',  key: 'status',        align: 'center', type: 'status' },
  ],
  topCustomers: [
    { label: 'Khách hàng',    key: 'customerName',        align: 'left'   },
    { label: 'Email',          key: 'email',               align: 'left'   },
    { label: 'Số đơn',        key: 'totalOrders',         align: 'center' },
    { label: 'Tổng chi tiêu', key: 'totalSpent',          align: 'right',  type: 'money'   },
    { label: 'Đóng góp',      key: 'contributionPercent', align: 'right',  type: 'percent' },
  ],
}

// ── state ────────────────────────────────────────────────────
const currentTab = ref('recentOrders')
const tableRows  = ref([])

const filters = reactive({
  search:   '',
  status:   '',
  category: '',
  date:     'month',
})

// ── computed ─────────────────────────────────────────────────
const activeColumns = computed(() => columnMap[currentTab.value] ?? [])
const statusOptions = computed(() => {
  if (currentTab.value === 'recentOrders') {
    return [
      { value: 'PENDING',    label: 'Chờ xử lý'      },
      { value: 'CONFIRMED',  label: 'Đã xác nhận'    },
      { value: 'SHIPPING',   label: 'Đang giao hàng' },
      { value: 'COMPLETED',  label: 'Hoàn thành'     },
      { value: 'CANCELLED',  label: 'Đã hủy'         },
      { value: 'PROCESSING', label: 'Đang xử lý'     },
    ]
  }
  if (currentTab.value === 'lowStock') {
    return [
      { value: 'Hết hàng',     label: 'Hết hàng'     },
      { value: 'Sắp hết hàng', label: 'Sắp hết hàng' },
    ]
  }
  return []
})

const categoryOptions = computed(() => {
  if (currentTab.value === 'recentOrders' || currentTab.value === 'lowStock') {
    return props.childCategories.map(c => c.categoryName)
  }
  return []
})

// ── fetch ────────────────────────────────────────────────────
async function fetchTableData() {
  const period = filters.date || props.selectedTime || 'month'
  const sd     = props.customDate || ''
  const ed     = props.customDate || ''

  try {
    let res
    if (currentTab.value === 'recentOrders') {
      res = await statisticsApi.getRecentOrders(50, period, sd, ed, filters.search, filters.status, filters.category)
    } else if (currentTab.value === 'topProducts') {
      res = await statisticsApi.getTopProducts(50, period, sd, ed, filters.search)
    } else if (currentTab.value === 'lowStock') {
      res = await statisticsApi.getLowStockProducts(filters.search, filters.category, filters.status)
    } else if (currentTab.value === 'topCustomers') {
      res = await statisticsApi.getTopCustomers(50, period, sd, ed, filters.search)
    }
    tableRows.value = res?.data?.data ?? []
  } catch {
    tableRows.value = []
  }
}

function applyFilters()  { fetchTableData() }

function switchTab(val) {
  currentTab.value   = val
  filters.status     = ''
  filters.category   = ''
  fetchTableData()
}

function resetFilters() {
  filters.search   = ''
  filters.status   = ''
  filters.category = ''
  filters.date     = props.selectedTime || 'month'
  fetchTableData()
}

// re-fetch khi period thay đổi từ parent
watch(() => props.selectedTime, (val) => {
  if (!val) return
  filters.date = val
  fetchTableData()
})

watch(() => props.customDate, () => {
  filters.date = props.customDate
  fetchTableData()
})

// ── helpers ──────────────────────────────────────────────────
function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

function alignClass(align) {
  return {
    'text-center': align === 'center',
    'text-right':  align === 'right',
    'text-left':   align !== 'center' && align !== 'right',
  }
}

const statusLabels = {
PENDING: 'Chờ xử lý', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang giao hàng',
  COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy', PROCESSING: 'Đang xử lý',
  PAID: 'Đã thanh toán', UNPAID: 'Chưa thanh toán',
  'Hết hàng': 'Hết hàng', 'Sắp hết hàng': 'Sắp hết hàng',
}

function statusLabel(s) { return statusLabels[s] || s }

function statusClass(status) {
  return {
    success: ['COMPLETED', 'PAID'].includes(status),
    warning: ['PENDING', 'PROCESSING', 'Sắp hết hàng'].includes(status),
    info:    ['SHIPPING', 'CONFIRMED'].includes(status),
    danger:  ['CANCELLED', 'Hết hàng', 'UNPAID'].includes(status),
  }
}

// expose để Dashboard.vue có thể trigger fetch lần đầu
defineExpose({ fetchTableData })
</script>

<style scoped>
.dashboard-card {
  background: #fff;
  border: 1px solid #ffe0ec;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.06);
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

.data-card {
  display: grid;
  gap: 18px;
}

.data-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.reset-btn {
  padding: 12px 18px;
  border-radius: 16px;
  background: #fff2f7;
  color: #e11d65;
  border: 1px solid #ffcfe0;
  cursor: pointer;
  font-weight: 800;
  transition: 0.2s ease;
}

/* Filter */
.filter-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1.4fr 1fr;
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
  font-size: 16px;
  outline: none;
  padding: 0 16px;
  box-sizing: border-box;
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

/* Tabs */
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
  cursor: pointer;
  font-weight: 800;
  transition: 0.2s ease;
}

.tab-list button.active {
  background: #ff4d8d;
  color: #fff;
  box-shadow: 0 10px 22px rgba(255, 77, 141, 0.22);
}

/* Table — dùng :deep để scoped ăn vào element selector */
.table-wrap {
  overflow: hidden;
  border: 1px solid #ffe0ec;
  border-radius: 22px;
  background: #fff;
}

:deep(table) {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  font-size: 14px;
}

:deep(thead) {
  background: #fff2f7;
}

:deep(th),
:deep(td) {
  padding: 15px 18px;
  border-bottom: 1px solid #fff0f6;
  white-space: nowrap;
}

:deep(th) {
color: #374151;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

:deep(tbody tr:hover) {
  background: rgba(255, 242, 247, 0.55);
}

.text-left   { text-align: left; }
.text-center { text-align: center; }
.text-right  { text-align: right; font-variant-numeric: tabular-nums; }

.cell-main {
  color: #374151;
  font-weight: 800;
}

.money {
  color: #111827;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* Status badge */
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

.status-badge.success   { background: #ecfdf5; color: #047857; border: 1px solid #d1fae5; }
.status-badge.warning   { background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }
.status-badge.info      { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.status-badge.danger    { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.status-badge.vip       { background: #fff2f7; color: #be185d; border: 1px solid #ffcfe0; }
.status-badge.potential { background: #eef2ff; color: #4338ca; border: 1px solid #c7d2fe; }

/* Empty state */
.empty-cell {
  padding: 44px 18px;
  text-align: center;
}

.empty-cell strong {
  display: block;
  color: #374151;
  font-size: 17px;
  font-weight: 900;
}

.empty-cell span {
  display: block;
  margin-top: 6px;
  color: #9ca3af;
  font-weight: 700;
}

@media (max-width: 992px) {
  .data-header {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .table-wrap {
    overflow-x: auto;
  }
}
</style>