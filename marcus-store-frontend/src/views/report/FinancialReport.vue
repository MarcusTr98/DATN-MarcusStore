<template>
  <div class="fin-page">
    <div class="fin-shell">
      <!-- Hero -->
      <div class="fin-hero">
        <div class="fin-hero-bg"></div>
        <div class="fin-hero-content">
          <div class="hero-title">
            <div class="hero-icon"><i class="bi bi-graph-up-arrow"></i></div>
            <div>
              <h1>Quản lý Đối soát Tài chính</h1>
              <p>Theo dõi giao dịch thu hộ, thanh toán và hoàn tiền trong hệ thống.</p>
            </div>
          </div>
          <div class="d-flex align-items-center gap-3">
            <div class="dynamic-total" v-if="filteredTotal > 0">
              Tổng giá trị lọc: <strong>{{ formatCurrency(filteredTotal) }}</strong>
            </div>
            <button
              @click="handleExportFilteredExcel"
              class="btn-export-excel"
              :disabled="exporting"
            >
              <i
                class="bi"
                :class="exporting ? 'bi-arrow-repeat spin' : 'bi-file-earmark-excel'"
              ></i>
              {{ exporting ? 'Đang xuất...' : 'Xuất Báo Cáo Excel' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Stats -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon stat-icon-blue"><i class="bi bi-receipt"></i></div>
          <div class="stat-body">
            <span>Tổng giao dịch</span>
            <strong>{{ stats.total }}</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-green"><i class="bi bi-check-circle"></i></div>
          <div class="stat-body">
            <span>Thành công</span>
            <strong class="fin-accent">{{ stats.success }}</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-amber"><i class="bi bi-hourglass-split"></i></div>
          <div class="stat-body">
            <span>Đang chờ xử lý</span>
            <strong>{{ stats.pending }}</strong>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-icon-navy"><i class="bi bi-cash-stack"></i></div>
          <div class="stat-body">
            <span>Tổng giá trị (VNĐ)</span>
            <strong>{{ formatCurrency(stats.totalAmount) }}</strong>
          </div>
        </div>
      </div>

      <!-- Mini Chart (Stacked Bar) -->
      <div class="chart-panel" v-if="filteredTransactions.length > 0">
        <div class="chart-header">
          <i class="bi bi-bar-chart-fill"></i> Phân bổ dòng tiền theo trạng thái
          <span class="chart-subnote">(theo khoảng ngày đang lọc)</span>
        </div>
        <apexchart
          type="bar"
          height="220"
          :options="chartOptions"
          :series="chartSeries"
        ></apexchart>
      </div>

      <!-- Filters -->
      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="field field-keyword">
            <label class="form-label">Tìm kiếm (Mã đơn, Ghi chú)</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                :value="keywordInput"
                @input="onSearchInput"
                type="text"
                class="form-control"
                placeholder="Nhập từ khóa..."
              />
            </div>
          </div>

          <div class="field">
            <label class="form-label">Loại giao dịch</label>
            <select v-model="filters.type" class="form-select" @change="onFilterChange">
              <option value="">Tất cả</option>
              <option value="COD_COLLECTION">Thu hộ (COD)</option>
              <option value="VNPAY_PAYMENT">Thanh toán (VNPAY)</option>
              <option value="REFUND_PENDING">Chờ hoàn tiền</option>
            </select>
          </div>

          <div class="field">
            <label class="form-label">Trạng thái</label>
            <select v-model="filters.status" class="form-select" @change="onFilterChange">
              <option value="">Tất cả</option>
              <option value="SUCCESS">Thành công</option>
              <option value="PENDING">Chờ xử lý</option>
              <option value="FAILED">Thất bại</option>
            </select>
          </div>

          <div class="field field-dates">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <label class="form-label mb-0">Thời gian</label>
              <div class="quick-dates">
                <button @click="applyDatePreset('today')" class="btn-quick-date">Hôm nay</button>
                <button @click="applyDatePreset('7days')" class="btn-quick-date">7 Ngày</button>
                <button @click="applyDatePreset('thisMonth')" class="btn-quick-date">
                  Tháng này
                </button>
                <button @click="applyDatePreset('lastMonth')" class="btn-quick-date">
                  Tháng trước
                </button>
              </div>
            </div>
            <div class="d-flex gap-2">
              <input
                v-model="filters.fromDate"
                type="date"
                class="form-control"
                @change="onFilterChange"
                title="Từ ngày"
              />
              <input
                v-model="filters.toDate"
                type="date"
                class="form-control"
                @change="onFilterChange"
                title="Đến ngày"
              />
              <button class="btn-soft" @click="resetFilters" title="Đặt lại bộ lọc">
                <i class="bi bi-arrow-counterclockwise"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Table -->
      <div class="table-panel">
        <div class="table-wrapper">
          <table class="financial-table">
            <thead>
              <tr>
                <th style="width: 56px">STT</th>
                <th>Đối soát</th>

                <th>Mã Đơn</th>
                <th>Loại Giao Dịch</th>
                <th class="text-end">Số Tiền (VNĐ)</th>
                <th>Trạng Thái</th>
                <th>Thời Gian</th>
                <th class="text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="text-center py-4">
                  <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
                </td>
              </tr>
              <tr v-else-if="pagedTransactions.length === 0">
                <td colspan="7" class="text-center py-4">
                  <i class="bi bi-inbox" style="font-size: 1.6rem; color: #9db8de"></i>
                  <div class="mt-2" style="color: #6b7280">Không có giao dịch nào phù hợp.</div>
                </td>
              </tr>
              <tr
                v-else
                v-for="(item, index) in pagedTransactions"
                :key="item.transactionId || index"
              >
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td class="text-center">
                  <div v-if="item.isReconciled" class="text-success">
                    <i class="bi bi-check-circle-fill" style="font-size: 1.3rem"></i>
                  </div>
                  <input
                    v-else
                    type="checkbox"
                    @change="confirmReconciliation(item)"
                    class="form-check-input"
                  />
                </td>

                <td class="fw-bold">{{ item.orderCode }}</td>
                <td>
                  <span :class="['badge', getTypeClass(item.type)]">
                    {{ formatType(item.type) }}
                  </span>
                </td>
                <td class="text-end text-danger fw-bold">{{ formatCurrency(item.amount) }}</td>
                <td>
                  <span :class="['badge', getStatusClass(item.status)]">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td>{{ formatDate(item.createdAt) }}</td>

                <td class="text-center">
                  <button class="btn-icon" @click="openDetailModal(item)" title="Xem chi tiết">
                    <i class="bi bi-eye"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="fin-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ filteredTransactions.length }}</strong> giao dịch
          </div>

          <div class="pagination-controls">
            <div class="page-size-group">
              <span class="page-size-label">Hiển thị</span>
              <select
                v-model.number="pageSize"
                class="form-select page-size-select"
                @change="currentPage = 1"
              >
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
              <span class="page-size-label page-size-suffix">/ trang</span>
            </div>

            <nav class="pager" aria-label="Phân trang">
              <button class="pager-arrow" :disabled="currentPage === 1" @click="currentPage = 1">
                <i class="bi bi-chevron-bar-left"></i>
              </button>
              <button class="pager-arrow" :disabled="currentPage === 1" @click="currentPage--">
                <i class="bi bi-chevron-left"></i>
              </button>
              <ul class="pager-list">
                <li v-for="(p, i) in pageItems" :key="i">
                  <span v-if="p === '...'" class="pager-ellipsis">…</span>
                  <button
                    v-else
                    class="pager-num"
                    :class="{ active: p === currentPage }"
                    @click="currentPage = p"
                  >
                    {{ p }}
                  </button>
                </li>
              </ul>
              <button
                class="pager-arrow"
                :disabled="currentPage === totalPages"
                @click="currentPage++"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
              <button
                class="pager-arrow"
                :disabled="currentPage === totalPages"
                @click="currentPage = totalPages"
              >
                <i class="bi bi-chevron-bar-right"></i>
              </button>
            </nav>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Chi tiết Giao dịch -->
    <div v-if="isModalOpen" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content">
        <div class="modal-header">
          <div class="modal-header-title">
            <span class="modal-header-icon"><i class="bi bi-file-earmark-text"></i></span>
            <h5 class="mb-0">Chi tiết Giao dịch</h5>
          </div>
          <button class="btn-close-modal" @click="closeDetailModal" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="id-row">
            <div class="id-block">
              <span class="detail-label">Mã giao dịch nội bộ</span>
              <strong class="font-monospace">
                {{ selectedTransaction?.transactionId || selectedTransaction?.id || '---' }}
              </strong>
            </div>
            <div class="id-block id-block-order">
              <span class="detail-label">Mã đơn hàng</span>
              <div class="d-flex align-items-center gap-2">
                <strong class="font-monospace text-primary">{{
                  selectedTransaction?.orderCode
                }}</strong>
                <button
                  class="btn-copy"
                  @click="copyToClipboard(selectedTransaction?.orderCode)"
                  title="Sao chép"
                >
                  <i class="bi bi-clipboard"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="badge-row">
            <div class="detail-group">
              <span class="detail-label">Loại GD</span>
              <span :class="['badge', 'badge-lg', getTypeClass(selectedTransaction?.type)]">
                {{ formatType(selectedTransaction?.type) }}
              </span>
            </div>
            <div class="detail-group">
              <span class="detail-label">Trạng thái</span>
              <span :class="['badge', 'badge-lg', getStatusClass(selectedTransaction?.status)]">
                {{ formatStatus(selectedTransaction?.status) }}
              </span>
            </div>
          </div>

          <div class="amount-box">
            <span class="detail-label">Số tiền</span>
            <strong class="amount-value">{{ formatCurrency(selectedTransaction?.amount) }}</strong>
          </div>

          <div class="note-box">
            <span class="detail-label"><i class="bi bi-info-circle"></i> Ghi chú (Log)</span>
            <p class="note-text">{{ selectedTransaction?.note || 'Không có ghi chú' }}</p>
          </div>

          <!-- Thông tin mở rộng Order -->
          <div v-if="selectedTransaction?.recipientName" class="recipient-box">
            <h6 class="recipient-title">
              <i class="bi bi-person-lines-fill"></i> Thông tin người nhận
            </h6>
            <div class="detail-group mb-2">
              <span class="detail-label">Tên &amp; SĐT</span>
              <strong
                >{{ selectedTransaction.recipientName || '---' }} ·
                {{ selectedTransaction.recipientPhone || '---' }}</strong
              >
            </div>
            <div class="detail-group mb-0">
              <span class="detail-label">Địa chỉ giao</span>
              <span class="text-muted address-text">
                {{ selectedTransaction.shippingAddress || '---' }}
              </span>
            </div>
          </div>

          <div class="modal-footer-note">
            <i class="bi bi-clock-history"></i>
            Tạo lúc: {{ formatDate(selectedTransaction?.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <transition name="fade">
      <div v-if="toast.show" class="toast-alert" :class="{ error: toast.type === 'error' }">
        <i
          class="bi"
          :class="toast.type === 'error' ? 'bi-x-circle-fill' : 'bi-check-circle-fill'"
        ></i>
        <div>
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import financialApi from '@/api/financialApi'

// State dữ liệu
const transactions = ref([])
const loading = ref(false)
const exporting = ref(false)

// State Modal
const isModalOpen = ref(false)
const selectedTransaction = ref(null)

// Bộ lọc
const keywordInput = ref('')
let searchTimeout = null
const filters = reactive({ keyword: '', type: '', status: '', fromDate: '', toDate: '' })

// Phân trang
const currentPage = ref(1)
const pageSize = ref(10)

// Cảnh báo Toast
const toast = reactive({ show: false, type: 'success', title: '', message: '' })
let toastTimer = null
const showToast = (type, title, message) => {
  toast.show = true
  toast.type = type
  toast.title = title
  toast.message = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.show = false), 3000)
}

const fetchTransactions = async () => {
  loading.value = true
  try {
    const res = await financialApi.getTransactions()
    // Lấy đúng mảng transactions từ response
    const data = res.data?.transactions || res.data || []

    transactions.value = data
      .map((t) => ({
        ...t,
        // Ưu tiên lấy orderCode trực tiếp, nếu không có mới tìm trong order
        orderCode: t.orderCode || t.order?.orderCode || '---',
      }))
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  } catch {
    showToast('error', 'Lỗi', 'Không thể tải dữ liệu')
  } finally {
    loading.value = false
  }
}

//TỐI ƯU UX/UI: DEBOUNCE & QUICK DATES
const onSearchInput = (e) => {
  keywordInput.value = e.target.value
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    filters.keyword = keywordInput.value
    currentPage.value = 1
  }, 300) // 300ms debounce
}

const applyDatePreset = (preset) => {
  const today = new Date()
  filters.toDate = today.toISOString().split('T')[0]

  if (preset === 'today') {
    filters.fromDate = filters.toDate
  } else if (preset === '7days') {
    const past7 = new Date()
    past7.setDate(today.getDate() - 6)
    filters.fromDate = past7.toISOString().split('T')[0]
  } else if (preset === 'thisMonth') {
    // Tháng này: lấy trọn từ ngày 1 đến ngày cuối cùng của tháng (không dừng ở hôm nay)
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0)
    filters.fromDate = firstDay.toISOString().split('T')[0]
    filters.toDate = lastDay.toISOString().split('T')[0]
  } else if (preset === 'lastMonth') {
    // Tháng trước: cả fromDate và toDate đều thuộc tháng trước, không phải hôm nay
    const firstDayLastMonth = new Date(today.getFullYear(), today.getMonth() - 1, 1)
    const lastDayLastMonth = new Date(today.getFullYear(), today.getMonth(), 0)
    filters.fromDate = firstDayLastMonth.toISOString().split('T')[0]
    filters.toDate = lastDayLastMonth.toISOString().split('T')[0]
  }
  currentPage.value = 1
}

