<template>
  <div class="wishlist-page">
    <h4 class="fw-bold text-dark mb-4">
      <i class="far fa-heart me-2 text-danger"></i>
      Sản phẩm yêu thích cá nhân
      <span v-if="totalElements > 0" class="badge bg-danger ms-2">
        {{ totalElements }}
      </span>
    </h4>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-5 bg-white rounded-3 shadow-sm border">
      <i class="fa-solid fa-spinner fa-spin-pulse display-6 text-muted mb-3 d-block"></i>
      <p class="text-muted mb-0">Đang tải danh sách yêu thích...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="alert alert-danger">
      {{ error }}
      <button class="btn btn-sm btn-outline-danger ms-2" @click="loadPage(0)">Thử lại</button>
    </div>

    <!-- Empty state -->
    <div
      v-else-if="items.length === 0"
      class="text-center py-5 bg-white rounded-3 shadow-sm border"
    >
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

    <!-- Grid sản phẩm -->
    <div v-else>
      <div class="row g-3">
        <div
          v-for="item in items"
          :key="item.wishlistId"
          class="col-6 col-md-3 col-lg-3 col-xl-3"
        >
          <div class="product-card position-relative">
            <!-- Badge discount -->
            <div v-if="item.discountPercent > 0" class="badge-discount">
              Giảm {{ item.discountPercent }}%
            </div>

            <!-- Nút action: xóa wishlist + thêm giỏ hàng -->
            <div class="card-actions">
              <button
                type="button"
                class="icon-btn wishlist-btn active"
                :class="{ loading: removingId === item.productId }"
                title="Bỏ yêu thích"
                @click.stop.prevent="handleRemove(item)"
              >
                <i v-if="removingId === item.productId" class="fa-solid fa-spinner fa-spin action-icon"></i>
                <svg v-else viewBox="0 0 24 24" class="action-icon heart-icon">
                  <path
                    d="M12 21s-6.7-4.35-9.3-8.1C0.9 9.9 1.6 6.4 4.6 4.9c2-1 4.4-0.5 5.9 1.2L12 7.6l1.5-1.5c1.5-1.7 3.9-2.2 5.9-1.2 3 1.5 3.7 5 1.9 8-2.6 3.75-9.3 8.1-9.3 8.1z"
                  />
                </svg>
              </button>

              <button
                type="button"
                class="icon-btn cart-btn"
                title="Thêm vào giỏ hàng"
                @click.stop.prevent="addToCart(item)"
              >
                <svg viewBox="0 0 24 24" class="action-icon cart-icon">
                  <circle cx="9" cy="21" r="1" />
                  <circle cx="19" cy="21" r="1" />
                  <path d="M2.5 3h2l2.6 12.4a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 2-1.6L21.5 7H6" />
                </svg>
              </button>
            </div>

            <router-link :to="`/product/${item.slug}`" class="card-link">
              <div class="card-thumbnail">
                <img :src="item.thumbnailUrl" :alt="item.productName" loading="lazy" />
              </div>

              <h3 class="card-title">{{ item.productName }}</h3>

              <div class="card-price">
                <span class="price-sale">{{ formatPrice(item.price) }}</span>
                <span
                  v-if="item.originalPrice && item.originalPrice > item.price"
                  class="price-original"
                >
                  {{ formatPrice(item.originalPrice) }}
                </span>
              </div>

              <div v-if="item.specs?.length" class="card-specs">
                <span v-for="(spec, idx) in item.specs" :key="idx" class="spec-chip">
                  {{ spec }}
                </span>
              </div>

              <div class="card-footer-row">
                <div class="card-rating">
                  <span class="star">★</span>
                  <span>{{ item.rating || '5.0' }}</span>
                </div>
                <span class="added-at" title="Ngày thêm">
                  <i class="far fa-clock me-1"></i>{{ formatDate(item.createdAt) }}
                </span>
              </div>
            </router-link>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <nav v-if="totalPages > 1" class="mt-4 d-flex justify-content-center">
        <ul class="pagination">
          <li :class="['page-item', { disabled: currentPage === 0 }]">
            <button class="page-link" @click="loadPage(currentPage - 1)">
              <i class="fa-solid fa-chevron-left"></i>
            </button>
          </li>
          <li
            v-for="p in pageNumbers"
            :key="p"
            :class="['page-item', { active: p === currentPage }]"
          >
            <button class="page-link" @click="loadPage(p)">{{ p + 1 }}</button>
          </li>
          <li
            :class="['page-item', { disabled: currentPage >= totalPages - 1 }]"
          >
            <button class="page-link" @click="loadPage(currentPage + 1)">
              <i class="fa-solid fa-chevron-right"></i>
            </button>
          </li>
        </ul>
      </nav>
    </div>

    <!-- Modal thông báo -->
    <BaseModal
      :visible="notifyModal.visible"
      :type="notifyModal.type"
      :title="notifyModal.title"
      :message="notifyModal.message"
      @close="notifyModal.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '@/utils/api'
