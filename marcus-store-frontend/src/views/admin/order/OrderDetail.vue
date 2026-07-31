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
                  <span class="info-label">
                    {{
                      orderDetail.fulfillmentMethod === 'STORE_PICKUP'
                        ? 'Nhận tại cửa hàng'
                        : 'Địa chỉ giao hàng'
                    }}
                  </span>
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
                        <span class="money">{{ formatCurrency(item.priceAtPurchase) }}</span>
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
                <!-- Marcus thêm danh sách lý do hủy chuẩn cho Admin; dữ liệu này
                     được lưu vào lịch sử để thống kê nguyên nhân hủy. -->
                <template v-if="selectedStatus === 'CANCELLED'">
                  <label class="status-note-label">Lý do hủy đơn</label>
                  <select v-model="selectedAdminCancelReason" class="status-note-input">
                    <option value="" disabled>Chọn lý do hủy</option>
                    <option v-for="reason in ADMIN_CANCEL_REASONS" :key="reason" :value="reason">
                      {{ reason }}
                    </option>
                  </select>
                </template>
                <label v-else class="status-note-label">Ghi chú trạng thái</label>
                <input
                  v-if="
                    selectedStatus !== 'CANCELLED' ||
                    selectedAdminCancelReason === OTHER_CANCEL_REASON
                  "
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

          <!-- Marcus lam them refund -->
          <section v-if="canManageRefund || refund" class="card section-card no-print">
            <div class="section-header">
              <div>
                <h4>Hoàn tiền VNPAY</h4>
                <!-- Marcus sửa mô tả trung lập vì số tiền hoàn phụ thuộc bên hủy đơn. -->
                <p>Theo dõi từ lúc tạo yêu cầu đến khi VNPAY xác nhận hoàn tất.</p>
              </div>
            </div>
            <div class="section-body">
              <div v-if="refund" class="mini-list">
                <div class="mini-row">
                  <span class="mini-label">Trạng thái</span>
                  <strong class="mini-value">{{ getRefundStatusLabel(refund.status) }}</strong>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Tiền hoàn</span>
                  <strong class="mini-value money">{{ formatCurrency(refund.amount) }}</strong>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Phí vận chuyển không hoàn</span>
                  <span class="mini-value">{{ formatCurrency(refund.shippingDeducted) }}</span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Lý do</span>
                  <span class="mini-value">{{ refund.reason }}</span>
                </div>
                <div class="mini-row">
                  <span class="mini-label">Diễn giải</span>
                  <!-- Marcus sửa: không đưa lỗi checksum/response kỹ thuật ra màn quản trị. -->
                  <span class="mini-value">{{ getRefundStatusDescription(refund.status) }}</span>
                </div>
              </div>

              <div v-else class="form-group">
                <label class="form-label" for="refundReason">Lý do hoàn tiền</label>
                <input
                  id="refundReason"
                  v-model="refundReason"
                  class="control"
                  maxlength="500"
                  placeholder="Nhập lý do để tạo yêu cầu..."
                />
              </div>

              <button
                v-if="isAdmin && !refund"
                class="primary-btn"
                type="button"
                :disabled="refundBusy || !refundReason.trim()"
                @click="createRefund"
              >
                {{ refundBusy ? 'Đang tạo...' : 'Tạo yêu cầu hoàn tiền' }}
              </button>
              <button
                v-else-if="isAdmin && refund.status === 'PENDING_APPROVAL'"
                class="primary-btn"
                type="button"
                :disabled="refundBusy"
                @click="approveRefund"
              >
                {{ refundBusy ? 'Đang gửi VNPAY...' : 'Duyệt & gửi hoàn tiền' }}
              </button>
              <button
                v-else-if="isAdmin && refund.status === 'RETRY_PENDING'"
                class="primary-btn"
                type="button"
                :disabled="refundBusy"
                @click="retryRefund"
              >
                {{ refundBusy ? 'Đang thử lại...' : 'Thử lại hoàn tiền' }}
              </button>
              <button
                v-else-if="isAdmin && refund.status === 'PROCESSING'"
                class="primary-btn"
                type="button"
                :disabled="refundBusy"
                @click="reconcileRefund"
              >
                {{ refundBusy ? 'Đang kiểm tra...' : 'Kiểm tra trạng thái với VNPAY' }}
              </button>
              <!-- Marcus xóa xác nhận thành công giả trên Sandbox; môi trường dev
                   dừng đúng tại trạng thái chờ VNPAY xác nhận. -->
            </div>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="card empty-card">
      Không tìm thấy đơn hàng. Vui lòng quay lại danh sách đơn hàng.
    </div>

    <div class="order-detail-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>

    <BaseModal
      :visible="statusSuccessModal"
      type="success"
      title="Cập nhật thành công"
      :message="statusSuccessMessage"
      @close="statusSuccessModal = false"
    />

    <!-- Modal nhập IMEI khi bắt đầu chuẩn bị hàng -->
    <div class="modal fade" id="imeiPrepareModal" tabindex="-1" ref="imeiPrepareModalRef">
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              <i class="bi bi-upc-scan me-2"></i>Nhập IMEI cho đơn hàng
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div v-if="loadingImeiPreview" class="text-center py-4">
              <div class="spinner-border text-primary" role="status"></div>
              <p class="mt-2 text-muted small">Đang tải IMEI khả dụng...</p>
            </div>

            <template v-else>
              <div class="alert alert-info small mb-3">
                <i class="bi bi-info-circle me-1"></i>
                Vui lòng nhập đủ IMEI cho các sản phẩm có quản lý IMEI bên dưới.
                Mỗi sản phẩm cần đúng <strong>số lượng đã đặt</strong> mã IMEI.
              </div>

              <div v-if="!imeiPreviewItems.length" class="text-center text-muted py-3">
                Đơn hàng không có sản phẩm nào yêu cầu IMEI.
              </div>

              <div
                v-for="item in imeiPreviewItems"
                :key="item.orderItemId"
                class="imei-item-card mb-3"
              >
                <div class="imei-item-header">
                  <div>
                    <strong>{{ item.productName }}</strong>
                    <span class="sku-badge ms-2">{{ item.skuCode }}</span>
                  </div>
                  <div class="text-end">
                    <span class="badge" :class="item.quantityAssigned >= item.quantityOrdered ? 'bg-success' : 'bg-warning'">
                      {{ item.quantityAssigned }} / {{ item.quantityOrdered }} IMEI
                    </span>
                  </div>
                </div>

                <!-- Đã gán đủ -->
                <div v-if="item.quantityAssigned >= item.quantityOrdered" class="imei-assigned-list mt-2">
                  <div v-for="assigned in item.assignedImeis" :key="assigned" class="imei-tag assigned">
                    <i class="bi bi-check-circle me-1"></i>{{ assigned }}
                  </div>
                </div>

                <!-- Chưa gán đủ -->
                <template v-else>
                  <div class="mt-2">
                    <!-- Nút chọn từ IMEI có sẵn -->
                    <div v-if="item.availableImeis.length > 0">
                      <label class="form-label small mb-1 fw-semibold">
                        Chọn IMEI khả dụng ({{ item.quantityOrdered - item.quantityAssigned }} mã còn thiếu):
                      </label>
                      <div class="d-flex flex-wrap gap-2 mb-3">
                        <button
                          v-for="avail in item.availableImeis"
                          :key="avail.itemId"
                          type="button"
                          class="btn btn-sm imei-select-btn"
                          :class="isImeiSelected(item.orderItemId, avail.imeiCode) ? 'btn-success selected' : 'btn-outline-success'"
                          @click="toggleImei(item.orderItemId, avail)"
                        >
                          <i :class="isImeiSelected(item.orderItemId, avail.imeiCode) ? 'bi bi-check-circle-fill' : 'bi bi-circle'"></i>
                          {{ avail.imeiCode }}
                        </button>
                      </div>
                    </div>

                    <!-- Nhập tay IMEI -->
                    <div class="imei-manual-input">
                      <label class="form-label small mb-1 fw-semibold">
                        Nhập IMEI đơn hàng:
                      </label>
                      <div class="input-group input-group-sm mb-2">
                        <input
                          type="text"
                          class="form-control"
                          :placeholder="`Nhập IMEI rồi nhấn Enter (còn thiếu ${item.quantityOrdered - item.quantityAssigned} mã)`"
                          @keydown.enter.prevent="addManualImei(item.orderItemId, $event)"
                        />
                      </div>
                    </div>

                    <!-- IMEI đã nhập tay -->
                    <div v-if="getManualImeis(item.orderItemId).length > 0" class="d-flex flex-wrap gap-2 mb-2">
                      <span
                        v-for="imei in getManualImeis(item.orderItemId)"
                        :key="imei"
                        class="imei-tag manual"
                      >
                        {{ imei }}
                        <button type="button" class="imei-remove-btn" @click="removeManualImei(item.orderItemId, imei)">
                          <i class="bi bi-x"></i>
                        </button>
                      </span>
                    </div>

                    <div v-if="item.availableImeis.length === 0" class="text-danger small mt-1">
                      <i class="bi bi-exclamation-triangle me-1"></i>
                      Không có IMEI khả dụng trong kho.
                    </div>
                  </div>
                </template>
              </div>
            </template>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
            <button
              class="btn btn-primary"
              :disabled="submittingImei || !canSubmitImei"
              @click="submitImeiAndProcess"
            >
              <span v-if="submittingImei" class="spinner-border spinner-border-sm me-1"></span>
              Xác nhận IMEI &amp; Bắt đầu chuẩn bị
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Modal } from 'bootstrap'
import '@/assets/css/OrderDetails.css'
import OrderDetailApi from '@/api/orderDetailApi.js'
import BaseModal from '@/components/BaseModal.vue'

