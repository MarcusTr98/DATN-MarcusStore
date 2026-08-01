<template>
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
        <input
          v-model.trim="filters.search"
          type="text"
          :placeholder="searchPlaceholder"
          @keyup.enter="applyFilters"
        />
      </div>

      <select v-model="filters.status" @change="applyFilters" v-if="showStatusFilter">
        <option value="">Tất cả trạng thái</option>
        <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <div v-else></div>

      <select v-if="showBrandFilter" v-model="filters.brand" @change="applyFilters">
        <option value="">Tất cả thương hiệu</option>
        <option v-for="b in brandOptions" :key="b" :value="b">{{ b }}</option>
      </select>
      <div v-else></div>

      <div></div>
    </div>

    <div class="tab-list">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.value === 'pendingOrders' && pendingCount > 0" class="tab-badge">
          {{ pendingCount }}
        </span>
      </button>
    </div>

    <div class="table-wrap">
      <div v-if="isLoading" class="loading-cell">
        <span class="spinner"></span>
        <span>Đang tải dữ liệu...</span>
      </div>

      <template v-else>
        <table>
          <thead>
            <tr>
              <th class="text-center" style="width:52px">STT</th>
              <th v-for="col in activeColumns" :key="col.key" :class="alignClass(col.align)">
                {{ col.label }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="tableRows.length === 0">
              <td :colspan="activeColumns.length + 1" class="empty-cell">
                <strong>Không có dữ liệu phù hợp</strong>
                <span>Hãy thử đổi từ khóa tìm kiếm hoặc đặt lại bộ lọc.</span>
              </td>
            </tr>
            <tr
              v-for="(row, idx) in tableRows"
              v-else
              :key="row.id ?? row.orderCode ?? row.skuCode ?? row.email"
            >
              <td class="text-center stt-cell">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
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

        <!-- FIX 7: phân trang backend — totalElements và totalPages từ server -->
        <div class="pagination-bar" v-if="totalPages > 1">
          <span class="pagination-info">
            Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, totalElements) }}
            / {{ totalElements }} bản ghi
          </span>
          <div class="pagination-btns">
            <button @click="goPage(1)"               :disabled="currentPage === 1"          class="pg-btn">«</button>
            <button @click="goPage(currentPage - 1)" :disabled="currentPage === 1"          class="pg-btn">‹</button>
            <button
              v-for="p in visiblePages"
              :key="p"
              @click="goPage(p)"
              :class="['pg-btn', { active: p === currentPage }]"
            >{{ p }}</button>
            <button @click="goPage(currentPage + 1)" :disabled="currentPage === totalPages" class="pg-btn">›</button>
            <button @click="goPage(totalPages)"       :disabled="currentPage === totalPages" class="pg-btn">»</button>
          </div>
          <select v-model="pageSize" @change="onPageSizeChange" class="page-size-select">
            <option :value="10">10 / trang</option>
            <option :value="20">20 / trang</option>
            <option :value="50">50 / trang</option>
          </select>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import statisticsApi from '@/api/statisticsApi'

const props = defineProps({
  selectedTime:    { type: String, default: 'today' },
  customDate:      { type: String, default: '' },
  childCategories: { type: Array,  default: () => [] },
  brandList:       { type: Array,  default: () => [] },
})

