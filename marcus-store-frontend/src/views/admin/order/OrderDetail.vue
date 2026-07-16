<template>
  <section class="order-detail-page">
    <div class="page-heading">
      <div>
        <div class="breadcrumb">
          <RouterLink to="/admin/order">Quản lý đơn hàng</RouterLink>
          <span>/</span>
          <span>{{ orderDetail?.orderCode || 'Không tìm thấy' }}</span>
        </div>
        <h3>Chi tiết đơn hàng</h3>
        <p v-if="orderDetail">
          Đơn {{ orderDetail.orderCode }} - {{ getOrderStatusLabel(orderDetail.orderStatus) }}
        </p>
        <p v-else>Không tìm thấy dữ liệu cho đơn hàng này.</p>
      </div>

      <div class="page-actions no-print">
        <RouterLink class="outline-btn" to="/admin/order">Quay lại</RouterLink>
        <button class="outline-btn" type="button" @click="printPage">In đơn hàng</button>
      </div>
    </div>

    <!-- PRINT-ONLY HEADER -->
    <div class="print-only-header">
      <h1>Marcus Store</h1>
      <p>Hóa đơn bán hàng</p>
      <p v-if="orderDetail">
        Mã đơn: <strong>{{ orderDetail.orderCode }}</strong>
      </p>
    </div>

    <template v-if="orderDetail">
      <div class="summary-card card">
        <div class="summary-item">
          <span class="summary-label">Mã đơn</span>
          <strong class="summary-value">{{ orderDetail.orderCode }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">Trạng thái</span>
          <span class="summary-value">
            <span class="badge" :class="getOrderStatusClass(orderDetail.orderStatus)">
              {{ getOrderStatusLabel(orderDetail.orderStatus) }}
            </span>
          </span>
        </div>
        <div class="summary-item">
          <span class="summary-label">Tổng thanh toán</span>
          <strong class="summary-value money">{{ formatCurrency(finalAmount) }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">Ngày tạo</span>
          <strong class="summary-value">{{ formatDateTime(orderDetail.createdAt) }}</strong>
        </div>
      </div>

      <div class="detail-layout">
        <div class="left-column">
          <section class="card section-card">
            <div class="section-header">
              <div>
                <h4>Thông tin khách hàng</h4>
                <p>Thông tin người nhận và địa chỉ giao hàng.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="info-grid">
                <div class="info-box">
                  <span class="info-label">Khách hàng</span>
                  <strong class="info-value">{{ orderDetail.fullName }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Số điện thoại</span>
                  <strong class="info-value">{{ orderDetail.phoneNumber }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Email</span>
                  <strong class="info-value">{{ orderDetail.email }}</strong>
                </div>
                <div class="info-box full">
                  <span class="info-label">Địa chỉ giao hàng</span>
                  <strong class="info-value">{{ orderDetail.shippingAddress }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="card section-card">
            <div class="section-header">
              <div>
                <h4>Sản phẩm trong đơn</h4>
                <p>
                  {{ orderDetail.items?.length || 0 }} dòng sản phẩm, tổng {{ totalQuantity }} sản
                  phẩm.
                </p>
              </div>
            </div>
            <div class="section-body">
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Sản phẩm</th>
                      <th>SKU</th>
                      <th>Số lượng</th>
                      <th>Giá mua</th>
                      <th>Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in orderDetail.items" :key="item.skuId">
                      <td>
                        <div class="product-cell">
                          <span class="product-thumb no-print">📦</span>
                          <span>
                            <span class="main-line">{{ item.productName }}</span>
                          </span>
                        </div>
                      </td>
                      <td>{{ item.skuCode }}</td>
                      <td>{{ item.quantity }}</td>
                      <td>
                        <div class="price-cell">
                          <template v-if="item.isFlashSale && item.originalPrice">
                            <span class="money original-price">{{ formatCurrency(item.originalPrice) }}</span>
                            <span class="money flash-price">{{ formatCurrency(item.priceAtPurchase) }}</span>
                            <span v-if="item.flashSaleSlotName" class="flash-badge">{{ item.flashSaleSlotName }}</span>
                          </template>
                          <template v-else>
                            <span class="money">{{ formatCurrency(item.priceAtPurchase) }}</span>
                          </template>
                        </div>
                      </td>
                      <td>
                        <span class="money">{{ formatCurrency(item.lineTotal) }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- NÂNG CẤP: Bảng tổng hợp dòng tiền chuẩn xác -->
              <div class="table-summary">
                <div class="summary-row">
                  <span>Tạm tính</span><strong>{{ formatCurrency(subTotal) }}</strong>
                </div>

                <div v-if="orderDetail.discountAmount > 0" class="summary-row">
                  <span
                    >Mã giảm giá:
                    <strong class="voucher-code">{{
                      orderDetail.voucherCode || 'VOUCHER'
                    }}</strong></span
                  >
                  <strong>- {{ formatCurrency(orderDetail.discountAmount) }}</strong>
                </div>

                <div class="summary-row">
                  <span>Phí vận chuyển</span>
                  <strong>{{ formatCurrency(orderDetail.shippingFee) }}</strong>
                </div>

                <div
                  v-if="orderDetail.shippingSubsidy > 0"
                  class="summary-row"
                  style="color: #10b981"
                >
                  <span>Trợ giá vận chuyển</span>
                  <strong>- {{ formatCurrency(orderDetail.shippingSubsidy) }}</strong>
                </div>

                <div class="summary-row total">
                  <span>Tổng thanh toán</span><strong>{{ formatCurrency(finalAmount) }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="card section-card no-print">
            <div class="section-header">
              <div>
                <h4>Mốc xử lý & lịch sử thao tác</h4>
                <p>Các trạng thái đã được ghi nhận theo thời gian.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="timeline">
                <div
                  v-for="item in orderHistory"
                  :key="`${item.status}-${item.time}`"
                  class="timeline-item"
                >
                  <span class="timeline-dot">✓</span>
                  <div class="timeline-content">
                    <p class="timeline-title">
                      {{ item.title || getOrderStatusLabel(item.status) }}
                      <span class="badge" :class="getOrderStatusClass(item.status)">
                        {{ getOrderStatusLabel(item.status) }}
                      </span>
                    </p>
                    <p v-if="item.createdByName" class="timeline-note">
                      Người thao tác: {{ item.createdByName }}
                    </p>
                    <p v-if="item.note" class="timeline-note">Lý do: {{ item.note }}</p>
                    <p class="timeline-time">{{ formatDateTime(item.time) }}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>

        <aside class="right-column">
          <section class="card section-card dispatch-card">
            <div class="section-header">
              <div>
                <h4>Điều phối đơn</h4>
                <p>Cập nhật trạng thái đơn hàng.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="current-status-box">
                <span>Trạng thái hiện tại</span>
                <strong>
                  <span class="badge" :class="getOrderStatusClass(orderDetail.orderStatus)">
                    {{ getOrderStatusLabel(orderDetail.orderStatus) }}
                  </span>
                </strong>
              </div>

              <div class="form-group">
                <label class="form-label" for="statusDropdown">Cập nhật trạng thái đơn hàng</label>
                <select
                  id="statusDropdown"
                  v-model="selectedStatus"
                  class="control"
                  :disabled="!nextStatuses.length"
                >
                  <option v-if="!nextStatuses.length" :value="orderDetail.orderStatus">
                    {{ getOrderStatusLabel(orderDetail.orderStatus) }}
                  </option>
                  <option v-for="item in nextStatuses" :key="item.value" :value="item.value">
                    {{ item.label }} ({{ item.value }})
                  </option>
                </select>
              </div>

              <div v-if="nextStatuses.length > 0 && isStatusNoteRequired" class="status-note-box">
                <label class="status-note-label">Ghi chú trạng thái</label>
                <input
                  v-model="statusNote"
                  type="text"
                  class="status-note-input"
                  placeholder="Nhập lý do xử lý trạng thái..."
                />
              </div>

              <button
                class="primary-btn"
                type="button"
                :disabled="!nextStatuses.length || updatingStatus"
                @click="saveStatusUpdate"
              >
                {{ updatingStatus ? 'Đang lưu...' : 'Lưu cập nhật' }}
              </button>
            </div>
          </section>

          <section class="card section-card">
            <div class="section-header">
              <div>
                <h4>Giao hàng & hóa đơn</h4>
                <p>Thông tin vận chuyển của đơn hàng.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="mini-list">
                <div class="mini-row">
                  <span class="mini-label">Mã hóa đơn</span
                  ><span class="mini-value">{{ orderDetail.orderCode }}</span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Mã vận đơn</span
                  ><span class="mini-value">{{ orderDetail.trackingCode || '---' }}</span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Phí ship gốc</span>
                  <span class="mini-value">{{ formatCurrency(orderDetail.shippingFee) }}</span>
                </div>
                <div class="mini-row" v-if="orderDetail.shippingSubsidy > 0">
                  <span class="mini-label">Trợ giá ship</span>
                  <span class="mini-value" style="color: #10b981"
                    >-{{ formatCurrency(orderDetail.shippingSubsidy) }}</span
                  >
                </div>
                <div class="mini-row align-items-start">
                  <span class="mini-label mt-1">Ghi chú</span>
                  <span class="mini-value" :class="{ 'text-muted': !orderDetail.deliveryNote }">
                    {{ orderDetail.deliveryNote || 'Không có ghi chú' }}
                  </span>
                </div>
              </div>
            </div>
          </section>

          <section class="card section-card">
            <div class="section-header">
              <div>
                <h4>Thanh toán</h4>
                <p>Trạng thái và phương thức thanh toán.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="mini-list">
                <div class="mini-row">
                  <span class="mini-label">Trạng thái</span>
                  <span class="mini-value">
                    <span class="badge" :class="getPaymentStatusClass(orderDetail.paymentStatus)">
                      {{ getPaymentStatusLabel(orderDetail.paymentStatus) }}
                    </span>
                  </span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Phương thức</span
                  ><span class="mini-value">{{
                    getPaymentMethodLabel(orderDetail.paymentMethod)
                  }}</span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Mã giao dịch</span
                  ><span class="mini-value">{{ orderDetail.transactionId || '---' }}</span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Thời gian TT</span
                  ><span class="mini-value">{{ orderDetail.paymentDate || '---' }}</span>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="card empty-card">
      Không tìm thấy đơn hàng. Vui lòng quay lại danh sách đơn hàng.
    </div>

    <div class="order-detail-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </section>
</template>

<script setup>
import { computed, ref, watch, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import '@/assets/css/OrderDetails.css'
import OrderDetailApi from '@/api/orderDetailApi.js'

const route = useRoute()
const toastMessage = ref('')
const orderDetail = ref(null)
const loading = ref(false)
const error = ref(null)

const selectedStatus = ref('')
const statusNote = ref('')
const updatingStatus = ref(false)

async function fetchGetDetailOrder(orderCode) {
  try {
    loading.value = true
    error.value = null
    const response = await OrderDetailApi.getOrderDetail(orderCode)
    orderDetail.value = response.data
  } catch (e) {
    error.value = 'Không tải được chi tiết đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      orderDetail.value = null
      fetchGetDetailOrder(newId)
    }
  },
  { immediate: true },
)

const orderStatusMap = {
  PENDING: { label: 'Chờ xác nhận', className: 'pending' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed' },
  PROCESSING: { label: 'Đang chuẩn bị', className: 'processing' },
  PACKED: { label: 'Đã đóng gói', className: 'processing' },
  SHIPPING: { label: 'Đang giao', className: 'shipping' },
  DELIVERED: { label: 'Giao thành công', className: 'shipping' },
  COMPLETED: { label: 'Hoàn thành', className: 'completed' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled' },
  FAILED: { label: 'Giao thất bại', className: 'failed' },
}

const paymentStatusMap = {
  PAID: { label: 'Đã thanh toán', className: 'confirmed' },
  UNPAID: { label: 'Chưa thanh toán', className: 'pending' },
  REFUNDED: { label: 'Đã hoàn tiền', className: 'cancelled' },
  FAILED: { label: 'Lỗi thanh toán', className: 'failed' },
}

const paymentMethodMap = {
  VNPay: 'VNPAY',
  VNPAY: 'VNPAY',
  COD: 'COD',
  MoMo: 'MoMo',
  BankTransfer: 'Chuyển khoản',
}

const allowedTransitions = {
  PENDING: [
    { value: 'CONFIRMED', label: 'Xác nhận đơn' },
    { value: 'CANCELLED', label: 'Hủy đơn' },
  ],
  CONFIRMED: [
    { value: 'PROCESSING', label: 'Bắt đầu chuẩn bị hàng' },
    { value: 'CANCELLED', label: 'Hủy đơn & Hoàn tiền' },
  ],
  PROCESSING: [
    { value: 'PACKED', label: 'Đã đóng gói' },
    { value: 'CANCELLED', label: 'Hủy đơn' },
  ],
  PACKED: [
    { value: 'SHIPPING', label: 'Bắt đầu giao hàng' },
    { value: 'CANCELLED', label: 'Hủy đơn' },
  ],
  SHIPPING: [
    { value: 'DELIVERED', label: 'Giao thành công' },
    { value: 'FAILED', label: 'Giao thất bại' },
  ],

  DELIVERED: [{ value: 'COMPLETED', label: 'Đối soát hoàn tất' }],
  COMPLETED: [],
  CANCELLED: [],
  FAILED: [
    { value: 'SHIPPING', label: 'Giao lại' },
    { value: 'CANCELLED', label: 'Hủy đơn' },
  ],
}

const statusesRequiringNote = ['CANCELLED', 'FAILED']

const nextStatuses = computed(() =>
  orderDetail.value ? allowedTransitions[orderDetail.value.orderStatus] || [] : [],
)

const subTotal = computed(
  () => orderDetail.value?.items?.reduce((sum, item) => sum + Number(item.lineTotal || 0), 0) || 0,
)

const totalQuantity = computed(
  () => orderDetail.value?.items?.reduce((sum, item) => sum + Number(item.quantity || 0), 0) || 0,
)

const finalAmount = computed(() => {
  if (!orderDetail.value) return 0

  const tempTotal = subTotal.value
  const discount = Number(orderDetail.value.discountAmount || 0)
  const shipFee = Number(orderDetail.value.shippingFee || 0)
  const shipSubsidy = Number(orderDetail.value.shippingSubsidy || 0)

  return tempTotal - discount + shipFee - shipSubsidy
})

const isStatusNoteRequired = computed(() => statusesRequiringNote.includes(selectedStatus.value))

const orderHistory = computed(() =>
  (orderDetail.value?.history || []).map((item) => ({
    status: item.status,
    title: item.title,
    note: item.note,
    createdByName: item.createdByName,
    time: item.createdAt,
  })),
)

watch(
  () => orderDetail.value?.orderStatus,
  () => {
    selectedStatus.value = nextStatuses.value[0]?.value || orderDetail.value?.orderStatus || ''
  },
  { immediate: true },
)

const formatCurrency = (value) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value || 0)

const getOrderStatusLabel = (status) => orderStatusMap[status]?.label || status || '---'
const getOrderStatusClass = (status) => orderStatusMap[status]?.className || 'pending'
const getPaymentStatusLabel = (status) => paymentStatusMap[status]?.label || status || '---'
const getPaymentStatusClass = (status) => paymentStatusMap[status]?.className || 'pending'
const getPaymentMethodLabel = (method) => paymentMethodMap[method] || method || '---'

const formatDateTime = (value) => {
  if (!value) return '---'

  return new Date(String(value).replace(' ', 'T')).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const showToast = (message) => {
  toastMessage.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

const saveStatusUpdate = async () => {
  try {
    updatingStatus.value = true
    if (!orderDetail.value || !selectedStatus.value) return

    const isValid = nextStatuses.value.some((item) => item.value === selectedStatus.value)
    if (!isValid) {
      showToast('Trạng thái mới không hợp lệ theo luồng xử lý đơn hàng.')
      return
    }

    if (isStatusNoteRequired.value && !statusNote.value.trim()) {
      showToast('Vui lòng nhập lý do cho trạng thái này.')
      return
    }

    const orderCode = orderDetail.value.orderCode
    const response = await OrderDetailApi.updateStatusOrder(orderCode, {
      status: selectedStatus.value,
      note: statusNote.value.trim() || null,
    })

    orderDetail.value = response.data
    await fetchGetDetailOrder(orderCode)
    statusNote.value = ''
    showToast(
      `Đã cập nhật trạng thái đơn sang ${getOrderStatusLabel(orderDetail.value.orderStatus)}.`,
    )
  } catch (e) {
    const message =
      e.response?.data?.message ||
      e.response?.data ||
      'Cập nhật trạng thái đơn hàng không thành công'
    error.value = message
    showToast(message)
    console.error(e)
  } finally {
    updatingStatus.value = false
  }
}

const resetPrintScale = () => {
  const page = document.querySelector('.order-detail-page')
  if (page) {
    page.style.transform = ''
    page.style.transformOrigin = ''
    page.style.width = ''
    page.style.maxWidth = ''
  }
  const tableWrap = document.querySelector(
    '.order-detail-page .table-wrap, .order-detail-page table',
  )
  if (tableWrap) {
    tableWrap.style.transform = ''
    tableWrap.style.transformOrigin = ''
    tableWrap.style.width = ''
  }
}

const getPrintableWidthPx = () => {
  const A4_MM = 210
  const MARGIN_MM = 12
  const printableMm = A4_MM - 2 * MARGIN_MM
  return printableMm * (96 / 25.4)
}

const applyPrintScale = () => {
  const page = document.querySelector('.order-detail-page')
  if (!page) return

  const contentWidth = page.scrollWidth
  if (!contentWidth) return

  const printableWidth = getPrintableWidthPx()

  if (contentWidth > printableWidth) {
    const scale = printableWidth / contentWidth
    page.style.transformOrigin = 'top left'
    page.style.transform = `scale(${scale})`
    page.style.width = `${contentWidth * scale}px`
    page.style.maxWidth = `${contentWidth * scale}px`
  }
}

const printPage = async () => {
  await nextTick()
  const chatElements = [
    document.getElementById('marcus-floating-actions'),
    document.getElementById('marcus-floating-actions-style'),
    ...document.querySelectorAll('.chat-trigger-btn'),
    document.querySelector('.admin-chat-widget'),
    ...document.querySelectorAll('#vue-devtools-container, [id^="vue-devtools"]'),
  ].filter(Boolean)

  const restoreData = chatElements.map((el) => ({
    el,
    parent: el.parentNode,
    nextSibling: el.nextSibling,
  }))
  restoreData.forEach(({ el }) => el.remove())

  requestAnimationFrame(() => {
    applyPrintScale()
    requestAnimationFrame(() => {
      window.print()
      setTimeout(() => {
        restoreData.forEach(({ el, parent, nextSibling }) => {
          if (parent) {
            if (nextSibling) {
              parent.insertBefore(el, nextSibling)
            } else {
              parent.appendChild(el)
            }
          }
        })
      }, 500)
    })
  })
}

const onBeforePrint = () => {
  applyPrintScale()
}
const onAfterPrint = () => {
  resetPrintScale()
}

onMounted(() => {
  window.addEventListener('beforeprint', onBeforePrint)
  window.addEventListener('afterprint', onAfterPrint)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeprint', onBeforePrint)
  window.removeEventListener('afterprint', onAfterPrint)
  resetPrintScale()
})
</script>

<style scoped>
.price-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.price-cell .original-price {
  text-decoration: line-through;
  color: #9ca3af;
  font-size: 12px;
}

.price-cell .flash-price {
  color: #ef4444;
  font-weight: 700;
}

.price-cell .flash-badge {
  font-size: 10px;
  background: #fee2e2;
  color: #ef4444;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}
</style>
