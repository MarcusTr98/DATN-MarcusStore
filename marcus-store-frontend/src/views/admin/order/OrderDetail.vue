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
        <p v-if="orderDetail">Đơn {{ orderDetail.orderCode }} - {{ orderStatusMap[orderDetail.orderStatus].label }}</p>
        <p v-else>Không tìm thấy dữ liệu cho đơn hàng này.</p>
      </div>

      <div class="page-actions">
        <RouterLink class="outline-btn" to="/admin/order">Quay lại</RouterLink>
        <button class="outline-btn" type="button" @click="printPage">In đơn hàng</button>
      </div>
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
            <span class="badge" :class="orderStatusMap[orderDetail.orderStatus].className">
              {{ orderStatusMap[orderDetail.orderStatus].label }}
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
                <p>{{ orderDetail.items?.length || 0 }} dòng sản phẩm, tổng {{ totalQuantity }} sản phẩm.</p>
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
                          <span class="product-thumb">📦</span>
                          <span>
                            <span class="main-line">{{ item.productName }}</span>

                          </span>
                        </div>
                      </td>
                      <td>{{ item.skuCode }}</td>
                      <td>{{ item.quantity }}</td>
                      <td><span class="money">{{ formatCurrency(item.priceAtPurchase) }}</span></td>
                      <td><span class="money">{{ formatCurrency(item.lineTotal) }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="table-summary">
                <div class="summary-row"><span>Tạm tính</span><strong>{{ formatCurrency(subTotal) }}</strong></div>
                <div class="summary-row"><span>Giảm giá</span><strong>- {{ formatCurrency(orderDetail.discountAmount) }}</strong></div>
                <div class="summary-row"><span>Phí vận chuyển</span><strong>{{ formatCurrency(orderDetail.shippingFee) }}</strong></div>
                <div class="summary-row total"><span>Tổng thanh toán</span><strong>{{ formatCurrency(finalAmount) }}</strong></div>
              </div>
            </div>
          </section>

          <section class="card section-card">
            <div class="section-header">
              <div>
                <h4>Mốc xử lý & lịch sử thao tác</h4>
                <p>Các trạng thái đã được ghi nhận theo thời gian.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="timeline">
                <div v-for="item in orderHistory" :key="`${item.status}-${item.time}`" class="timeline-item">
                  <span class="timeline-dot">✓</span>
                  <div class="timeline-content">
                    <p class="timeline-title">
                      {{ item.title }}
                      <span class="badge" :class="orderStatusMap[item.status].className">
                        {{ orderStatusMap[item.status].label }}
                      </span>
                    </p>
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
                  <span class="badge" :class="orderStatusMap[orderDetail.orderStatus].className">
                    {{ orderStatusMap[orderDetail.orderStatus].label }}
                  </span>
                </strong>
              </div>

              <div class="form-group">
                <label class="form-label" for="statusDropdown">Cập nhật trạng thái</label>
                <select id="statusDropdown" v-model="selectedStatus" class="control" :disabled="!nextStatuses.length">
                  <option v-if="!nextStatuses.length" :value="orderDetail.orderStatus">
                    {{ orderStatusMap[orderDetail.orderStatus].label }}
                  </option>
                  <option v-for="item in nextStatuses" :key="item.value" :value="item.value">
                    {{ item.label }} ({{ item.value }})
                  </option>
                </select>
                <p class="helper-text">{{ statusHints[orderDetail.orderStatus] }}</p>
              </div>

              <button class="primary-btn" type="button" :disabled="!nextStatuses.length" @click="saveStatusUpdate">
                Lưu cập nhật
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
                <div class="mini-row"><span class="mini-label">Mã hóa đơn</span><span class="mini-value">{{ orderDetail.orderCode }}</span></div>
                <div class="mini-row"><span class="mini-label">Hình thức vận chuyển</span><span class="mini-value">{{ orderDetail.trackingCode || '---' }}</span></div>
                <div class="mini-row"><span class="mini-label">Phí ship</span><span class="mini-value">{{ formatCurrency(orderDetail.shippingFee) }}</span></div>
                <div class="mini-row"><span class="mini-label">Ghi chú</span><span class="mini-value">{{ '---' }}</span></div>
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
                    <span class="badge" :class="paymentStatusMap[orderDetail.paymentStatus].className">
                      {{ paymentStatusMap[orderDetail.paymentStatus].label }}
                    </span>
                  </span>
                </div>
                <div class="mini-row"><span class="mini-label">Phương thức</span><span class="mini-value">{{ paymentMethodMap[orderDetail.paymentMethod] }}</span></div>
                <div class="mini-row"><span class="mini-label">Mã giao dịch</span><span class="mini-value">{{ orderDetail.transactionId || '---' }}</span></div>
                <div class="mini-row"><span class="mini-label">Thời gian thanh toán</span><span class="mini-value">{{ orderDetail.paidAt ? formatDateTime(orderDetail.paidAt) : '---' }}</span></div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="card empty-card">
      Không tìm thấy đơn hàng. Vui lòng quay lại danh sách đơn hàng.
    </div>

    <div class="toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </section>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import '@/assets/css/OrderDetails.css'
import OrderDetailApi from '@/api/orderDetailApi.js'
const route = useRoute()
const toastMessage = ref('')

const orderDetail = ref(null)
const loading = ref(false)
const error = ref(null)
async function fetchGetDetailOrder(orderCode){
  try {
    loading.value = true
    error.value = null
    const response = await  OrderDetailApi.getOrderDetail(orderCode)
    orderDetail.value = response.data
  }catch (e){
    error.value = "Không tải được chi tiết đơn hàng"
    console.error(e)
  }finally {
    loading.value = false
  }
}
onMounted(()=>{
  fetchGetDetailOrder(route.params.id)
})
const orderStatusMap = {
  PENDING: { label: 'Chờ xác nhận', className: 'pending' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed' },
  SHIPPING: { label: 'Đang giao', className: 'shipping' },
  COMPLETED: { label: 'Hoàn thành', className: 'completed' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled' },
  FAILED: { label: 'Giao thất bại', className: 'failed' },
}

const paymentStatusMap = {
  PAID: { label: 'Đã thanh toán', className: 'confirmed' },
  UNPAID: { label: 'Chưa thanh toán', className: 'pending' },
  REFUNDED: { label: 'Đã hoàn tiền', className: 'cancelled' },
}

const paymentMethodMap = {
  VNPay: 'VNPay',
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
    { value: 'SHIPPING', label: 'Đang giao hàng' },
    { value: 'CANCELLED', label: 'Hủy đơn' },
  ],
  SHIPPING: [
    { value: 'COMPLETED', label: 'Giao thành công' },
    { value: 'FAILED', label: 'Giao thất bại' },
  ],
  COMPLETED: [],
  CANCELLED: [],
  FAILED: [],
}

const statusHints = {
  PENDING: 'Đơn mới tạo. Nhân viên có thể xác nhận đơn hoặc hủy đơn nếu chưa xử lý.',
  CONFIRMED: 'Đơn đã xác nhận. Có thể chuyển sang đang giao hoặc hủy nếu chưa gửi hàng.',
  SHIPPING: 'Đơn đang giao. Chỉ được ghi nhận giao thành công hoặc giao thất bại.',
  COMPLETED: 'Đơn đã hoàn thành. Dropdown bị khóa để tránh sửa ngược quy trình.',
  CANCELLED: 'Đơn đã hủy. Dropdown bị khóa vì đây là trạng thái cuối.',
  FAILED: 'Đơn giao thất bại. Dropdown bị khóa vì đây là trạng thái cuối.',
}

const timelineTitleByStatus = {
  CONFIRMED: 'Nhân viên xác nhận đơn',
  SHIPPING: 'Đơn chuyển sang đang giao hàng',
  COMPLETED: 'Giao hàng thành công',
  CANCELLED: 'Đơn hàng đã bị hủy',
  FAILED: 'Giao hàng thất bại',
}

const selectedStatus = ref('')
const orderHistory = computed(() => orderDetail.value ? [{
  status: orderDetail.value.orderStatus,
  title: 'Trạng thái hiện tại',
  time: orderDetail.value.updatedAt || orderDetail.value.createdAt,
}] : [])

const nextStatuses = computed(() => (orderDetail.value ? allowedTransitions[orderDetail.value.orderStatus] || [] : []))
const subTotal = computed(() => orderDetail.value?.items?.reduce((sum, item) => sum + Number(item.lineTotal || 0), 0) || 0)
const totalQuantity = computed(() => orderDetail.value?.items?.reduce((sum, item) => sum + Number(item.quantity || 0), 0) || 0)
const finalAmount = computed(() =>
  orderDetail.value?.finalAmount || 0,
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

const saveStatusUpdate = () => {
  if (!orderDetail.value || !selectedStatus.value) return

  const isValid = nextStatuses.value.some((item) => item.value === selectedStatus.value)
  if (!isValid) {
    showToast('Trạng thái mới không hợp lệ theo luồng xử lý đơn hàng.')
    return
  }

  const nextStatus = selectedStatus.value
  orderDetail.value.orderStatus = nextStatus
  showToast(`Đã cập nhật trạng thái đơn sang ${orderStatusMap[orderDetail.value.orderStatus].label}.`)
}

const printPage = () => {
  window.print()
}
</script>

<style scoped>

</style>



