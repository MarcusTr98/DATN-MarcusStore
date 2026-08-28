<template>
  <div class="warranty-page">
    <div class="warranty-shell">
      <section class="warranty-hero">
        <div class="warranty-hero-title">
          <div class="warranty-hero-icon">
            <i class="bi bi-arrow-repeat"></i>
          </div>
          <div>
            <h1>Bảo hành</h1>
            <p>Theo dõi và xử lý các yêu cầu đổi trả, bảo hành từ khách hàng.</p>
          </div>
        </div>
      </section>

      <section class="warranty-stats-grid">
        <article class="warranty-stat-card">
          <span>Tổng yêu cầu</span>
          <strong>{{ totalElements }}</strong>
        </article>

        <article class="warranty-stat-card">
          <span>Chờ xác nhận</span>
          <strong class="text-accent">{{ stats.pending }}</strong>
        </article>

        <article class="warranty-stat-card">
          <span>Đang xử lý</span>
          <strong class="text-accent">{{ stats.confirmed }}</strong>
        </article>

        <article class="warranty-stat-card">
          <span>Đã duyệt</span>
          <strong>{{ stats.approved }}</strong>
        </article>

        <article class="warranty-stat-card">
          <span>Từ chối</span>
          <strong>{{ stats.rejected }}</strong>
        </article>
      </section>

      <section class="warranty-toolbar">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-6 col-lg-4">
            <label class="form-label warranty-form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text warranty-input-group-text">
                <i class="bi bi-search"></i>
              </span>
              <input
                v-model.trim="keyword"
                type="search"
                class="form-control warranty-form-control"
                placeholder="Tìm theo mã đơn hoặc tên sản phẩm"
              />
            </div>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label warranty-form-label">Trạng thái</label>
            <select v-model="filterStatus" class="form-select warranty-form-select">
              <option value="">Tất cả</option>
              <option v-for="item in warrantyStatusList" :key="item" :value="item">
                {{ warrantyStatusMap[item]?.label || item }}
              </option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label warranty-form-label">Lý do</label>
            <select v-model="filterReason" class="form-select warranty-form-select">
              <option value="">Tất cả</option>
              <option v-for="item in warrantyReasonList" :key="item" :value="item">
                {{ warrantyReasonMap[item]?.label || item }}
              </option>
            </select>
          </div>

          <div class="col-6 col-md-3 col-lg">
            <label class="form-label warranty-form-label">Từ ngày</label>
            <input
              v-model="fromDate"
              type="date"
              class="form-control warranty-form-control"
              :max="toDate || undefined"
            />
          </div>

          <div class="col-6 col-md-3 col-lg">
            <label class="form-label warranty-form-label">Đến ngày</label>
            <input
              v-model="toDate"
              type="date"
              class="form-control warranty-form-control"
              :min="fromDate || undefined"
            />
          </div>

          <div class="col-12 col-md-6 col-lg-auto">
            <button type="button" class="btn warranty-btn-soft w-100" title="Xóa lọc" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </div>
        </div>
      </section>

      <section class="warranty-table-panel">
        <div class="table-responsive">
          <table class="table align-middle warranty-data-table mb-0">
            <thead>
              <tr>
                <th>ID</th>
                <th>Mã đơn</th>
                <th>Sản phẩm</th>
                <th>Lý do</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th class="text-end">Thao tác</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(w, index) in warranties" :key="w.warrantyId">
                <td class="fw-bold">#{{ currentPage * pageSize + index + 1 }}</td>

                <td>
                  <router-link :to="`/admin/order/${w.orderCode}`" class="warranty-link">
                    {{ w.orderCode }}
                  </router-link>
                </td>
                <td>
                  <div class="warranty-product-cell">
                    <img
                      v-if="w.productImage"
                      :src="w.productImage"
                      :alt="w.productName"
                      class="warranty-thumb"
                    />
                    <div v-else class="warranty-thumb-placeholder">
                      <i class="fa-solid fa-mobile-screen-button"></i>
                    </div>
                    <span class="warranty-product-name">{{ w.productName }}</span>
                  </div>
                </td>
                <td>
                  <span>{{ w.reasonLabel }}</span>
                </td>
                <td>
                  <span
                    class="warranty-status-badge"
                    :class="warrantyStatusMap[w.status]?.className || 'warranty-status-pending'"
                  >
                    {{ w.statusLabel }}
                  </span>
                </td>
                <td>
                  <div class="warranty-date-line">{{ formatDate(w.createdAt) }}</div>
                  <div class="warranty-date-line">{{ formatTime(w.createdAt) }}</div>
                </td>
                <td>
                  <div class="d-flex justify-content-end gap-2">
                    <button
                      type="button"
                      class="warranty-icon-button"
                      title="Xem chi tiết"
                      @click="openDetail(w.warrantyId)"
                    >
                      <i class="bi bi-eye"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="warranties.length === 0 && !loading" class="warranty-empty-state">
          <i class="bi bi-inbox"></i>
          <h3>Chưa có yêu cầu bảo hành</h3>
          <p>Hãy thay đổi bộ lọc hoặc làm mới danh sách.</p>
        </div>

        <div v-if="loading" class="warranty-empty-state">
          <i class="bi bi-arrow-clockwise"></i>
          <h3>Đang tải dữ liệu...</h3>
        </div>

        <div v-if="totalPages > 0" class="warranty-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ totalElements }}</strong> yêu cầu
          </div>
          <div class="pagination-controls">
            <label class="page-size-control">
              <span>Hiển thị</span>
              <select v-model.number="pageSize" class="form-select form-select-sm">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
            </label>
            <button
              type="button"
              class="pagination-button"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              Trước
            </button>
            <span class="page-indicator">
              Trang <strong>{{ currentPage + 1 }}</strong> / {{ totalPages }}
            </span>
            <button
              type="button"
              class="pagination-button"
              :disabled="currentPage + 1 >= totalPages"
              @click="goToPage(currentPage + 1)"
            >
              Sau
            </button>
          </div>
        </div>
      </section>
    </div>

    <div class="warranty-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { AdminWarrantyApi } from '@/api/warrantyApi'
