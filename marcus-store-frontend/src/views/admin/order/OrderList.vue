<template>
  <section class="order-page">
    <div class="page-heading">
      <div>
        <h3>Quản lý đơn hàng</h3>
        <p>Theo dõi đơn hàng và cập nhật trạng thái theo đúng luồng xử lý.</p>
      </div>

      <div class="stat-pills">
        <span class="stat-pill"><strong>{{ orders.length }}</strong> tổng đơn</span>
        <span class="stat-pill"><strong>{{ statusCount.PENDING }}</strong> chờ xác nhận</span>
        <span class="stat-pill"><strong>{{ statusCount.SHIPPING }}</strong> đang giao</span>
        <span class="stat-pill"><strong>{{ statusCount.COMPLETED }}</strong> hoàn thành</span>
      </div>
    </div>

    <div class="order-card toolbar-card">
      <div class="filters">
        <input
          v-model.trim="keyword"
          class="control"
          type="search"
          placeholder="Tìm theo mã đơn, người nhận hoặc số điện thoại"
        />

        <select v-model="paymentMethod" class="control">
          <option value="all">Tất cả thanh toán</option>
          <option value="VNPay">VNPay</option>
          <option value="COD">COD</option>
          <option value="MoMo">MoMo</option>
          <option value="BankTransfer">Chuyển khoản</option>
        </select>

        <select v-model="orderStatus" class="control">
          <option value="all">Tất cả trạng thái</option>
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>

        <button class="outline-btn" type="button" @click="resetFilters">Làm mới</button>
      </div>
    </div>

    <div class="order-card table-card">
      <div class="table-top">
        <h4>Danh sách đơn hàng</h4>
        <span>Hiển thị {{ filteredOrders.length }} đơn hàng</span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Người nhận</th>
              <th>Thành tiền</th>
              <th>Thanh toán</th>
              <th>Trạng thái TT</th>
              <th>Trạng thái đơn</th>
              <th>Ngày tạo</th>
              <th>Thao tác</th>
            </tr>
          </thead>

          <tbody v-if="filteredOrders.length">
            <tr v-for="order in filteredOrders" :key="order.id">
              <td>
                <span class="main-line">{{ order.orderCode }}</span>
                <span class="sub-line">{{ getItemCount(order) }} sản phẩm</span>
              </td>
              <td>
                <span class="main-line">{{ order.recipientName }}</span>
                <span class="sub-line">{{ order.recipientPhone }}</span>
              </td>
              <td>
                <span class="money">{{ formatCurrency(order.finalAmount) }}</span>
              </td>
              <td>{{ paymentMethodMap[order.paymentMethod] || order.paymentMethod }}</td>
              <td>
                <span class="badge" :class="paymentStatusMap[order.paymentStatus]?.className">
                  {{ paymentStatusMap[order.paymentStatus]?.label || order.paymentStatus }}
                </span>
              </td>
              <td>
                <span class="badge" :class="orderStatusMap[order.orderStatus]?.className">
                  {{ orderStatusMap[order.orderStatus]?.label || order.orderStatus }}
                </span>
              </td>
              <td>
                <span class="main-line">{{ formatDate(order.createdAt) }}</span>
                <span class="sub-line">{{ formatTime(order.createdAt) }}</span>
              </td>
              <td>
                <div class="row-actions">
                  <button class="action-btn detail-btn" type="button" @click="showOrderDetail(order)">
                    Chi tiết
                  </button>
                  <button class="action-btn hide-btn" type="button" @click="hideOrder(order)">
                    Ẩn đơn hàng
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!filteredOrders.length" class="empty-state">
          Không tìm thấy đơn hàng phù hợp.
        </div>
      </div>
    </div>

    <div class="toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')
const paymentMethod = ref('all')
const orderStatus = ref('all')
const toastMessage = ref('')

