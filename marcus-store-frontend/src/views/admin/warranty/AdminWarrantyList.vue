<template>
  <div class="admin-warranty-list">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">Đổi trả / Bảo hành</h1>
        <p class="page-subtitle">Quản lý yêu cầu đổi trả và bảo hành từ khách hàng</p>
      </div>
      <div class="header-stats">
        <div class="stat-card stat-total">
          <span class="stat-number">{{ stats.total }}</span>
          <span class="stat-label">Tổng yêu cầu</span>
        </div>
        <div class="stat-card stat-pending">
          <span class="stat-number">{{ stats.pending }}</span>
          <span class="stat-label">Chờ xử lý</span>
        </div>
        <div class="stat-card stat-approved">
          <span class="stat-number">{{ stats.approved }}</span>
          <span class="stat-label">Đã duyệt</span>
        </div>
        <div class="stat-card stat-rejected">
          <span class="stat-number">{{ stats.rejected }}</span>
          <span class="stat-label">Từ chối</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">Trạng thái</label>
        <select v-model="filterStatus" class="filter-select" @change="fetchWarranties">
          <option value="">Tất cả</option>
          <option value="PENDING">Chờ xử lý</option>
          <option value="APPROVED">Đã duyệt</option>
          <option value="REJECTED">Từ chối</option>
          <option value="COMPLETED">Hoàn thành</option>
        </select>
      </div>

      <div class="filter-group">
        <label class="filter-label">Lý do</label>
        <select v-model="filterReason" class="filter-select" @change="fetchWarranties">
          <option value="">Tất cả</option>
          <option value="DEFECTIVE">Sản phẩm lỗi</option>
          <option value="DAMAGED">Bị hư hỏng</option>
          <option value="WRONG_ITEM">Giao sai sản phẩm</option>
          <option value="NOT_AS_DESCRIBED">Không đúng mô tả</option>
          <option value="ACCESSORY_MISSING">Thiếu phụ kiện</option>
          <option value="OTHER">Lý do khác</option>
        </select>
      </div>

      <div class="filter-group filter-search">
        <label class="filter-label">Tìm kiếm</label>
        <input
          v-model="searchKeyword"
          @input="onSearchInput"
          type="text"
          class="filter-input"
          placeholder="Mã đơn, tên sản phẩm..."
        />
      </div>

      <button class="btn-reset" @click="resetFilters">
        <i class="bi bi-arrow-counterclockwise"></i>
        Đặt lại
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu...</p>
    </div>

    <div v-else-if="filteredWarranties.length === 0" class="empty-state">
      <i class="bi bi-inbox"></i>
      <p>Chưa có yêu cầu bảo hành nào</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="warranty-table">
        <thead>
          <tr>
            <th>Mã BH</th>
            <th>Đơn hàng</th>
            <th>Sản phẩm</th>
            <th>Lý do</th>
            <th>Trạng thái</th>
            <th>Ngày tạo</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="w in filteredWarranties" :key="w.warrantyId">
            <td>
              <span class="warranty-code">#WR{{ String(w.warrantyId).padStart(4, '0') }}</span>
            </td>
            <td>
              <router-link :to="`/admin/order/${w.orderItemId}`" class="order-link">
                {{ w.orderCode }}
              </router-link>
            </td>
            <td>
              <div class="product-cell">
                <img :src="w.productImage || '/placeholder.png'" :alt="w.productName" class="product-thumb" />
                <span class="product-name">{{ w.productName }}</span>
              </div>
            </td>
            <td>
              <span class="reason-text">{{ w.reasonLabel }}</span>
            </td>
            <td>
              <span class="status-badge" :class="`status-${w.status.toLowerCase()}`">
                {{ w.statusLabel }}
              </span>
            </td>
            <td>
              <span class="date-text">{{ formatDate(w.createdAt) }}</span>
            </td>
            <td>
              <button class="btn-view" @click="openDetail(w.warrantyId)">
                <i class="bi bi-eye"></i>
                Xem
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { AdminWarrantyApi } from '@/api/warrantyApi'

const router = useRouter()

const loading = ref(false)
const warranties = ref([])