import { getWarrantyStats } from '@/api/warrantyStatsApi'
import '@/assets/css/WarrantyList.css'

const router = useRouter()
const toastMessage = ref('')

const warranties = ref([])
const loading = ref(false)
const keyword = ref('')
const filterStatus = ref('')
const filterReason = ref('')
const fromDate = ref('')
const toDate = ref('')

const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)

const warrantyStatusList = ['PENDING', 'CONFIRMED', 'APPROVED', 'REJECTED']
const warrantyReasonList = [
  'DEFECTIVE',
  'DAMAGED',
  'WRONG_ITEM',
  'NOT_AS_DESCRIBED',
  'ACCESSORY_MISSING',
  'OTHER',
]

const warrantyStatusMap = {
  PENDING: { label: 'Chờ xác nhận', className: 'warranty-status-pending' },
  CONFIRMED: { label: 'Admin đang xử lý', className: 'warranty-status-confirmed' },
  APPROVED: { label: 'Đồng ý bảo hành', className: 'warranty-status-approved' },
  REJECTED: { label: 'Từ chối', className: 'warranty-status-rejected' },
}

const warrantyReasonMap = {
  DEFECTIVE: { label: 'Sản phẩm lỗi' },
  DAMAGED: { label: 'Bị hư hỏng' },
  WRONG_ITEM: { label: 'Giao sai sản phẩm' },
  NOT_AS_DESCRIBED: { label: 'Không đúng mô tả' },
  ACCESSORY_MISSING: { label: 'Thiếu phụ kiện' },
  OTHER: { label: 'Lý do khác' },
}

const stats = ref({
  pending: 0,
  confirmed: 0,
  approved: 0,
  rejected: 0,
})

async function fetchWarranties() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterReason.value) params.reason = filterReason.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await AdminWarrantyApi.getAllWarranties(params)
    const data = res.data?.data || res.data || {}
    warranties.value = data.content || []
    totalPages.value = data.totalPages ?? 0
    totalElements.value = data.totalElements ?? 0
  } catch (err) {
    console.error('Lỗi tải danh sách bảo hành:', err)
    warranties.value = []
    totalPages.value = 0
    totalElements.value = 0
    showToast('Không thể tải danh sách yêu cầu.')
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getWarrantyStats()
    const data = res.data?.data || res.data || {}
    stats.value = {
      pending: data.pending || 0,
      confirmed: data.confirmed || 0,
      approved: data.approved || 0,
      rejected: data.rejected || 0,
    }
  } catch (err) {
    console.error('Lỗi tải thống kê bảo hành:', err)
  }
}

let searchTimer = null
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 0
    fetchWarranties()
  }, 400)
})

watch([filterStatus, filterReason, fromDate, toDate], () => {
  currentPage.value = 0
  fetchWarranties()
})

watch(pageSize, () => {
  currentPage.value = 0
  fetchWarranties()
})

function goToPage(page) {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchWarranties()
}

function resetFilters() {
  keyword.value = ''
  filterStatus.value = ''
  filterReason.value = ''
  fromDate.value = ''
  toDate.value = ''
  currentPage.value = 0
  showToast('Đã làm mới bộ lọc.')
  fetchWarranties()
  fetchStats()
}

function openDetail(warrantyId) {
  router.push(`/admin/warranty/${warrantyId}`)
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date.getTime())) return String(value).split('T')[0]
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  return `${day}/${month}/${year}`
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date.getTime())) return String(value).split('T')[1]?.split('.')[0] || ''
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

function showToast(message) {
  toastMessage.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

onMounted(() => {
  fetchWarranties()
  fetchStats()
})
</script>

<style scoped></style>