const orders = ref([
  {
    id: 1,
    orderCode: 'ORD-20260501-001',
    recipientName: 'Nguyễn Văn An',
    recipientPhone: '0983333333',
    finalAmount: 24490000,
    paymentMethod: 'VNPay',
    paymentStatus: 'PAID',
    orderStatus: 'COMPLETED',
    createdAt: '2026-05-01 10:30:00',
    items: [{ quantity: 1 }],
  },
  {
    id: 2,
    orderCode: 'ORD-20260515-002',
    recipientName: 'Trần Thị Bình',
    recipientPhone: '0974444444',
    finalAmount: 27990000,
    paymentMethod: 'COD',
    paymentStatus: 'UNPAID',
    orderStatus: 'SHIPPING',
    createdAt: '2026-05-15 14:00:00',
    items: [{ quantity: 1 }],
  },
  {
    id: 3,
    orderCode: 'ORD-20260530-003',
    recipientName: 'Lê Minh Cường',
    recipientPhone: '0965555555',
    finalAmount: 29480000,
    paymentMethod: 'MoMo',
    paymentStatus: 'PAID',
    orderStatus: 'CONFIRMED',
    createdAt: '2026-05-30 09:15:00',
    items: [{ quantity: 1 }, { quantity: 1 }],
  },
  {
    id: 4,
    orderCode: 'ORD-20260605-004',
    recipientName: 'Phạm Thị Dung',
    recipientPhone: '0956666666',
    finalAmount: 8990000,
    paymentMethod: 'BankTransfer',
    paymentStatus: 'PAID',
    orderStatus: 'PENDING',
    createdAt: '2026-06-05 20:00:00',
    items: [{ quantity: 1 }],
  },
  {
    id: 5,
    orderCode: 'ORD-20260608-005',
    recipientName: 'Nguyễn Hoàng Nam',
    recipientPhone: '0912888999',
    finalAmount: 6490000,
    paymentMethod: 'COD',
    paymentStatus: 'UNPAID',
    orderStatus: 'CANCELLED',
    createdAt: '2026-06-08 11:20:00',
    items: [{ quantity: 1 }],
  },
  {
    id: 6,
    orderCode: 'ORD-20260610-006',
    recipientName: 'Vũ Minh Anh',
    recipientPhone: '0933777666',
    finalAmount: 19990000,
    paymentMethod: 'VNPay',
    paymentStatus: 'PAID',
    orderStatus: 'FAILED',
    createdAt: '2026-06-10 16:40:00',
    items: [{ quantity: 1 }],
  },
])

const hiddenOrderIds = ref([])

const orderStatusMap = {
  PENDING: { label: 'Chờ xác nhận', className: 'pending' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed' },
  SHIPPING: { label: 'Đang giao', className: 'shipping' },
  COMPLETED: { label: 'Hoàn thành', className: 'completed' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled' },
  FAILED: { label: 'Giao thất bại', className: 'failed' },
}

const statusOptions = Object.entries(orderStatusMap).map(([value, item]) => ({
  value,
  label: item.label,
}))

const statusTransitions = {
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

const terminalStatuses = new Set(['COMPLETED', 'CANCELLED', 'FAILED'])

const paymentStatusMap = {
  PAID: { label: 'Đã thanh toán', className: 'confirmed' },
  UNPAID: { label: 'Chưa thanh toán', className: 'pending' },
  PENDING: { label: 'Chờ thanh toán', className: 'pending' },
}

const paymentMethodMap = {
  VNPay: 'VNPay',
  COD: 'COD',
  MoMo: 'MoMo',
  BankTransfer: 'Chuyển khoản',
}

const filteredOrders = computed(() => {
  const search = keyword.value.toLowerCase()

  return orders.value.filter((order) => {
    if (hiddenOrderIds.value.includes(order.id)) return false

    const matchKeyword =
      !search ||
      [order.orderCode, order.recipientName, order.recipientPhone].some((value) =>
        String(value).toLowerCase().includes(search),
      )
    const matchPayment = paymentMethod.value === 'all' || order.paymentMethod === paymentMethod.value
    const matchStatus = orderStatus.value === 'all' || order.orderStatus === orderStatus.value

    return matchKeyword && matchPayment && matchStatus
  })
})

const statusCount = computed(() =>
  Object.keys(orderStatusMap).reduce((result, status) => {
    result[status] = orders.value.filter((order) => order.orderStatus === status).length
    return result
  }, {}),
)

const formatCurrency = (value) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)

const parseDate = (value) => new Date(value.replace(' ', 'T'))

const formatDate = (value) => parseDate(value).toLocaleDateString('vi-VN')

const formatTime = (value) =>
  parseDate(value).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  })

const getItemCount = (order) => order.items.reduce((total, item) => total + item.quantity, 0)

const getNextStatuses = (status) => statusTransitions[status] || []

const getDefaultNextStatus = (status) => getNextStatuses(status)[0]?.value || status

const isTerminalStatus = (status) => terminalStatuses.has(status)

const getStatusNote = (status) => {
  if (isTerminalStatus(status)) {
    return 'Đơn đã ở trạng thái cuối.'
  }

  return 'Chỉ hiện các trạng thái hợp lệ.'
}