import wishlist from '@/composables/useWishlistShared'
import { useCartStore } from '@/stores/cartStore'
import BaseModal from '@/components/BaseModal.vue'

// ---- Local state ----
const items = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 12
const loading = ref(false)
const error = ref(null)
const removingId = ref(null)

const cartStore = useCartStore()

const notifyModal = reactive({
  visible: false,
  type: 'info',
  title: 'Thông báo',
  message: '',
})

function showNotify(type, title, message) {
  notifyModal.type = type
  notifyModal.title = title
  notifyModal.message = message
  notifyModal.visible = true
}

// ---- API: lấy danh sách wishlist phân trang ----
async function loadPage(page) {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/user/wishlist', {
      params: { page, size: pageSize },
    })
    const pageData = res.data?.data ?? {}
    items.value = pageData.content ?? []
    currentPage.value = pageData.number ?? 0
    totalPages.value = pageData.totalPages ?? 0
    totalElements.value = pageData.totalElements ?? 0
  } catch (err) {
    console.error('[wishlist] Lỗi tải danh sách:', err)
    error.value = 'Không thể tải danh sách yêu thích'
    items.value = []
  } finally {
    loading.value = false
  }
}

// ---- Xóa 1 item khỏi wishlist ----
async function handleRemove(item) {
  removingId.value = item.productId
  const result = await wishlist.remove(item.productId)
  removingId.value = null
  if (result.success) {
    items.value = items.value.filter((it) => it.productId !== item.productId)
    totalElements.value = Math.max(0, totalElements.value - 1)
    // Nếu trang hiện tại rỗng mà còn trang trước → lùi về
    if (items.value.length === 0 && currentPage.value > 0) {
      loadPage(currentPage.value - 1)
    } else if (items.value.length === 0) {
      // Trang cuối cùng rỗng → tải lại trang 0
      loadPage(0)
    }
  } else {
    showNotify('error', 'Xóa thất bại', result.message)
  }
}

// ---- Thêm vào giỏ hàng ----
async function addToCart(item) {
  if (!item.skuId) {
    showNotify('error', 'Lỗi', 'Sản phẩm chưa có SKU khả dụng')
    return
  }
  const ok = await cartStore.addToCart(item.skuId, 1)
  if (ok) {
    showNotify('success', 'Đã thêm vào giỏ', `"${item.productName}" đã được thêm vào giỏ hàng`)
  } else {
    showNotify('error', 'Lỗi', cartStore.error || 'Không thể thêm vào giỏ hàng')
  }
}

// ---- Pagination: hiển thị tối đa 5 trang quanh current ----
const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  const pages = []
  const start = Math.max(0, cur - 2)
  const end = Math.min(total, start + 5)
  for (let i = start; i < end; i++) pages.push(i)
  return pages
})

// ---- Utils ----
function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN').format(value) + 'đ'
}

function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: '2-digit' })
}

onMounted(() => {
  loadPage(0)
})
</script>

<style scoped>
.wishlist-page {
  font-family: var(--font-body);
}

/* ============ Product Card (đồng bộ ProductCard.vue) ============ */
.product-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 12px;
  height: 100%;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.product-card:hover {
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.card-link {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  color: inherit;
  flex: 1;
}

/* Badge discount */
.badge-discount {
  position: absolute;
  top: 10px;
  left: 0;
  background: #d70018;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  z-index: 2;
}

/* Card actions (wishlist + cart) */
.card-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 3;
}
.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eee;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
  color: #999;
  transition: all 0.15s ease;
}
.icon-btn:hover {
  border-color: #d70018;
  color: #d70018;
}
.icon-btn.wishlist-btn.active {
  background: #d70018;
  border-color: #d70018;
  color: #fff;
}
.icon-btn.wishlist-btn.active:hover {
  background: #b50015;
  border-color: #b50015;
}
.icon-btn.loading {
  opacity: 0.6;
  cursor: default;
}
.action-icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

/* Thumbnail */
.card-thumbnail {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 180px;
  margin-bottom: 10px;
}
.card-thumbnail img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
}

/* Title */
.card-title {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin: 0 0 8px;
  color: #222;
}

/* Price */
.card-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}
.price-sale {
  color: #d70018;
  font-size: 16px;
  font-weight: 700;
}
.price-original {
  color: #999;
  font-size: 13px;
  text-decoration: line-through;
}

/* Specs */
.card-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
  min-height: 22px;
}
.spec-chip {
  font-size: 11px;
  background: #f3f4f6;
  color: #4b5563;
  padding: 2px 8px;
  border-radius: 4px;
}

/* Footer row: rating + date */
.card-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  font-size: 12px;
  padding-top: 8px;
  border-top: 1px dashed #eee;
}
.card-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f59e0b;
  font-weight: 600;
}
.card-rating .star {
  color: #f59e0b;
}
.added-at {
  color: #888;
}

/* ============ Pagination ============ */
.page-link {
  color: #d70018;
}
.page-item.active .page-link {
  background-color: #d70018;
  border-color: #d70018;
  color: #fff;
}
</style>