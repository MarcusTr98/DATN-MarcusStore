<template>
  <div v-if="items.length > 0 || loading" class="pd-suggest">
    <h3 class="pd-suggest__title">Phụ kiện đi kèm</h3>

    <div v-if="loading" class="pd-suggest__loading">
      <i class="ti ti-loader-2 pd-suggest__spin" aria-hidden="true" />
      <span>Đang tải gợi ý...</span>
    </div>

    <div v-else class="pd-suggest__grid">
      <router-link
        v-for="item in items"
        :key="item.productId"
        :to="`/product/${item.slug}`"
        class="pd-suggest__card"
      >
        <div class="pd-suggest__img-wrap">
          <span v-if="item.discountPercent > 0" class="pd-suggest__badge">
            -{{ item.discountPercent }}%
          </span>
          <img :src="item.thumbnailUrl" :alt="item.productName" class="pd-suggest__img" />
        </div>
        <div class="pd-suggest__body">
          <div class="pd-suggest__name">{{ item.productName }}</div>
          <div class="pd-suggest__price-row">
            <span class="pd-suggest__price">{{ formatPrice(item.price) }}</span>
            <span
              v-if="item.originalPrice && item.originalPrice > item.price"
              class="pd-suggest__original-price"
            >
              {{ formatPrice(item.originalPrice) }}
            </span>
          </div>
          <div v-if="item.reviewCount > 0" class="pd-suggest__rating">
            <i class="ti ti-star-filled" aria-hidden="true" />
            {{ item.rating.toFixed(1) }} ({{ item.reviewCount }})
          </div>
          <div v-if="!item.inStock" class="pd-suggest__out-stock">Hết hàng</div>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

const props = defineProps({
  categoryId: { type: Number, default: 6 },
  limit: { type: Number, default: 4 },
})

const items = ref([])
const loading = ref(false)

async function fetchSuggested() {
  loading.value = true
  try {
    const res = await api.get('/client/products/suggested', {
      params: { categoryId: props.categoryId, limit: props.limit },
    })
    items.value = res.data?.data ?? []
  } catch (err) {
    console.error('Lỗi tải sản phẩm gợi ý:', err)
    items.value = []
  } finally {
    loading.value = false
  }
}

function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

onMounted(fetchSuggested)
</script>

<style scoped>
.pd-suggest {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 20px 24px;
}
.pd-suggest__title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.pd-suggest__loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 14px;
  padding: 20px 0;
}
.pd-suggest__spin {
  animation: pd-suggest-spin 1s linear infinite;
}
@keyframes pd-suggest-spin {
  to { transform: rotate(360deg); }
}

.pd-suggest__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.pd-suggest__card {
  display: flex;
  flex-direction: column;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.pd-suggest__card:hover {
  border-color: #e11d1d;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.pd-suggest__img-wrap {
  position: relative;
  aspect-ratio: 1 / 1;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pd-suggest__img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
}
.pd-suggest__badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #e11d1d;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.pd-suggest__body {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pd-suggest__name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}
.pd-suggest__price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}
.pd-suggest__price {
  font-size: 14px;
  font-weight: 700;
  color: #e11d1d;
}
.pd-suggest__original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}
.pd-suggest__rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}
.pd-suggest__rating i {
  color: #ffb800;
  font-size: 12px;
}
.pd-suggest__out-stock {
  font-size: 12px;
  color: #999;
  font-weight: 600;
}
</style>