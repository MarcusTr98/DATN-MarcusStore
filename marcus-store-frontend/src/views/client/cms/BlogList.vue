<template>
  <div class="container py-5">
    <h4 class="fw-bold text-dark mb-4">
      <i class="far fa-newspaper me-2 text-danger"></i>Tin tức công nghệ mới
    </h4>
    <div class="row g-4">
      <div class="col-md-4">
        <div class="card border-0 shadow-sm h-100 rounded-3 overflow-hidden bg-white">
          <div class="bg-secondary text-white text-center py-5 small" style="height: 180px">
            Ảnh bài viết hiển thị
          </div>
          <div class="card-body p-3">
            <h6 class="fw-bold text-dark line-clamp-2">
              Đánh giá chi tiết hiệu năng iPhone 16 Pro Max thực tế
            </h6>
            <p class="text-muted small line-clamp-3 mb-3">
              Phân tích trải nghiệm chip xử lý đồ họa thế hệ mới kết hợp cùng ma trận AI lõi cứng
              trên hệ sinh thái iOS.
            </p>
            <router-link
              to="/blog/detail-slug"
              class="btn btn-light btn-sm text-danger w-100 fw-bold border"
            >
              Đọc chi tiết bài viết
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
