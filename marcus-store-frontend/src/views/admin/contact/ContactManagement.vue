<template>
  <div class="cm-page">
    <div class="cm-shell">
      <section class="cm-hero">
        <div class="cm-hero-title">
          <div class="cm-hero-icon">
            <i class="fa-solid fa-headset"></i>
          </div>
          <div>
            <h1>Quản lý Yêu cầu & Khiếu nại</h1>
            <p>Xử lý các vấn đề, khiếu nại, và tư vấn từ phía khách hàng.</p>
          </div>
        </div>
      </section>

      <section class="cm-stats-grid">
        <article class="cm-stat-card">
          <span>Tổng số yêu cầu</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="cm-stat-card">
          <span>Mới / đang xử lý</span>
          <strong class="cm-text-accent">{{ stats.new + stats.inProgress }}</strong>
        </article>
        <article class="cm-stat-card">
          <span>Đã giải quyết</span>
          <strong>{{ stats.resolved }}</strong>
        </article>
        <article class="cm-stat-card">
          <span>Khách vãng lai</span>
          <strong>{{ stats.guest }}</strong>
        </article>
      </section>

      <section class="cm-toolbar">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-5">
            <label class="form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                v-model.trim="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Tìm theo tên, SĐT hoặc User ID..."
              />
            </div>
          </div>
          <div class="col-12 col-md-4">
            <label class="form-label">Trạng thái xử lý</label>
            <select v-model="filters.status" class="form-select">
              <option value="ALL">Tất cả trạng thái</option>
              <option value="NEW">Mới</option>
              <option value="IN_PROGRESS">Đang xử lý</option>
              <option value="RESOLVED">Đã giải quyết</option>
              <option value="SPAM">Spam</option>
            </select>
          </div>
          <div class="col-12 col-md-3">
            <button type="button" class="btn cm-btn-soft w-100" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise me-2"></i> Đặt lại
            </button>
          </div>
        </div>
      </section>

      <section class="cm-table-panel">
        <div class="table-responsive">
          <table class="table align-middle cm-table mb-0">
            <thead>
              <tr>
                <th>ID</th>
                <th>Người gửi</th>
                <th>User ID</th>
                <th>Liên hệ</th>
                <th style="max-width: 250px">Nội dung</th>
                <th>Thời gian</th>
                <th>Trạng thái</th>
                <th class="text-end">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredContacts.length === 0">
                <td colspan="8" class="text-center py-5 text-muted">Không có dữ liệu.</td>
              </tr>
              <tr v-for="(item, index) in filteredContacts" :key="item.contactId">
                <td class="fw-bold">#{{ currentPage * pageSize + index + 1 }}</td>
                <td>
                  <div class="cm-customer-name">{{ item.customerName }}</div>
                  <span v-if="item.userId" class="cm-role-badge member">Thành viên</span>
                  <span v-else class="cm-role-badge guest">Khách vãng lai</span>
                </td>
                <td>
                  <button
                    v-if="item.userId"
                    type="button"
                    class="cm-uid-chip"
                    :class="{ copied: copiedId === item.userId }"
                    :title="`Bấm để sao chép User ID #${item.userId}`"
                    @click="copyUserId(item.userId)"
                  >
                    <i
                      class="fa-solid"
                      :class="copiedId === item.userId ? 'fa-check' : 'fa-hashtag'"
                    ></i>
                    {{ item.userId }}
                  </button>
                  <span v-else class="cm-uid-empty">—</span>
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
                  <div class="cm-date-line">{{ formatDateTime(item.createdAt) }}</div>
                </td>
                <td>
                  <span class="cm-status-badge" :class="{ pending: ['NEW', 'IN_PROGRESS'].includes(item.status) }">
                    {{ statusLabel(item.status) }}
                  </span>
                </td>
                <td>
                  <div class="d-flex justify-content-end gap-2">
                    <button class="cm-icon-btn" title="Xem chi tiết" @click="viewDetail(item)">
                      <i class="fa-regular fa-eye"></i>
                    </button>
                    <button
                      v-if="item.status === 'NEW'"
                      class="cm-icon-btn success"
                      title="Tiếp nhận xử lý"
                      @click="confirmStatus(item, 'IN_PROGRESS')"
                    >
                      <i class="fa-solid fa-user-check"></i>
                    </button>
                    <button v-if="!['RESOLVED', 'SPAM'].includes(item.status)" class="cm-icon-btn success" title="Đã giải quyết" @click="confirmStatus(item, 'RESOLVED')">
                      <i class="fa-solid fa-check"></i>
                    </button>
                    <button v-if="item.status !== 'SPAM'" class="cm-icon-btn" title="Đánh dấu Spam" @click="confirmStatus(item, 'SPAM')">
                      <i class="fa-solid fa-ban"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="totalPages > 0" class="cm-pagination mt-4">
          <div class="cm-pagination-summary">
            Tổng <strong>{{ totalElements }}</strong> yêu cầu
          </div>
          <div class="cm-pagination-controls">
            <label class="cm-page-size">
              <span>Hiển thị</span>
              <select v-model.number="pageSize" class="form-select form-select-sm">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
            </label>
            <button
              class="cm-page-btn"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              Trước
            </button>
            <span class="cm-page-indicator"
              >Trang <strong>{{ currentPage + 1 }}</strong> / {{ totalPages }}</span
            >
            <button
              class="cm-page-btn"
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

        <!-- User ID: để admin lấy nhanh mã khách hàng, phục vụ tặng voucher / đánh giá tài khoản -->
        <div class="detail-row" v-if="detailModal.data.userId">
          <i class="fa-solid fa-id-badge text-primary"></i>
          <span
            >User ID: <strong>#{{ detailModal.data.userId }}</strong></span
          >
          <button
            type="button"
            class="detail-copy-btn"
            :class="{ copied: copiedId === detailModal.data.userId }"
            @click="copyUserId(detailModal.data.userId)"
          >
            <i
              class="fa-solid"
              :class="copiedId === detailModal.data.userId ? 'fa-check' : 'fa-copy'"
            ></i>
            {{ copiedId === detailModal.data.userId ? 'Đã sao chép' : 'Sao chép' }}
          </button>
        </div>
        <div class="detail-row" v-else>
          <i class="fa-solid fa-user-slash text-secondary"></i>
          <span>Khách vãng lai (chưa đăng nhập)</span>
        </div>

        <div class="detail-message-box">
          <div class="msg-title">Nội dung khiếu nại/tư vấn:</div>
          <p class="msg-content">"{{ detailModal.data.message }}"</p>
        </div>
        <div class="detail-row" v-if="detailModal.data.handledBy">
          <i class="fa-solid fa-user-check text-primary"></i>
          <span>Xử lý bởi <strong>{{ detailModal.data.handledBy }}</strong></span>
        </div>
        <div class="detail-row" v-if="detailModal.data.processingStartedAt">
          <i class="fa-regular fa-clock"></i>
          <span>Tiếp nhận lúc {{ formatDateTime(detailModal.data.processingStartedAt) }}</span>
        </div>
        <div class="detail-row" v-if="detailModal.data.resolvedAt">
          <i class="fa-solid fa-circle-check text-success"></i>
          <span>Kết thúc lúc {{ formatDateTime(detailModal.data.resolvedAt) }}</span>
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
import '@/assets/css/ContactManagement.css'

