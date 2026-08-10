<template>
  <main class="success-page">
    <section v-if="paymentStatus === 'SUCCESS'" class="status-card">
      <div class="success-icon"><i class="fas fa-check"></i></div>
      <p class="status-card__eyebrow">ĐẶT HÀNG THÀNH CÔNG</p>
      <h1>Cảm ơn bạn đã mua sắm!</h1>
      <p class="status-card__lead">
        Đơn hàng đã được ghi nhận. {{ siteName }} sẽ cập nhật trạng thái sớm nhất cho bạn.
      </p>

      <div class="order-code-box">
        <span>Mã đơn hàng</span>
        <strong>{{ orderCode }}</strong>
      </div>

      <!-- Marcus thêm: hướng dẫn riêng cho đơn nhận tại cửa hàng. -->
      <div v-if="isLoading" class="fulfillment-loading">
        <i class="fas fa-spinner fa-spin"></i> Đang tải thông tin nhận hàng...
      </div>
      <div v-else-if="isStorePickup" class="pickup-guide">
        <div class="pickup-guide__header">
          <span class="pickup-guide__icon"><i class="fas fa-store"></i></span>
          <div>
            <small>PHƯƠNG THỨC NHẬN HÀNG</small>
            <h2>Nhận tại {{ siteName }}</h2>
          </div>
          <span class="pickup-guide__badge">Miễn phí</span>
        </div>
        <div class="pickup-guide__address">
          <i class="fas fa-location-dot"></i>
          <span>{{ orderDetail.shippingAddress }}</span>
        </div>
        <div class="pickup-guide__notice">
          <i class="fas fa-circle-info"></i>
          <span>
            Vui lòng chờ đơn chuyển sang <strong>“Sẵn sàng nhận tại cửa hàng”</strong>
            trước khi đến lấy sản phẩm.
          </span>
        </div>
      </div>
      <div v-else-if="orderDetail" class="delivery-guide">
        <i class="fas fa-truck-fast"></i>
        <span>Đơn hàng sẽ được giao đến địa chỉ bạn đã chọn.</span>
      </div>

      <div v-if="orderDetail" class="order-quick-info">
        <div>
          <span>Thanh toán</span>
          <strong>{{ paymentMethodLabel }}</strong>
        </div>
        <div>
          <span>Tổng cộng</span>
          <strong class="amount">{{ formatMoney(orderDetail.finalAmount) }}</strong>
        </div>
      </div>

      <div class="status-card__actions">
        <router-link to="/" class="btn-home">Tiếp tục mua sắm</router-link>
        <router-link to="/profile/orders" class="btn-orders">
          Xem đơn hàng <i class="fas fa-arrow-right"></i>
        </router-link>
      </div>
    </section>

    <section v-else class="status-card status-card--failed">
      <div class="failed-icon"><i class="fas fa-xmark"></i></div>
      <p class="status-card__eyebrow">THANH TOÁN CHƯA HOÀN TẤT</p>
      <h1>Giao dịch không thành công</h1>
      <p class="status-card__lead">
        Giao dịch của đơn {{ orderCode }} đã bị hủy hoặc gặp lỗi. Bạn chưa bị ghi nhận thanh toán.
      </p>
      <router-link to="/" class="btn-orders">Về trang chủ</router-link>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import userOrderApi from '@/api/userOrder'
import { useSettings } from '@/composables/useSettings'

const { siteName, fetchSettings } = useSettings()

const route = useRoute()
const paymentStatus = ref('SUCCESS')
const orderCode = ref('Đang cập nhật')
const orderDetail = ref(null)
const isLoading = ref(false)

const isStorePickup = computed(() => orderDetail.value?.fulfillmentMethod === 'STORE_PICKUP')

const paymentMethodLabel = computed(() => {
  const method = String(orderDetail.value?.paymentMethod || '').toUpperCase()
  if (method === 'COD') return isStorePickup.value ? 'Thanh toán tại cửa hàng' : 'COD'
  if (method === 'VNPAY') return 'VNPAY'
  if (method === 'BANKING') return 'Chuyển khoản'
  return method || 'Đang cập nhật'
})

const formatMoney = (value) => `${Number(value || 0).toLocaleString('vi-VN')}₫`

