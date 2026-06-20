<template>
  <div class="voucher-page">
    <div class="voucher-shell">
      <section class="voucher-hero">
        <div class="hero-title">
          <div class="hero-icon bg-warning text-white">
            <i class="fa-solid fa-headset"></i>
          </div>
          <div>
            <h1>Quản lý Yêu cầu & Khiếu nại</h1>
            <p>Xử lý các vấn đề, khiếu nại, và tư vấn từ phía khách hàng.</p>
          </div>
        </div>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng số yêu cầu</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="stat-card">
          <span>Chờ xử lý</span>
          <strong class="text-danger">{{ stats.pending }}</strong>
        </article>
        <article class="stat-card">
          <span>Đã giải quyết</span>
          <strong class="text-success">{{ stats.resolved }}</strong>
        </article>
        <article class="stat-card">
          <span>Khách vãng lai</span>
          <strong>{{ stats.guest }}</strong>
        </article>
      </section>

      <section class="toolbar-panel">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-5">
            <label class="form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                v-model.trim="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Tìm theo tên hoặc số điện thoại..."
              />
            </div>
          </div>

          <div class="col-12 col-md-4">
            <label class="form-label">Trạng thái xử lý</label>
            <select v-model="filters.status" class="form-select">
              <option value="ALL">Tất cả trạng thái</option>
              <option value="PENDING">Chờ xử lý</option>
              <option value="RESOLVED">Đã giải quyết</option>
            </select>
          </div>

          <div class="col-12 col-md-3">
            <button type="button" class="btn btn-soft w-100" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise me-2"></i> Đặt lại bộ lọc
            </button>
          </div>
        </div>
      </section>

      <section class="table-panel">
        <div class="table-responsive">
          <table class="table align-middle voucher-table mb-0">
            <thead>
              <tr>
                <th>ID</th>
                <th>Người gửi</th>
                <th>Liên hệ</th>
                <th style="width: 30%">Nội dung</th>
                <th>Thời gian</th>
                <th>Trạng thái</th>
                <th class="text-end">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(item, index) in filteredContacts"
                :key="item.contactId"
                :class="{ 'bg-light': item.status === 'RESOLVED' }"
              >
                <td class="fw-bold">#{{ currentPage * pageSize + index + 1 }}</td>
                <td>
                  <div class="fw-bold text-dark">{{ item.customerName }}</div>
                  <span v-if="item.userId" class="badge bg-info text-dark" style="font-size: 10px"
                    >Thành viên</span
                  >
                  <span v-else class="badge bg-secondary" style="font-size: 10px"
                    >Khách vãng lai</span
                  >
                </td>
                <td>
                  <div class="small fw-semibold">
                    <i class="fa-solid fa-phone text-success me-1"></i> {{ item.phoneNumber }}
                  </div>
                  <div class="small text-muted" v-if="item.email">
                    <i class="fa-solid fa-envelope me-1"></i> {{ item.email }}
                  </div>
                </td>
                <td>
                  <div class="text-truncate" style="max-width: 250px" :title="item.message">
                    {{ item.message }}
                  </div>
                </td>
                <td class="small">{{ formatDateTime(item.createdAt) }}</td>
                <td>
                  <span class="status-badge" :class="{ inactive: item.status === 'PENDING' }">
                    {{ item.status === 'PENDING' ? 'Chờ xử lý' : 'Đã giải quyết' }}
                  </span>
                </td>
                <td>
                  <div class="d-flex justify-content-end gap-2">
                    <button class="icon-button" title="Xem chi tiết" @click="viewDetail(item)">
                      <i class="fa-solid fa-eye"></i>
                    </button>
                    <button
                      v-if="item.status === 'PENDING'"
                      class="icon-button text-success border-success"
                      title="Đánh dấu đã xử lý"
                      @click="markResolved(item.contactId)"
                    >
                      <i class="fa-solid fa-check"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="filteredContacts.length === 0" class="empty-state">
          <i class="fa-solid fa-inbox fs-1 text-muted mb-3"></i>
          <h3>Không có dữ liệu</h3>
          <p>Không tìm thấy khiếu nại nào phù hợp với bộ lọc.</p>
        </div>

        <div v-if="totalPages > 0" class="voucher-pagination mt-4">
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
              class="pagination-button"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              Trước
            </button>
            <span class="page-indicator"
              >Trang <strong>{{ currentPage + 1 }}</strong> / {{ totalPages }}</span
            >
            <button
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

    <BaseModal
      :visible="modal.visible"
      type="info"
      title="Chi tiết Yêu cầu hỗ trợ"
      :message="modal.message"
      @close="modal.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'

