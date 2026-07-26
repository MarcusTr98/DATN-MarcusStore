<template>
  <main class="client-order-page">
    <section class="client-order-shell">
      <div class="main-card">
        <div class="main-header-card">
          <div>
            <h2 class="main-title">
              <i class="fa-solid fa-box-open"></i>
              Đơn hàng của tôi
            </h2>
            <p class="main-note">Theo dõi danh sách đơn hàng đã đặt tại Marcus Store.</p>
          </div>
          <span class="order-count">{{ filteredOrders.length }} đơn</span>
        </div>

        <div class="main-body">
          <section class="panel">
            <div class="panel-header">
              <h3 class="panel-title">
                <i class="fa-solid fa-receipt"></i>
                Danh sách đơn
              </h3>
            </div>

            <div class="filter-row">
              <input
                v-model.trim="keyword"
                class="filter-input"
                type="text"
                placeholder="Tìm theo mã đơn"
              />
              <select v-model="statusFilter" class="filter-select">
                <option value="ALL">Tất cả</option>
                <option v-for="status in statusOptions" :key="status" :value="status">
                  {{ statusConfig[status]?.label || status }}
                </option>
              </select>
            </div>

            <div v-if="loading" class="empty-state">Đang tải danh sách đơn hàng...</div>
            <div v-else-if="error" class="empty-state text-danger">{{ error }}</div>
            <div v-else-if="filteredOrders.length === 0" class="empty-state">
              Không tìm thấy đơn hàng phù hợp.
            </div>

            <div v-else class="order-list my-orders-list">
              <router-link
                v-for="order in filteredOrders"
                :key="order.orderCode"
                class="order-card my-order-card"
                :to="`/profile/orders/${order.orderCode}`"
              >
                <div class="order-card-top">
                  <div>
                    <div class="order-code">{{ order.orderCode }}</div>
                    <div class="order-date">{{ formatDateTime(order.createdAt) }}</div>
                  </div>
                  <span class="status-pill" :class="statusConfig[order.orderStatus]?.className">
                    <i class="fa-solid" :class="statusConfig[order.orderStatus]?.icon"></i>
                    {{ statusConfig[order.orderStatus]?.label || order.orderStatus }}
                  </span>
                </div>

                <div class="order-card-info">
                  <div class="order-products">
                    {{ order.itemCount || 0 }} sản phẩm ·
                    {{ getPaymentMethodLabel(order.paymentMethod) }} ·
                    {{ order.paymentStatus || '---' }}
                  </div>
                  <div class="order-total">{{ formatMoney(order.finalAmount) }}</div>
                </div>
              </router-link>
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import UserOrderApi from '@/api/userOrder.js'
import '@/assets/css/OrderDetailView.css'

const orders = ref([])
const loading = ref(false)
const error = ref(null)
const keyword = ref('')
const statusFilter = ref('ALL')

async function fetchUserOrders() {
  try {
    loading.value = true
    error.value = null
    const response = await UserOrderApi.userOrder()
    orders.value = response.data || []
  } catch (e) {
    error.value = 'Không thể lấy đơn hàng'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchUserOrders)

const statusConfig = {
  PENDING: { label: 'Chờ xác nhận', className: 'pending', icon: 'fa-clock' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'confirmed', icon: 'fa-circle-check' },
  PROCESSING: { label: 'Đang xử lý', className: 'processing', icon: 'fa-boxes-packing' },
  PACKED: { label: 'Đang xử lý', className: 'processing', icon: 'fa-box' },
  READY_FOR_PICKUP: { label: 'Sẵn sàng nhận', className: 'completed', icon: 'fa-store' },
  SHIPPING: { label: 'Đang giao', className: 'shipping', icon: 'fa-truck-fast' },
  DELIVERED: { label: 'Đang giao', className: 'shipping', icon: 'fa-box-anchor' },
  COMPLETED: { label: 'Hoàn thành', className: 'completed', icon: 'fa-house-circle-check' },
  CANCELLED: { label: 'Đã hủy', className: 'cancelled', icon: 'fa-ban' },
  FAILED: { label: 'Giao thất bại', className: 'failed', icon: 'fa-triangle-exclamation' },
}

const statusOptions = computed(() => Object.keys(statusConfig))

const filteredOrders = computed(() =>
  orders.value.filter((order) => {
    const keywordValue = keyword.value.toLowerCase()
    const matchesKeyword = (order.orderCode || '').toLowerCase().includes(keywordValue)
    const matchesStatus = statusFilter.value === 'ALL' || order.orderStatus === statusFilter.value
    return matchesKeyword && matchesStatus
  }),
)

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
.my-orders-list {
  max-height: none;
}

.my-order-card {
  display: block;
  color: inherit;
  text-decoration: none;
}
</style>
