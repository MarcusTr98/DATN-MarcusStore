<template>
  <div class="home-category-blocks">
    <div v-if="loading" class="text-center py-5 text-muted">Đang tải danh mục...</div>

    <div v-else-if="mainCategories.length === 0" class="text-center py-5 text-muted">
      Chưa có danh mục nào có sản phẩm.
    </div>

    <!-- Mỗi category cha là 1 block độc lập: tự có BrandFilterBar + ProductListSection riêng -->
    <div
      v-for="cate in mainCategories"
      :key="cate.categoryId"
      class="category-block mb-5"
    >
      <BrandFilterBar
        :parent-category-id="cate.categoryId"
        :parent-category-name="cate.categoryName"
        @select="(brand) => onBrandSelect(cate.categoryId, brand)"
      />

      <ProductListSection
        :ref="(el) => setProductListRef(cate.categoryId, el)"
        :parent-category-name="cate.categoryName"
        :fixed-category-id="cate.categoryId"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import BrandFilterBar from '@/layouts/home/BrandFilterBar.vue'
import ProductListSection from '@/components/client/ProductCard.vue'

const mainCategories = ref([])
const loading = ref(false)
const productListRefs = ref({})

function setProductListRef(categoryId, el) {
  if (el) productListRefs.value[categoryId] = el
}

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

function onBrandSelect(parentCategoryId, brand) {
  const listRef = productListRefs.value[parentCategoryId]
  if (listRef) listRef.filterByCategory(brand)
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