const route = useRoute()
const toastMessage = ref('')
const statusSuccessModal = ref(false)
const statusSuccessMessage = ref('')
const orderDetail = ref(null)
const loading = ref(false)
const error = ref(null)

const selectedStatus = ref('')
const statusNote = ref('')
const OTHER_CANCEL_REASON = 'Lý do khác'
const ADMIN_CANCEL_REASONS = [
  'Khách hàng yêu cầu hủy',
  'Không liên hệ được với khách hàng',
  'Sản phẩm hết hàng hoặc lỗi tồn kho',
  'Thông tin nhận hàng không hợp lệ',
  'Phát hiện đơn hàng bất thường',
  OTHER_CANCEL_REASON,
]
const selectedAdminCancelReason = ref('')
const updatingStatus = ref(false)

const imeiPrepareModalRef = ref(null)
let imeiPrepareModalInstance = null
const loadingImeiPreview = ref(false)
const submittingImei = ref(false)
const imeiPreviewItems = ref([])
const selectedImeis = ref({}) // { [orderItemId]: Set of imeiCodes from available list }
const manualImeis = ref({})    // { [orderItemId]: Array of manually typed IMEI strings }

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
  READY_FOR_PICKUP: { label: 'Sẵn sàng nhận tại cửa hàng', className: 'confirmed' },
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
  READY_FOR_PICKUP: [
    { value: 'COMPLETED', label: 'Xác nhận khách đã nhận hàng' },
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

  DELIVERED: [{ value: 'COMPLETED', label: 'Đơn hoàn thành' }],
  COMPLETED: [],
  CANCELLED: [],
  FAILED: [
    { value: 'SHIPPING', label: 'Giao lại' },
    { value: 'CANCELLED', label: 'Hủy đơn' },
  ],
}

