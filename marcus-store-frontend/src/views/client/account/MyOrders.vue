<template>
  <main class="client-order-page">
    <section class="client-order-shell">
      <div class="main-card">
        <div class="main-header-card">
          <div>
            <h2 class="main-title">
              <i class="fa-solid fa-box-open"></i>
              Đơn hàng của tôi
            </h2>
            <p class="main-note">Theo dõi danh sách đơn hàng đã đặt tại Marcus Store.</p>
          </div>
          <span class="order-count">{{ filteredOrders.length }} đơn</span>
        </div>

        <div class="main-body">
          <section class="panel">
            <div class="panel-header">
              <h3 class="panel-title">
                <i class="fa-solid fa-receipt"></i>
                Danh sách đơn
              </h3>
            </div>

            <div class="filter-row">
              <input
                v-model.trim="keyword"
                class="filter-input"
                type="text"
                placeholder="Tìm theo mã đơn"
              />
              <input
                v-model="fromDate"
                class="filter-date"
                type="date"
                aria-label="Từ ngày"
              />
              <input
                v-model="toDate"
                class="filter-date"
                type="date"
                aria-label="Đến ngày"
              />
              <select v-model="statusFilter" class="filter-select">
                <option value="ALL">Tất cả</option>
                <option v-for="status in statusOptions" :key="status" :value="status">
                  {{ statusConfig[status]?.label || status }}
                </option>
              </select>
              <button
                type="button"
                class="filter-clear-btn"
                :disabled="!hasActiveFilter"
                title="Xóa bộ lọc"
                @click="clearFilters"
              >
                <i class="fa-solid fa-rotate-left"></i>
                Xóa lọc
              </button>
            </div>

            <div v-if="loading" class="empty-state">Đang tải danh sách đơn hàng...</div>
            <div v-else-if="error" class="empty-state text-danger">{{ error }}</div>
            <div v-else-if="filteredOrders.length === 0" class="empty-state">
              Không tìm thấy đơn hàng phù hợp.
            </div>

            <div v-else class="order-list my-orders-list">
              <router-link
                v-for="order in paginatedOrders"
                :key="order.orderCode"
                class="order-card my-order-card"
                :to="`/profile/orders/${order.orderCode}`"
              >
                <div class="order-card-top">
                  <div>
                    <div class="order-code">{{ order.orderCode }}</div>
                    <div class="order-date">{{ formatDateTime(order.createdAt) }}</div>
                  </div>
                  <span class="status-pill" :class="statusConfig[order.orderStatus]?.className">
                    <i class="fa-solid" :class="statusConfig[order.orderStatus]?.icon"></i>
                    {{ statusConfig[order.orderStatus]?.label || order.orderStatus }}
                  </span>
                </div>

                <div class="order-card-info">
                  <div class="order-products">
                    {{ order.itemCount || 0 }} sản phẩm ·
                    {{ getPaymentMethodLabel(order.paymentMethod) }} ·
                    {{ order.paymentStatus || '---' }}
                  </div>
                  <div class="order-total">{{ formatMoney(order.finalAmount) }}</div>
                </div>
              </router-link>
            </div>

            <!-- Pagination -->
            <nav v-if="totalPages > 1" class="fsp-pagination" aria-label="Phân trang đơn hàng">
              <button
                type="button"
                class="fsp-page-btn"
                :disabled="currentPage === 1"
                aria-label="Trang trước"
                @click="goToPage(currentPage - 1)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>

              <template v-for="(page, idx) in visiblePages" :key="`${page}-${idx}`">
                <span v-if="page === '...'" class="fsp-page-ellipsis">…</span>
                <button
                  v-else
                  type="button"
                  class="fsp-page-btn"
                  :class="{ active: page === currentPage }"
                  :aria-current="page === currentPage ? 'page' : undefined"
                  @click="goToPage(page)"
                >
                  {{ page }}
                </button>
              </template>

              <button
                type="button"
                class="fsp-page-btn"
                :disabled="currentPage === totalPages"
                aria-label="Trang sau"
                @click="goToPage(currentPage + 1)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </nav>

            <div v-if="filteredOrders.length > 0" class="fsp-pagination-info">
              Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filteredOrders.length) }} của {{ filteredOrders.length }} đơn hàng
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import UserOrderApi from '@/api/userOrder.js'
import '@/assets/css/OrderDetailView.css'

const orders = ref([])
const loading = ref(false)
const error = ref(null)
const keyword = ref('')
const statusFilter = ref('ALL')
const fromDate = ref('')
const toDate = ref('')

// ==== Pagination (client-side, 10 records/page) ====
const currentPage = ref(1)
const pageSize = 10