const tabs = [
  { label: 'Đơn hàng gần nhất',             value: 'recentOrders'  },
  { label: 'Đơn cần xử lý',                 value: 'pendingOrders' },
  { label: 'Sản phẩm đã bán ra',            value: 'topProducts'   },
  { label: 'Sản phẩm sắp hết / hết kho',   value: 'lowStock'      },
  { label: 'Khách hàng mua nhiều nhất',     value: 'topCustomers'  },
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
  pendingOrders: [
    { label: 'Mã đơn',     key: 'orderCode',    align: 'left'   },
    { label: 'Khách hàng', key: 'customerName',  align: 'left'   },
    { label: 'SĐT',        key: 'phone',         align: 'left'   },
    { label: 'Thanh toán', key: 'paymentMethod', align: 'left'   },
    { label: 'Trạng thái', key: 'orderStatus',   align: 'center', type: 'status' },
    { label: 'Tổng tiền',  key: 'totalAmount',   align: 'right',  type: 'money'  },
    { label: 'Đặt lúc',    key: 'createdAt',     align: 'left'   },
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

const currentTab    = ref('recentOrders')
const tableRows     = ref([])
const isLoading     = ref(false)
const pendingCount  = ref(0)
// FIX 7: state phân trang backend
const currentPage   = ref(1)
const pageSize      = ref(10)
const totalElements = ref(0)
const totalPages    = ref(1)

const filters = reactive({ search: '', status: '', brand: '' })

const visiblePages = computed(() => {
  const pages = []
  for (let i = Math.max(1, currentPage.value - 2); i <= Math.min(totalPages.value, currentPage.value + 2); i++) {
    pages.push(i)
  }
  return pages
})

const activeColumns    = computed(() => columnMap[currentTab.value] ?? [])
const showStatusFilter = computed(() => ['recentOrders', 'lowStock'].includes(currentTab.value))
const showBrandFilter  = computed(() => ['recentOrders', 'lowStock'].includes(currentTab.value) && brandOptions.value.length > 0)
const brandOptions     = computed(() => props.brandList)

const searchPlaceholder = computed(() => {
  if (currentTab.value === 'pendingOrders') return 'Tìm mã đơn / tên / SĐT...'
  if (currentTab.value === 'topCustomers')  return 'Tìm tên / email khách...'
  if (currentTab.value === 'topProducts')   return 'Tìm tên sản phẩm...'
  if (currentTab.value === 'lowStock')      return 'Tìm SKU / tên sản phẩm...'
  return 'Tìm mã đơn / tên khách...'
})

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

function getActivePeriod() {
  return props.customDate ? 'today' : (props.selectedTime || 'today')
}
function getActiveDates() {
  return { sd: props.customDate || '', ed: props.customDate || '' }
}

// FIX 7: nhận page param, đọc PagedResponseDTO từ backend
async function fetchTableData(page = 1) {
  const period     = getActivePeriod()
  const { sd, ed } = getActiveDates()
  const sz         = pageSize.value

  isLoading.value = true
  try {
    let res
    if (currentTab.value === 'recentOrders') {
      res = await statisticsApi.getRecentOrders(period, sd, ed, filters.search, filters.status, filters.brand, page, sz)
    } else if (currentTab.value === 'pendingOrders') {
      res = await statisticsApi.getPendingOrders(filters.search, page, sz)
    } else if (currentTab.value === 'topProducts') {
      res = await statisticsApi.getTopProducts(period, sd, ed, filters.search, page, sz)
    } else if (currentTab.value === 'lowStock') {
      res = await statisticsApi.getLowStockProducts(filters.search, filters.brand, filters.status, page, sz)
    } else if (currentTab.value === 'topCustomers') {
      res = await statisticsApi.getTopCustomers(period, sd, ed, filters.search, page, sz)
    }

    const paged = res?.data?.data
    if (paged && paged.content !== undefined) {
      // Backend trả PagedResponseDTO
      tableRows.value     = paged.content
      totalElements.value = paged.totalElements ?? 0
      totalPages.value    = paged.totalPages    ?? 1
      currentPage.value   = paged.page          ?? page
      if (currentTab.value === 'pendingOrders') {
        pendingCount.value = paged.totalElements ?? 0
      }
    } else {
      // Fallback nếu endpoint chưa trả paged
      tableRows.value     = paged ?? []
      totalElements.value = tableRows.value.length
      totalPages.value    = 1
      currentPage.value   = 1
    }
  } catch {
    tableRows.value     = []
    totalElements.value = 0
    totalPages.value    = 1
  } finally {
    isLoading.value = false
  }
}

async function fetchPendingCount() {
  try {
    const res = await statisticsApi.getPendingOrders('', 1, 1)
    pendingCount.value = res?.data?.data?.totalElements ?? 0
  } catch { pendingCount.value = 0 }
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return
  fetchTableData(p)
}

function onPageSizeChange() { fetchTableData(1) }

function applyFilters() { fetchTableData(1) }

function switchTab(val) {
  currentTab.value = val
  filters.status   = ''
  filters.brand    = ''
  fetchTableData(1)
}

function resetFilters() {
  filters.search = ''
  filters.status = ''
  filters.brand  = ''
  fetchTableData(1)
}

watch(() => props.selectedTime, () => fetchTableData(1))
watch(() => props.customDate,   () => fetchTableData(1))

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
function alignClass(align) {
  return {
    'text-center': align === 'center',
    'text-right':  align === 'right',
    'text-left':   align !== 'center' && align !== 'right',
  }
}
function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value || 0)
}

defineExpose({
  fetchTableData,
  switchToLowStock()      { switchTab('lowStock')      },
  switchToPendingOrders() { switchTab('pendingOrders') },
  pendingCount,
})

