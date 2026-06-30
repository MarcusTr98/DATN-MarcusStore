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
          <button @click="handleExportExcel" class="btn-export-excel" :disabled="exporting">
            <i class="bi" :class="exporting ? 'bi-arrow-repeat spin' : 'bi-file-earmark-excel'"></i>
            {{ exporting ? 'Đang xuất...' : 'Xuất Báo Cáo Excel' }}
          </button>
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

      <!-- Filters -->
      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="field field-keyword">
            <label class="form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                v-model="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Tìm theo mã đơn, ghi chú..."
                @input="onFilterChange"
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

          <div class="field">
            <label class="form-label">Từ ngày</label>
            <input
              v-model="filters.fromDate"
              type="date"
              class="form-control"
              @change="onFilterChange"
            />
          </div>

          <div class="field">
            <label class="form-label">Đến ngày</label>
            <div class="d-flex gap-2">
              <input
                v-model="filters.toDate"
                type="date"
                class="form-control"
                @change="onFilterChange"
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
                <th>Mã Đơn</th>
                <th>Loại Giao Dịch</th>
                <th class="text-end">Số Tiền (VNĐ)</th>
                <th>Trạng Thái</th>
                <th>Ghi Chú</th>
                <th>Thời Gian</th>
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
                :key="item.orderCode + '-' + item.createdAt"
              >
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td class="fw-bold">{{ item.orderCode || item.order?.orderCode || '---' }}</td>
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
                <td>{{ item.note || '—' }}</td>
                <td>{{ formatDate(item.createdAt) }}</td>
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
              <button
                class="pager-arrow"
                :disabled="currentPage === 1"
                @click="currentPage = 1"
                title="Trang đầu"
              >
                <i class="bi bi-chevron-bar-left"></i>
              </button>
              <button
                class="pager-arrow"
                :disabled="currentPage === 1"
                @click="currentPage--"
                title="Trang trước"
              >
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
                title="Trang sau"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
              <button
                class="pager-arrow"
                :disabled="currentPage === totalPages"
                @click="currentPage = totalPages"
                title="Trang cuối"
              >
                <i class="bi bi-chevron-bar-right"></i>
              </button>
            </nav>
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

const transactions = ref([])
const loading = ref(false)
const exporting = ref(false)

const filters = reactive({
  keyword: '',
  type: '',
  status: '',
  fromDate: '',
  toDate: '',
})

const currentPage = ref(1)
const pageSize = ref(10)

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

    // FIX: Backend giờ trả object { transactions: [...], totalCount, ... }
    // nên phải lấy field transactions ra, không lấy thẳng object làm mảng
    const payload = res.data?.data || res.data || res || {}
    let rawData = Array.isArray(payload) ? payload : payload.transactions || []

    if (Array.isArray(rawData)) {
      // Ép kiểu sắp xếp mới nhất lên đầu
      rawData.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      transactions.value = rawData
    } else {
      transactions.value = []
      console.warn('API trả về không phải là mảng hợp lệ:', rawData)
    }
  } catch (error) {
    console.error('Lỗi khi lấy dữ liệu đối soát:', error)
    showToast(
      'error',
      'Lỗi tải dữ liệu',
      'Không thể tải dữ liệu. Vui lòng kiểm tra quyền truy cập!',
    )
  } finally {
    loading.value = false
  }
}

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const response = await financialApi.exportExcel()
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Doi_Soat_Tai_Chinh_${new Date().getTime()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    showToast('success', 'Thành công', 'Xuất báo cáo Excel hoàn tất!')
  } catch (error) {
    console.error('Lỗi khi tải Excel:', error)
    showToast('error', 'Thất bại', 'Xuất file thất bại, vui lòng thử lại!')
  } finally {
    exporting.value = false
  }
}

const onFilterChange = () => {
  currentPage.value = 1
}

const resetFilters = () => {
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  filters.fromDate = ''
  filters.toDate = ''
  currentPage.value = 1
}

const filteredTransactions = computed(() => {
  return transactions.value.filter((item) => {
    const kw = filters.keyword.trim().toLowerCase()
    if (kw) {
      const matchKw =
        item.orderCode?.toLowerCase().includes(kw) ||
        item.order?.orderCode?.toLowerCase().includes(kw) ||
        item.note?.toLowerCase().includes(kw)
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
  for (let i = Math.max(2, current - delta); i <= Math.min(total - 1, current + delta); i++) {
    range.push(i)
  }

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

const stats = computed(() => {
  const total = transactions.value.length
  const success = transactions.value.filter((t) => t.status === 'SUCCESS').length
  const pending = transactions.value.filter((t) => t.status === 'PENDING').length
  const totalAmount = transactions.value.reduce((sum, t) => sum + (Number(t.amount) || 0), 0)
  return { total, success, pending, totalAmount }
})

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN').format(value || 0)
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('vi-VN')
}

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

const getTypeClass = (type) => {
  if (type === 'VNPAY_PAYMENT') return 'bg-primary'
  if (type === 'REFUND_PENDING') return 'bg-warning'
  return 'bg-info'
}

const getStatusClass = (status) => {
  return status === 'SUCCESS' ? 'bg-success' : status === 'PENDING' ? 'bg-warning' : 'bg-danger'
}

onMounted(() => {
  fetchTransactions()
})
</script>

<style scoped>
/* ============ Layout shell ============ */
.fin-page {
  min-height: 100%;
  padding: 28px;
  background: #eef3fb;
  font-family:
    'Inter',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    Helvetica,
    Arial,
    sans-serif;
}

.fin-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* ============ Hero ============ */
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

/* ============ Excel export button ============ */
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
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.22);
}

.btn-export-excel:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 576px) {
  .btn-export-excel {
    width: 100%;
  }
}

/* ============ Stats ============ */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
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
  box-shadow: 0 10px 22px -10px rgba(28, 100, 214, 0.3);
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

/* ============ Toolbar / filters ============ */
.toolbar-panel {
  background: #ffffff;
  border: 1px solid #dce8f9;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 4px 14px -10px rgba(28, 100, 214, 0.16);
}

.toolbar-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr 1fr;
  gap: 16px;
  align-items: end;
}