async function fetchUserOrders() {
  try {
    loading.value = true
    error.value = null
    const response = await UserOrderApi.userOrder()
    orders.value = response.data || []
  } catch (e) {
    error.value = 'Không thể lấy đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchUserOrders)

const statusConfig = {
  PENDING: { label: 'Chờ xác nhận', className: 'pending', icon: 'fa-clock' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed', icon: 'fa-circle-check' },
  PROCESSING: { label: 'Đang xử lý', className: 'processing', icon: 'fa-boxes-packing' },
  PACKED: { label: 'Đã đóng gói', className: 'packed', icon: 'fa-box' },
  READY_FOR_PICKUP: { label: 'Sẵn sàng nhận', className: 'completed', icon: 'fa-store' },
  SHIPPING: { label: 'Đang giao', className: 'shipping', icon: 'fa-truck-fast' },
  DELIVERED: { label: 'Giao thành công', className: 'delivered', icon: 'fa-box-anchor' },
  COMPLETED: { label: 'Hoàn thành', className: 'completed', icon: 'fa-house-circle-check' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled', icon: 'fa-ban' },
  FAILED: { label: 'Giao thất bại', className: 'failed', icon: 'fa-triangle-exclamation' },
}

const statusOptions = computed(() => Object.keys(statusConfig))

const filteredOrders = computed(() =>
  orders.value.filter((order) => {
    const keywordValue = keyword.value.toLowerCase()
    const matchesKeyword = (order.orderCode || '').toLowerCase().includes(keywordValue)
    const matchesStatus = statusFilter.value === 'ALL' || order.orderStatus === statusFilter.value

    // Lọc theo khoảng ngày (so sánh yyyy-mm-dd)
    const orderDate = order.createdAt ? String(order.createdAt).slice(0, 10) : ''
    const matchesFromDate = !fromDate.value || orderDate >= fromDate.value
    const matchesToDate = !toDate.value || orderDate <= toDate.value

    return matchesKeyword && matchesStatus && matchesFromDate && matchesToDate
  }),
)

// Reset về trang 1 mỗi khi filter/search thay đổi
watch([keyword, statusFilter, fromDate, toDate], () => {
  currentPage.value = 1
})

const hasActiveFilter = computed(() =>
  !!keyword.value || !!fromDate.value || !!toDate.value || statusFilter.value !== 'ALL'
)

function clearFilters() {
  keyword.value = ''
  statusFilter.value = 'ALL'
  fromDate.value = ''
  toDate.value = ''
  currentPage.value = 1
}

// Tổng số trang dựa trên danh sách đã filter
const totalPages = computed(() => {
  const total = filteredOrders.value.length
  return total === 0 ? 1 : Math.ceil(total / pageSize)
})

// Cắt lát 10 bản ghi / trang từ danh sách đã filter
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredOrders.value.slice(start, start + pageSize)
})

// Hiển thị tối đa 5 nút trang kèm dấu "..."
const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = []
  if (cur <= 3) {
    pages.push(1, 2, 3, 4, '...', total)
  } else if (cur >= total - 2) {
    pages.push(1, '...', total - 3, total - 2, total - 1, total)
  } else {
    pages.push(1, '...', cur - 1, cur, cur + 1, '...', total)
  }
  return pages
})

function goToPage(page) {
  if (page === '...' || page < 1 || page > totalPages.value) return
  currentPage.value = page
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function formatMoney(value) {
  return `${new Intl.NumberFormat('vi-VN').format(value || 0)}đ`
}

function formatDateTime(value) {
  if (!value) return ''
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})[T\s-]+(\d{2}):(\d{2})/)
  if (!match) return String(value).split('.')[0].replace('T', ' ')

  const [, year, month, day, hour, minute] = match
  return `${day}/${month}/${year} ${hour}:${minute}`
}

function getPaymentMethodLabel(method) {
  if (method === 'VNPay' || method === 'VNPAY') return 'VNPAY'
  if (method === 'BankTransfer') return 'Chuyển khoản'
  return method || '---'
}
</script>

<style scoped>
.my-orders-list {
  max-height: none;
}

.my-order-card {
  display: block;
  color: inherit;
  text-decoration: none;
}

/* ===== Filter row ===== */
.filter-row {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  align-items: center;
  overflow-x: auto;
  padding: 14px;
  border-bottom: 1px solid #eff1f5;
  scrollbar-width: thin;
}

.filter-row::-webkit-scrollbar {
  height: 4px;
}

.filter-row::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

.filter-row :deep(.filter-input) {
  flex: 1 1 200px;
  min-width: 160px;
  max-width: 240px;
}

.filter-date {
  flex: 0 0 150px;
  min-width: 150px;
  height: 40px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  color: #374151;
  font-family: inherit;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.filter-date:hover {
  border-color: #f59e0b;
}

.filter-date:focus {
  outline: none;
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15);
}

.filter-select {
  flex: 0 0 180px;
  min-width: 160px;
  height: 40px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  color: #374151;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.filter-select:hover {
  border-color: #f59e0b;
}

.filter-select:focus {
  outline: none;
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15);
}

.filter-clear-btn {
  flex: 0 0 auto;
  height: 40px;
  padding: 0 14px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.filter-clear-btn:hover:not(:disabled) {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.filter-clear-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.filter-clear-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ===== Pagination (client-side 10/page) ===== */
.fsp-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin: 20px 0 6px;
  flex-wrap: wrap;
}

.fsp-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  height: 38px;
  padding: 0 10px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
}

.fsp-page-btn svg {
  width: 18px;
  height: 18px;
}

.fsp-page-btn:hover:not(:disabled):not(.active) {
  background: #fffbeb;
  border-color: #f59e0b;
  color: #d97706;
}

.fsp-page-btn.active {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border-color: #f59e0b;
  color: #fff;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35);
}

.fsp-page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.fsp-page-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 38px;
  color: #9ca3af;
  font-weight: 600;
  user-select: none;
}

.fsp-pagination-info {
  text-align: center;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 12px;
  font-weight: 500;
}
</style>
