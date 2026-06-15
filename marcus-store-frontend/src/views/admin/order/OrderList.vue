<template>
  <div class="order-page">
    <div class="order-shell">
      <section class="order-hero">
        <div class="hero-title">
          <div class="hero-icon">
            <i class="bi bi-bag-check-fill"></i>
          </div>
          <div>
            <h1>Quản lý đơn hàng</h1>
            <p>Theo dõi đơn hàng, thanh toán và trạng thái xử lý.</p>
          </div>
        </div>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng đơn</span>
          <strong>{{ orders.length }}</strong>
        </article>

        <article class="stat-card">
          <span>Chờ xác nhận</span>
          <strong class="text-accent">{{ statusCount.PENDING }}</strong>
        </article>

        <article class="stat-card">
          <span>Đang giao</span>
          <strong>{{ statusCount.SHIPPING }}</strong>
        </article>

        <article class="stat-card">
          <span>Hoàn thành</span>
          <strong>{{ statusCount.COMPLETED }}</strong>
        </article>
      </section>

      <section class="toolbar-panel">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-lg-5">
            <label class="form-label">Tìm kiếm</label>
            <div class="input-group">
              <span class="input-group-text">
                <i class="bi bi-search"></i>
              </span>
              <input
                v-model.trim="keyword"
                type="search"
                class="form-control"
                placeholder="Tìm theo mã đơn, người nhận hoặc số điện thoại"
              />
            </div>
          </div>

          <div class="col-12 col-md-6 col-lg-3">
            <label class="form-label">Thanh toán</label>
            <select v-model="paymentMethod" class="form-select">
              <option value="all">Tất cả</option>
              <option value="VNPay">VNPay</option>
              <option value="COD">COD</option>
              <option value="MoMo">MoMo</option>
              <option value="BankTransfer">Chuyển khoản</option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg-3">
            <label class="form-label">Trạng thái</label>
            <select v-model="orderStatus" class="form-select">
              <option value="all">Tất cả</option>
              <option v-for="item in statusOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </div>

          <div class="col-12 col-lg-1">
            <button type="button" class="btn btn-soft w-100" title="Xóa lọc" @click="resetFilters">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </div>
        </div>
      </section>

      <section class="table-panel">
        <div class="table-responsive">
          <table class="table align-middle order-table mb-0">
            <thead>
            <tr>
              <th>ID</th>
              <th>Mã đơn</th>
              <th>Người nhận</th>
              <th>Thành tiền</th>
              <th>Thanh toán</th>
              <th>Trạng thái TT</th>
              <th>Trạng thái đơn</th>
              <th>Ngày tạo</th>
              <th class="text-end">Thao tác</th>
            </tr>
            </thead>

            <tbody>
            <tr v-for="(order, index) in filteredOrders" :key="order.id">
              <td class="fw-bold">#{{ index + 1 }}</td>
              <td>
                <div class="order-code">{{ order.orderCode }}</div>
                <small>{{ getItemCount(order) }} sản phẩm</small>
              </td>
              <td>
                <div class="order-code">{{ order.recipientName }}</div>
                <small>{{ order.recipientPhone }}</small>
              </td>
              <td class="fw-semibold">{{ formatCurrency(order.finalAmount) }}</td>
              <td>{{ paymentMethodMap[order.paymentMethod] || order.paymentMethod }}</td>
              <td>
                <span class="status-badge" :class="paymentStatusMap[order.paymentStatus]?.className">
                  {{ paymentStatusMap[order.paymentStatus]?.label || order.paymentStatus }}
                </span>
              </td>
              <td>
                <span class="status-badge" :class="orderStatusMap[order.orderStatus]?.className">
                  {{ orderStatusMap[order.orderStatus]?.label || order.orderStatus }}
                </span>
              </td>
              <td>
                <div class="date-line">Ngày: {{ formatDate(order.createdAt) }}</div>
                <div class="date-line">Giờ: {{ formatTime(order.createdAt) }}</div>
              </td>
              <td>
                <div class="d-flex justify-content-end gap-2">
                  <button
                    type="button"
                    class="icon-button"
                    title="Xem chi tiết"
                    @click="showOrderDetail(order)"
                  >
                    <i class="bi bi-eye"></i>
                  </button>
                  <button
                    type="button"
                    class="icon-button danger"
                    title="Ẩn đơn hàng"
                    @click="hideOrder(order)"
                  >
                    <i class="bi bi-eye-slash"></i>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-if="filteredOrders.length === 0" class="empty-state">
          <i class="bi bi-bag-x"></i>
          <h3>Không có đơn hàng nào</h3>
          <p>Hãy thay đổi bộ lọc hoặc làm mới danh sách.</p>
        </div>
      </section>
    </div>

    <div class="toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import '@/assets/css/OrderList.css'
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

const showToast = (message) => {
  toastMessage.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
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

</style>