const statusesRequiringNote = ['CANCELLED', 'FAILED']

const nextStatuses = computed(() => {
  if (!orderDetail.value) return []
  // Marcus thêm: đơn tại quầy bỏ qua đóng gói/vận chuyển GHN.
  if (
    orderDetail.value.fulfillmentMethod === 'STORE_PICKUP' &&
    orderDetail.value.orderStatus === 'PROCESSING'
  ) {
    return [
      { value: 'READY_FOR_PICKUP', label: 'Sẵn sàng nhận tại cửa hàng' },
      { value: 'CANCELLED', label: 'Hủy đơn' },
    ]
  }
  return allowedTransitions[orderDetail.value.orderStatus] || []
})
// Marcus lam them refund
const isAdmin = computed(() => {
  try {
    const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
    return Array.isArray(roles) && roles.includes('ROLE_ADMIN')
  } catch {
    return false
  }
})

const canManageRefund = computed(() => {
  const order = orderDetail.value
  return (
    isAdmin.value &&
    order &&
    String(order.paymentMethod).toUpperCase() === 'VNPAY' &&
    ['CANCELLED', 'FAILED'].includes(order.orderStatus) &&
    ['PAID', 'REFUND_PENDING', 'REFUND_FAILED'].includes(order.paymentStatus)
  )
})

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

const canSubmitImei = computed(() => {
  if (!imeiPreviewItems.value.length) return false
  return imeiPreviewItems.value.every(item => {
    const needed = item.quantityOrdered - item.quantityAssigned
    const selected = (selectedImeis.value[item.orderItemId]?.size || 0) + (manualImeis.value[item.orderItemId]?.length || 0)
    return selected >= needed
  })
})

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

watch(selectedStatus, (status) => {
  if (status !== 'CANCELLED') selectedAdminCancelReason.value = ''
  statusNote.value = ''
})

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

function isImeiSelected(orderItemId, imeiCode) {
  return selectedImeis.value[orderItemId]?.has(imeiCode) || false
}

function getManualImeis(orderItemId) {
  return manualImeis.value[orderItemId] || []
}