const contacts = ref([])
const loading = ref(false)

// Pagination & Filters
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const filters = reactive({ keyword: '', status: 'ALL' })
const stats = reactive({ total: 0, new: 0, inProgress: 0, resolved: 0, guest: 0 })

// Trạng thái "vừa sao chép User ID" (dùng chung cho bảng + modal chi tiết)
const copiedId = ref(null)
let copiedTimer = null

// State Modals
const detailModal = reactive({ visible: false, data: null })
const actionModal = reactive({
  visible: false,
  type: 'info',
  title: '',
  message: '',
  targetId: null,
  targetStatus: null,
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
  const keyword = filters.keyword.toLowerCase()
  return contacts.value.filter((item) => {
    const matchKey =
      item.customerName.toLowerCase().includes(keyword) ||
      item.phoneNumber.includes(filters.keyword) ||
      String(item.userId ?? '').includes(filters.keyword)
    const matchStatus = filters.status === 'ALL' || item.status === filters.status
    return matchKey && matchStatus
  })
})

const calculateStats = (dataList) => {
  stats.total = dataList.length
  stats.new = dataList.filter((i) => i.status === 'NEW').length
  stats.inProgress = dataList.filter((i) => i.status === 'IN_PROGRESS').length
  stats.resolved = dataList.filter((i) => i.status === 'RESOLVED').length
  stats.guest = dataList.filter((i) => !i.userId).length
}

const fetchContacts = async () => {
  loading.value = true
  try {
    const res = await api.get(`/admin/contacts?page=${currentPage.value}&size=${pageSize.value}`)
    const payload = res.data?.data
    // Backend (ContactRequest) đã có userId (null nếu là khách vãng lai) -> chỉ cần
    // hiển thị nó ra giao diện, không cần gọi thêm API nào khác.
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

const confirmStatus = (item, status) => {
  actionModal.targetStatus = status
  showActionModal(
    'confirm',
    status === 'SPAM' ? 'Đánh dấu Spam?' : 'Cập nhật trạng thái?',
    `Yêu cầu #${item.contactId} sẽ chuyển sang “${statusLabel(status)}”.`,
    item.contactId,
  )
}

// Xử lý nút Đồng Ý trên Action Modal (chỉ chạy khi type = 'confirm')
const executeAction = async () => {
  if (actionModal.type === 'confirm' && actionModal.targetId) {
    actionModal.visible = false
    try {
      await api.put(`/admin/contacts/${actionModal.targetId}/status`, {
        status: actionModal.targetStatus,
      })
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

// Sao chép User ID vào clipboard để admin dán nhanh khi tặng voucher / tra cứu tài khoản
const copyUserId = async (userId) => {
  if (!userId) return
  try {
    await navigator.clipboard.writeText(String(userId))
    copiedId.value = userId
    clearTimeout(copiedTimer)
    copiedTimer = setTimeout(() => {
      if (copiedId.value === userId) copiedId.value = null
    }, 1500)
  } catch {
    showActionModal(
      'error',
      'Không thể sao chép',
      'Trình duyệt không hỗ trợ sao chép tự động, vui lòng chọn và copy thủ công.',
    )
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

const statusLabel = (status) =>
  ({ NEW: 'Mới', IN_PROGRESS: 'Đang xử lý', RESOLVED: 'Đã giải quyết', SPAM: 'Spam', PENDING: 'Mới' })[status] || status

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

.detail-copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  padding: 5px 10px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.detail-copy-btn:hover {
  background: #dbeafe;
}

.detail-copy-btn.copied {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
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
