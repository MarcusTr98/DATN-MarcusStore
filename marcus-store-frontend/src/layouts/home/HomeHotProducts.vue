<template>
  <!-- ============ SẢN PHẨM HOT ============ -->
  <section class="featured py-5">
    <div class="section-head text-center mb-4">
      <span class="kicker">ĐƯỢC SĂN ĐÓN NHIỀU NHẤT</span>
      <h2 class="section-title">Sản phẩm HOT🔥🔥🔥</h2>
    </div>

    <div class="tab-bar mb-4">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Trạng thái đang tải -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-danger" role="status">
        <span class="visually-hidden">Đang tải...</span>
      </div>
    </div>

    <!-- Trạng thái lỗi -->
    <div v-else-if="error" class="text-center py-5 text-danger">
      {{ error }}
    </div>

    <!-- Trạng thái rỗng -->
    <div v-else-if="filteredProducts.length === 0" class="text-center py-5 text-muted">
      Chưa có sản phẩm nào trong danh mục này.
    </div>

    <!-- Danh sách sản phẩm -->
    <div v-else class="row g-4">
      <div class="col-6 col-md-4 col-lg-3" v-for="p in filteredProducts" :key="p.id">
        <router-link :to="`/product/${p.id}`" class="product-card static text-decoration-none">
          <div class="product-thumb" :style="{ background: p.color }">
            <img v-if="p.image" :src="p.image" :alt="p.name" class="product-img" />
            <i v-else :class="p.icon"></i>
          </div>
          <h6 class="product-name">{{ p.name }}</h6>
          <div class="product-rating">
            <i class="fas fa-star" v-for="n in 5" :key="n"></i>
            <span>({{ p.sold }} đã bán)</span>
          </div>
          <div class="product-price">
            <span class="price-now">{{ formatPrice(p.price) }}</span>
            <span v-if="p.oldPrice" class="price-old">{{ formatPrice(p.oldPrice) }}</span>
          </div>
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
// import axiosClient from '@/api/axiosClient' // TODO: import instance axios/service API sẵn có của dự án

/* ---------- Tabs lọc theo danh mục ---------- */
const tabs = [
  { key: 'all', label: 'Tất cả' },
  { key: 'phone', label: 'Điện thoại' },
  { key: 'tablet', label: 'iPad / Tablet' },
  { key: 'audio', label: 'Âm thanh' },
  { key: 'accessory', label: 'Phụ kiện' },
]
const activeTab = ref('all')

/* ---------- State gọi API ---------- */
const allProducts = ref([])
const loading = ref(false)
const error = ref(null)

const filteredProducts = computed(() =>
  activeTab.value === 'all'
    ? allProducts.value
    : allProducts.value.filter((p) => p.category === activeTab.value),
)

function formatPrice(v) {
  return v.toLocaleString('vi-VN') + 'đ'
}

/**
 * TODO (bạn phụ trách API):
 * Gọi API lấy danh sách "Sản phẩm HOT" tại đây, map dữ liệu trả về
 * cho đúng field đang được template sử dụng: id, name, price, oldPrice,
 * sold, category, icon (fallback khi không có ảnh), image, color.
 *
 * Gợi ý:
 * async function fetchHotProducts() {
 *   loading.value = true
 *   error.value = null
 *   try {
 *     const res = await axiosClient.get('/products/hot') // đổi endpoint cho đúng
 *     allProducts.value = res.data.map((item) => ({
 *       id: item.id,
 *       name: item.name,
 *       price: item.price,
 *       oldPrice: item.old_price,
 *       sold: item.sold_count,
 *       category: item.category_slug,
 *       image: item.thumbnail_url,
 *       color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
 *     }))
 *   } catch (err) {
 *     error.value = 'Không tải được sản phẩm HOT, vui lòng thử lại.'
 *   } finally {
 *     loading.value = false
 *   }
 * }
 *
 * Nếu API hỗ trợ lọc theo tab trên server, có thể watch(activeTab, fetchHotProducts)
 * thay vì lọc ở computed phía trên.
 */
