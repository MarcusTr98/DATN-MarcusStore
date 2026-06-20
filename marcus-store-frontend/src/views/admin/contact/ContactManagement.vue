<template>
  <div class="voucher-page">
    <div class="voucher-shell">
      <section class="voucher-hero">
        <div class="hero-title">
          <div class="hero-icon">
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
          <strong class="text-accent">{{ stats.pending }}</strong>
        </article>
        <article class="stat-card">
          <span>Đã giải quyết</span>
          <strong>{{ stats.resolved }}</strong>
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
              <i class="bi bi-arrow-counterclockwise me-2"></i> Đặt lại
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
                <th style="max-width: 250px">Nội dung</th>
                <th>Thời gian</th>
                <th>Trạng thái</th>
                <th class="text-end">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredContacts.length === 0">
                <td colspan="7" class="text-center py-5 text-muted">Không có dữ liệu.</td>
              </tr>
              <tr v-for="(item, index) in filteredContacts" :key="item.contactId">
                <td class="fw-bold">#{{ currentPage * pageSize + index + 1 }}</td>
                <td>
                  <div class="voucher-code">{{ item.customerName }}</div>
                  <span
                    v-if="item.userId"
                    class="type-badge percent mt-1"
                    style="font-size: 0.65rem"
                    >Thành viên</span
                  >
                  <span v-else class="type-badge amount mt-1" style="font-size: 0.65rem"
                    >Khách vãng lai</span
                  >
                </td>
                <td>
                  <div class="fw-semibold text-secondary">
                    <i class="fa-solid fa-phone text-success me-1"></i> {{ item.phoneNumber }}
                  </div>
                  <div class="small" v-if="item.email">
                    <i class="fa-regular fa-envelope me-1"></i> {{ item.email }}
                  </div>
                </td>
                <td>
                  <div class="text-truncate" style="max-width: 250px" :title="item.message">
                    {{ item.message }}
                  </div>
                </td>
                <td>
                  <div class="date-line">{{ formatDateTime(item.createdAt) }}</div>
                </td>
                <td>
                  <span class="status-badge" :class="{ inactive: item.status === 'PENDING' }">
                    {{ item.status === 'PENDING' ? 'Chờ xử lý' : 'Đã giải quyết' }}
                  </span>
                </td>
                <td>
                  <div class="d-flex justify-content-end gap-2">
                    <button class="icon-button" title="Xem chi tiết" @click="viewDetail(item)">
                      <i class="fa-regular fa-eye"></i>
                    </button>
                    <button
                      v-if="item.status === 'PENDING'"
                      class="icon-button"
                      style="color: #15803d; border-color: #bbf7d0; background: #f0fdf4"
                      title="Đánh dấu đã xử lý"
                      @click="confirmResolve(item.contactId)"
                    >
                      <i class="fa-solid fa-check"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
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
      :visible="detailModal.visible"
      type="info"
      title="Chi tiết Yêu cầu"
      @close="detailModal.visible = false"
    >
      <div v-if="detailModal.data" class="detail-box">
        <div class="detail-row">
          <i class="fa-solid fa-user text-secondary"></i>
          <strong>{{ detailModal.data.customerName }}</strong>
        </div>
        <div class="detail-row">
          <i class="fa-solid fa-phone text-success"></i>
          <span>{{ detailModal.data.phoneNumber }}</span>
        </div>
        <div class="detail-row" v-if="detailModal.data.email">
          <i class="fa-solid fa-envelope text-primary"></i>
          <span>{{ detailModal.data.email }}</span>
        </div>
        <div class="detail-message-box">
          <div class="msg-title">Nội dung khiếu nại/tư vấn:</div>
          <p class="msg-content">"{{ detailModal.data.message }}"</p>
        </div>
      </div>
    </BaseModal>

    <BaseModal
      :visible="actionModal.visible"
      :type="actionModal.type"
      :title="actionModal.title"
      :message="actionModal.message"
      @close="actionModal.visible = false"
      @confirm="executeAction"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import api from '@/utils/api'
import BaseModal from '@/components/BaseModal.vue'
import '@/assets/css/Voucher.css'
const contacts = ref([])
const loading = ref(false)

// Pagination & Filters
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const filters = reactive({ keyword: '', status: 'ALL' })
const stats = reactive({ total: 0, pending: 0, resolved: 0, guest: 0 })

// State Modals
const detailModal = reactive({ visible: false, data: null })
const actionModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
  targetId: null,
})

// Logic Modal
const showActionModal = (type, title, message, targetId = null) => {
  actionModal.type = type
  actionModal.title = title
  actionModal.message = message
  actionModal.targetId = targetId
  actionModal.visible = true
}

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
    const res = await api.get(`/admin/contacts?page=${currentPage.value}&size=${pageSize.value}`)
    const payload = res.data?.data
    contacts.value = payload?.content || []
    totalElements.value = payload?.totalElements || contacts.value.length
    totalPages.value = payload?.totalPages || 1
    calculateStats(contacts.value)
  } catch {
    showActionModal('error', 'Lỗi hệ thống', 'Không thể tải danh sách khiếu nại.')
  } finally {
    loading.value = false
  }
}

watch([currentPage, pageSize], () => {
  fetchContacts()
})

const viewDetail = (item) => {
  detailModal.data = item
  detailModal.visible = true
}

const confirmResolve = (id) => {
  showActionModal(
    'confirm',
    'Xác nhận xử lý',
    'Bạn có chắc chắn đã giải quyết xong yêu cầu này?',
    id,
  )
}

// Xử lý nút Đồng Ý trên Action Modal (chỉ chạy khi type = 'confirm')
const executeAction = async () => {
  if (actionModal.type === 'confirm' && actionModal.targetId) {
    actionModal.visible = false
    try {
      await api.put(`/admin/contacts/${actionModal.targetId}/resolve`)
      fetchContacts()
      setTimeout(
        () => showActionModal('success', 'Thành công', 'Đã cập nhật trạng thái yêu cầu.'),
        300,
      )
    } catch {
      setTimeout(
        () => showActionModal('error', 'Cập nhật thất bại', 'Có lỗi xảy ra khi lưu dữ liệu.'),
        300,
      )
    }
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
/* Style riêng cho phần nội dung Chi tiết khiếu nại hiển thị trong BaseModal */
.detail-box {
  text-align: left;
  padding: 0 10px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 15px;
  color: #334155;
}

.detail-row i {
  width: 20px;
  text-align: center;
  font-size: 16px;
}

.detail-message-box {
  margin-top: 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
}

.msg-title {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 8px;
  text-transform: uppercase;
}

.msg-content {
  margin: 0;
  font-size: 15px;
  color: #0f172a;
  line-height: 1.6;
  font-style: italic;
  white-space: pre-wrap;
}
</style>
