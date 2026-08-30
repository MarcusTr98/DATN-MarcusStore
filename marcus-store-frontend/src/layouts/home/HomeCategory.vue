<template>
  <div class="home-category-blocks">
    <div v-if="loading" class="text-center py-5 text-muted">Đang tải danh mục...</div>

    <div v-else-if="mainCategories.length === 0" class="text-center py-5 text-muted">
      Chưa có danh mục nào có sản phẩm.
    </div>

    <!-- Mỗi category cha là 1 block độc lập -->
    <div
      v-for="cate in mainCategories"
      :key="cate.categoryId"
      class="category-block"
    >
      <ProductBlockSection
        :parent-category-id="cate.categoryId"
        :parent-category-name="cate.categoryName"
        :parent-category-slug="cate.slug"
        mode="standalone"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import ProductBlockSection from '@/components/client/ProductBlockSection.vue'

const mainCategories = ref([])
const loading = ref(false)

async function fetchMainCategories() {
  loading.value = true
  try {
    const res = await api.get('/client/categories/main')
    mainCategories.value = res.data?.data ?? []
  } catch (err) {
    console.error('Lỗi khi tải danh mục chính trang Home:', err)
    mainCategories.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchMainCategories)
</script>

<style scoped>
.category-block {
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.category-block:last-child {
  border-bottom: none;
}
</style>
