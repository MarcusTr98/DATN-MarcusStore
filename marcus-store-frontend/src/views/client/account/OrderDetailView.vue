<template>
  <main class="client-order-page">
    <section class="client-order-shell">
      <div class="main-card">
        <div class="main-header-card">
          <div>
            <h2 class="main-title">
              <i class="fa-solid fa-box-open"></i>
              Chi tiết đơn hàng
            </h2>
            <p class="main-note">Theo dõi trạng thái, sản phẩm và thông tin giao hàng.</p>
          </div>
          <router-link class="back-link" to="/profile/orders">
            <i class="fa-solid fa-arrow-left"></i>
            Danh sách đơn
          </router-link>
        </div>

        <div class="main-body">
          <div v-if="loading" class="empty-state">Đang tải chi tiết đơn hàng...</div>
          <div v-else-if="error" class="empty-state text-danger">{{ error }}</div>

          <section v-else-if="selectedOrder" class="detail-stack">
            <div class="detail-header">
              <div class="detail-heading">
                <div>
                  <h3 class="detail-title">{{ selectedOrder.orderCode }}</h3>
                  <p class="detail-subtitle">
                    Ngày tạo: {{ formatDateTime(selectedOrder.createdAt) }}
                  </p>
                </div>
                <span
                  class="status-pill"
                  :class="statusConfig[selectedOrder.orderStatus]?.className"
                >
                  <i class="fa-solid" :class="statusConfig[selectedOrder.orderStatus]?.icon"></i>
                  {{ statusConfig[selectedOrder.orderStatus]?.label || selectedOrder.orderStatus }}
                </span>
              </div>

              <div class="meta-grid">
                <div class="meta-item">
                  <span class="meta-label"><i class="fa-solid fa-hashtag"></i>Mã đơn</span>
                  <strong class="meta-value">{{ selectedOrder.orderCode }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-credit-card"></i>Phương thức thanh toán</span
                  >
                  <strong class="meta-value">{{
                    getPaymentMethodLabel(selectedOrder.paymentMethod)
                  }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-circle-check"></i>Trạng thái TT</span
                  >
                  <strong class="meta-value">
                    {{ getPaymentStatusLabel(selectedOrder.paymentStatus) }}
                  </strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"><i class="fa-solid fa-truck-fast"></i>Mã vận đơn</span>
                  <strong class="meta-value">{{ selectedOrder.trackingCode || 'Chưa có' }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"><i class="fa-solid fa-clock"></i>Cập nhật</span>
                  <strong class="meta-value">{{ selectedOrder.updatedAt || '---' }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-calendar-check"></i>Thời gian TT</span
                  >
                  <strong class="meta-value">{{ displayPaymentDate || '---' }}</strong>
                </div>
              </div>

              <div class="timeline-body">
                <div
                  class="timeline"
                  :style="{
                    '--progress-width': timelineProgress,
                    '--timeline-columns': visibleTimelineSteps.length,
                  }"
                >
                  <div class="timeline-progress"></div>
                  <div
                    v-for="(step, index) in visibleTimelineSteps"
                    :key="step.key"
                    class="step"
                    :class="getStepClass(step, index)"
                  >
                    <div class="step-icon">
                      <i class="fa-solid" :class="step.icon"></i>
                    </div>

                    <div class="step-title">{{ step.title }}</div>
                    <div class="step-time">
                      {{ step.isCurrent ? 'Hiện tại' : formatDateTime(step.createdAt) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Marcus thêm thẻ theo dõi refund cho khách, không hiển thị dữ liệu kỹ thuật VNPAY. -->
            <section v-if="refund" class="refund-tracking-card">
              <div class="refund-icon"><i class="fa-solid fa-rotate-left"></i></div>
              <div class="refund-content">
                <div class="refund-heading">
                  <div>
                    <h3>Tiến trình hoàn tiền</h3>
                    <p>{{ refundStatusConfig[refund.status]?.description }}</p>
                  </div>
                  <span class="refund-pill" :class="refundStatusConfig[refund.status]?.className">
                    {{ refundStatusConfig[refund.status]?.label || refund.status }}
                  </span>
                </div>
                <div class="refund-values">
                  <div>
                    <span>Số tiền hoàn</span><strong>{{ formatMoney(refund.amount) }}</strong>
                  </div>
                  <div>
                    <span>Phí vận chuyển không hoàn</span
                    ><strong>{{ formatMoney(refund.shippingDeducted) }}</strong>
                  </div>
                  <div>
                    <span>Ngày yêu cầu</span><strong>{{ formatDateTime(refund.createdAt) }}</strong>
                  </div>
                </div>
                <p class="refund-reason"><strong>Lý do:</strong> {{ refund.reason }}</p>
              </div>
            </section>

            <div class="detail-grid">
              <div class="inner-stack">
                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-mobile-screen-button"></i>
                      Sản phẩm trong đơn
                    </h3>
                  </div>

                  <div class="product-list">
                    <article
                      v-for="item in selectedOrder.items || []"
                      :key="item.skuId || item.skuCode"
                      class="product-item"
                    >
                      <div class="product-thumb">
                        <img
                          v-if="item.productImage"
                          class="product-image"
                          :src="item.productImage"
                          :alt="item.productName"
                        />
                        <i v-else class="fa-solid fa-mobile-screen"></i>
                      </div>
                      <div>
                        <h4 class="product-name">{{ item.productName }}</h4>
                        <div v-if="getVariantText(item)" class="product-variants">
                          <i class="fa-solid fa-tags"></i>
                          <span>{{ getVariantText(item) }}</span>
                        </div>
                        <div class="product-meta">
                          <span
                            >SKU: <strong>{{ item.skuCode }}</strong></span
                          >
                          <span
                            >Số lượng: <strong>{{ item.quantity }}</strong></span
                          >
                          <span v-if="item.isFlashSale && item.originalPrice">
                            <span class="price-original">{{
                              formatMoney(item.originalPrice)
                            }}</span>
                            <span class="price-flashsale">{{
                              formatMoney(item.priceAtPurchase)
                            }}</span>
                            <span v-if="item.flashSaleSlotName" class="flash-badge">{{
                              item.flashSaleSlotName
                            }}</span>
                          </span>
                          <span v-else
                            >Đơn giá: <strong>{{ formatMoney(item.priceAtPurchase) }}</strong></span
                          >
                        </div>
                      </div>
                      <strong class="product-total">{{ formatMoney(item.lineTotal) }}</strong>
                    </article>
                  </div>
                </section>

                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-clock-rotate-left"></i>
                      Lịch sử trạng thái
                    </h3>
                  </div>

                  <div class="history-list">
                    <div
                      v-for="item in displayHistory"
                      :key="`${item.status}-${item.title}-${item.createdAt}`"
                      class="history-item"
                    >
                      <div class="history-icon">
                        <i
                          class="fa-solid"
                          :class="statusConfig[item.status]?.icon || 'fa-circle-info'"
                        ></i>
                      </div>
                      <div>
                        <div class="history-title">{{ item.title }}</div>
                        <div class="history-meta">
                          {{ statusConfig[item.status]?.label || item.status }} ·
                          {{ formatDateTime(item.createdAt) }}
                        </div>
                        <div v-if="item.note" class="history-note">
                          <span>Lý do:</span> {{ item.note }}
                        </div>
                      </div>
                    </div>
                  </div>
                </section>
              </div>

              <div class="inner-stack">
                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-location-dot"></i>
                      Thông tin giao hàng
                    </h3>
                  </div>

                  <div class="shipping-body">
                    <div class="receiver-card">
                      <div class="receiver-icon">
                        <i class="fa-solid fa-user"></i>
                      </div>
                      <div>
                        <div class="receiver-name">{{ selectedOrder.recipientName }}</div>
                        <div class="receiver-phone">
                          <i class="fa-solid fa-phone-volume"></i>
                          {{ selectedOrder.recipientPhone }}
                        </div>
                      </div>
                    </div>

                    <div class="info-line">
                      <i class="fa-solid fa-map-location-dot"></i>
                      <div>
                        <div class="info-label">Địa chỉ nhận hàng</div>
                        <div class="info-value">{{ selectedOrder.shippingAddress }}</div>
                      </div>
                    </div>

                    <div class="info-line">
                      <i class="fa-solid fa-truck-ramp-box"></i>
                      <div>
                        <div class="info-label">Mã vận đơn</div>
                        <div class="info-value">
                          {{ selectedOrder.trackingCode || 'Chưa có mã vận đơn' }}
                        </div>
                      </div>
                    </div>
                    <div class="info-line" v-if="selectedOrder.deliveryNote">
                      <i class="fa-solid fa-clipboard-user"></i>
                      <div>
                        <div class="info-label">Ghi chú của bạn</div>
                        <div class="info-value" style="font-style: italic">
                          "{{ selectedOrder.deliveryNote }}"
                        </div>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="panel">
                  <div class="panel-header">
                    <h3 class="panel-title">
                      <i class="fa-solid fa-file-invoice-dollar"></i>
                      Tổng thanh toán
                    </h3>
                  </div>

                  <div class="summary-body">
                    <!-- NÂNG CẤP: Bảng tóm tắt tài chính chuẩn xác cho Client -->
                    <div class="summary-row">
                      <span>Tạm tính</span>
                      <strong>{{ formatMoney(selectedOrder.totalAmount) }}</strong>
                    </div>

                    <div class="summary-row discount" v-if="selectedOrder.discountAmount > 0">
                      <span>Giảm giá Voucher</span>
                      <strong>-{{ formatMoney(selectedOrder.discountAmount) }}</strong>
                    </div>

                    <div class="summary-row">
                      <span>Phí vận chuyển</span>
                      <strong>{{ formatMoney(selectedOrder.shippingFee) }}</strong>
                    </div>

                    <div
                      class="summary-row discount"
                      v-if="selectedOrder.shippingSubsidy > 0"
                      style="color: #10b981"
                    >
                      <span>Trợ giá vận chuyển</span>
                      <strong>-{{ formatMoney(selectedOrder.shippingSubsidy) }}</strong>
                    </div>

                    <div class="summary-total">
                      <div class="summary-row">
                        <span>Thanh toán</span>
                        <strong>{{ formatMoney(selectedOrder.finalAmount) }}</strong>
                      </div>
                    </div>

                    <div class="detail-actions">
                      <button
                        type="button"
                        class="btn-dark btn-cancel-order"
                        :disabled="cancelling || !canCancelOrder"
                        @click="handleCancelOrder"
                      >
                        <i v-if="cancelling" class="fa-solid fa-spinner fa-spin"></i>
                        <i v-else class="fa-solid fa-ban"></i>
                        {{ cancelling ? 'Đang hủy đơn...' : 'Hủy đơn hàng' }}
                      </button>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </section>
        </div>
      </div>
      <div v-if="cancelModal.open" class="modal-backdrop" @click.self="closeCancelModal">
        <div class="modal-card" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h4 class="modal-title">
              <i class="fa-solid fa-ban"></i>
              Hủy đơn hàng
            </h4>
            <button
              type="button"
              class="modal-close"
              :disabled="cancelling"
              @click="closeCancelModal"
            >
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>

          <div class="modal-body">
            <p class="modal-text">
              Vui lòng cho Marcus Store biết lý do bạn muốn hủy đơn

              <strong>{{ cancelModal.orderCode }}</strong
              >.
            </p>
            <textarea
              v-model="cancelModal.reason"
              class="modal-input"
              rows="3"
              maxlength="500"
              placeholder="Ví dụ: Đặt nhầm size, đổi ý không muốn mua nữa..."
              :disabled="cancelling"
            ></textarea>

            <div class="modal-counter">{{ cancelModal.reason.length }}/500</div>

            <div
              v-if="cancelModal.feedback.message"
              class="modal-feedback"
              :class="`modal-feedback-${cancelModal.feedback.type}`"
              role="alert"
            >
              <i
                class="fa-solid"
                :class="
                  cancelModal.feedback.type === 'error'
                    ? 'fa-circle-exclamation'
                    : 'fa-circle-check'
                "
              ></i>
              <span>{{ cancelModal.feedback.message }}</span>
            </div>
          </div>

          <div class="modal-footer">
            <button
              type="button"
              class="btn-ghost"
              :disabled="cancelling"
              @click="closeCancelModal"
            >
              Quay lại
            </button>
            <button
              type="button"
              class="btn-danger"
              :disabled="cancelling || !cancelModal.reason.trim()"
              @click="confirmCancelOrder"
            >
              <i v-if="cancelling" class="fa-solid fa-spinner fa-spin"></i>
              <i v-else class="fa-solid fa-check"></i>
              {{ cancelling ? 'Đang xử lý...' : 'Xác nhận hủy' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import UserOrderApi from '@/api/userOrder.js'
import '@/assets/css/OrderDetailView.css'

const selectedOrder = ref(null)
// Marcus thêm state refund tách khỏi chi tiết đơn để không ảnh hưởng API cũ của thành viên.
const refund = ref(null)
const loading = ref(false)
const error = ref(null)
const route = useRoute()
let refundPollingTimer = null

// Marcus thêm hàm đồng bộ nhẹ để trạng thái khách cập nhật sau khi admin refund,
// không bật lại loading toàn trang và không làm nháy giao diện.
async function refreshRefundStatus() {
  const orderCode = route.params.id
  if (!orderCode) return
  try {
    const [orderResponse, refundResponse] = await Promise.all([
      UserOrderApi.userOrderDetail(orderCode),
      UserOrderApi.userRefund(orderCode),
    ])
    selectedOrder.value = orderResponse.data
    refund.value = refundResponse.status === 204 ? null : refundResponse.data
  } catch (refreshError) {
    console.error(refreshError)
  }
}

async function fetchOrderDetail() {
  try {
    loading.value = true
    error.value = null
    const orderCode = route.params.id

    const response = await UserOrderApi.userOrderDetail(orderCode)

    selectedOrder.value = response.data
    // Marcus sửa: lỗi tải refund không được làm mất toàn bộ trang chi tiết đơn.
    try {
      const refundResponse = await UserOrderApi.userRefund(orderCode)
      refund.value = refundResponse.status === 204 ? null : refundResponse.data
    } catch (refundError) {
      refund.value = null
      console.error(refundError)
    }
  } catch (e) {
    error.value = 'Không thể lấy chi tiết đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrderDetail()
  // Marcus thêm polling khi refund chưa kết thúc và refresh ngay khi khách quay lại tab.
  refundPollingTimer = window.setInterval(() => {
  if (refund.value && !['SUCCESS', 'FAILED', 'MANUAL_REVIEW'].includes(refund.value.status)) {
      refreshRefundStatus()
    }
  }, 10000)
  window.addEventListener('focus', refreshRefundStatus)
})

onBeforeUnmount(() => {
  if (refundPollingTimer) window.clearInterval(refundPollingTimer)
  window.removeEventListener('focus', refreshRefundStatus)
})

const USER_CANCELLABLE_STATUSES = ['PENDING', 'PROCESSING', 'PACKED']

// Marcus thêm nội dung thân thiện với khách thay cho response code kỹ thuật của VNPAY.
const refundStatusConfig = {
  PENDING_APPROVAL: {
    label: 'Chờ cửa hàng duyệt',
    className: 'pending',
    description: 'Cửa hàng đã tiếp nhận và đang kiểm tra yêu cầu hoàn tiền.',
  },
  PROCESSING: {
    label: 'Chờ VNPAY xác nhận',
    className: 'processing',
    description: 'Yêu cầu đã được gửi đến VNPAY và đang chờ xác nhận hoàn trả.',
  },
  SUBMITTING: {
    label: 'Đang gửi yêu cầu',
    className: 'processing',
    description: 'MarcusStore đang gửi yêu cầu hoàn tiền sang VNPAY.',
  },
  RETRY_PENDING: {
    label: 'Đang gửi lại yêu cầu',
    className: 'processing',
    description: 'Kết nối VNPAY chưa thành công; hệ thống sẽ tự động gửi lại.',
  },
  SUCCESS: {
    label: 'Đã hoàn tiền',
    className: 'success',
    description: 'VNPAY đã xác nhận hoàn tiền thành công.',
  },
  FAILED: {
    label: 'Chưa hoàn tất',
    className: 'failed',
    description: 'Yêu cầu hoàn tiền chưa hoàn tất. Cửa hàng sẽ kiểm tra và hỗ trợ bạn.',
  },
  MANUAL_REVIEW: {
    label: 'Cửa hàng đang kiểm tra',
    className: 'processing',
    description: 'Chưa có kết quả cuối từ VNPAY; cửa hàng đang kiểm tra giao dịch.',
  },
}

// Marcus sửa nhãn thanh toán để client không còn hiển thị "---" khi đang refund.
const getPaymentStatusLabel = (status) =>
  ({
    PAID: 'Đã thanh toán',
    UNPAID: 'Chưa thanh toán',
    FAILED: 'Thanh toán thất bại',
    REFUND_PENDING: 'Đang chờ hoàn tiền',
    REFUND_FAILED: 'Hoàn tiền cần hỗ trợ',
    REFUNDED: 'Đã hoàn tiền',
  })[status] ||
  status ||
  '---'

const canCancelOrder = computed(() => {
  const order = selectedOrder.value
  if (!order) return false
  // Marcus sửa: khách được hủy cả COD và VNPAY khi đơn chưa bước vào giao hàng.
  if (!['COD', 'VNPAY'].includes((order.paymentMethod || '').toUpperCase())) return false
  const status = (order.orderStatus || '').toUpperCase()
  return USER_CANCELLABLE_STATUSES.includes(status)
})

const cancelling = ref(false)

const cancelModal = ref({
  open: false,
  orderCode: '',
  reason: '',
  feedback: { type: '', message: '' },
})

function openCancelModal() {
  const order = selectedOrder.value
  if (!order || !canCancelOrder.value || cancelling.value) return
  cancelModal.value = {
    open: true,
    orderCode: order.orderCode,
    reason: '',
    feedback: { type: '', message: '' },
  }
}

function closeCancelModal() {
  if (cancelling.value) return
  cancelModal.value = {
    open: false,
    orderCode: '',
    reason: '',
    feedback: { type: '', message: '' },
  }
}

async function handleCancelOrder() {
  if (!canCancelOrder.value || cancelling.value) return
  openCancelModal()
}

async function confirmCancelOrder() {
  const modal = cancelModal.value
  if (!modal.open || cancelling.value) return

  const reason = modal.reason.trim()
  if (!reason) return

  cancelling.value = true
  modal.feedback = { type: '', message: '' }
  try {
    const response = await UserOrderApi.cancelOrder(modal.orderCode, { note: reason })
    if (response?.data) {
      selectedOrder.value = response.data
    } else {
      await fetchOrderDetail()
    }
    cancelling.value = false
    closeCancelModal()
  } catch (e) {
    modal.feedback = {
      type: 'error',
      message: e?.response?.data?.message || e?.message || 'Hủy đơn thất bại, vui lòng thử lại.',
    }
    cancelling.value = false
  }
}

const statusConfig = {
  CREATED: { label: 'Tạo đơn', className: 'pending', icon: 'fa-file-circle-plus' },
  PENDING: { label: 'Chờ xác nhận', className: 'pending', icon: 'fa-clock' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed', icon: 'fa-circle-check' },
  PROCESSING: { label: 'Đang chuẩn bị', className: 'processing', icon: 'fa-boxes-packing' },
  PACKED: { label: 'Đã đóng gói', className: 'processing', icon: 'fa-box' },
  SHIPPING: { label: 'Đang giao', className: 'shipping', icon: 'fa-truck-fast' },
  DELIVERED: { label: 'Giao thành công', className: 'delivered', icon: 'fa-circle-check' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled', icon: 'fa-ban' },
  FAILED: { label: 'Giao thất bại', className: 'failed', icon: 'fa-triangle-exclamation' },
}

const defaultTimelineSteps = [
  { status: 'PENDING' },
  { status: 'CONFIRMED' },
  { status: 'PROCESSING' },
  { status: 'PACKED' },
  { status: 'SHIPPING' },
  { status: 'DELIVERED' },
]

const visibleTimelineSteps = computed(() => {
  if (!selectedOrder.value) return []

  const currentStatus = selectedOrder.value.orderStatus
  const historyByStatus = new Map(
    (selectedOrder.value.history || []).map((item) => [item.status, item]),
  )

  const currentIndex = defaultTimelineSteps.findIndex((step) => step.status === currentStatus)
  const isTerminalStatus = currentStatus === 'CANCELLED' || currentStatus === 'FAILED'
  const isCompletedStatus = currentStatus === 'COMPLETED'

  let flowStatuses
  if (isTerminalStatus) {
    flowStatuses = defaultTimelineSteps
      .filter((step) => historyByStatus.has(step.status))
      .map((step) => step.status)
    flowStatuses.push(currentStatus)
  } else if (currentIndex >= 0 || isCompletedStatus) {
    // COMPLETED: hiển thị đầy đủ flow như DELIVERED (không hiện COMPLETED trên timeline)
    flowStatuses = [...defaultTimelineSteps.map((step) => step.status)]
  } else {
    flowStatuses = ['PENDING', currentStatus]
  }

  const statuses = ['CREATED', ...new Set(flowStatuses)]

  return statuses.map((status, index) => {
    const historyItem = historyByStatus.get(status)
    const timelineStep = createTimelineStep(
      status,
      index,
      status === 'CREATED' ? selectedOrder.value.createdAt : historyItem?.createdAt,
      historyItem?.note,
    )

    timelineStep.isCurrent =
      currentStatus === status || (currentStatus === 'COMPLETED' && status === 'DELIVERED')
    return timelineStep
  })
})

const displayHistory = computed(() => {
  if (!selectedOrder.value) return []

  // Lọc bỏ COMPLETED cho UI client (vẫn giữ ở admin)
  const history = (selectedOrder.value.history || []).filter((item) => item.status !== 'COMPLETED')
  const hasCreated = history.some((item) => item.status === 'CREATED')

  if (hasCreated) return history

  const createdItem = {
    status: 'CREATED',
    title: 'Tạo đơn',
    createdAt: selectedOrder.value.createdAt,
    note: null,
  }

  return [createdItem, ...history]
})

const displayPaymentDate = computed(() => {
  if (!selectedOrder.value) return ''

  const order = selectedOrder.value
  const isCOD = order.paymentMethod === 'COD'
  const hasPaymentDate = order.paymentDate

  // Nếu đã có paymentDate thì hiển thị paymentDate
  if (hasPaymentDate) {
    return formatDateTime(hasPaymentDate)
  }

  // Nếu là COD và chưa có paymentDate, lấy thời gian DELIVERED từ history
  if (isCOD) {
    const history = order.history || []
    const deliveredItem = history.find((item) => item.status === 'DELIVERED')
    if (deliveredItem?.createdAt) {
      return formatDateTime(deliveredItem.createdAt)
    }
  }

  return '---'
})

const timelineProgress = computed(() => {
  const steps = visibleTimelineSteps.value
  if (steps.length <= 1) return '0%'

  const currentIndex = steps.findIndex((step) => step.isCurrent)
  const safeIndex = currentIndex >= 0 ? currentIndex : steps.length - 1

  return `${(safeIndex / (steps.length - 1)) * 80}%`
})

function createTimelineStep(status, index, createdAt = null, note = null) {
  const config = statusConfig[status] || {}
  return {
    key: `${status}-${index}`,
    status,
    title: config.label || status,
    icon: config.icon || 'fa-circle-info',
    createdAt,
    note,
    isCurrent: false,
  }
}

function getStepClass(step, index) {
  if (step.status === 'CANCELLED' || step.status === 'FAILED') return 'cancel'
  if (step.isCurrent) return 'active'
  const currentIndex = visibleTimelineSteps.value.findIndex((item) => item.isCurrent)
  if (currentIndex === -1) {
    return index < visibleTimelineSteps.value.length - 1 ? 'done' : ''
  }
  if (index < currentIndex) return 'done'
  return ''
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

function getVariantText(item) {
  if (!item || !Array.isArray(item.variants) || item.variants.length === 0) return ''
  return item.variants
    .filter((v) => v && v.valueString)
    .map((v) => {
      if (v.attributeName) return `${v.attributeName}: ${v.valueString}`
      return v.valueString
    })
    .join(' | ')
}
</script>

<style scoped>
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 8px 12px;
  background: #fff1f2;
  color: #e60012;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.product-thumb {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #ccc;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.price-original {
  text-decoration: line-through;
  color: #9ca3af;
  font-size: 12px;
  margin-right: 4px;
}

.product-variants {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 4px 10px;
  background: #f3f4f6;
  color: #374151;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.product-variants i {
  color: #e60012;
  font-size: 11px;
}

.price-flashsale {
  color: #ef4444;
  font-weight: 700;
}

.flash-badge {
  font-size: 10px;
  background: #fee2e2;
  color: #ef4444;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  margin-left: 4px;
}

/* Marcus thêm giao diện theo dõi refund cho client. */
.refund-tracking-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  border: 1px solid #fde68a;
  border-radius: 14px;
  background: #fffbeb;
}
.refund-icon {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fef3c7;
  color: #d97706;
}
.refund-content {
  flex: 1;
  min-width: 0;
}
.refund-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.refund-heading h3 {
  margin: 0 0 4px;
  font-size: 18px;
}
.refund-heading p,
.refund-reason {
  margin: 0;
  color: #6b7280;
}
.refund-pill {
  padding: 6px 10px;
  border-radius: 999px;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
  background: #e5e7eb;
}
.refund-pill.pending,
.refund-pill.processing {
  color: #92400e;
  background: #fef3c7;
}
.refund-pill.success {
  color: #166534;
  background: #dcfce7;
}
.refund-pill.failed {
  color: #991b1b;
  background: #fee2e2;
}
.refund-values {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}
.refund-values div {
  padding: 12px;
  border-radius: 10px;
  background: #fff;
}
.refund-values span {
  display: block;
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
}
@media (max-width: 768px) {
  .refund-heading {
    flex-direction: column;
  }
  .refund-values {
    grid-template-columns: 1fr;
  }
}
</style>
