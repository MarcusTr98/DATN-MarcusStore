<template>
  <div class="admin-warranty-detail">
    <div class="detail-header">
      <button class="btn-back" @click="goBack">
        <i class="bi bi-arrow-left"></i>
        Quay lại
      </button>
      <div class="header-status">
        <span class="status-badge" :class="`status-${warranty.status.toLowerCase()}`">
          {{ warranty.statusLabel }}
        </span>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu...</p>
    </div>

    <div v-else class="detail-content">
      <div class="detail-main">
        <div class="card">
          <h3 class="card-title">Thông tin yêu cầu #{{ warrantyId }}</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">Mã đơn hàng</span>
              <router-link :to="`/admin/order/${warranty.orderItemId}`" class="info-value link">
                {{ warranty.orderCode }}
              </router-link>
            </div>
            <div class="info-item">
              <span class="info-label">Ngày tạo</span>
              <span class="info-value">{{ formatDate(warranty.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Lý do</span>
              <span class="info-value">{{ warranty.reasonLabel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Cập nhật cuối</span>
              <span class="info-value">{{ formatDate(warranty.updatedAt) }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">Sản phẩm yêu cầu</h3>
          <div class="product-card">
            <img :src="warranty.productImage || '/placeholder.png'" :alt="warranty.productName" class="product-image" />
            <div class="product-info">
              <h4 class="product-name">{{ warranty.productName }}</h4>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">Mô tả từ khách hàng</h3>
          <p class="description-text">{{ warranty.description || 'Không có mô tả' }}</p>
        </div>

        <div v-if="warranty.attachments && warranty.attachments.length" class="card">
          <h3 class="card-title">Hình ảnh / Video từ khách</h3>
          <div class="attachments-grid">
            <div
              v-for="att in warranty.attachments"
              :key="att.attachmentId"
              class="attachment-item"
              @click="openMedia(att)"
            >
              <img v-if="att.fileType === 'IMAGE'" :src="att.fileUrl" :alt="att.fileName" />
              <div v-else class="video-thumb">
                <i class="bi bi-play-circle-fill"></i>
              </div>
            </div>
          </div>
        </div>

        <div v-if="warranty.adminNote" class="card card-note">
          <h3 class="card-title">Ghi chú xử lý hiện tại</h3>
          <p class="note-text">{{ warranty.adminNote }}</p>
          <div v-if="warranty.processedByName" class="note-meta">
            <i class="bi bi-person-check"></i>
            <span>Xử lý bởi: <strong>{{ warranty.processedByName }}</strong></span>
            <span class="dot">•</span>
            <span>{{ formatDate(warranty.processedAt) }}</span>
          </div>
        </div>
      </div>

      <div class="detail-sidebar">
        <div class="card card-action">
          <h3 class="card-title">Cập nhật trạng thái</h3>

          <div class="form-group">
            <label class="form-label">Trạng thái mới</label>
            <select v-model="newStatus" class="form-control">
              <option value="PENDING">Chờ xử lý</option>
              <option value="APPROVED">Đã duyệt</option>
              <option value="REJECTED">Từ chối</option>
              <option value="COMPLETED">Hoàn thành</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Ghi chú xử lý</label>
            <textarea
              v-model="adminNote"
              class="form-control"
              rows="5"
              placeholder="Nhập ghi chú cho khách hàng..."
            ></textarea>
          </div>

          <div class="action-buttons">
            <button class="btn-cancel" @click="resetForm" :disabled="updating">
              Hủy
            </button>
            <button class="btn-submit" @click="submitUpdate" :disabled="updating">
              <i v-if="updating" class="bi bi-arrow-clockwise spinning"></i>
              <i v-else class="bi bi-check-circle"></i>
              {{ updating ? 'Đang xử lý...' : 'Cập nhật' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast thông báo -->
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="`toast-${toast.type}`">
        <i
          :class="
            toast.type === 'success'
              ? 'bi bi-check-circle-fill'
              : toast.type === 'error'
              ? 'bi bi-x-circle-fill'
              : 'bi bi-exclamation-triangle-fill'
          "
        ></i>
        <span>{{ toast.message }}</span>
      </div>
    </transition>

    <!-- Modal xem ảnh/video -->
    <div v-if="mediaModal.open" class="media-modal" @click.self="closeMedia">
      <div class="media-modal-content">
        <button class="media-close" @click="closeMedia">
          <i class="bi bi-x-lg"></i>
        </button>
        <img v-if="mediaModal.type === 'IMAGE'" :src="mediaModal.url" alt="attachment" />
        <video v-else :src="mediaModal.url" controls autoplay></video>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AdminWarrantyApi } from '@/api/warrantyApi'

const route = useRoute()
const router = useRouter()

const warrantyId = ref(parseInt(route.params.id))
const loading = ref(false)
const updating = ref(false)
const warranty = ref({})

const newStatus = ref('')
const adminNote = ref('')

const mediaModal = ref({
  open: false,
  type: '',
  url: '',
})

const toast = ref({
  show: false,
  message: '',
  type: 'success',
})

function showToast(message, type = 'success') {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

async function fetchWarranty() {
  loading.value = true
  try {
    const res = await AdminWarrantyApi.getWarrantyDetail(warrantyId.value)
    warranty.value = res.data?.data || res.data || {}
    newStatus.value = warranty.value.status || 'PENDING'
    adminNote.value = warranty.value.adminNote || ''
  } catch (err) {
    console.error('Lỗi tải chi tiết bảo hành:', err)
    showToast('Không thể tải chi tiết yêu cầu', 'error')
  } finally {
    loading.value = false
  }
}

async function submitUpdate() {
  if (!newStatus.value) {
    showToast('Vui lòng chọn trạng thái', 'warning')
    return
  }

  updating.value = true
  try {
    await AdminWarrantyApi.updateStatus(warrantyId.value, {
      status: newStatus.value,
      adminNote: adminNote.value.trim() || null,
    })
    showToast('Cập nhật trạng thái thành công', 'success')
    await fetchWarranty()
  } catch (err) {
    console.error('Lỗi cập nhật:', err)
    showToast('Cập nhật thất bại: ' + (err.response?.data?.message || err.message), 'error')
  } finally {
    updating.value = false
  }
}

function resetForm() {
  newStatus.value = warranty.value.status || 'PENDING'
  adminNote.value = warranty.value.adminNote || ''
}

function goBack() {
  router.push('/admin/warranty')
}

function openMedia(att) {
  mediaModal.value = {
    open: true,
    type: att.fileType,
    url: att.fileUrl,
  }
}

function closeMedia() {
  mediaModal.value = { open: false, type: '', url: '' }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchWarranty()
})
</script>

<style scoped>
.admin-warranty-detail {
  padding: 24px 28px;
  background: #fff7fa;
  min-height: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  background: #fff;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #fff5f8;
  border-color: #ff4d94;
  color: #ff4d94;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
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

.loading-state {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  padding: 60px 20px;
  text-align: center;
  color: #6b7280;
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

.detail-content {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}

@media (max-width: 1024px) {
  .detail-content {
    grid-template-columns: 1fr;
  }
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-sidebar {
  position: sticky;
  top: 24px;
  align-self: flex-start;
}

.card {
  background: #fff;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(255, 77, 148, 0.04);
}

.card-title {
  font-size: 15px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #fef2f3;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.info-value.link {
  color: #3b82f6;
  text-decoration: none;
}

.info-value.link:hover {
  text-decoration: underline;
}

.product-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 14px;
  background: #fff5f8;
  border-radius: 10px;
  border: 1px solid #fee2e2;
}

.product-image {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  object-fit: cover;
  background: #f5f5f5;
}

.product-name {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.description-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

.attachments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.attachment-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #fee2e2;
  transition: transform 0.2s;
}

.attachment-item:hover {
  transform: scale(1.03);
}

.attachment-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-thumb {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  color: #fff;
}

.video-thumb i {
  font-size: 40px;
  color: #ff4d94;
}

.card-note {
  background: linear-gradient(135deg, #fff5f8 0%, #fff 100%);
}

.note-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0 0 12px;
  white-space: pre-wrap;
}

.note-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
  padding-top: 12px;
  border-top: 1px solid #fee2e2;
}

.dot {
  color: #d1d5db;
}

.card-action {
  position: sticky;
  top: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
  margin-bottom: 6px;
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #111827;
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
  resize: vertical;
}

.form-control:focus {
  border-color: #ff4d94;
}

.form-control:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.btn-cancel,
.btn-submit {
  flex: 1;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-cancel {
  background: #fff;
  border: 1.5px solid #e5e7eb;
  color: #6b7280;
}

.btn-cancel:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #d1d5db;
}

.btn-submit {
  background: linear-gradient(135deg, #ff4d94 0%, #ff1a75 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 77, 148, 0.3);
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 77, 148, 0.4);
}

.btn-cancel:disabled,
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinning {
  animation: spin 0.7s linear infinite;
}

/* Media Modal */
.media-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.media-modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.media-modal-content img,
.media-modal-content video {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
}

.media-close {
  position: absolute;
  top: -40px;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: background 0.2s;
}

.media-close:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* Toast */
.toast {
  position: fixed;
  top: 24px;
  right: 24px;
  padding: 12px 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  z-index: 99999;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  max-width: 380px;
}

.toast-success {
  background: #fff;
  border-left: 4px solid #10b981;
  color: #065f46;
}

.toast-error {
  background: #fff;
  border-left: 4px solid #ef4444;
  color: #991b1b;
}

.toast-warning {
  background: #fff;
  border-left: 4px solid #f59e0b;
  color: #92400e;
}

.toast i {
  font-size: 18px;
}

.toast-success i { color: #10b981; }
.toast-error i { color: #ef4444; }
.toast-warning i { color: #f59e0b; }

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
