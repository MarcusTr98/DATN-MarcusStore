<template>
  <div class="container py-5">
    <div class="mb-4">
      <h4 class="fw-bold text-dark">
        <i class="fas fa-search me-2 text-danger"></i>Kết quả tìm kiếm cho:
        <span class="text-muted">"{{ searchQuery }}"</span>
      </h4>
      <p class="small text-muted">Tìm thấy 0 sản phẩm phù hợp</p>
    </div>

    <div class="text-center py-5 bg-white rounded-3 shadow-sm border">
      <i class="fas fa-box-open display-1 text-muted mb-3"></i>
      <h5 class="fw-bold text-secondary">Không tìm thấy sản phẩm nào</h5>
      <p class="text-muted small">Vui lòng thử lại với từ khóa khác hoặc kiểm tra lại chính tả.</p>
      <router-link
        to="/category/dien-thoai"
        class="btn btn-danger rounded-pill px-4 mt-2"
        style="background-color: #e11d1d"
      >
        Khám phá điện thoại hot
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const searchQuery = ref(route.query.q || '')

watch(
  () => route.query.q,
  (newQuery) => {
    searchQuery.value = newQuery || ''
    // Logic gọi API tìm kiếm sản phẩm theo keyword chèn tại đây
  },
)
</script>
