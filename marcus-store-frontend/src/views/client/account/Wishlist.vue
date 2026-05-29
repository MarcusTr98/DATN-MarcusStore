<template>
  <div class="container py-5">
    <h4 class="fw-bold text-dark mb-4">
      <i class="far fa-heart me-2 text-danger"></i>Sản phẩm yêu thích cá nhân
    </h4>

    <div class="text-center py-5 bg-white rounded-3 shadow-sm border">
      <i class="far fa-heart display-1 text-muted mb-3 d-block"></i>
      <h5 class="fw-bold text-secondary">Danh sách yêu thích trống</h5>
      <p class="text-muted small">
        Hãy click vào biểu tượng trái tim ở sản phẩm để lưu lại tại đây nhé.
      </p>
      <router-link
        to="/"
        class="btn btn-danger rounded-pill px-4 mt-2"
        style="background-color: #e11d1d"
      >
        Quay lại trang chủ mua sắm
      </router-link>
    </div>
  </div>
</template>
