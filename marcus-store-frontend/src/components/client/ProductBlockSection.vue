<template>
  <div class="product-block mb-5">
    <!-- Header: tiêu đề (đổi theo brand đang chọn) + Sắp xếp theo -->
    <div class="block-header d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center gap-2">
        <h4 class="block-title mb-0">{{ sectionTitle }}</h4>
        <button
          v-if="selectedBrandName"
          type="button"
          class="reset-btn"
          title="Bỏ chọn hãng, xem tất cả"
          @click="resetBrand"
        >
          ✕
        </button>
        <button
          v-if="getActiveFilterCount() > 0"
          type="button"
          class="reset-btn"
          title="Xóa hết lọc, về danh sách ban đầu"
          @click="resetFilterAndBrand"
        >
          ✕
        </button>
      </div>

      <div class="sort-options">
        <button
          v-for="opt in sortOptions"
          :key="opt.value"
          type="button"
          class="sort-btn"
          :class="{ active: sortBy === opt.value }"
          @click="onSortChange(opt.value)"
        >
          <span class="sort-icon">{{ opt.icon }}</span>
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- Brand bar: ẩn logo brand khi filter active; ẩn nút Lọc khi đang chọn brand -->
    <div class="brand-grid mb-3">
      <button
        v-if="selectedBrandId == null"
        type="button"
        class="filter-btn"
        @click="openFilter"
      >
        <i class="fa-solid fa-sliders"></i>
        Lọc
        <span v-if="getActiveFilterCount() > 0" class="filter-badge">
          {{ getActiveFilterCount() }}
        </span>
      </button>

      <template v-if="getActiveFilterCount() === 0">
        <button
          v-for="brand in brands"
          :key="brand.categoryId"
          type="button"
          class="brand-item"
          :class="{ active: selectedBrandId === brand.categoryId }"
          @click="onBrandClick(brand)"
        >
          <img :src="brand.categoryImg" :alt="brand.categoryName" class="brand-logo" />
        </button>
      </template>
    </div>

    <!-- Loading state -->
    <div v-if="loadingProducts" class="text-center py-5 text-muted">
      Đang tải sản phẩm...
    </div>

    <!-- Error state -->
    <div v-else-if="productError" class="text-center py-5 text-danger">
      {{ productError }}
    </div>

    <!-- Empty state -->
    <div v-else-if="products.length === 0" class="text-center py-5 text-muted">
      Chưa có sản phẩm nào.
    </div>

    <!-- Grid sản phẩm -->
    <template v-else>
      <div class="row g-3">
        <div
          v-for="product in products"
          :key="product.productId"
          class="col-6 col-md-3 col-lg-3 col-xl-3"
        >
          <router-link :to="`/product/${product.slug}`" class="product-card">
            <div v-if="product.discountPercent > 0" class="badge-discount">
              Giảm {{ product.discountPercent }}%
            </div>

            <div class="card-actions">
              <button
                type="button"
                class="icon-btn wishlist-btn"
                :class="{
                  active: isWished(product.productId),
                  loading: togglingIds.has(product.productId),
                }"
                title="Yêu thích"
                @click.stop.prevent="toggleWishlist(product.productId)"
              >
                <svg viewBox="0 0 24 24" class="action-icon heart-icon">
                  <path
                    d="M12 21s-6.7-4.35-9.3-8.1C0.9 9.9 1.6 6.4 4.6 4.9c2-1 4.4-0.5 5.9 1.2L12 7.6l1.5-1.5c1.5-1.7 3.9-2.2 5.9-1.2 3 1.5 3.7 5 1.9 8-2.6 3.75-9.3 8.1-9.3 8.1z"
                  />
                </svg>
              </button>

              <button
                type="button"
                class="icon-btn cart-btn"
                title="Thêm vào giỏ hàng"
                @click.stop.prevent="addToCart(product)"
              >
                <svg viewBox="0 0 24 24" class="action-icon cart-icon">
                  <circle cx="9" cy="21" r="1" />
                  <circle cx="19" cy="21" r="1" />
                  <path d="M2.5 3h2l2.6 12.4a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 2-1.6L21.5 7H6" />
                </svg>
              </button>
            </div>

            <div class="card-thumbnail">
              <img :src="product.thumbnailUrl" :alt="product.productName" loading="lazy" />
            </div>

            <h3 class="card-title">{{ product.productName }}</h3>

            <div class="card-price">
              <span class="price-sale">{{ formatPrice(product.price) }}</span>
              <span v-if="product.originalPrice" class="price-original">
                {{ formatPrice(product.originalPrice) }}
              </span>
            </div>

            <div v-if="product.specs?.length" class="card-specs">
              <span v-for="(spec, idx) in product.specs" :key="idx" class="spec-chip">
                {{ spec }}
              </span>
            </div>

            <VoucherCard />

            <div class="card-footer-row">
              <div class="card-rating">
                <span class="star">★</span>
                <span>{{ product.rating }}</span>
              </div>
            </div>
          </router-link>
        </div>
      </div>

      <div v-if="totalElements > 0" class="load-more-wrap">
        <router-link
          v-if="mode === 'standalone'"
          :to="viewMoreLink"
          class="load-more-link"
        >
          Xem thêm {{ categoryName }}
          <i class="fa-solid fa-arrow-right"></i>
        </router-link>
        <button
          v-else-if="!loadingMore"
          type="button"
          class="load-more-btn"
          :disabled="products.length >= totalElements"
          @click="loadMore"
        >
          Xem thêm {{ remainingCount() }} sản phẩm
        </button>
        <span v-else class="load-more-loading">
          <i class="fa-solid fa-spinner fa-spin-pulse"></i>
          Đang tải...
        </span>
      </div>
    </template>
  </div>

  <!-- Notification Modal -->
  <BaseModal
    :visible="notifyModal.visible"
    :type="notifyModal.type"
    :title="notifyModal.title"
    :message="notifyModal.message"
    @close="notifyModal.visible = false"
  />
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import VoucherCard from '@/layouts/home/VoucherCard.vue'
import BaseModal from '@/components/BaseModal.vue'
import { useCartStore } from '@/stores/cartStore'
import wishlist from '@/composables/useWishlistShared'

