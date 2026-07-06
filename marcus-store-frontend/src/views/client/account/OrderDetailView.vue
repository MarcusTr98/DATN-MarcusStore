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
                  <span class="meta-label"><i class="fa-solid fa-credit-card"></i>Thanh toán</span>
                  <strong class="meta-value">{{
                    getPaymentMethodLabel(selectedOrder.paymentMethod)
                  }}</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label"
                    ><i class="fa-solid fa-circle-check"></i>Trạng thái TT</span
                  >
                  <strong class="meta-value">{{ selectedOrder.paymentStatus || '---' }}</strong>
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
                  <strong class="meta-value">{{ selectedOrder.paymentDate || '---' }}</strong>
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
                    <div class="step-code">{{ step.status }}</div>
                    <div class="step-title">{{ step.title }}</div>
                    <div class="step-time">
                      {{ step.isCurrent ? 'Hiện tại' : formatDateTime(step.createdAt) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

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
                        <div class="product-meta">
                          <span
                            >SKU: <strong>{{ item.skuCode }}</strong></span
                          >
                          <span
                            >Số lượng: <strong>{{ item.quantity }}</strong></span
                          >
                          <span
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
                    <div class="summary-row">
                      <span>Tạm tính</span>
                      <strong>{{ formatMoney(selectedOrder.totalAmount) }}</strong>
                    </div>
                    <div class="summary-row discount">
                      <span>Giảm giá</span>
                      <strong>-{{ formatMoney(selectedOrder.discountAmount) }}</strong>
                    </div>
                    <div class="summary-row">
                      <span>Phí vận chuyển</span>
                      <strong>{{ formatMoney(selectedOrder.shippingFee) }}</strong>
                    </div>
                    <div class="summary-total">
                      <div class="summary-row">
                        <span>Thanh toán</span>
                        <strong>{{ formatMoney(selectedOrder.finalAmount) }}</strong>
                      </div>
                    </div>

                    <div class="detail-actions">
                      <button class="btn-dark" type="button">
                        <i class="fa-solid fa-phone"></i>
                        Liên hệ shop
                      </button>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

// 1. Đổi lại đúng API của Client (chứ không dùng AdminOrderApi nữa)
import UserOrderApi from '@/api/userOrder.js'
import '@/assets/css/OrderDetailView.css'

const selectedOrder = ref(null)
const loading = ref(false)
const error = ref(null)
const route = useRoute()

async function fetchOrderDetail() {
  try {
    loading.value = true
    error.value = null
    const orderCode = route.params.id

    // 2. Gọi đúng hàm lấy chi tiết đơn của User
    const response = await UserOrderApi.userOrderDetail(orderCode)

    selectedOrder.value = response.data
  } catch (e) {
    error.value = 'Không thể lấy chi tiết đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchOrderDetail)

const statusConfig = {
  CREATED: { label: 'Tạo đơn', className: 'pending', icon: 'fa-file-circle-plus' },
  PENDING: { label: 'Chờ xác nhận', className: 'pending', icon: 'fa-clock' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed', icon: 'fa-circle-check' },
  PROCESSING: { label: 'Đang chuẩn bị', className: 'processing', icon: 'fa-boxes-packing' },
  SHIPPING: { label: 'Đang giao', className: 'shipping', icon: 'fa-truck-fast' },
  COMPLETED: { label: 'Hoàn thành', className: 'completed', icon: 'fa-house-circle-check' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled', icon: 'fa-ban' },
  FAILED: { label: 'Giao thất bại', className: 'failed', icon: 'fa-triangle-exclamation' },
}

const defaultTimelineSteps = [
  { status: 'PENDING' },
  { status: 'CONFIRMED' },
  { status: 'PROCESSING' },
  { status: 'SHIPPING' },
  { status: 'COMPLETED' },
]

const visibleTimelineSteps = computed(() => {
  if (!selectedOrder.value) return []

  const currentStatus = selectedOrder.value.orderStatus
  const historyByStatus = new Map(
    (selectedOrder.value.history || []).map((item) => [item.status, item]),
  )

  // Xác định các bước flow đã đi qua dựa trên currentStatus
  const currentIndex = defaultTimelineSteps.findIndex((step) => step.status === currentStatus)
  const isTerminalStatus = currentStatus === 'CANCELLED' || currentStatus === 'FAILED'

  let flowStatuses
  if (isTerminalStatus) {
    // Trạng thái kết thúc: hiển thị tất cả các bước đã đi qua
    // từ PENDING đến bước hiện tại trước khi cancel/fail, lấy từ history
    flowStatuses = defaultTimelineSteps
      .filter((step) => historyByStatus.has(step.status))
      .map((step) => step.status)
    flowStatuses.push(currentStatus)
  } else if (currentIndex >= 0) {
    flowStatuses = defaultTimelineSteps.slice(0, currentIndex + 1).map((step) => step.status)
  } else {
    flowStatuses = ['PENDING', currentStatus]
  }

  // Luôn bắt đầu timeline bằng CREATED
  const statuses = ['CREATED', ...new Set(flowStatuses)]

  return statuses.map((status, index) => {
    const historyItem = historyByStatus.get(status)
    const timelineStep = createTimelineStep(
      status,
      index,
      status === 'CREATED' ? selectedOrder.value.createdAt : historyItem?.createdAt,
      historyItem?.note,
    )
    timelineStep.isCurrent = currentStatus === status
    return timelineStep
  })
})

const displayHistory = computed(() => {
  if (!selectedOrder.value) return []

  const history = selectedOrder.value.history || []
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

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