function addManualImei(orderItemId, event) {
  const input = event.target
  const imei = input.value.trim()
  if (!imei) return

  const item = imeiPreviewItems.value.find(i => i.orderItemId === orderItemId)
  if (!item) return
  const needed = item.quantityOrdered - item.quantityAssigned
  const totalSelected = (selectedImeis.value[orderItemId]?.size || 0) + (manualImeis.value[orderItemId]?.length || 0)
  if (totalSelected >= needed) {
    showToast(`Đã đủ ${needed} IMEI cho dòng này`)
    return
  }

  if (!manualImeis.value[orderItemId]) {
    manualImeis.value[orderItemId] = []
  }
  if (!manualImeis.value[orderItemId].includes(imei)) {
    manualImeis.value[orderItemId].push(imei)
  }
  input.value = ''
}

function removeManualImei(orderItemId, imei) {
  const list = manualImeis.value[orderItemId]
  if (list) {
    const idx = list.indexOf(imei)
    if (idx > -1) list.splice(idx, 1)
  }
}

function toggleImei(orderItemId, availItem) {
  if (!selectedImeis.value[orderItemId]) {
    selectedImeis.value[orderItemId] = new Set()
  }
  const set = selectedImeis.value[orderItemId]
  if (set.has(availItem.imeiCode)) {
    set.delete(availItem.imeiCode)
  } else {
    // Chỉ cho chọn đúng số lượng còn thiếu
    const item = imeiPreviewItems.value.find(i => i.orderItemId === orderItemId)
    if (!item) return
    const needed = item.quantityOrdered - item.quantityAssigned
    if (set.size >= needed) {
      showToast(`Chỉ cần chọn đủ ${needed} IMEI cho dòng này`)
      return
    }
    set.add(availItem.imeiCode)
  }
}

async function openImeiPrepareModal() {
  loadingImeiPreview.value = true
  selectedImeis.value = {}
  manualImeis.value = {}
  try {
    const res = await OrderDetailApi.getImeiPreview(orderDetail.value.orderCode)
    imeiPreviewItems.value = res.data || []
  } catch (e) {
    showToast('Không tải được danh sách IMEI')
  } finally {
    loadingImeiPreview.value = false
  }
  imeiPrepareModalInstance?.show()
}

async function submitImeiAndProcess() {
  submittingImei.value = true
  try {
    const requests = imeiPreviewItems.value
      .filter(item => {
        const selected = selectedImeis.value[item.orderItemId]
        const manual = manualImeis.value[item.orderItemId]
        return (selected && selected.size > 0) || (manual && manual.length > 0)
      })
      .map(item => ({
        orderItemId: item.orderItemId,
        imeiCodes: [
          ...Array.from(selectedImeis.value[item.orderItemId] || []),
          ...(manualImeis.value[item.orderItemId] || [])
        ]
      }))

    if (requests.length > 0) {
      await OrderDetailApi.assignOrderImeis(orderDetail.value.orderCode, requests)
    }

    imeiPrepareModalInstance?.hide()
    await fetchGetDetailOrder(orderDetail.value.orderCode)
    showToast('Đã xác nhận IMEI. Tiếp tục cập nhật trạng thái...')
  } catch (e) {
    showToast(e?.response?.data?.message || 'Gán IMEI thất bại')
    submittingImei.value = false
    return
  }
  submittingImei.value = false
  await saveStatusUpdate()
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

    const resolvedNote =
      selectedStatus.value === 'CANCELLED' &&
      selectedAdminCancelReason.value !== OTHER_CANCEL_REASON
        ? selectedAdminCancelReason.value
        : statusNote.value.trim()

    if (isStatusNoteRequired.value && !resolvedNote) {
      showToast('Vui lòng nhập lý do cho trạng thái này.')
      return
    }

    // Nếu chuyển sang PROCESSING → mở modal nhập IMEI trước
    if (selectedStatus.value === 'PROCESSING') {
      await openImeiPrepareModal()
      updatingStatus.value = false
      return
    }

    const orderCode = orderDetail.value.orderCode
    const response = await OrderDetailApi.updateStatusOrder(orderCode, {
      status: selectedStatus.value,
      note: resolvedNote || null,
    })

    orderDetail.value = response.data
    await fetchGetDetailOrder(orderCode)
    statusNote.value = ''
    selectedAdminCancelReason.value = ''
    statusSuccessMessage.value = `Đơn ${orderDetail.value.orderCode} đã được chuyển sang trạng thái ${getOrderStatusLabel(orderDetail.value.orderStatus)}.`
    statusSuccessModal.value = true
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
  imeiPrepareModalInstance = new Modal(imeiPrepareModalRef.value)
  window.addEventListener('beforeprint', onBeforePrint)
  window.addEventListener('afterprint', onAfterPrint)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeprint', onBeforePrint)
  window.removeEventListener('afterprint', onAfterPrint)
  resetPrintScale()
})
</script>

<style scoped></style>