const showToast = (message) => {
  toastMessage.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

const updateOrderStatus = (order, nextStatus) => {
  const allow = getNextStatuses(order.orderStatus).some((item) => item.value === nextStatus)
  if (!allow) {
    showToast('Trạng thái chuyển không hợp lệ.')
    return
  }

  const oldStatus = orderStatusMap[order.orderStatus]?.label || order.orderStatus
  order.orderStatus = nextStatus
  const newStatus = orderStatusMap[nextStatus]?.label || nextStatus

  showToast(`Đã chuyển đơn ${order.orderCode} từ ${oldStatus} sang ${newStatus}.`)
}

const showOrderDetail = (order) => {
  router.push(`/admin/order/${order.id}`)
}

const hideOrder = (order) => {
  if (!hiddenOrderIds.value.includes(order.id)) {
    hiddenOrderIds.value.push(order.id)
  }

  showToast(`Đã ẩn đơn ${order.orderCode} khỏi danh sách.`)
}

const resetFilters = () => {
  keyword.value = ''
  paymentMethod.value = 'all'
  orderStatus.value = 'all'
  showToast('Đã làm mới bộ lọc.')
}
</script>

<style scoped>
.order-page {
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
  font-weight: 600;
}

.stat-pills {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-pill {
  padding: 10px 14px;
  border: 1px solid #ffd4e4;
  border-radius: 999px;
  background: #ffffff;
  color: #6b5660;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(94, 74, 84, 0.05);
}

.stat-pill strong {
  color: #ff4d94;
  margin-right: 4px;
}

.order-card {
  background: #ffffff;
  border: 1px solid rgba(255, 220, 233, 0.85);
  border-radius: 20px;
  box-shadow: 0 10px 24px rgba(94, 74, 84, 0.07);
}

.toolbar-card {
  padding: 22px;
}

.filters {
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) minmax(170px, 0.8fr) minmax(180px, 0.8fr) auto;
  gap: 14px;
  align-items: center;
}

.control {
  width: 100%;
  min-height: 48px;
  border: 1px solid #ffd4e4;
  border-radius: 14px;
  background: #ffffff;
  color: #5e4a54;
  outline: none;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 650;
}

.control:focus {
  border-color: #ff4d94;
  box-shadow: 0 0 0 4px rgba(255, 77, 148, 0.12);
}

.control:disabled {
  color: #9b8791;
  background: #fff7fa;
  cursor: not-allowed;
}

.outline-btn {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #ffd4e4;
  border-radius: 14px;
  padding: 0 18px;
  background: #ffffff;
  color: #5e4a54;
  font-weight: 850;
  white-space: nowrap;
}

.outline-btn:hover {
  background: #fff0f6;
}

.table-card {
  overflow: hidden;
}

.table-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(255, 212, 228, 0.7);
}

.table-top h4 {
  margin: 0;
  color: #5e4a54;
  font-size: 18px;
  font-weight: 850;
}

.table-top span {
  color: #6b5660;
  font-size: 14px;
  font-weight: 700;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1060px;
  border-collapse: separate;
  border-spacing: 0;
}

thead th {
  padding: 18px 16px;
  background: #fffafb;
  border-bottom: 1px solid rgba(255, 212, 228, 0.72);
  color: #5e4a54;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-align: left;
  text-transform: uppercase;
  white-space: nowrap;
}

tbody td {
  padding: 17px 16px;
  border-bottom: 1px solid rgba(255, 212, 228, 0.58);
  vertical-align: middle;
  color: #5e4a54;
  font-size: 14px;
  font-weight: 650;
}

tbody tr:hover {
  background: #fff0f6;
}

tbody tr:last-child td {
  border-bottom: 0;
}

.main-line {
  display: block;
  color: #5e4a54;
  font-weight: 900;
}

.sub-line {
  display: block;
  margin-top: 5px;
  color: #6b5660;
  font-size: 12px;
  font-weight: 700;
}

.money {
  color: #4b3943;
  font-weight: 900;
  white-space: nowrap;
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
  background: #e9ddff;
  color: #6f3bbd;
}

.badge.cancelled {
  background: #f2edf0;
  color: #8c5f6f;
}

.badge.failed {
  background: #ffe2e8;
  color: #c72250;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 210px;
}

.action-btn {
  min-height: 38px;
  border-radius: 12px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 850;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 14px rgba(255, 77, 148, 0.12);
}

.detail-btn {
  color: #5e4a54;
  background: #ffffff;
  border: 1px solid #ffd4e4;
}

.hide-btn {
  color: #ff4d94;
  background: #fff0f6;
  border: 1px solid #ffd4e4;
}

.empty-state {
  padding: 44px 20px;
  text-align: center;
  color: #6b5660;
  font-weight: 800;
}

.toast {
  position: fixed;
  right: 26px;
  bottom: 26px;
  max-width: 420px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #ffd4e4;
  box-shadow: 0 18px 38px rgba(94, 74, 84, 0.16);
  color: #5e4a54;
  font-weight: 750;
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

@media (max-width: 1180px) {
  .filters {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .order-page {
    padding: 12px;
  }

  .page-heading,
  .table-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