const fetchOrderDetail = async () => {
  if (!orderCode.value || orderCode.value === 'Đang cập nhật') return
  isLoading.value = true
  try {
    const response = await userOrderApi.userOrderDetail(orderCode.value)
    orderDetail.value = response.data?.data ?? response.data
  } catch {
    // Marcus sửa: trang thành công vẫn dùng được nếu API chi tiết tạm thời chưa phản hồi.
    orderDetail.value = null
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await fetchSettings()
  const vnpResponseCode = route.query.vnp_ResponseCode
  orderCode.value = route.query.vnp_TxnRef || route.query.orderCode || 'Đang cập nhật'
  paymentStatus.value = vnpResponseCode && vnpResponseCode !== '00' ? 'FAILED' : 'SUCCESS'

  if (paymentStatus.value === 'SUCCESS') await fetchOrderDetail()
})
</script>

<style scoped>
.success-page {
  min-height: calc(100vh - 80px);
  display: grid;
  place-items: center;
  padding: 48px 20px;
  background: radial-gradient(circle at top, rgba(220, 38, 38, 0.06), transparent 34%), #f5f6f8;
  font-family: 'Be Vietnam Pro', system-ui, sans-serif;
}
.status-card {
  width: min(100%, 620px);
  padding: 38px;
  border: 1px solid #e5e7eb;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(17, 24, 39, 0.08);
  text-align: center;
}
.success-icon,
.failed-icon {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: #dcfce7;
  color: #15803d;
  font-size: 30px;
}
.failed-icon {
  background: #fee2e2;
  color: #dc2626;
}
.status-card__eyebrow {
  margin: 0 0 8px;
  color: #15803d;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
}
.status-card--failed .status-card__eyebrow {
  color: #dc2626;
}
.status-card h1 {
  margin: 0;
  color: #111827;
  font-size: 27px;
}
.status-card__lead {
  max-width: 480px;
  margin: 12px auto 24px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
}
.order-code-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px dashed #d1d5db;
  border-radius: 11px;
  background: #f9fafb;
  text-align: left;
}
.order-code-box span {
  color: #6b7280;
  font-size: 11px;
}
.order-code-box strong {
  color: #111827;
  font-size: 17px;
}
.fulfillment-loading,
.delivery-guide {
  margin-top: 16px;
  padding: 12px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
}
.pickup-guide {
  overflow: hidden;
  margin-top: 16px;
  border: 1px solid #bbf7d0;
  border-radius: 13px;
  text-align: left;
}
.pickup-guide__header {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px;
  background: #f0fdf4;
}
.pickup-guide__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #15803d;
  color: #fff;
}
.pickup-guide__header div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pickup-guide__header small {
  color: #6b7280;
  font-size: 9px;
  font-weight: 700;
}
.pickup-guide__header h2 {
  margin: 0;
  font-size: 14px;
}
.pickup-guide__badge {
  margin-left: auto;
  padding: 4px 8px;
  border-radius: 999px;
  background: #dcfce7;
  color: #15803d;
  font-size: 10px;
  font-weight: 800;
}
.pickup-guide__address,
.pickup-guide__notice {
  display: flex;
  gap: 9px;
  padding: 13px 15px;
  color: #374151;
  font-size: 12px;
  line-height: 1.55;
}
.pickup-guide__address i {
  margin-top: 3px;
  color: #dc2626;
}
.pickup-guide__notice {
  margin: 0 14px 14px;
  border-radius: 9px;
  background: #fffbeb;
  color: #92400e;
}
.order-quick-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
}
.order-quick-info div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 10px;
  background: #f9fafb;
  text-align: left;
}
.order-quick-info span {
  color: #6b7280;
  font-size: 10px;
}
.order-quick-info strong {
  font-size: 12px;
}
.order-quick-info .amount {
  color: #dc2626;
  font-size: 15px;
}
.status-card__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}
.btn-home,
.btn-orders {
  padding: 11px 18px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}
.btn-orders {
  border-color: #dc2626;
  background: #dc2626;
  color: #fff;
}
@media (max-width: 560px) {
  .status-card {
    padding: 26px 18px;
  }
  .status-card h1 {
    font-size: 22px;
  }
  .status-card__actions {
    flex-direction: column;
  }
}
</style>