const props = defineProps({
  parentCategoryId: { type: Number, required: true },
  parentCategoryName: { type: String, required: true },
  parentCategorySlug: { type: String, default: '' },
  mode: { type: String, default: 'standalone' }, // 'standalone' | 'category'
})

const emit = defineEmits(['open-filter'])

// ---- STATE ----
const brands = ref([])
const products = ref([])
const loadingProducts = ref(false)
const productError = ref(null)
const totalElements = ref(0)
const totalPages = ref(0)

const selectedBrandId = ref(null)
const selectedBrandName = ref(null)
const sectionTitle = ref('Sắp xếp theo')
const sortBy = ref('popular')
const page = ref(0)
const size = 8
const loadingMore = ref(false)

// Filter fields
const selectedMinPrice = ref(null)
const selectedMaxPrice = ref(null)
const selectedValueIds = ref([])
const selectedBrandIds = ref([])

const router = useRouter()
const cartStore = useCartStore()

// ---- SORT OPTIONS ----
const sortOptions = [
  { value: 'popular', label: 'Phổ biến', icon: '★' },
  { value: 'discount', label: 'Khuyến mãi HOT', icon: '⊗' },
  { value: 'price_asc', label: 'Giá Thấp - Cao', icon: '↑' },
  { value: 'price_desc', label: 'Giá Cao - Thấp', icon: '↓' },
]

const categoryName = computed(() => props.parentCategoryName)

const viewMoreLink = computed(() => `/category/${props.parentCategorySlug}`)

// ---- HELPERS ----
function getActiveFilterCount() {
  return (selectedMinPrice.value != null || selectedMaxPrice.value != null ? 1 : 0)
    + (selectedValueIds.value?.length ?? 0)
    + (selectedBrandIds.value?.length ?? 0)
}

function buildParams() {
  const allBrandIds = [
    ...(selectedBrandId.value != null ? [selectedBrandId.value] : []),
    ...(selectedBrandIds.value ?? []),
  ]
  const uniqueBrandIds = [...new Set(allBrandIds)]
  return {
    sortBy: sortBy.value,
    categoryId: selectedBrandId.value,
    parentCategoryId: props.parentCategoryId,
    page: page.value,
    size,
    minPrice: selectedMinPrice.value,
    maxPrice: selectedMaxPrice.value,
    valueIds: selectedValueIds.value?.length ? selectedValueIds.value.join(',') : null,
    brandIds: uniqueBrandIds.length ? uniqueBrandIds.join(',') : null,
  }
}

