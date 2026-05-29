<template>
  <div class="container py-5 text-center">
    <div class="row justify-content-center">
      <div class="col-md-6 bg-white p-5 rounded-3 shadow-sm border">
        <div class="success-icon-wrapper mb-4">
          <i class="fas fa-check-circle display-1 text-success"></i>
        </div>
        <h3 class="fw-bold text-dark mb-2">Đặt hàng thành công!</h3>
        <p class="text-muted small mb-4">
          Cảm ơn bạn đã tin tưởng Marcus Store. Mã đơn hàng của bạn là
          <strong class="text-danger">#MS-{{ orderCode }}</strong
          >. Nhân viên sẽ liên hệ xác nhận sớm nhất.
        </p>
        <div class="d-flex gap-3 justify-content-center">
          <router-link to="/my-orders" class="btn btn-outline-secondary rounded-pill px-4">
            Xem lịch sử mua
          </router-link>
          <router-link
            to="/"
            class="btn btn-danger rounded-pill px-4"
            style="background-color: #e11d1d"
          >
            Tiếp tục mua sắm
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const orderCode = ref(Math.floor(100000 + Math.random() * 900000))
</script>