const onFilterChange = () => {
  currentPage.value = 1
}

const resetFilters = () => {
  keywordInput.value = ''
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  filters.fromDate = ''
  filters.toDate = ''
  currentPage.value = 1
}

// COMPUTED: LỌC & TỔNG TIỀN ĐỘNG
const filteredTransactions = computed(() => {
  return transactions.value.filter((item) => {
    const kw = filters.keyword.trim().toLowerCase()
    if (kw) {
      const matchKw =
        item.orderCode.toLowerCase().includes(kw) || item.note?.toLowerCase().includes(kw)
      if (!matchKw) return false
    }
    if (filters.type && item.type !== filters.type) return false
    if (filters.status && item.status !== filters.status) return false
    if (filters.fromDate) {
      if (new Date(item.createdAt) < new Date(filters.fromDate)) return false
    }
    if (filters.toDate) {
      const to = new Date(filters.toDate)
      to.setHours(23, 59, 59, 999)
      if (new Date(item.createdAt) > to) return false
    }
    return true
  })
})

const filteredTotal = computed(() => {
  return filteredTransactions.value
    .filter((t) => t.status === 'SUCCESS')
    .reduce((sum, t) => sum + (Number(t.amount) || 0), 0)
})

// MODAL & UTILS
const openDetailModal = (item) => {
  console.log('Dữ liệu dòng được chọn:', item) //log ktra lỗi
  selectedTransaction.value = item
  isModalOpen.value = true
}
const closeDetailModal = () => {
  isModalOpen.value = false
  selectedTransaction.value = null
}

