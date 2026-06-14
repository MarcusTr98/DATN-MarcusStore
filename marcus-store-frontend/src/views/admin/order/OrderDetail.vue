<template>
  <section class="order-detail-page">
    <div class="page-heading">
      <div>
        <div class="breadcrumb">
          <RouterLink to="/admin/order">Quản lý đơn hàng</RouterLink>
          <span>/</span>
          <span>{{ order?.orderCode || 'Không tìm thấy' }}</span>
        </div>
        <h3>Chi tiết đơn hàng</h3>
        <p v-if="order">Đơn {{ order.orderCode }} - {{ orderStatusMap[order.currentStatus].label }}</p>
        <p v-else>Không tìm thấy dữ liệu mẫu cho đơn hàng này.</p>
      </div>

      <div class="page-actions">
        <RouterLink class="outline-btn" to="/admin/order">Quay lại</RouterLink>
        <button class="outline-btn" type="button" @click="printPage">In đơn hàng</button>
      </div>
    </div>

    <template v-if="order">
      <div class="summary-card card">
        <div class="summary-item">
          <span class="summary-label">Mã đơn</span>
          <strong class="summary-value">{{ order.orderCode }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">Trạng thái</span>
          <span class="summary-value">
            <span class="badge" :class="orderStatusMap[order.currentStatus].className">
              {{ orderStatusMap[order.currentStatus].label }}
            </span>
          </span>
        </div>
        <div class="summary-item">
          <span class="summary-label">Tổng thanh toán</span>
          <strong class="summary-value money">{{ formatCurrency(finalAmount) }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">Ngày tạo</span>
          <strong class="summary-value">{{ formatDateTime(order.createdAt) }}</strong>
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
                  <strong class="info-value">{{ order.customerName }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Mã khách hàng</span>
                  <strong class="info-value">{{ order.customerCode }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Số điện thoại</span>
                  <strong class="info-value">{{ order.customerPhone }}</strong>
                </div>
                <div class="info-box">
                  <span class="info-label">Email</span>
                  <strong class="info-value">{{ order.customerEmail }}</strong>
                </div>
                <div class="info-box full">
                  <span class="info-label">Địa chỉ giao hàng</span>
                  <strong class="info-value">{{ order.shippingAddress }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="card section-card">
            <div class="section-header">
              <div>
                <h4>Sản phẩm trong đơn</h4>
                <p>{{ order.products.length }} dòng sản phẩm, tổng {{ totalQuantity }} sản phẩm.</p>
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
                    <tr v-for="product in order.products" :key="product.sku">
                      <td>
                        <div class="product-cell">
                          <span class="product-thumb">📦</span>
                          <span>
                            <span class="main-line">{{ product.name }}</span>
                            <span class="sub-line">ID sản phẩm: {{ product.productId }}</span>
                          </span>
                        </div>
                      </td>
                      <td>{{ product.sku }}</td>
                      <td>{{ product.qty }}</td>
                      <td><span class="money">{{ formatCurrency(product.price) }}</span></td>
                      <td><span class="money">{{ formatCurrency(product.qty * product.price) }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="table-summary">
                <div class="summary-row"><span>Tạm tính</span><strong>{{ formatCurrency(subTotal) }}</strong></div>
                <div class="summary-row"><span>Giảm giá</span><strong>- {{ formatCurrency(order.discountAmount) }}</strong></div>
                <div class="summary-row"><span>Phí vận chuyển</span><strong>{{ formatCurrency(order.shippingFee) }}</strong></div>
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
                <div v-for="item in order.history" :key="`${item.status}-${item.time}`" class="timeline-item">
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
                <p>Cập nhật trạng thái trên dữ liệu mẫu.</p>
              </div>
            </div>
            <div class="section-body">
              <div class="current-status-box">
                <span>Trạng thái hiện tại</span>
                <strong>
                  <span class="badge" :class="orderStatusMap[order.currentStatus].className">
                    {{ orderStatusMap[order.currentStatus].label }}
                  </span>
                </strong>
              </div>

              <div class="form-group">
                <label class="form-label" for="statusDropdown">Cập nhật trạng thái</label>
                <select id="statusDropdown" v-model="selectedStatus" class="control" :disabled="!nextStatuses.length">
                  <option v-if="!nextStatuses.length" :value="order.currentStatus">
                    {{ orderStatusMap[order.currentStatus].label }}
                  </option>
                  <option v-for="item in nextStatuses" :key="item.value" :value="item.value">
                    {{ item.label }} ({{ item.value }})
                  </option>
                </select>
                <p class="helper-text">{{ statusHints[order.currentStatus] }}</p>
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
                <div class="mini-row"><span class="mini-label">Mã hóa đơn</span><span class="mini-value">{{ order.invoiceNo }}</span></div>
                <div class="mini-row"><span class="mini-label">Hình thức vận chuyển</span><span class="mini-value">{{ order.shippingMethod }}</span></div>
                <div class="mini-row"><span class="mini-label">Phí ship</span><span class="mini-value">{{ formatCurrency(order.shippingFee) }}</span></div>
                <div class="mini-row"><span class="mini-label">Ghi chú</span><span class="mini-value">{{ order.note || 'Không có ghi chú' }}</span></div>
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
                    <span class="badge" :class="paymentStatusMap[order.paymentStatus].className">
                      {{ paymentStatusMap[order.paymentStatus].label }}
                    </span>
                  </span>
                </div>
                <div class="mini-row"><span class="mini-label">Phương thức</span><span class="mini-value">{{ paymentMethodMap[order.paymentMethod] }}</span></div>
                <div class="mini-row"><span class="mini-label">Mã giao dịch</span><span class="mini-value">{{ order.transactionId || '---' }}</span></div>
                <div class="mini-row"><span class="mini-label">Thời gian thanh toán</span><span class="mini-value">{{ order.paidAt ? formatDateTime(order.paidAt) : '---' }}</span></div>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="card empty-card">
      Không tìm thấy đơn hàng mẫu. Vui lòng quay lại danh sách đơn hàng.
    </div>

    <div class="toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const toastMessage = ref('')

const sampleOrders = ref([
  {
    id: 1,
    orderCode: 'ORD-20260501-001',
    invoiceNo: 'INV-20260501-001',
    customerCode: 'KH-0001',
    customerName: 'Nguyễn Văn An',
    customerPhone: '0983333333',
    customerEmail: 'an.nguyen@gmail.com',
    shippingAddress: 'Số 12 Nguyễn Trãi, Quận Thanh Xuân, Hà Nội',
    shippingMethod: 'Giao hàng tận nơi',
    shippingFee: 30000,
    note: 'Giao giờ hành chính.',
    paymentStatus: 'PAID',
    paymentMethod: 'VNPay',
    transactionId: 'VNP-20260501-A11',
    paidAt: '2026-05-01 10:31:00',
    currentStatus: 'COMPLETED',
    createdAt: '2026-05-01 10:30:00',
    discountAmount: 500000,
    products: [{ productId: 5, sku: 'IP15P-NAT-128', name: 'iPhone 15 Pro - Titan tự nhiên 128GB', qty: 1, price: 24990000 }],
    history: [
      { status: 'PENDING', title: 'Khách hàng tạo đơn', time: '2026-05-01 10:30:00' },
      { status: 'CONFIRMED', title: 'Nhân viên xác nhận đơn', time: '2026-05-01 10:45:00' },
      { status: 'SHIPPING', title: 'Đơn chuyển sang đang giao hàng', time: '2026-05-01 14:10:00' },
      { status: 'COMPLETED', title: 'Giao hàng thành công', time: '2026-05-02 09:20:00' },
    ],
  },
  {
    id: 2,
    orderCode: 'ORD-20260515-002',
    invoiceNo: 'INV-20260515-002',
    customerCode: 'KH-0002',
    customerName: 'Trần Thị Bình',
    customerPhone: '0974444444',
    customerEmail: 'binh.tran@gmail.com',
    shippingAddress: '45 Lê Lợi, Quận 1, TP. Hồ Chí Minh',
    shippingMethod: 'Giao hàng nhanh',
    shippingFee: 40000,
    note: 'Khách thanh toán khi nhận hàng.',
    paymentStatus: 'UNPAID',
    paymentMethod: 'COD',
    transactionId: '',
    paidAt: '',
    currentStatus: 'SHIPPING',
    createdAt: '2026-05-15 14:00:00',
    discountAmount: 40000,
    products: [{ productId: 12, sku: 'S24U-BLK-256', name: 'Samsung Galaxy S24 Ultra - Đen 256GB', qty: 1, price: 27990000 }],
    history: [
      { status: 'PENDING', title: 'Khách hàng tạo đơn', time: '2026-05-15 14:00:00' },
      { status: 'CONFIRMED', title: 'Nhân viên xác nhận đơn', time: '2026-05-15 15:05:00' },
      { status: 'SHIPPING', title: 'Đơn chuyển sang đang giao hàng', time: '2026-05-16 08:30:00' },
    ],
  },
  {
    id: 3,
    orderCode: 'ORD-20260530-003',
    invoiceNo: 'INV-20260530-003',
    customerCode: 'KH-0005',
    customerName: 'Lê Minh Cường',
    customerPhone: '0965555555',
    customerEmail: 'khach3@gmail.com',
    shippingAddress: 'Số 78, Ngõ 56, Đường Nguyễn Lương Bằng, Phường Khâm Thiên, Quận Đống Đa, Hà Nội',
    shippingMethod: 'Giao hàng tận nơi',
    shippingFee: 30000,
    note: 'Khách yêu cầu gọi trước khi giao.',
    paymentStatus: 'PAID',
    paymentMethod: 'MoMo',
    transactionId: 'MOMO-20260530-8X21CN',
    paidAt: '2026-05-30 09:16:00',
    currentStatus: 'CONFIRMED',
    createdAt: '2026-05-30 09:15:00',
    discountAmount: 500000,
    products: [
      { productId: 3, sku: 'IP15-BLK-128', name: 'iPhone 15 - Đen 128GB', qty: 1, price: 19990000 },
      { productId: 7, sku: 'A55-BLU-128', name: 'Samsung Galaxy A55 5G - Xanh 128GB', qty: 1, price: 9990000 },
    ],
    history: [
      { status: 'PENDING', title: 'Khách hàng tạo đơn', time: '2026-05-30 09:15:00' },
      { status: 'CONFIRMED', title: 'Nhân viên xác nhận đơn', time: '2026-05-30 09:20:00' },
    ],
  },
  {
    id: 4,
    orderCode: 'ORD-20260605-004',
    invoiceNo: 'INV-20260605-004',
    customerCode: 'KH-0006',
    customerName: 'Phạm Thị Dung',
    customerPhone: '0956666666',
    customerEmail: 'dung.pham@gmail.com',
    shippingAddress: '20 Trần Phú, Thành phố Đà Nẵng',
    shippingMethod: 'Giao hàng tiêu chuẩn',
    shippingFee: 30000,
    note: '',
    paymentStatus: 'PAID',
    paymentMethod: 'BankTransfer',
    transactionId: 'BANK-20260605-004',
    paidAt: '2026-06-05 20:03:00',
    currentStatus: 'PENDING',
    createdAt: '2026-06-05 20:00:00',
    discountAmount: 30000,
    products: [{ productId: 19, sku: 'RN13P-BLK-256', name: 'Xiaomi Redmi Note 13 Pro+ - Đen 256GB', qty: 1, price: 8990000 }],
    history: [{ status: 'PENDING', title: 'Khách hàng tạo đơn', time: '2026-06-05 20:00:00' }],
  },
  {
    id: 5,
    orderCode: 'ORD-20260608-005',
    invoiceNo: 'INV-20260608-005',
    customerCode: 'KH-0007',
    customerName: 'Nguyễn Hoàng Nam',
    customerPhone: '0912888999',
    customerEmail: 'nam.nguyen@gmail.com',
    shippingAddress: '88 Hai Bà Trưng, Quận Hoàn Kiếm, Hà Nội',
    shippingMethod: 'Giao hàng tận nơi',
    shippingFee: 25000,
    note: 'Đơn đã hủy theo yêu cầu khách.',
    paymentStatus: 'UNPAID',
    paymentMethod: 'COD',
    transactionId: '',
    paidAt: '',
    currentStatus: 'CANCELLED',
    createdAt: '2026-06-08 11:20:00',
    discountAmount: 25000,
    products: [{ productId: 22, sku: 'APP2-WHT-STD', name: 'AirPods Pro thế hệ 2', qty: 1, price: 6490000 }],
    history: [
      { status: 'PENDING', title: 'Khách hàng tạo đơn', time: '2026-06-08 11:20:00' },
      { status: 'CANCELLED', title: 'Đơn hàng đã bị hủy', time: '2026-06-08 12:05:00' },
    ],
  },
  {
    id: 6,
    orderCode: 'ORD-20260610-006',
    invoiceNo: 'INV-20260610-006',
    customerCode: 'KH-0008',
    customerName: 'Vũ Minh Anh',
    customerPhone: '0933777666',
    customerEmail: 'anh.vu@gmail.com',
    shippingAddress: '19 Phan Chu Trinh, Thành phố Huế',
    shippingMethod: 'Giao hàng nhanh',
    shippingFee: 30000,
    note: 'Giao thất bại do không liên hệ được khách.',
    paymentStatus: 'PAID',
    paymentMethod: 'VNPay',
    transactionId: 'VNP-20260610-Z91',
    paidAt: '2026-06-10 16:41:00',
    currentStatus: 'FAILED',
    createdAt: '2026-06-10 16:40:00',
    discountAmount: 30000,
    products: [{ productId: 7, sku: 'IP15-BLK-128', name: 'iPhone 15 - Đen 128GB', qty: 1, price: 19990000 }],
    history: [
      { status: 'PENDING', title: 'Khách hàng tạo đơn', time: '2026-06-10 16:40:00' },
      { status: 'CONFIRMED', title: 'Nhân viên xác nhận đơn', time: '2026-06-10 17:00:00' },
      { status: 'SHIPPING', title: 'Đơn chuyển sang đang giao hàng', time: '2026-06-11 08:00:00' },
      { status: 'FAILED', title: 'Giao hàng thất bại', time: '2026-06-11 18:30:00' },
    ],
  },
])

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

const order = computed(() => sampleOrders.value.find((item) => item.id === Number(route.params.id)))
const selectedStatus = ref('')

const nextStatuses = computed(() => (order.value ? allowedTransitions[order.value.currentStatus] || [] : []))
const subTotal = computed(() => order.value?.products.reduce((sum, product) => sum + product.qty * product.price, 0) || 0)
const totalQuantity = computed(() => order.value?.products.reduce((sum, product) => sum + product.qty, 0) || 0)
const finalAmount = computed(() =>
  order.value ? subTotal.value - order.value.discountAmount + order.value.shippingFee : 0,
)

watch(
  () => order.value?.currentStatus,
  () => {
    selectedStatus.value = nextStatuses.value[0]?.value || order.value?.currentStatus || ''
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
  if (!order.value || !selectedStatus.value) return

  const isValid = nextStatuses.value.some((item) => item.value === selectedStatus.value)
  if (!isValid) {
    showToast('Trạng thái mới không hợp lệ theo luồng xử lý đơn hàng.')
    return
  }

  const nextStatus = selectedStatus.value
  order.value.currentStatus = nextStatus
  order.value.history.push({
    status: nextStatus,
    title:
      timelineTitleByStatus[nextStatus] ||
      `Cập nhật trạng thái sang ${orderStatusMap[nextStatus].label}`,
    time: new Date().toISOString().slice(0, 19).replace('T', ' '),
  })

  showToast(`Đã cập nhật trạng thái đơn sang ${orderStatusMap[order.value.currentStatus].label}.`)
}

const printPage = () => {
  window.print()
}
</script>

<style scoped>
.order-detail-page {
  display: grid;
  gap: 20px;
  padding: 16px;
  color: #5e4a54;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #6b5660;
  font-size: 14px;
  font-weight: 800;
}

.breadcrumb a {
  color: #ff4d94;
  text-decoration: none;
}

.page-heading h3 {
  margin: 0;
  color: #ff4d94;
  font-size: 34px;
  font-weight: 850;
}

.page-heading p {
  margin: 8px 0 0;
  color: #6b5660;
  font-size: 15px;
  font-weight: 650;
}

.page-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.card {
  background: #ffffff;
  border: 1px solid rgba(255, 220, 233, 0.85);
  border-radius: 20px;
  box-shadow: 0 10px 24px rgba(94, 74, 84, 0.07);
}

.summary-card {
  padding: 22px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr 0.8fr 0.8fr;
  gap: 14px;
}

.summary-item {
  padding: 18px;
  border: 1px solid #ffd4e4;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff, rgba(255, 240, 246, 0.72));
}

.summary-label {
  display: block;
  color: #6b5660;
  font-size: 13px;
  font-weight: 850;
  text-transform: uppercase;
}

.summary-value {
  display: block;
  margin-top: 8px;
  color: #5e4a54;
  font-size: 18px;
  font-weight: 950;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.8fr);
  gap: 20px;
  align-items: start;
}

.left-column,
.right-column {
  display: grid;
  gap: 20px;
}

.right-column {
  position: sticky;
  top: 20px;
}

.section-card {
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 20px 22px;
  border-bottom: 1px solid rgba(255, 212, 228, 0.72);
  background: #fffafb;
}

.section-header h4 {
  margin: 0;
  color: #5e4a54;
  font-size: 18px;
  font-weight: 950;
}

.section-header p {
  margin: 4px 0 0;
  color: #6b5660;
  font-size: 13px;
  font-weight: 700;
}

.section-body {
  padding: 22px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-box {
  min-height: 82px;
  padding: 16px;
  border: 1px solid rgba(255, 212, 228, 0.74);
  border-radius: 18px;
  background: #ffffff;
}

.info-box.full {
  grid-column: 1 / -1;
}

.info-label {
  display: block;
  color: #6b5660;
  font-size: 13px;
  font-weight: 850;
  margin-bottom: 8px;
}

.info-value {
  display: block;
  color: #5e4a54;
  font-size: 15px;
  font-weight: 850;
  line-height: 1.45;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: separate;
  border-spacing: 0;
}

thead th {
  padding: 16px 14px;
  background: #fffafb;
  border-bottom: 1px solid rgba(255, 212, 228, 0.72);
  color: #5e4a54;
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.08em;
  text-align: left;
  text-transform: uppercase;
  white-space: nowrap;
}

tbody td {
  padding: 17px 14px;
  border-bottom: 1px solid rgba(255, 212, 228, 0.58);
  vertical-align: middle;
  color: #5e4a54;
  font-size: 14px;
  font-weight: 700;
}

tbody tr:hover {
  background: #fff0f6;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 280px;
}

.product-thumb {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff0f6;
  border: 1px solid #ffd4e4;
}

.main-line {
  display: block;
  color: #5e4a54;
  font-weight: 950;
}

.sub-line {
  display: block;
  margin-top: 5px;
  color: #6b5660;
  font-size: 12px;
  font-weight: 750;
}

.money {
  color: #4b3943;
  font-weight: 950;
  white-space: nowrap;
}

.table-summary {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px dashed rgba(255, 212, 228, 0.9);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #6b5660;
  font-size: 15px;
  font-weight: 800;
}

.summary-row.total {
  color: #ff4d94;
  font-size: 20px;
  font-weight: 950;
}

.timeline {
  display: grid;
  gap: 16px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  align-items: start;
}

.timeline-dot {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff8fb7, #ff5d99);
  color: #ffffff;
  box-shadow: 0 8px 16px rgba(255, 77, 148, 0.18);
}

.timeline-title {
  margin: 0;
  color: #5e4a54;
  font-size: 15px;
  font-weight: 950;
}

.timeline-time {
  margin: 4px 0 0;
  color: #6b5660;
  font-size: 13px;
  font-weight: 750;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 7px 13px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.15;
  font-weight: 950;
  text-transform: uppercase;
  white-space: nowrap;
}

.badge.pending {
  background: #fff1c7;
  color: #8a5700;
}

.badge.confirmed {
  background: #d9f8ed;
  color: #087a5b;
}

.badge.shipping {
  background: #dff0ff;
  color: #1d67a6;
}

.badge.completed {
  background: #e5f9da;
  color: #2f7d20;
}

.badge.cancelled {
  background: #f2edf0;
  color: #8c5f6f;
}

.badge.failed {
  background: #fff0f4;
  color: #d93672;
}

.current-status-box {
  padding: 16px;
  border: 1px solid #ffd4e4;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff, rgba(255, 240, 246, 0.9));
}

.current-status-box span:first-child {
  display: block;
  color: #6b5660;
  font-size: 13px;
  font-weight: 850;
  margin-bottom: 10px;
}

.form-group {
  display: grid;
  gap: 8px;
}

.form-label {
  color: #6b5660;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.control {
  width: 100%;
  min-height: 52px;
  border: 1px solid #ffd4e4;
  border-radius: 16px;
  background: #ffffff;
  color: #5e4a54;
  outline: none;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 750;
}

.control:focus {
  border-color: #ff4d94;
  box-shadow: 0 0 0 4px rgba(255, 77, 148, 0.12);
}

.control:disabled {
  color: #9b8791;
  background: #f7f1f4;
  cursor: not-allowed;
}

.helper-text {
  margin: 0;
  color: #6b5660;
  font-size: 13px;
  line-height: 1.5;
  font-weight: 700;
}

.primary-btn,
.outline-btn {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 15px;
  border: 1px solid transparent;
  padding: 0 20px;
  font-weight: 900;
  text-decoration: none;
  white-space: nowrap;
}

.primary-btn {
  background: #ff4d94;
  color: #ffffff;
  box-shadow: 0 12px 20px rgba(255, 77, 148, 0.2);
}

.primary-btn:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  box-shadow: none;
}

.outline-btn {
  background: #ffffff;
  color: #5e4a54;
  border-color: #ffd4e4;
}

.mini-list {
  display: grid;
  gap: 12px;
}

.mini-row {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(255, 212, 228, 0.9);
}

.mini-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.mini-label {
  color: #6b5660;
  font-weight: 800;
}

.mini-value {
  color: #5e4a54;
  text-align: right;
  font-weight: 950;
  max-width: 58%;
  line-height: 1.4;
  word-break: break-word;
}

.empty-card {
  padding: 32px;
  color: #6b5660;
  font-weight: 800;
}

.toast {
  position: fixed;
  right: 26px;
  bottom: 26px;
  max-width: 430px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #ffd4e4;
  box-shadow: 0 18px 38px rgba(94, 74, 84, 0.16);
  color: #5e4a54;
  font-weight: 800;
  opacity: 0;
  pointer-events: none;
  transform: translateY(12px);
  transition: opacity 0.2s ease, transform 0.2s ease;
  z-index: 100;
}

.toast.show {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 1280px) {
  .summary-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }

  .right-column {
    position: static;
  }
}

@media (max-width: 768px) {
  .order-detail-page {
    padding: 12px;
  }

  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-card,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
