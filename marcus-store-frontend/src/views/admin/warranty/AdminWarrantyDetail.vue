<template>
  <section class="warranty-detail-page">
    <div class="page-heading">
      <div>
        <div class="breadcrumb">
          <RouterLink to="/admin/warranty">Đổi trả / Bảo hành</RouterLink>
          <span>/</span>
          <span>#WR{{ String(warrantyId).padStart(4, '0') }}</span>
        </div>
        <h3>Chi tiết yêu cầu bảo hành</h3>
        <p v-if="warranty.status">
          Trạng thái hiện tại: {{ warranty.statusLabel }}
        </p>
        <p v-else>Không tìm thấy dữ liệu cho yêu cầu này.</p>
      </div>

      <div class="page-actions">
        <RouterLink class="outline-btn" to="/admin/warranty">Quay lại</RouterLink>
      </div>
    </div>

    <div v-if="warranty.status" class="warranty-summary-card warranty-detail-card">
      <div class="summary-item">
        <span class="summary-label">Mã yêu cầu</span>
        <strong class="summary-value">#WR{{ String(warranty.warrantyId).padStart(4, '0') }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">Trạng thái</span>
        <span class="summary-value">
          <span class="badge" :class="getStatusClass(warranty.status)">
            {{ warranty.statusLabel }}
          </span>
        </span>
      </div>
      <div class="summary-item">
        <span class="summary-label">Lý do</span>
        <strong class="summary-value">{{ warranty.reasonLabel }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">Ngày tạo</span>
        <strong class="summary-value">{{ formatDate(warranty.createdAt) }}</strong>
      </div>
    </div>

    <template v-if="warranty.status">
      <div class="warranty-detail-layout">
        <div class="left-column">
          <section class="warranty-detail-card section-card">
            <div class="section-header">
              <div>
                <h4>Thông tin khách hàng</h4>
                <p>Thông tin người gửi yêu cầu đổi trả / bảo hành.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="info-grid">
                <div class="info-box">
                  <span class="info-label">Họ và tên</span>
                  <strong class="info-value">{{ warranty.userFullName || '---' }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Mã khách hàng</span>
                  <strong class="info-value">#{{ warranty.userId || '---' }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Số điện thoại</span>
                  <strong class="info-value">{{ warranty.userPhone || '---' }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Email</span>
                  <strong class="info-value">{{ warranty.userEmail || '---' }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="warranty-detail-card section-card">
            <div class="section-header">
              <div>
                <h4>Sản phẩm yêu cầu</h4>
                <p>Thông tin sản phẩm khách hàng muốn đổi trả hoặc bảo hành.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="warranty-product-card">
                <img
                  v-if="warranty.productImage"
                  :src="warranty.productImage"
                  :alt="warranty.productName"
                  class="warranty-product-image"
                />
                <div v-else class="warranty-product-image-placeholder">
                  <i class="fa-solid fa-mobile-screen-button"></i>
                </div>
                <div class="warranty-product-info">
                  <h4>{{ warranty.productName }}</h4>
                  <small style="color: #6b5660">
                    Đơn hàng:
                    <RouterLink
                      :to="`/admin/order/${warranty.orderItemId}`"
                      class="info-value link"
                    >
                      {{ warranty.orderCode }}
                    </RouterLink>
                  </small>
                </div>
              </div>
            </div>
          </section>

          <section class="warranty-detail-card section-card">
            <div class="section-header">
              <div>
                <h4>Mô tả từ khách hàng</h4>
                <p>Lý do và tình trạng sản phẩm được khách hàng mô tả.</p>
              </div>
            </div>
            <div class="section-body">
              <p class="warranty-description-text">{{ warranty.description || 'Không có mô tả' }}</p>
            </div>
          </section>

          <section
            v-if="warranty.attachments && warranty.attachments.length"
            class="warranty-detail-card section-card"
          >
            <div class="section-header">
              <div>
                <h4>Hình ảnh / Video từ khách</h4>
                <p>Bằng chứng đính kèm kèm theo yêu cầu.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="warranty-attachments-grid">
                <div
                  v-for="att in warranty.attachments"
                  :key="att.attachmentId"
                  class="warranty-attachment-item"
                  @click="openMedia(att)"
                >
                  <img v-if="att.fileType === 'IMAGE'" :src="att.fileUrl" :alt="att.fileName" />
                  <div v-else class="warranty-attachment-video">
                    <i class="bi bi-play-circle-fill"></i>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section v-if="warranty.adminNote" class="warranty-detail-card warranty-note-card section-card">
            <div class="section-header">
              <div>
                <h4>Ghi chú xử lý hiện tại</h4>
                <p>Ghi nhận từ admin trong lần cập nhật trước.</p>
              </div>
            </div>
            <div class="section-body">
              <p class="warranty-note-text">{{ warranty.adminNote }}</p>
              <div v-if="warranty.processedByName" class="warranty-note-meta">
                <i class="bi bi-person-check"></i>
                <span>Xử lý bởi: <strong>{{ warranty.processedByName }}</strong></span>
                <span style="color: #d1d5db">•</span>
                <span>{{ formatDate(warranty.processedAt) }}</span>
              </div>
            </div>
          </section>
        </div>

        <aside class="right-column">
          <section class="warranty-detail-card section-card dispatch-card">
            <div class="section-header">
              <div>
                <h4>Cập nhật trạng thái</h4>
                <p>Thay đổi trạng thái và ghi chú cho khách hàng.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="current-status-box">
                <span>Trạng thái hiện tại</span>
                <span class="badge" :class="getStatusClass(warranty.status)">
                  {{ warranty.statusLabel }}
                </span>
              </div>

              <div class="form-group">
                <label class="form-label" for="statusDropdown">Trạng thái mới</label>
                <select
                  id="statusDropdown"
                  v-model="newStatus"
                  class="control"
                  :disabled="isLocked || updating"
                >
                  <option
                    v-for="item in nextStatuses"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label" for="adminNote">
                  Ghi chú cho khách <span class="required-mark">*</span>
                </label>
                <textarea
                  id="adminNote"
                  v-model="adminNote"
                  class="control"
                  :class="{ 'is-invalid': noteError }"
                  rows="4"
                  :disabled="isLocked || updating"
                  placeholder="Nhập ghi chú để khách hàng nắm rõ lý do xử lý..."
                  style="resize: vertical; min-height: 90px"
                  @input="noteError = ''"
                ></textarea>
                <p v-if="noteError" class="error-text">{{ noteError }}</p>
              </div>

              <button
                v-if="!isLocked"
                class="primary-btn"
                type="button"
                :disabled="updating || !canSubmit"
                @click="submitUpdate"
              >
                <span v-if="updating" class="spinner-border spinner-border-sm me-2" role="status"></span>
                {{ updating ? 'Đang lưu...' : 'Lưu cập nhật' }}
              </button>
              <button
                v-else
                class="primary-btn"
                type="button"
                disabled
              >
                Đã hoàn tất
              </button>
            </div>
          </section>

          <section class="warranty-detail-card section-card">
            <div class="section-header">
              <div>
                <h4>Thông tin thời gian</h4>
                <p>Mốc xử lý của yêu cầu bảo hành.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="info-grid">
                <div class="info-box">
                  <span class="info-label">Ngày tạo</span>
                  <strong class="info-value">{{ formatDate(warranty.createdAt) }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Cập nhật cuối</span>
                  <strong class="info-value">{{ formatDate(warranty.updatedAt) }}</strong>
                </div>
                <div v-if="warranty.processedAt" class="info-box">
                  <span class="info-label">Thời điểm xử lý</span>
                  <strong class="info-value">{{ formatDate(warranty.processedAt) }}</strong>
                </div>
                <div v-if="warranty.processedByName" class="info-box">
                  <span class="info-label">Người xử lý</span>
                  <strong class="info-value">{{ warranty.processedByName }}</strong>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="warranty-detail-card empty-card">
      Không tìm thấy yêu cầu bảo hành. Vui lòng quay lại danh sách.
    </div>

    <div class="warranty-detail-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>

    <!-- Modal xem ảnh/video -->
    <div v-if="mediaModal.open" class="warranty-media-modal" @click.self="closeMedia">
      <div class="warranty-media-content">
        <button class="warranty-media-close" @click="closeMedia">
          <i class="bi bi-x-lg"></i>
        </button>
        <img v-if="mediaModal.type === 'IMAGE'" :src="mediaModal.url" alt="attachment" />
        <video v-else :src="mediaModal.url" controls autoplay></video>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AdminWarrantyApi } from '@/api/warrantyApi'
import '@/assets/css/WarrantyDetail.css'

const route = useRoute()
const toastMessage = ref('')

const warrantyId = ref(parseInt(route.params.id))
const loading = ref(false)
const updating = ref(false)
const canSubmit = ref(true)
const warranty = ref({})

const newStatus = ref('PENDING')
const adminNote = ref('')
const noteError = ref('')

const mediaModal = ref({
  open: false,
  type: '',
  url: '',
})

const warrantyStatusMap = {
  PENDING: { label: 'Chờ duyệt', className: 'pending' },
  APPROVED: { label: 'Đồng ý', className: 'approved' },
  REJECTED: { label: 'Từ chối', className: 'rejected' },
}

const allowedTransitions = {
  PENDING: [
    { value: 'APPROVED', label: 'Đồng ý' },
    { value: 'REJECTED', label: 'Từ chối' },
  ],
  APPROVED: [],
  REJECTED: [],
}

const isLocked = computed(() => {
  const s = warranty.value?.status
  return s === 'APPROVED' || s === 'REJECTED'
})

const nextStatuses = computed(() => {
  if (!warranty.value || !warranty.value.status) return []
  return allowedTransitions[warranty.value.status] || []
})

watch(
  () => warranty.value?.status,
  () => {
    const first = nextStatuses.value[0]?.value
    newStatus.value = first || warranty.value?.status || 'PENDING'
  },
  { immediate: true },
)

async function fetchWarranty() {
  try {
    loading.value = true
    const res = await AdminWarrantyApi.getWarrantyDetail(warrantyId.value)
    warranty.value = res.data?.data || res.data || {}
    newStatus.value =
      allowedTransitions[warranty.value.status]?.[0]?.value || warranty.value.status || 'PENDING'
    adminNote.value = warranty.value.adminNote || ''
  } catch (err) {
    console.error('Lỗi tải chi tiết bảo hành:', err)
    warranty.value = {}
    showToast('Không thể tải chi tiết yêu cầu.')
  } finally {
    loading.value = false
  }
}

async function submitUpdate() {
  if (updating.value) return
  updating.value = true
  try {
    if (!warranty.value || !newStatus.value) return

    const isValid = nextStatuses.value.some((item) => item.value === newStatus.value)
    if (!isValid) {
      showToast('Trạng thái mới không hợp lệ theo luồng xử lý.')
      return
    }

    if (!adminNote.value.trim()) {
      noteError.value = 'Vui lòng nhập ghi chú xử lý cho khách hàng.'
      return
    }
    noteError.value = ''

    await AdminWarrantyApi.updateStatus(warrantyId.value, {
      status: newStatus.value,
      adminNote: adminNote.value.trim(),
    })
    showToast('Cập nhật trạng thái thành công.')
    await fetchWarranty()
    adminNote.value = warranty.value.adminNote || ''
  } catch (err) {
    const message =
      err.response?.data?.message ||
      err.response?.data ||
      'Cập nhật trạng thái không thành công'
    showToast(message)
    console.error(err)
  } finally {
    updating.value = false
  }
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

function getStatusClass(status) {
  return warrantyStatusMap[status]?.className || 'pending'
}

function formatDate(value) {
  if (!value) return '---'
  return new Date(String(value).replace(' ', 'T')).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function showToast(message) {
  toastMessage.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      warrantyId.value = parseInt(newId)
      fetchWarranty()
    }
  },
)

onMounted(() => {
  fetchWarranty()
})
</script>

<style scoped></style>