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

      <section v-if="isStaff" class="staff-assignment-bar">
        <div>
          <strong>Nhận đơn chủ động</strong>
          <span>
            {{ staffStatus.activeOrderCount }} / {{ staffStatus.maxActiveOrders }} đơn ·
            {{ staffStatus.workloadScore }} điểm tải
          </span>
        </div>
        <label class="availability-toggle">
          <input
            v-model="staffStatus.acceptingOrders"
            type="checkbox"
            :disabled="staffActionLoading"
            @change="updateAvailability"
          />
          <span>{{ staffStatus.acceptingOrders ? 'Đang sẵn sàng' : 'Tạm dừng nhận đơn' }}</span>
        </label>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="staffActionLoading || !staffStatus.canClaim"
          :title="staffStatus.unavailableReason || 'Nhận đơn cũ nhất đang chờ'"
          @click="claimNextOrder"
        >
          {{ staffActionLoading ? 'Đang xử lý...' : 'Nhận đơn tiếp theo' }}
        </button>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>Tổng đơn</span>
          <strong>{{ orderStats.total }}</strong>
        </article>

        <article class="stat-card">
          <span>Chờ xác nhận</span>
          <strong class="text-accent">{{ orderStats.pending }}</strong>
        </article>

        <article class="stat-card">
          <span>Đang giao</span>
          <strong>{{ orderStats.shipping }}</strong>
        </article>

        <article class="stat-card">
          <span>Hoàn thành</span>
          <strong>{{ orderStats.completed }}</strong>
        </article>
      </section>

      <section class="toolbar-panel">
        <div class="row g-3 align-items-end">
          <div class="col-12 col-md-6 col-lg-4">
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

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label">Thanh toán</label>
            <select v-model="paymentMethod" class="form-select">
              <option value="all">Tất cả</option>
              <option v-for="item in paymentOptions" :key="item" :value="item">
                {{ paymentMethodMap[item] || item }}
              </option>
            </select>
          </div>

          <div class="col-12 col-md-6 col-lg">
            <label class="form-label">Trạng thái</label>
            <select v-model="orderStatus" class="form-select">
              <option value="all">Tất cả</option>
              <option v-for="item in orderOptions" :key="item" :value="item">
                {{ orderStatusMap[item]?.label || item }}
              </option>
            </select>
          </div>

          <div class="col-6 col-md-3 col-lg">
            <label class="form-label">Từ ngày</label>
            <input v-model="fromDate" type="date" class="form-control" :max="toDate || undefined" />
          </div>

          <div class="col-6 col-md-3 col-lg">
            <label class="form-label">Đến ngày</label>
            <input v-model="toDate" type="date" class="form-control" :min="fromDate || undefined" />
          </div>

          <div class="col-12 col-md-6 col-lg-auto">
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
                <th>Phụ trách</th>
                <th>Ngày tạo</th>
                <th class="text-end">Thao tác</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(orders, index) in filteredOrders" :key="orders.orderId">
                <td class="fw-bold">#{{ currentPage * pageSize + index + 1 }}</td>
                <td>
                  <div class="order-code">{{ orders.orderCode }}</div>
                  <small>{{ orders.itemCount }} sản phẩm</small>
                </td>
                <td>
                  <div class="order-code">{{ orders.recipientName }}</div>
                  <small>{{ orders.recipientPhone }}</small>
                </td>
                <td class="fw-semibold">{{ formatCurrency(orders.finalAmount) }}</td>
                <td>
                  <div>{{ paymentMethodMap[orders.paymentMethod] || orders.paymentMethod }}</div>
                  <small v-if="orders.fulfillmentMethod === 'STORE_PICKUP'" class="text-success">
                    <i class="bi bi-shop"></i> Nhận tại cửa hàng
                  </small>
                </td>
                <td>
                  <span
                    class="status-badge"
                    :class="paymentStatusMap[orders.paymentStatus]?.className"
                  >
                    {{ paymentStatusMap[orders.paymentStatus]?.label || orders.paymentStatus }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="orderStatusMap[orders.orderStatus]?.className">
                    {{ orderStatusMap[orders.orderStatus]?.label || orders.orderStatus }}
                  </span>
                </td>
                <td>
                  <div v-if="orders.assignment" class="order-code">
                    {{ orders.assignment.staffName }}
                    <small class="d-block">{{
                      orders.assignment.assignmentType === 'AUTO' ? 'Tự động' : 'Thủ công'
                    }}</small>
                  </div>
                  <small v-else class="text-muted">Chưa phân công</small>
                </td>
                <td>
                  <div class="date-line">Ngày: {{ formatDate1(orders.createdAt) }}</div>
                  <div class="date-line">Giờ: {{ formatTime1(orders.createdAt) }}</div>
                </td>
                <td>
                  <div class="d-flex justify-content-end gap-2">
                    <button
                      type="button"
                      class="icon-button"
                      title="Xem chi tiết"
                      @click="showOrderDetail(orders)"
                    >
                      <i class="bi bi-eye"></i>
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
        <div v-if="totalPages > 0" class="order-pagination">
          <div class="pagination-summary">
            Tổng <strong>{{ totalElements }}</strong> đơn hàng
          </div>
          <div class="pagination-controls">
            <label class="page-size-control">
              <span>Hiển thị</span>
              <select v-model.number="pageSize" class="form-select form-select-sm">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
            </label>
            <button
              type="button"
              class="pagination-button"
              :disabled="currentPage === 0"
              @click="goToPage(currentPage - 1)"
            >
              Trước
            </button>
            <span class="page-indicator">
              Trang <strong>{{ currentPage + 1 }}</strong> / {{ totalPages }}
            </span>
            <button
              type="button"
              class="pagination-button"
              :disabled="currentPage + 1 >= totalPages"
              @click="goToPage(currentPage + 1)"
            >
              Sau
            </button>
          </div>
        </div>
      </section>
    </div>

    <div class="order-toast" :class="{ show: toastMessage }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import OrderListApi from '@/api/orderListApi.js'
import StaffOrderAssignmentApi from '@/api/staffOrderAssignmentApi.js'
import '@/assets/css/OrderList.css'

const toastMessage = ref('')
const roles = JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
const isStaff = roles.includes('ROLE_STAFF') && !roles.includes('ROLE_ADMIN')
const staffActionLoading = ref(false)
const staffStatus = ref({
  acceptingOrders: true,
  maxActiveOrders: 5,
  activeOrderCount: 0,
  workloadScore: 0,
  canClaim: false,
  unavailableReason: '',
})
const router = useRouter()
const keyword = ref('')
const paymentMethod = ref('all')
const orderStatus = ref('all')
const fromDate = ref('')
const toDate = ref('')

const orders = ref([])
const currentPage = ref(0)
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const error = ref(null)
const paymentOptions = ref([])
const orderOptions = ref([])
const orderStats = ref({
  total: 0,
  pending: 0,
  confirmed: 0,
  shipping: 0,
  completed: 0,
  cancelled: 0,
})
async function fetchGetAllOrder() {
  try {
    loading.value = true
    error.value = null
    const response = await OrderListApi.getAllOrder({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      paymentMethod: paymentMethod.value === 'all' ? undefined : paymentMethod.value,
      orderStatus: orderStatus.value === 'all' ? undefined : orderStatus.value,
      fromDate: fromDate.value || undefined,
      toDate: toDate.value || undefined,
    })
    orders.value = response.data.content || []
    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements
  } catch (e) {
    error.value = 'Không thể tải được đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}
async function fetchOrderStats() {
  const response = await OrderListApi.getOrderStats()
  orderStats.value = response.data
}
onMounted(() => {
  fetchGetAllOrder()
  fetchOrderStats()
  getFilterOption()
  if (isStaff) fetchStaffStatus()
})

async function fetchStaffStatus() {
  const { data } = await StaffOrderAssignmentApi.getStatus()
  staffStatus.value = data
}

async function updateAvailability() {
  staffActionLoading.value = true
  try {
    const { data } = await StaffOrderAssignmentApi.setAvailability(
      staffStatus.value.acceptingOrders,
    )
    staffStatus.value = data
    showToast(data.acceptingOrders ? 'Bạn đã sẵn sàng nhận đơn.' : 'Đã tạm dừng nhận đơn mới.')
  } catch (e) {
    await fetchStaffStatus()
    showToast(e.response?.data?.message || 'Không thể cập nhật trạng thái nhận đơn.')
  } finally {
    staffActionLoading.value = false
  }
}

async function claimNextOrder() {
  staffActionLoading.value = true
  try {
    const { data } = await StaffOrderAssignmentApi.claimNext()
    showToast(`Đã nhận đơn ${data.orderCode}.`)
    await Promise.all([fetchStaffStatus(), fetchGetAllOrder(), fetchOrderStats()])
    router.push(`/admin/order/${data.orderCode}`)
  } catch (e) {
    showToast(e.response?.data?.message || 'Không thể nhận đơn lúc này.')
    await fetchStaffStatus()
  } finally {
    staffActionLoading.value = false
  }
}
watch([keyword, paymentMethod, orderStatus, fromDate, toDate], () => {
  currentPage.value = 0
  fetchGetAllOrder()
})

watch(pageSize, () => {
  currentPage.value = 0
  fetchGetAllOrder()
})

function goToPage(page) {
  if (page < 0 || page >= totalPages.value) {
    return
  }

  currentPage.value = page
  fetchGetAllOrder()
}

async function getFilterOption() {
  const response = await OrderListApi.getFilterOption()
  paymentOptions.value = response.data.paymentMethods || []
  orderOptions.value = response.data.orderStatuses || []
}
const formatDate1 = (value) => {
  if (!value) return ''
  return String(value).split(' ')[0]
}

const formatTime1 = (value) => {
  if (!value) return ''
  return String(value).split(' ')[1] || ''
}
const orderStatusMap = {
  // Marcus thêm: trạng thái riêng cho đơn khách đến nhận tại cửa hàng.
  PENDING: { label: 'Chờ xác nhận', className: 'pending' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed' },
  PROCESSING: { label: 'Đang chuẩn bị', className: 'processing' },
  READY_FOR_PICKUP: { label: 'Sẵn sàng nhận tại cửa hàng', className: 'confirmed' },
  PICKED_UP: { label: 'Đã nhận tại cửa hàng', className: 'completed' },
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
  PENDING: { label: 'Chờ thanh toán', className: 'pending' },
  FAILED: { label: 'Lỗi thanh toán', className: 'failed' },
}

const paymentMethodMap = {
  VNPay: 'VNPAY',
  VNPAY: 'VNPAY',
  COD: 'COD',
  MoMo: 'MoMo',
  BankTransfer: 'Chuyển khoản',
}

const filteredOrders = computed(() => orders.value)

const formatCurrency = (value) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)

const showToast = (message) => {
  toastMessage.value = message
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

const showOrderDetail = (order) => {
  router.push(`/admin/order/${order.orderCode}`)
}

const resetFilters = () => {
  keyword.value = ''
  paymentMethod.value = 'all'
  orderStatus.value = 'all'
  fromDate.value = ''
  toDate.value = ''
  showToast('Đã làm mới bộ lọc.')
}
</script>

<style scoped>
.staff-assignment-bar {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  background: #eff6ff;
}
.staff-assignment-bar > div:first-child {
  display: grid;
  margin-right: auto;
}
.staff-assignment-bar span {
  color: #64748b;
  font-size: 13px;
}
.availability-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
@media (max-width: 760px) {
  .staff-assignment-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
