<template>
  <div class="success-page py-5">
    <div class="container d-flex justify-content-center">
      <div
        v-if="paymentStatus === 'SUCCESS'"
        class="status-card text-center p-5 bg-white rounded-4 shadow-sm"
        style="max-width: 500px; width: 100%"
      >
        <div class="icon-wrap mb-4">
          <i class="fas fa-check-circle text-success" style="font-size: 72px"></i>
        </div>
        <h2 class="fw-bold text-success mb-3">Đặt hàng thành công!</h2>
        <p class="text-muted mb-2">
          Cảm ơn bạn đã mua sắm tại Marcus Store. Đơn hàng của bạn đã được hệ thống ghi nhận.
        </p>

        <div class="p-3 mb-4 mt-3 bg-light rounded-3 border">
          <span class="text-muted d-block mb-1" style="font-size: 13px">Mã đơn hàng của bạn:</span>
          <span class="fw-bold fs-4 text-dark">{{ orderCode }}</span>
        </div>

        <div class="d-flex justify-content-center gap-3">
          <router-link to="/" class="btn btn-outline-secondary rounded-pill px-4"
            >Trang chủ</router-link
          >
          <router-link to="/profile/orders" class="btn btn-danger rounded-pill px-4"
            >Xem đơn hàng</router-link
          >
        </div>
      </div>

      <div
        v-else
        class="status-card text-center p-5 bg-white rounded-4 shadow-sm"
        style="max-width: 500px; width: 100%"
      >
        <div class="icon-wrap mb-4">
          <i class="fas fa-exclamation-triangle text-warning" style="font-size: 72px"></i>
        </div>
        <h2 class="fw-bold text-danger mb-3">Chưa hoàn tất thanh toán</h2>
        <p class="text-muted mb-2">
          Giao dịch trực tuyến bị hủy hoặc xảy ra lỗi. Đơn hàng của bạn đã được tạo nhưng đang ở
          trạng thái <strong>Chờ thanh toán</strong>.
        </p>

        <div class="p-3 mb-4 mt-3 bg-light rounded-3 border border-warning border-opacity-25">
          <span class="text-muted d-block mb-1" style="font-size: 13px">Mã đơn hàng:</span>
          <span class="fw-bold fs-4 text-danger">{{ orderCode }}</span>
        </div>

        <div class="d-flex justify-content-center gap-3">
          <router-link to="/" class="btn btn-outline-secondary rounded-pill px-4"
            >Trang chủ</router-link
          >
          <router-link
            to="/account/orders"
            class="btn btn-warning text-dark fw-bold rounded-pill px-4"
            >Xem đơn chờ thanh toán</router-link
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const paymentStatus = ref('SUCCESS')
const orderCode = ref('Đang cập nhật')

onMounted(() => {
  // 1. Lấy tất cả params có trên URL
  const vnpResponseCode = route.query.vnp_ResponseCode
  const vnpTxnRef = route.query.vnp_TxnRef // Mã đơn do VNPAY trả về
  const regularOrderCode = route.query.orderCode // Mã đơn do COD/PayOS truyền sang

  // 2. Gán Mã Đơn Hàng (Ưu tiên VNPAY trước, nếu không có thì lấy mã thường)
  if (vnpTxnRef || regularOrderCode) {
    orderCode.value = vnpTxnRef || regularOrderCode
  }

  // 3. Xử lý logic Đỏ/Xanh
  if (vnpResponseCode) {
    // Nếu có mã của VNPAY trên URL
    if (vnpResponseCode === '00') {
      paymentStatus.value = 'SUCCESS' // 00: Khách đã trả tiền
    } else {
      paymentStatus.value = 'FAILED' // Khách bấm Hủy hoặc lỗi thẻ
    }
  } else {
    // Nếu không có mã VNPAY, nghĩa là khách thanh toán COD hoặc PayOS => Vào thẳng màn SUCCESS
    paymentStatus.value = 'SUCCESS'
  }
})
</script>

<style scoped>
.success-page {
  min-height: 80vh;
  background-color: #f3f4f6;
  display: flex;
  align-items: center;
}
.status-card {
  border: 1px solid #e5e7eb;
}
</style>
