<template>
  <div class="brand-filter-block mb-4">
    <h4 class="brand-title">{{ parentCategoryName }}</h4>

    <div v-if="loading" class="text-center py-4 text-muted small">Đang tải danh mục...</div>

    <div v-else-if="brands.length" class="brand-grid">
      <button
        v-for="brand in brands"
        :key="brand.categoryId"
        type="button"
        class="brand-item"
        :class="{ active: selectedId === brand.categoryId }"
        @click="onSelect(brand)"
      >
        <img :src="brand.categoryImg" :alt="brand.categoryName" class="brand-logo" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

const props = defineProps({
  // categoryId của danh mục cha cố định, VD: id của "Điện thoại"
  parentCategoryId: {
    type: Number,
    required: true,
  },
  parentCategoryName: {
    type: String,
    default: 'Điện thoại',
  },
})

// emit ra ngoài: null = bấm lại để bỏ chọn (xem tất cả), object brand = đã chọn
const emit = defineEmits(['select'])

const brands = ref([])
const loading = ref(false)
const selectedId = ref(null)

async function fetchBrands() {
  loading.value = true
  try {
    const res = await api.get(`/client/categories/${props.parentCategoryId}/children`)
    brands.value = res.data?.data ?? []
  } catch (err) {
    console.error('Lỗi khi tải danh sách hãng:', err)
    brands.value = []
  } finally {
    loading.value = false
  }
}

function onSelect(brand) {
  // Bấm lại logo đang chọn -> bỏ chọn, xem lại tất cả sản phẩm trong category cha
  if (selectedId.value === brand.categoryId) {
    selectedId.value = null
    emit('select', null)
    return
  }
  selectedId.value = brand.categoryId
  emit('select', brand)
}

onMounted(fetchBrands)
</script>

<style scoped>
.brand-filter-block {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}

.brand-title {
  font-size: 1.05rem;
  font-weight: 800;
  margin: 0 0 14px;
  color: var(--cps-dark, #222);
}

.brand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
}

.brand-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 46px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.brand-item:hover {
  border-color: #bbb;
}
.brand-item.active {
  border-color: #d70018;
  box-shadow: 0 0 0 1px #d70018 inset;
}

.brand-logo {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>