// (Giả sử template CSS của ông đã có sẵn trong file Voucher.css, nếu file tách riêng thì Import nó vào đây)
// import '@/assets/css/Voucher.css'

const contacts = ref([])
const loading = ref(false)
const modal = ref({ visible: false, message: '' })

// Pagination State
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

// Filters & Stats
const filters = reactive({ keyword: '', status: 'ALL' })
const stats = reactive({ total: 0, pending: 0, resolved: 0, guest: 0 })

// Tính toán lọc dữ liệu tại Frontend (Vì Backend API chưa hỗ trợ Search trực tiếp)
const filteredContacts = computed(() => {
  return contacts.value.filter((item) => {
    const matchKey =
      item.customerName.toLowerCase().includes(filters.keyword.toLowerCase()) ||
      item.phoneNumber.includes(filters.keyword)
    const matchStatus = filters.status === 'ALL' || item.status === filters.status
    return matchKey && matchStatus
  })
})

const calculateStats = (dataList) => {
  stats.total = dataList.length
  stats.pending = dataList.filter((i) => i.status === 'PENDING').length
  stats.resolved = dataList.filter((i) => i.status === 'RESOLVED').length
  stats.guest = dataList.filter((i) => !i.userId).length
}

const fetchContacts = async () => {
  loading.value = true
  try {
    // Gọi API (Nếu backend hỗ trợ params truyền thẳng currentPage, pageSize)
    const res = await api.get(`/admin/contacts?page=${currentPage.value}&size=${pageSize.value}`)
    const payload = res.data?.data
    contacts.value = payload?.content || []

    // Gắn thông số phân trang
    totalElements.value = payload?.totalElements || contacts.value.length
    totalPages.value = payload?.totalPages || 1

    // Cập nhật 4 thẻ thống kê
    calculateStats(contacts.value)
  } catch (error) {
    console.error('Lỗi tải danh sách', error)
  } finally {
    loading.value = false
  }
}

// Watchers: Tự động load lại khi đổi phân trang/bộ lọc
watch([currentPage, pageSize], () => {
  fetchContacts()
})

const viewDetail = (item) => {
  modal.value.message = `Người gửi: ${item.customerName}\nSĐT: ${item.phoneNumber}\nEmail: ${item.email || 'Không có'}\n\nNội dung chi tiết:\n"${item.message}"`
  modal.value.visible = true
}

const markResolved = async (id) => {
  if (!confirm('Xác nhận đã xử lý xong yêu cầu này?')) return
  try {
    await api.put(`/admin/contacts/${id}/resolve`)
    fetchContacts()
  } catch {
    alert('Lỗi cập nhật trạng thái')
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = 'ALL'
}

const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
  }
}

const formatDateTime = (val) =>
  new Date(val).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })

onMounted(fetchContacts)
</script>

<style scoped>
/* Có thể ông đã có file Voucher.css import toàn bộ style, 
   nếu bị vỡ layout ở cột nào thì ông tự điều chỉnh lại width ở đây nhé */
.status-badge {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #e8f5e9;
  color: #2e7d32;
}
.status-badge.inactive {
  background: #ffebee;
  color: #c62828;
}
.icon-button {
  border: 1px solid #ddd;
  background: white;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.icon-button:hover {
  background: #f5f5f5;
}
</style>
