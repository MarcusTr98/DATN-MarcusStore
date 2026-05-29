<template>
  <div class="container py-5">
    <div class="row g-4">
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm p-4 rounded-3 bg-white">
          <h5 class="fw-bold text-dark mb-4 border-bottom pb-2">
            <i class="fas fa-truck me-2 text-danger"></i>Thông tin nhận hàng
          </h5>
          <form @submit.prevent="handleCheckout">
            <div class="mb-3">
              <label class="form-label small fw-semibold">Họ và tên người nhận</label>
              <input
                type="text"
                class="form-control"
                placeholder="Nhập đầy đủ họ tên..."
                required
              />
            </div>
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Số điện thoại</label>
                <input
                  type="tel"
                  class="form-control"
                  placeholder="Nhập số điện thoại..."
                  required
                />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Địa chỉ Email</label>
                <input
                  type="email"
                  class="form-control"
                  placeholder="Nhập email nhận hóa đơn..."
                  required
                />
              </div>
            </div>
            <div class="mb-4">
              <label class="form-label small fw-semibold">Địa chỉ giao hàng chính xác</label>
              <textarea
                class="form-control"
                rows="3"
                placeholder="Số nhà, tên đường, phường/xã, quận/huyện..."
                required
              ></textarea>
            </div>

            <h5 class="fw-bold text-dark mb-3 border-bottom pb-2">
              <i class="fas fa-credit-card me-2 text-danger"></i>Phương thức thanh toán
            </h5>
            <div class="d-flex flex-column gap-2 mb-4">
              <label
                class="border rounded-3 p-3 d-flex align-items-center justify-content-between cursor-pointer"
              >
                <div class="d-flex align-items-center gap-3">
                  <input type="radio" name="payment" value="COD" checked />
                  <div>
                    <span class="fw-bold d-block small">Thanh toán khi nhận hàng (COD)</span>
                    <span class="text-muted extra-small"
                      >Thanh toán tiền mặt khi giao hàng tận nơi</span
                    >
                  </div>
                </div>
                <i class="fas fa-money-bill-wave text-success fs-4"></i>
              </label>
              <label
                class="border rounded-3 p-3 d-flex align-items-center justify-content-between cursor-pointer"
              >
                <div class="d-flex align-items-center gap-3">
                  <input type="radio" name="payment" value="VNPAY" />
                  <div>
                    <span class="fw-bold d-block small">Thanh toán qua VNPAY</span>
                    <span class="text-muted extra-small"
                      >Chuyển khoản hoặc quét mã QR nhanh chóng</span
                    >
                  </div>
                </div>
                <i class="fas fa-qrcode text-primary fs-4"></i>
              </label>
            </div>
          </form>
        </div>
      </div>

      <div class="col-lg-5">
        <div class="card border-0 shadow-sm p-4 rounded-3 bg-white sticky-top" style="top: 100px">
          <h5 class="fw-bold text-dark mb-3 border-bottom pb-2">Đơn hàng của bạn</h5>

          <div class="input-group mb-4">
            <input
              type="text"
              class="form-control border-end-0"
              placeholder="Nhập mã giảm giá (Voucher)..."
            />
            <button class="btn btn-outline-danger border-start-0" type="button">Áp dụng</button>
          </div>

          <div class="d-flex flex-column gap-2 mb-4 border-bottom pb-3 text-secondary small">
            <div class="d-flex justify-content-between">
              <span>Tạm tính sản phẩm:</span>
              <span class="fw-semibold text-dark">0 ₫</span>
            </div>
            <div class="d-flex justify-content-between">
              <span>Giảm giá Voucher:</span>
              <span class="fw-semibold text-success">-0 ₫</span>
            </div>
            <div class="d-flex justify-content-between">
              <span>Phí vận chuyển:</span>
              <span class="fw-semibold text-dark">Miễn phí</span>
            </div>
          </div>

          <div class="d-flex justify-content-between align-items-center mb-4">
            <span class="fw-bold text-dark">Tổng tiền thực thanh toán:</span>
            <span class="fw-bold fs-4 text-danger">0 ₫</span>
          </div>

          <button
            class="btn btn-danger w-100 py-3 rounded-pill fw-bold text-uppercase shadow-sm"
            style="background-color: #e11d1d"
          >
            Xác nhận đặt hàng ngay
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const handleCheckout = () => {
  // Logic gọi API POST /api/orders chèn tại đây
}
</script>

<style scoped>
.extra-small {
  font-size: 11px;
}
.cursor-pointer {
  cursor: pointer;
}
</style>