@media (max-width: 992px) {
  .toolbar-row {
    grid-template-columns: 1fr 1fr;
  }
  .field-keyword {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .toolbar-row {
    grid-template-columns: 1fr;
  }
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #2f6fc4;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin: 0;
}

.form-control,
.form-select {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #d6e6fb;
  border-radius: 9px;
  background-color: #f7fbff;
  color: #0f2c5c;
  font-size: 0.88rem;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.form-control:hover,
.form-select:hover {
  border-color: #b9d6f7;
  box-shadow: 0 0 0 0.12rem rgba(47, 128, 237, 0.08);
}

.form-control:focus,
.form-select:focus {
  outline: none;
  border-color: #2f80ed;
  background-color: #ffffff;
  box-shadow: 0 0 0 0.18rem rgba(47, 128, 237, 0.16);
}

.input-group {
  display: flex;
  align-items: stretch;
  border: 1px solid #d6e6fb;
  border-radius: 9px;
  overflow: hidden;
  background-color: #f7fbff;
  transition:
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.input-group:focus-within {
  border-color: #2f80ed;
  box-shadow: 0 0 0 0.18rem rgba(47, 128, 237, 0.16);
}

.input-group-text {
  display: flex;
  align-items: center;
  padding: 0 12px;
  color: #5d8fd9;
  background-color: #eef6ff;
  border-right: 1px solid #d6e6fb;
}

.input-group .form-control {
  border: none;
  border-radius: 0;
  background: transparent;
}

.input-group .form-control:focus {
  box-shadow: none;
}

.d-flex {
  display: flex;
}
.gap-2 {
  gap: 8px;
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
    border-color 0.15s ease,
    transform 0.1s ease;
}

.btn-soft:hover:not(:disabled) {
  border-color: #b9d6f7;
  background: #eef6ff;
}

.btn-soft:active:not(:disabled) {
  transform: scale(0.97);
}

/* ============ Table ============ */
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
  letter-spacing: 0.04em;
  border-bottom: 1px solid #d6e6fb;
  position: sticky;
  top: 0;
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
.text-danger {
  color: #e0445c;
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
.py-4 {
  padding-top: 32px;
  padding-bottom: 32px;
}
.mt-2 {
  margin-top: 8px;
}

/* ============ Pagination ============ */
.fin-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  padding: 18px 22px;
  border-top: 1px solid #e9f1fb;
  background: #f9fcff;
}

.pagination-summary {
  color: #6b7c93;
  font-size: 0.9rem;
}

.pagination-summary strong {
  color: #1c64d6;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.page-size-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-label {
  color: #6b7c93;
  font-size: 0.85rem;
  white-space: nowrap;
}

.page-size-suffix {
  margin-left: -2px;
}

.page-size-select {
  width: auto;
  height: 36px;
  padding: 0 28px 0 10px;
  font-weight: 700;
}

/* ============ Pager (pro pill style) ============ */
.pager {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  background: #f3f8ff;
  border: 1px solid #dce8f9;
  border-radius: 12px;
}

.pager-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #1c64d6;
  font-size: 0.85rem;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease,
    transform 0.1s ease;
}

.pager-arrow:hover:not(:disabled) {
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(28, 100, 214, 0.16);
}

.pager-arrow:active:not(:disabled) {
  transform: scale(0.94);
}

.pager-arrow:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.pager-list {
  display: flex;
  align-items: center;
  gap: 4px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.pager-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #4a5d80;
  font-size: 0.86rem;
  font-weight: 700;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.1s ease;
}

.pager-num:hover {
  background: #ffffff;
  color: #1c64d6;
}

.pager-num:active {
  transform: scale(0.94);
}

.pager-num.active {
  background: linear-gradient(135deg, #2f80ed, #1c64d6);
  color: #ffffff;
  box-shadow: 0 4px 12px -2px rgba(28, 100, 214, 0.55);
}

.pager-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 34px;
  color: #9aabc7;
  font-weight: 700;
  letter-spacing: 1px;
}

/* ============ Toast ============ */
.toast-alert {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 280px;
  max-width: 360px;
  padding: 16px 18px;
  border-radius: 12px;
  background: #ffffff;
  border-left: 4px solid #1f9d5e;
  box-shadow: 0 14px 32px -10px rgba(15, 64, 152, 0.35);
  color: #0f2c5c;
  font-size: 0.85rem;
}

.toast-alert i {
  font-size: 1.2rem;
  color: #1f9d5e;
  margin-top: 2px;
}

.toast-alert.error {
  border-left-color: #e0445c;
}

.toast-alert.error i {
  color: #e0445c;
}

.toast-alert div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toast-alert strong {
  font-weight: 800;
}

.toast-alert span {
  color: #6b7c93;
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* ============ Misc ============ */
.spin {
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .fin-pagination {
    flex-direction: column;
    align-items: stretch;
  }
  .pagination-controls {
    justify-content: space-between;
    gap: 14px;
  }
  .pager {
    flex-wrap: wrap;
    justify-content: center;
    width: 100%;
  }
  .fin-hero-content {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 576px) {
  .pager-num,
  .pager-arrow {
    width: 30px;
    height: 30px;
    min-width: 30px;
  }
  .fin-page {
    padding: 16px;
  }
}
</style>