fetchTableData(1)
fetchPendingCount()
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
.dashboard-card h2 { margin: 0; color: #111827; font-weight: 900; font-size: 20px; }
.dashboard-card p  { margin: 4px 0 0; font-size: 13px; color: #6b7280; }
.data-card { display: grid; gap: 18px; }
.data-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.reset-btn {
  padding: 12px 18px; border-radius: 16px; background: #fff2f7;
  color: #e11d65; border: 1px solid #ffcfe0; cursor: pointer; font-weight: 800; transition: 0.2s ease;
}
.filter-grid { display: grid; grid-template-columns: 1.2fr 1fr 1.4fr 1fr; gap: 14px; }
.filter-grid input, .filter-grid select {
  width: 100%; height: 52px; border-radius: 16px; border: 1px solid #ffe0ec;
  background: #fff; color: #374151; font-weight: 700; font-size: 16px;
  outline: none; padding: 0 16px; box-sizing: border-box;
}
.filter-grid input:focus, .filter-grid select:focus {
  border-color: #f9a8c9; box-shadow: 0 0 0 4px #fff2f7;
}
.search-box { position: relative; }
.search-box i { position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: #f0528f; }
.search-box input { padding-left: 44px; }
.tab-list { display: flex; flex-wrap: wrap; gap: 10px; }
.tab-list button {
  position: relative; padding: 12px 16px; border-radius: 16px;
  background: #fff; color: #374151; border: 1px solid #ffe0ec;
  cursor: pointer; font-weight: 800; transition: 0.2s ease;
  display: inline-flex; align-items: center; gap: 6px;
}
.tab-list button.active { background: #ff4d8d; color: #fff; box-shadow: 0 10px 22px rgba(255,77,141,0.22); }
.tab-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 20px; height: 20px; padding: 0 5px; border-radius: 999px;
  background: #dc2626; color: #fff; font-size: 11px; font-weight: 900;
}
.tab-list button.active .tab-badge { background: rgba(255,255,255,0.3); }
.table-wrap { overflow: hidden; border: 1px solid #ffe0ec; border-radius: 22px; background: #fff; }
:deep(table) { width: 100%; min-width: 980px; border-collapse: collapse; font-size: 14px; }
:deep(thead) { background: #fff2f7; }
:deep(th), :deep(td) { padding: 15px 18px; border-bottom: 1px solid #fff0f6; white-space: nowrap; }
:deep(th) { color: #374151; font-size: 12px; font-weight: 900; letter-spacing: 0.04em; text-transform: uppercase; }
:deep(tbody tr:hover) { background: rgba(255,242,247,0.55); }
.text-left   { text-align: left; }
.text-center { text-align: center; }
.text-right  { text-align: right; font-variant-numeric: tabular-nums; }
.stt-cell  { color: #9ca3af; font-weight: 700; font-size: 13px; }
.cell-main { color: #374151; font-weight: 800; }
.money { color: #111827; font-weight: 900; font-variant-numeric: tabular-nums; white-space: nowrap; }
.status-badge {
  display: inline-flex; min-width: 108px; justify-content: center;
  padding: 7px 12px; border-radius: 999px; font-size: 12px; font-weight: 900; white-space: nowrap;
}
.status-badge.success { background: #ecfdf5; color: #047857; border: 1px solid #d1fae5; }
.status-badge.warning { background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }
.status-badge.info    { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.status-badge.danger  { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.empty-cell { padding: 44px 18px; text-align: center; }
.empty-cell strong { display: block; color: #374151; font-size: 17px; font-weight: 900; }
.empty-cell span   { display: block; margin-top: 6px; color: #9ca3af; font-weight: 700; }
.loading-cell { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 44px 18px; color: #9ca3af; font-weight: 700; }
.spinner { width: 20px; height: 20px; border: 3px solid #ffe0ec; border-top-color: #ff4d8d; border-radius: 50%; animation: spin 0.7s linear infinite; flex: none; }
@keyframes spin { to { transform: rotate(360deg); } }
.pagination-bar { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-top: 1px solid #fff0f6; gap: 12px; flex-wrap: wrap; }
.pagination-info { font-size: 13px; color: #6b7280; font-weight: 700; white-space: nowrap; }
.pagination-btns { display: flex; gap: 6px; flex-wrap: wrap; }
.pg-btn { min-width: 36px; height: 36px; padding: 0 10px; border-radius: 10px; border: 1px solid #ffe0ec; background: #fff; color: #374151; font-weight: 800; font-size: 13px; cursor: pointer; transition: 0.15s ease; }
.pg-btn:hover:not(:disabled) { background: #fff2f7; border-color: #f9a8c9; color: #e11d65; }
.pg-btn.active { background: #ff4d8d; color: #fff; border-color: #ff4d8d; box-shadow: 0 4px 12px rgba(255,77,141,0.25); }
.pg-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-size-select { height: 36px; border-radius: 10px; border: 1px solid #ffe0ec; background: #fff; color: #374151; font-weight: 700; font-size: 13px; padding: 0 10px; outline: none; cursor: pointer; }
@media (max-width: 992px) {
  .data-header  { flex-direction: column; align-items: stretch; }
  .filter-grid  { grid-template-columns: 1fr; }
  .table-wrap   { overflow-x: auto; }
  .pagination-bar { flex-direction: column; align-items: flex-start; }
}
</style>