// ---- FETCH ----
async function fetchBrands() {
  try {
    const res = await api.get(`/client/categories/${props.parentCategoryId}/children`)
    brands.value = res.data?.data ?? []
  } catch (err) {
    console.error('[ProductBlockSection] Lỗi khi tải danh sách hãng:', err)
    brands.value = []
  }
}

function fetchProducts({ append = false } = {}) {
  loadingProducts.value = !append
  loadingMore.value = append
  productError.value = null
  return api
    .get('/home', { params: buildParams() })
    .then((res) => {
      const pageData = res.data?.data
      const list = pageData?.content ?? []
      if (append) {
        products.value.push(...list)
      } else {
        products.value = list
      }
      totalPages.value = pageData?.totalPages ?? 0
      totalElements.value = pageData?.totalElements ?? 0
    })
    .catch((err) => {
      console.error('[ProductBlockSection] Lỗi khi tải sản phẩm:', err)
      productError.value = 'Không thể tải sản phẩm, vui lòng thử lại.'
      if (append) page.value -= 1
    })
    .finally(() => {
      loadingProducts.value = false
      loadingMore.value = false
    })
}

// ---- ACTIONS ----
function onBrandClick(brand) {
  if (selectedBrandId.value === brand.categoryId) {
    resetBrand()
    return
  }
  selectedBrandId.value = brand.categoryId
  selectedBrandName.value = brand.categoryName
  sectionTitle.value = `${categoryName.value}: ${brand.categoryName}`
  page.value = 0
  fetchProducts()
}

function resetBrand() {
  selectedBrandId.value = null
  selectedBrandName.value = null
  sectionTitle.value = 'Sắp xếp theo'
  page.value = 0
  fetchProducts()
}

function resetFilterAndBrand() {
  selectedBrandIds.value = []
  selectedMinPrice.value = null
  selectedMaxPrice.value = null
  selectedValueIds.value = []
  selectedBrandId.value = null
  selectedBrandName.value = null
  sectionTitle.value = 'Sắp xếp theo'
  page.value = 0
  fetchProducts()
}

function onSortChange(newSortBy) {
  sortBy.value = newSortBy
  page.value = 0
  fetchProducts()
}

function loadMore() {
  if (loadingMore.value) return
  loadingMore.value = true
  page.value += 1
  fetchProducts({ append: true })
}

function remainingCount() {
  const remain = (totalElements.value ?? 0) - (products.value?.length ?? 0)
  return remain > 0 ? remain : 0
}

function openFilter() {
  emit('open-filter', {
    categoryId: props.parentCategoryId,
    selectedBrandIds: selectedBrandIds.value,
    selectedMinPrice: selectedMinPrice.value,
    selectedMaxPrice: selectedMaxPrice.value,
    selectedValueIds: selectedValueIds.value,
  })
}

function applyFilter({ brandIds, minPrice, maxPrice, valueIds }) {
  selectedBrandIds.value = brandIds
  selectedMinPrice.value = minPrice
  selectedMaxPrice.value = maxPrice
  selectedValueIds.value = valueIds
  page.value = 0
  fetchProducts()
}

const notifyModal = reactive({ visible: false, type: 'info', title: '', message: '' })

defineExpose({ applyFilter, notifyModal })

// ---- WISHLIST + CART ----
const togglingIds = ref(new Set())

function isLoggedIn() {
  return !!localStorage.getItem('ACCESS_TOKEN')
}

function isWished(productId) {
  return wishlist.isWished(productId)
}

async function toggleWishlist(productId) {
  if (!isLoggedIn()) return
  if (togglingIds.value.has(productId)) return
  togglingIds.value.add(productId)
  try {
    await wishlist.toggle(productId)
  } finally {
    togglingIds.value.delete(productId)
  }
}

function showNotify(type, title, message) {
  notifyModal.type = type
  notifyModal.title = title
  notifyModal.message = message
  notifyModal.visible = true
}