const filterStatus = ref('')
const filterReason = ref('')
const searchKeyword = ref('')
let searchTimer = null

const stats = computed(() => {
  return {
    total: warranties.value.length,
    pending: warranties.value.filter(w => w.status === 'PENDING').length,
    approved: warranties.value.filter(w => w.status === 'APPROVED').length,
    rejected: warranties.value.filter(w => w.status === 'REJECTED').length,
  }
})

const filteredWarranties = computed(() => {
  let result = warranties.value
  if (filterReason.value) {
    result = result.filter(w => w.reason === filterReason.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim()
    result = result.filter(
      w =>
        (w.orderCode && w.orderCode.toLowerCase().includes(kw)) ||
        (w.productName && w.productName.toLowerCase().includes(kw))
    )
  }
  return result
})

async function fetchWarranties() {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value) params.status = filterStatus.value
    const res = await AdminWarrantyApi.getAllWarranties(params)
    warranties.value = res.data?.data || res.data || []
  } catch (err) {
    console.error('Lỗi tải danh sách bảo hành:', err)
    warranties.value = []
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {}, 300)
}

function resetFilters() {
  filterStatus.value = ''
  filterReason.value = ''
  searchKeyword.value = ''
  fetchWarranties()
}

function openDetail(warrantyId) {
  router.push(`/admin/warranty/${warrantyId}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchWarranties()
})
</script>

<style scoped>
.admin-warranty-list {
  padding: 24px 28px;
  background: #fff7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 24px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-card {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 12px;
  padding: 12px 20px;
  min-width: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 8px rgba(255, 77, 148, 0.06);
}

.stat-number {
  font-size: 22px;
  font-weight: 900;
  color: #111827;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
  font-weight: 600;
}

.stat-pending .stat-number { color: #f59e0b; }
.stat-approved .stat-number { color: #10b981; }
.stat-rejected .stat-number { color: #ef4444; }

.filter-bar {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(255, 77, 148, 0.04);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 160px;
}

.filter-search {
  flex: 1;
  min-width: 220px;
}

.filter-label {
  font-size: 12px;
  font-weight: 700;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-select,
.filter-input {
  height: 40px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  color: #111827;
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
}

.filter-select:focus,
.filter-input:focus {
  border-color: #ff4d94;
}

.btn-reset {
  height: 40px;
  padding: 0 18px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-reset:hover {
  background: #f9fafb;
  border-color: #ff4d94;
  color: #ff4d94;
}

.loading-state,
.empty-state {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  padding: 60px 20px;
  text-align: center;
  color: #6b7280;
}

.empty-state i {
  font-size: 56px;
  color: #fee2e2;
  margin-bottom: 12px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #fee2e2;
  border-top-color: #ff4d94;
  border-radius: 50%;
  margin: 0 auto 12px;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.table-wrapper {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(255, 77, 148, 0.04);
}

.warranty-table {
  width: 100%;
  border-collapse: collapse;
}

.warranty-table thead {
  background: #fff5f8;
}

.warranty-table th {
  padding: 14px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 800;
  color: #111827;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1.5px solid #fee2e2;
}

.warranty-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: #374151;
  border-bottom: 1px solid #fef2f3;
  vertical-align: middle;
}

.warranty-table tbody tr:hover {
  background: #fff5f8;
}

.warranty-code {
  font-weight: 800;
  color: #ff4d94;
  font-family: 'Courier New', monospace;
}

.order-link {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
}

.order-link:hover {
  text-decoration: underline;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
  border: 1px solid #fee2e2;
}

.product-name {
  font-weight: 600;
  color: #111827;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reason-text {
  font-size: 13px;
  color: #374151;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pending {
  background: #fef3c7;
  color: #b45309;
}

.status-approved {
  background: #d1fae5;
  color: #047857;
}

.status-rejected {
  background: #fee2e2;
  color: #b91c1c;
}

.status-completed {
  background: #dbeafe;
  color: #1d4ed8;
}

.date-text {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

.btn-view {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: linear-gradient(135deg, #ff4d94 0%, #ff1a75 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(255, 77, 148, 0.25);
}

.btn-view:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 148, 0.4);
}
</style>
