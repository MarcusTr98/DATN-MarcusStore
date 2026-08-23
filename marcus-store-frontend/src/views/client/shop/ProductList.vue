<template>
  <div class="container py-4 product-list-page">
    <!-- Breadcrumb -->
    <nav v-if="category" aria-label="breadcrumb" class="mb-3">
      <ol class="breadcrumb mb-0">
        <li class="breadcrumb-item">
          <router-link to="/" class="text-decoration-none">
            <i class="fas fa-home me-1"></i>Trang chủ
          </router-link>
        </li>
        <li class="breadcrumb-item active" aria-current="page">
          {{ category.categoryName }}
        </li>
      </ol>
    </nav>

    <!-- Loading state -->
    <div v-if="loading" class="text-center py-5 text-muted">
      <i class="fas fa-spinner fa-spin me-2"></i>Đang tải danh mục...
    </div>

    <!-- Error / not found -->
    <div v-else-if="error" class="text-center py-5">
      <i class="fas fa-exclamation-triangle text-warning fa-2x mb-3"></i>
      <h5 class="text-danger">{{ error }}</h5>
      <router-link to="/" class="btn btn-primary btn-sm mt-2">
        <i class="fas fa-arrow-left me-1"></i>Về trang chủ
      </router-link>
    </div>

    <!-- Content -->
    <ProductBlockSection
      v-else-if="category"
      :key="category.categoryId"
      :parent-category-id="category.categoryId"
      :parent-category-name="category.categoryName"
      :parent-category-slug="category.slug"
      mode="category"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'
import ProductBlockSection from '@/components/client/ProductBlockSection.vue'

const route = useRoute()

const category = ref(null)
const loading = ref(true)
const error = ref(null)

async function resolveCategory(slug) {
  if (!slug) {
    category.value = null
    loading.value = false
    error.value = null
    return
  }
  loading.value = true
  error.value = null
  category.value = null
  try {
    const { data } = await api.get(`/client/categories/slug/${encodeURIComponent(slug)}`)
    category.value = data ?? null
    if (!category.value) {
      error.value = 'Danh mục không tồn tại hoặc đã bị ẩn.'
    }
  } catch (err) {
    console.error('[ProductList] Lỗi khi lookup category theo slug:', err)
    if (err?.response?.status === 404) {
      error.value = 'Danh mục không tồn tại hoặc đã bị ẩn.'
    } else {
      error.value = 'Không thể tải danh mục, vui lòng thử lại.'
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.slug,
  (slug) => {
    // Scroll lên đầu trang khi chuyển sang danh mục khác
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
    resolveCategory(slug)
  },
  { immediate: true },
)
</script>

<style scoped>
.product-list-page {
  min-height: 60vh;
}

.breadcrumb {
  background: transparent;
  padding: 0;
  font-size: 14px;
}
.breadcrumb-item a {
  color: var(--cps-dark, #222);
}
.breadcrumb-item.active {
  color: var(--cps-red, #d70018);
  font-weight: 600;
}
</style>