async function addToCart(product) {
  if (!isLoggedIn()) {
    showNotify('info', 'Đăng nhập', 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.')
    return
  }
  const skuId = product?.defaultSkuId ?? product?.skuId
  if (!skuId) {
    if (product?.slug) router.push(`/product/${product.slug}`)
    return
  }
  const ok = await cartStore.addToCart(skuId, 1)
  showNotify(
    ok ? 'success' : 'error',
    ok ? 'Thêm vào giỏ hàng' : 'Thêm thất bại',
    ok ? `Đã thêm "${product.productName}" vào giỏ hàng` : (cartStore.error || 'Có lỗi xảy ra'),
  )
}

// ---- UTILS ----
function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN').format(value) + 'đ'
}

// ---- INIT ----
onMounted(async () => {
  await Promise.all([fetchBrands(), fetchProducts()])
})

// Reset khi đổi parent category (phòng trường hợp dùng lại component)
watch(
  () => props.parentCategoryId,
  async () => {
    selectedBrandId.value = null
    selectedBrandName.value = null
    sectionTitle.value = 'Sắp xếp theo'
    page.value = 0
    products.value = []
    await Promise.all([fetchBrands(), fetchProducts()])
  },
)
</script>

<style scoped>
.product-block {
  position: relative;
}

/* ===== Sort bar header ===== */
.block-header {
  border-bottom: 2px solid var(--cps-red, #d70018);
  padding-bottom: 10px;
}
.block-title {
  font-size: 1.15rem;
  font-weight: 800;
  font-family: var(--font-display);
  color: var(--cps-dark, #222);
  letter-spacing: -0.2px;
}

.reset-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #f1f1f1;
  color: #777;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.reset-btn:hover {
  background: #ffe0e0;
  color: #d70018;
}

.sort-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  transition: all 0.15s ease;
}
.sort-btn:hover {
  border-color: #bbb;
}
.sort-btn.active {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eaf1ff;
}
.sort-icon {
  font-size: 12px;
}

@media (max-width: 768px) {
  .block-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

/* ===== Brand bar ===== */
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

/* ===== Product card ===== */
.product-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s ease;
  height: 100%;
}
.product-card:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.badge-discount {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #d70018;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  z-index: 1;
}

.card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 2;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 50%;
  cursor: pointer;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.icon-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.14);
}

.action-icon {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: #555;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.wishlist-btn .heart-icon {
  transition: fill 0.15s ease, stroke 0.15s ease;
}
.wishlist-btn.active .heart-icon {
  fill: #d70018;
  stroke: #d70018;
}
.wishlist-btn.loading {
  pointer-events: none;
  opacity: 0.6;
}

.cart-btn .cart-icon {
  stroke: #3b5ba9;
}
.cart-btn:hover .cart-icon {
  stroke: #1e3a8a;
}

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

.card-title {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  height: 20px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  margin: 0 0 8px;
}

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

.card-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.spec-chip {
  font-size: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 3px 8px;
  color: #333;
}

.card-footer-row {
  display: flex;
  align-items: center;
  margin-top: auto;
}

.card-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #555;
}
.star {
  color: #ffb800;
}

/* ===== Filter button (trong brand bar) ===== */
.filter-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 46px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, color 0.15s ease;
  position: relative;
  flex-shrink: 0;
}
.filter-btn:hover {
  border-color: #bbb;
  color: #d70018;
}

.filter-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #d70018;
  color: #fff;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* ===== Xem thêm ===== */
.load-more-wrap {
  display: flex;
  justify-content: center;
  padding-top: 24px;
}

.load-more-btn {
  height: 42px;
  padding: 0 36px;
  background: #fff;
  border: 1px solid #d70018;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #d70018;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.load-more-btn:hover:not(:disabled) {
  background: #d70018;
  color: #fff;
}
.load-more-btn:disabled {
  border-color: #e0e0e0;
  color: #bbb;
  cursor: default;
}

.load-more-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #999;
}

.load-more-link {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 36px;
  background: #d70018;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  transition: background 0.15s ease;
}
.load-more-link:hover {
  background: #b80015;
  color: #fff;
}
</style>