const copyToClipboard = async (text) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    showToast('success', 'Đã sao chép', `Copied: ${text}`)
  } catch (err) {
    console.error('Copy failed', err)
  }
}

// XUẤT EXCEL CHỈ XUẤT DATA ĐANG LỌC
const handleExportFilteredExcel = () => {
  exporting.value = true
  try {
    if (filteredTransactions.value.length === 0) {
      showToast('error', 'Lỗi', 'Không có dữ liệu để xuất!')
      exporting.value = false
      return
    }

    const BOM = '\uFEFF'
    let csvContent = BOM + 'STT,Mã Đơn,Loại Giao Dịch,Số Tiền,Trạng Thái,Ghi Chú,Thời Gian\n'

    filteredTransactions.value.forEach((item, index) => {
      const row = [
        index + 1,
        item.orderCode,
        formatType(item.type),
        item.amount,
        formatStatus(item.status),
        `"${item.note ? item.note.replace(/"/g, '""') : ''}"`,
        formatDate(item.createdAt),
      ]
      csvContent += row.join(',') + '\n'
    })

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `DoiSoat_Filtered_${new Date().getTime()}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    showToast('success', 'Thành công', 'Xuất báo cáo theo bộ lọc hoàn tất!')
  } catch {
    showToast('error', 'Lỗi xuất file', 'Đã xảy ra lỗi trong quá trình tạo file.')
  } finally {
    exporting.value = false
  }
}

// BIỂU ĐỒ APEXCHARTS
// Parse chuỗi 'YYYY-MM-DD' thành Date theo giờ local (tránh lệch ngày do quy đổi UTC)
const parseLocalDate = (str) => {
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

const MAX_CONTINUOUS_CHART_DAYS = 31 // đủ cho preset "Tháng này"

// Gom toàn bộ logic group-by-ngày vào MỘT computed duy nhất, dùng chung
// cho cả chartSeries và chartOptions => tránh lệch dữ liệu giữa 2 bên,
// và luôn bám sát filteredTransactions (đúng bộ lọc đang chọn).
const chartData = computed(() => {
  const grouped = {}

  filteredTransactions.value.forEach((t) => {
    const dStr = new Date(t.createdAt).toLocaleDateString('vi-VN')
    if (!grouped[dStr]) grouped[dStr] = { SUCCESS: 0, PENDING: 0, FAILED: 0 }
    if (grouped[dStr][t.status] !== undefined) {
      grouped[dStr][t.status] += Number(t.amount) || 0
    }
  })

  let categories = null

  // Khi có bộ lọc khoảng ngày (kể cả preset Hôm nay / 7 Ngày / Tháng này), hiển thị
  // ĐỦ các ngày trong khoảng đó => ngày không có giao dịch vẫn hiện với giá trị 0
  if (filters.fromDate && filters.toDate) {
    const start = parseLocalDate(filters.fromDate)
    const end = parseLocalDate(filters.toDate)
    const dayCount = Math.round((end - start) / 86400000) + 1

    if (dayCount > 0 && dayCount <= MAX_CONTINUOUS_CHART_DAYS) {
      categories = []
      const cursor = new Date(start)
      for (let i = 0; i < dayCount; i++) {
        categories.push(cursor.toLocaleDateString('vi-VN'))
        cursor.setDate(cursor.getDate() + 1)
      }
    }
  }

  // Không lọc theo ngày (hoặc khoảng quá dài) => chỉ hiện các ngày có dữ liệu,
  // theo thứ tự cũ => mới, tối đa 7 ngày gần nhất.
  if (!categories) {
    categories = Object.keys(grouped).reverse().slice(-7)
  }

  return {
    categories,
    successData: categories.map((d) => grouped[d]?.SUCCESS || 0),
    pendingData: categories.map((d) => grouped[d]?.PENDING || 0),
    failedData: categories.map((d) => grouped[d]?.FAILED || 0),
  }
})

const chartSeries = computed(() => [
  { name: 'Thành công', data: chartData.value.successData },
  { name: 'Đang treo', data: chartData.value.pendingData },
  { name: 'Thất bại/Hủy', data: chartData.value.failedData },
])

const chartOptions = computed(() => {
  const categories = chartData.value.categories
  const todayLabel = new Date().toLocaleDateString('vi-VN')
  const isToday = categories.includes(todayLabel)

  return {
    chart: { type: 'bar', stacked: true, toolbar: { show: false }, fontFamily: 'Inter' },
    plotOptions: { bar: { columnWidth: '40%', borderRadius: 4 } },
    colors: ['#1f9d5e', '#f29c1f', '#e0445c'],
    xaxis: {
      categories,
      labels: {
        style: {
          // Tô đậm màu cho nhãn ngày hôm nay, các ngày khác giữ màu xám như cũ
          colors: categories.map((c) => (c === todayLabel ? '#0b3d91' : '#6b7c93')),
        },
      },
    },
    yaxis: { labels: { formatter: (val) => new Intl.NumberFormat('vi-VN').format(val) } },
    legend: { position: 'top', horizontalAlign: 'right' },
    dataLabels: { enabled: false },
    fill: { opacity: 1 },
    // Đánh dấu cột của ngày hôm nay bằng 1 dải nền + nhãn "Hôm nay"
    annotations: {
      xaxis: isToday
        ? [
            {
              x: todayLabel,
              borderColor: '#0b3d91',
              fillColor: 'rgba(11, 61, 145, 0.14)',
              label: {
                text: 'Hôm nay',
                orientation: 'horizontal',
                style: {
                  color: '#ffffff',
                  background: '#0b3d91',
                  fontSize: '10px',
                  fontWeight: 700,
                },
              },
            },
          ]
        : [],
    },
  }
})

// PHÂN TRANG & THỐNG KÊ GỐC
const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredTransactions.value.length / pageSize.value)),
)
const pagedTransactions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTransactions.value.slice(start, start + pageSize.value)
})

const pageItems = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const delta = 1
  const items = []
  const range = []
  for (let i = Math.max(2, current - delta); i <= Math.min(total - 1, current + delta); i++)
    range.push(i)
  items.push(1)
  if (range.length && range[0] > 2) items.push('...')
  items.push(...range)
  if (range.length && range[range.length - 1] < total - 1) items.push('...')
  if (total > 1) items.push(total)
  return items
})

watch(totalPages, (val) => {
  if (currentPage.value > val) currentPage.value = val
})

// Thống kê tổng quan (dựa trên toàn bộ transactions, không bị ảnh hưởng bởi bộ lọc)
const stats = computed(() => {
  const dataset = transactions.value
  return {
    total: dataset.length,
    success: dataset.filter((t) => t.status === 'SUCCESS').length,
    pending: dataset.filter((t) => t.status === 'PENDING').length,
    totalAmount: dataset.reduce((sum, t) => sum + (Number(t.amount) || 0), 0),
  }
})

// UTILS FORMATTING
const formatCurrency = (value) => new Intl.NumberFormat('vi-VN').format(value || 0)
const formatDate = (dateString) => (dateString ? new Date(dateString).toLocaleString('vi-VN') : '')
const formatType = (type) => {
  if (type === 'COD_COLLECTION') return 'Thu hộ (COD)'
  if (type === 'VNPAY_PAYMENT') return 'Thanh toán (VNPAY)'
  if (type === 'REFUND_PENDING') return 'Chờ hoàn tiền'
  return type
}

const formatStatus = (status) => {
  if (status === 'SUCCESS') return 'Thành công'
  if (status === 'PENDING') return 'Chờ xử lý'
  if (status === 'FAILED') return 'Thất bại'
  return status
}

const getTypeClass = (type) =>
  type === 'VNPAY_PAYMENT' ? 'bg-primary' : type === 'REFUND_PENDING' ? 'bg-warning' : 'bg-info'

const getStatusClass = (status) =>
  status === 'SUCCESS' ? 'bg-success' : status === 'PENDING' ? 'bg-warning' : 'bg-danger'

const confirmReconciliation = async (item) => {
  const originalStatus = item.isReconciled
  item.isReconciled = true // Tích V ngay trên UI

  try {
    // Gọi API cập nhật DB
    await financialApi.reconcile(item.transactionId, true)
    showToast('success', 'Thành công', 'Đã đối soát đơn ' + item.orderCode)
  } catch {
    item.isReconciled = originalStatus // Hoàn tác nếu lỗi
    showToast('error', 'Lỗi', 'Không thể cập nhật trạng thái đối soát.')
  }
}

onMounted(() => {
  fetchTransactions()
})
</script>

<style scoped>
.fin-page {
  min-height: 100%;
  padding: 28px;
  background: #eef3fb;
  font-family: 'Inter', sans-serif;
}
.fin-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* Hero */
.fin-hero {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(120deg, #0b3d91 0%, #1c64d6 55%, #2f80ed 100%);
  box-shadow: 0 14px 32px -12px rgba(15, 64, 152, 0.45);
}
.fin-hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 88% -10%, rgba(255, 255, 255, 0.22) 0%, transparent 45%),
    radial-gradient(circle at 8% 120%, rgba(255, 255, 255, 0.12) 0%, transparent 50%);
  pointer-events: none;
}
.fin-hero-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 18px;
  padding: 28px 32px;
}
.hero-title {
  display: flex;
  align-items: center;
  gap: 18px;
}
.hero-icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  color: #ffffff;
  font-size: 1.5rem;
  backdrop-filter: blur(4px);
}
.hero-title h1 {
  margin: 0 0 4px;
  font-size: 1.5rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.01em;
}
.hero-title p {
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.92rem;
}

/* Dynamic Total & Excel Btn */
.dynamic-total {
  background: rgba(0, 0, 0, 0.15);
  color: #fff;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 0.9rem;
}
.btn-export-excel {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 10px;
  background: #ffffff;
  color: #0b3d91;
  font-weight: 700;
  padding: 12px 20px;
  font-size: 0.92rem;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.18);
  transition:
    background-color 0.18s ease,
    transform 0.12s ease,
    box-shadow 0.18s ease;
  cursor: pointer;
}
.btn-export-excel:hover:not(:disabled) {
  background: #f0f6ff;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.22);
}
.btn-export-excel:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #ffffff;
  border: 1px solid #dce8f9;
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 4px 14px -8px rgba(28, 100, 214, 0.18);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px -8px rgba(28, 100, 214, 0.3);
}
.stat-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: #ffffff;
}
.stat-icon-blue {
  background: linear-gradient(135deg, #2f80ed, #1c64d6);
}
.stat-icon-green {
  background: linear-gradient(135deg, #34c77b, #1f9d5e);
}
.stat-icon-amber {
  background: linear-gradient(135deg, #ffb547, #f29c1f);
}
.stat-icon-navy {
  background: linear-gradient(135deg, #1c64d6, #0b3d91);
}
.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-body span {
  font-size: 0.8rem;
  color: #6b7c93;
  font-weight: 600;
}
.stat-body strong {
  font-size: 1.4rem;
  font-weight: 800;
  color: #0f2c5c;
}
.fin-accent {
  color: #1c64d6;
}

/* Chart Panel */
.chart-panel {
  background: #fff;
  border: 1px solid #dce8f9;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 4px 14px -10px rgba(28, 100, 214, 0.16);
}
.chart-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-weight: 700;
  color: #1f3a63;
  margin-bottom: 10px;
  font-size: 0.95rem;
}
.chart-subnote {
  font-weight: 500;
  font-size: 0.78rem;
  color: #8ba0c2;
}

/* Filters */
.toolbar-panel {
  background: #ffffff;
  border: 1px solid #dce8f9;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 4px 14px -10px rgba(28, 100, 214, 0.16);
}
.toolbar-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 2fr;
  gap: 20px;
  align-items: end;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-dates {
  gap: 8px;
}
.form-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #2f6fc4;
  text-transform: uppercase;
  margin: 0;
}
.form-control,
.form-select {
  height: 42px;
  width: 100px;
  padding: 0 12px;
  border: 1px solid #d6e6fb;
  border-radius: 9px;
  background-color: #f7fbff;
  font-size: 0.88rem;
  outline: none;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}
.form-control:focus,
.form-select:focus {
  border-color: #2f80ed;
  background-color: #ffffff;
  box-shadow: 0 0 0 0.18rem rgba(47, 128, 237, 0.16);
}
.input-group {
  display: flex;
  align-items: stretch;
  border: 1px solid #d6e6fb;
  border-radius: 9px;
  background-color: #f7fbff;
}
.input-group-text {
  display: flex;
  align-items: center;
  padding: 0 12px;
  color: #5d8fd9;
  border-right: 1px solid #d6e6fb;
}
.input-group .form-control {
  width: 100%;
  border: none;
  border-radius: 0;
  background: transparent;
}
.field-dates .form-control {
  width: auto;
  flex: 1;
}
.quick-dates {
  display: flex;
  gap: 6px;
}
.btn-quick-date {
  font-size: 0.75rem;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid #d6e6fb;
  background: #fff;
  color: #2f80ed;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}
.btn-quick-date:hover {
  background: #2f80ed;
  color: #ffffff;
}
.btn-soft {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #d6e6fb;
  border-radius: 9px;
  background: #f7fbff;
  color: #1c64d6;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    transform 0.12s ease;
}
.btn-soft:hover {
  background: #e3effd;
  transform: rotate(-25deg);
}

/* Table */
.table-panel {
  background: #ffffff;
  border: 1px solid #dce8f9;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 20px -12px rgba(28, 100, 214, 0.22);
}
.table-wrapper {
  overflow-x: auto;
}
.financial-table {
  width: 100%;
  border-collapse: collapse;
}
.financial-table th,
.financial-table td {
  padding: 14px 18px;
  text-align: left;
  border-bottom: 1px solid #e9f1fb;
  font-size: 0.9rem;
  color: #1f3a63;
  white-space: nowrap;
}
.financial-table thead th {
  background: linear-gradient(180deg, #eef6ff, #e3effd);
  color: #1c64d6;
  font-size: 0.76rem;
  font-weight: 800;
  text-transform: uppercase;
  border-bottom: 1px solid #d6e6fb;
}
.financial-table tbody tr {
  transition: background-color 0.12s ease;
}
.financial-table tbody tr:hover {
  background: #f5faff;
}
.badge {
  display: inline-block;
  padding: 5px 13px;
  border-radius: 20px;
  color: white;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.01em;
}
.bg-success {
  background-color: #1f9d5e;
}
.bg-warning {
  background-color: #f29c1f;
  color: #ffffff;
}
.bg-danger {
  background-color: #e0445c;
}
.bg-primary {
  background-color: #2f80ed;
}
.bg-info {
  background-color: #0b3d91;
}
.btn-icon {
  background: none;
  border: none;
  color: #2f80ed;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: background-color 0.15s ease;
}
.btn-icon:hover {
  background: #eef6ff;
}
.fw-bold {
  font-weight: 700;
}
.text-end {
  text-align: right;
}
.text-center {
  text-align: center;
}
.text-danger {
  color: #e0445c;
}

/* Pager */
.fin-pagination {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-top: 1px solid #e9f1fb;
  background: #ffffff;
  overflow-x: auto;
}
.pagination-summary {
  flex-shrink: 0;
  color: #6b7c93;
  font-size: 0.9rem;
  white-space: nowrap;
}
.pagination-controls {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 28px;
}
.page-size-group {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.page-size-label {
  white-space: nowrap;
}
.pager {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
  padding: 4px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
.pager-arrow,
.pager-num {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #4a5d80;
  font-weight: 700;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}
.pager-arrow:hover:not(:disabled),
.pager-num:hover {
  background: #eef6ff;
  color: #1c64d6;
}
.pager-num.active {
  background: #0b3d91;
  color: #ffffff;
}
.pager-list {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
}

/* Modal Chi tiết */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 44, 92, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  backdrop-filter: blur(3px);
  padding: 20px;
}
.modal-content {
  background: #fff;
  width: 480px;
  max-width: 100%;
  max-height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(11, 61, 145, 0.28);
  overflow: hidden;
  animation: modalPop 0.18s ease-out;
}
.modal-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 22px;
  background: linear-gradient(120deg, #0b3d91 0%, #1c64d6 65%, #2f80ed 100%);
  color: #ffffff;
}
.modal-header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.modal-header-title h5 {
  font-size: 1.02rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.01em;
}
.modal-header-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  font-size: 1.05rem;
}
.btn-close-modal {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.14);
  border: none;
  border-radius: 9px;
  font-size: 1rem;
  cursor: pointer;
  color: #ffffff;
  transition:
    background-color 0.15s ease,
    transform 0.12s ease;
}
.btn-close-modal:hover {
  background: rgba(224, 68, 92, 0.85);
  transform: rotate(90deg);
}
.modal-body {
  padding: 22px;
  overflow-y: auto;
}
.detail-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}
.detail-label {
  font-size: 0.72rem;
  color: #6b7c93;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  font-weight: 700;
}

/* Mã GD nội bộ / mã đơn hàng */
.id-row {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #f7fbff;
  border: 1px solid #e3effd;
  border-radius: 12px;
}
.id-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.id-block strong {
  font-size: 0.95rem;
  color: #0f2c5c;
}
.id-block-order {
  border-left: 1px solid #d6e6fb;
  padding-left: 12px;
}
.btn-copy {
  flex-shrink: 0;
  background: #eef6ff;
  border: none;
  color: #2f80ed;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: background-color 0.15s ease;
}
.btn-copy:hover {
  background: #d6e6fb;
}

/* Loại GD / Trạng thái */
.badge-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.badge-lg {
  padding: 8px 16px;
  font-size: 0.82rem;
  width: fit-content;
}

/* Số tiền */
.amount-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 18px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fff5f6, #fdeaec);
  border: 1px solid #f7d3d8;
  border-radius: 12px;
}
.amount-value {
  font-size: 1.55rem;
  font-weight: 800;
  color: #e0445c;
  letter-spacing: -0.01em;
}

/* Ghi chú */
.note-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #f7fbff;
  border: 1px solid #e3effd;
  border-radius: 12px;
}
.note-text {
  margin: 0;
  color: #46587a;
  font-size: 0.88rem;
  line-height: 1.5;
}

/* Người nhận */
.recipient-box {
  padding: 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border: 1px dashed #d6e6fb;
  border-radius: 12px;
}
.recipient-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 0.8rem;
  font-weight: 800;
  color: #1c64d6;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.address-text {
  font-size: 0.9rem;
  line-height: 1.5;
}

.modal-footer-note {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  color: #9aa8bf;
  font-size: 0.78rem;
}

/* Toast & Animations */
.toast-alert {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 1100;
  display: flex;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 12px;
  background: #ffffff;
  border-left: 4px solid #1f9d5e;
  box-shadow: 0 14px 32px rgba(15, 64, 152, 0.2);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}

.toast-alert div {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.toast-alert.error {
  border-left-color: #e0445c;
}
.toast-alert i {
  font-size: 1.2rem;
  color: #1f9d5e;
}
.toast-alert.error i {
  color: #e0445c;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.spin {
  animation: spin 1s linear infinite;
  display: inline-block;
}
@keyframes modalPop {
  from {
    transform: translateY(-12px) scale(0.98);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}
@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .toolbar-row {
    grid-template-columns: 1fr 1fr;
  }
  .field-keyword,
  .field-dates {
    grid-column: 1 / -1;
  }
}
@media (max-width: 576px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .fin-hero-content {
    padding: 22px;
  }
  .id-row,
  .badge-row {
    grid-template-columns: 1fr;
  }
  .id-block-order {
    border-left: none;
    padding-left: 0;
    padding-top: 10px;
    border-top: 1px solid #d6e6fb;
  }
}
.form-check-input {
  width: 1.4rem !important;
  height: 1.4rem !important;
  cursor: pointer;
  accent-color: #1f9d5e;
  border: 2px solid #ced4da;
}

.form-check-input:checked {
  background-color: #0b3d91 !important;
  border-color: #0b3d91 !important;
}

.form-check-input:disabled {
  opacity: 1 !important;
  cursor: not-allowed;
  background-color: #e9ecef;
}
</style>