async function fetchHotProducts() {
  // Xoá dữ liệu mẫu bên dưới khi đã nối API thật
  loading.value = true
  try {
    allProducts.value = [
      {
        id: 11,
        name: 'iPhone 17 Pro Max',
        price: 32990000,
        oldPrice: null,
        sold: 245,
        category: 'phone',
        icon: 'fas fa-mobile-alt',
        color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
      },
      {
        id: 12,
        name: 'Samsung Galaxy S26 Ultra',
        price: 26990000,
        oldPrice: 29990000,
        sold: 158,
        category: 'phone',
        icon: 'fas fa-mobile-alt',
        color: 'linear-gradient(135deg,#14151a,#3a3c46)',
      },
      {
        id: 13,
        name: 'Xiaomi 17',
        price: 15990000,
        oldPrice: 17490000,
        sold: 96,
        category: 'phone',
        icon: 'fas fa-mobile-alt',
        color: 'linear-gradient(135deg,#ffb627,#c98600)',
      },
      {
        id: 14,
        name: 'iPad Pro M4 11-inch',
        price: 26990000,
        oldPrice: null,
        sold: 74,
        category: 'tablet',
        icon: 'fas fa-tablet-alt',
        color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
      },
      {
        id: 15,
        name: 'iPad Air M3',
        price: 16990000,
        oldPrice: 18490000,
        sold: 112,
        category: 'tablet',
        icon: 'fas fa-tablet-alt',
        color: 'linear-gradient(135deg,#14151a,#3a3c46)',
      },
      {
        id: 16,
        name: 'Apple Watch Series 10',
        price: 10990000,
        oldPrice: null,
        sold: 88,
        category: 'accessory',
        icon: 'far fa-clock',
        color: 'linear-gradient(135deg,#ffb627,#c98600)',
      },
      {
        id: 17,
        name: 'AirPods Max',
        price: 12990000,
        oldPrice: 13990000,
        sold: 41,
        category: 'audio',
        icon: 'fas fa-headphones',
        color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
      },
      {
        id: 18,
        name: 'Ốp lưng MagSafe chính hãng',
        price: 690000,
        oldPrice: null,
        sold: 203,
        category: 'accessory',
        icon: 'fas fa-shield-alt',
        color: 'linear-gradient(135deg,#14151a,#3a3c46)',
      },
    ]
  } catch {
    error.value = 'Không tải được sản phẩm HOT, vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchHotProducts()
})
</script>

<style scoped>
.section-head .kicker {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--clr-red, #e1121c);
  margin-bottom: 0.5rem;
}

.section-title {
  font-weight: 700;
}

.tab-bar {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  justify-content: center;
}

.tab-btn {
  border: 1px solid #e5e2dd;
  background: #fff;
  color: var(--clr-muted, #6b7280);
  padding: 0.5rem 1.2rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  transition: all 0.2s ease;
}

.tab-btn.active,
.tab-btn:hover {
  background: var(--clr-red, #e1121c);
  border-color: var(--clr-red, #e1121c);
  color: #fff;
}

.product-card {
  position: relative;
  display: block;
  background: #fff;
  border-radius: 18px;
  padding: 1.25rem;
  transition: all 0.25s ease;
  color: inherit;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 14px 28px rgba(20, 21, 26, 0.08);
  color: inherit;
}

.product-card.static {
  width: 100%;
  background: var(--clr-surface-alt, #f6f5f3);
}

.product-thumb {
  width: 100%;
  height: 110px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 34px;
  margin-bottom: 0.9rem;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.product-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--clr-ink, #14151a);
  margin-bottom: 0.5rem;
  min-height: 2.4em;
}

.product-rating {
  color: var(--clr-amber, #ffb627);
  font-size: 0.72rem;
  margin-bottom: 0.5rem;
}

.product-rating span {
  color: var(--clr-muted, #6b7280);
  margin-left: 4px;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 0.6rem;
}

.price-now {
  font-weight: 700;
  color: var(--clr-red, #e1121c);
  font-size: 1rem;
}

.price-old {
  font-size: 0.78rem;
  color: var(--clr-muted, #6b7280);
  text-decoration: line-through;
}
</style>
