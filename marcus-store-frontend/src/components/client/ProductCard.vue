<template>
  <div class="category-blocks-wrapper">
    <div v-if="loadingCategories" class="text-center py-5 text-muted">Đang tải danh mục...</div>

    <div v-else-if="mainCategories.length === 0" class="text-center py-5 text-muted">
      Chưa có danh mục nào có sản phẩm.
    </div>

    <!-- Mỗi category cha (Điện thoại, Phụ kiện...) là 1 block độc lập, tự quản lý state riêng -->
    <div v-for="block in blocks" :key="block.categoryId" class="product-block mb-5">
      <!-- Header: tiêu đề (đổi theo brand đang chọn) + Sắp xếp theo -->
      <div class="block-header d-flex align-items-center justify-content-between mb-3">
        <div class="d-flex align-items-center gap-2">
          <h4 class="block-title mb-0">{{ block.sectionTitle }}</h4>
          <button
            v-if="block.selectedBrandName"
            type="button"
            class="reset-btn"
            title="Bỏ chọn hãng, xem tất cả"
            @click="resetBrand(block)"
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
            :class="{ active: block.sortBy === opt.value }"
            @click="onSortChange(block, opt.value)"
          >
            <span class="sort-icon">{{ opt.icon }}</span>
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- Brand bar: luôn hiện (ít nhất có nút Lọc), thêm logo hãng nếu có -->
      <div class="brand-grid mb-3">
        <!-- Nút Lọc: phần tử grid đầu tiên -->
        <button type="button" class="filter-btn" @click="openFilter(block)">
          <i class="fa-solid fa-sliders"></i>
          Lọc
          <span v-if="getActiveFilterCount(block) > 0" class="filter-badge">
            {{ getActiveFilterCount(block) }}
          </span>
        </button>

        <!-- Logo các hãng con (nếu có) -->
        <button
          v-for="brand in block.brands"
          :key="brand.categoryId"
          type="button"
          class="brand-item"
          :class="{ active: block.selectedBrandId === brand.categoryId }"
          @click="onBrandClick(block, brand)"
        >
          <img :src="brand.categoryImg" :alt="brand.categoryName" class="brand-logo" />
        </button>
      </div>

      <!-- Loading state -->
      <div v-if="block.loadingProducts" class="text-center py-5 text-muted">
        Đang tải sản phẩm...
      </div>

      <!-- Error state -->
      <div v-else-if="block.productError" class="text-center py-5 text-danger">
        {{ block.productError }}
      </div>

      <!-- Empty state -->
      <div v-else-if="block.products.length === 0" class="text-center py-5 text-muted">
        Chưa có sản phẩm nào.
      </div>

      <!-- Grid sản phẩm -->
      <template v-else>
        <div class="row g-3">
          <div
            v-for="product in block.products"
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
                  :class="{ active: wishlistMap[product.productId] }"
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

              <!-- Ảnh -->
              <div class="card-thumbnail">
                <img :src="product.thumbnailUrl" :alt="product.productName" loading="lazy" />
              </div>

              <!-- Tên sản phẩm -->
              <h3 class="card-title">{{ product.productName }}</h3>

              <!-- Giá -->
              <div class="card-price">
                <span class="price-sale">{{ formatPrice(product.price) }}</span>
                <span v-if="product.originalPrice" class="price-original">
                  {{ formatPrice(product.originalPrice) }}
                </span>
              </div>

              <!-- Chip specs -->
              <div v-if="product.specs?.length" class="card-specs">
                <span v-for="(spec, idx) in product.specs" :key="idx" class="spec-chip">
                  {{ spec }}
                </span>
              </div>

              <!-- Khuyến mãi / ưu đãi (sửa nội dung trong PromotionCard.vue) -->
              <VoucherCard />

              <!-- Rating -->
              <div class="card-footer-row">
                <div class="card-rating">
                  <span class="star">★</span>
                  <span>{{ product.rating }}</span>
                </div>
              </div>
            </router-link>
          </div>
        </div>

        <div v-if="block.totalElements > 0" class="load-more-wrap">
          <router-link
            v-if="props.mode === 'standalone'"
            :to="viewMoreLink(block)"
            class="load-more-link"
          >
            Xem thêm {{ block.categoryName }}
            <i class="fa-solid fa-arrow-right"></i>
          </router-link>
          <button
            v-else-if="!block.loadingMore"
            type="button"
            class="load-more-btn"
            :disabled="block.products.length >= block.totalElements"
            @click="loadMore(block)"
          >
            Xem thêm {{ remainingCount(block) }} sản phẩm
          </button>
          <span v-else class="load-more-loading">
            <i class="fa-solid fa-spinner fa-spin-pulse"></i>
            Đang tải...
          </span>
        </div>
      </template>
    </div>
  </div>

  <!-- Filter Modal -->
  <FilterModal
    :visible="filterModal.visible"
    :categoryId="filterModal.categoryId"
    @close="filterModal.visible = false"
    @apply="onFilterApply"
  />
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import api from '@/utils/api'
import FilterModal from '@/layouts/home/FilterModal.vue'
import VoucherCard from '@/layouts/home/VoucherCard.vue'

const props = defineProps({
  mode: {
    type: String,
    default: 'standalone', // 'filter' | 'standalone'
  },
})

// ---- VIEW MORE LINK (standalone mode) ----
function viewMoreLink(block) {
  return `/category/${block.categorySlug}`
}

// ---- SORT OPTIONS (chung cho mọi block) ----
const sortOptions = [
  { value: 'popular', label: 'Phổ biến', icon: '★' },
  { value: 'discount', label: 'Khuyến mãi HOT', icon: '⊗' },
  { value: 'price_asc', label: 'Giá Thấp - Cao', icon: '↑' },
  { value: 'price_desc', label: 'Giá Cao - Thấp', icon: '↓' },
]

// ---- DANH SÁCH CATEGORY CHA (chỉ cate có sản phẩm) ----
const mainCategories = ref([])
const loadingCategories = ref(false)

const blocks = ref([])

// ---- FILTER MODAL STATE ----
const filterModal = reactive({
  visible: false,
  categoryId: null,
})

function openFilter(block) {
  filterModal.categoryId = block.categoryId
  filterModal.visible = true
}

function getActiveFilterCount(block) {
  return (
    (block.selectedMinPrice != null || block.selectedMaxPrice != null ? 1 : 0) +
    (block.selectedValueIds?.length ?? 0) +
    (block.selectedBrandIds?.length ?? 0)
  )
}

function onFilterApply({ brandIds, minPrice, maxPrice, valueIds }) {
  const block = blocks.value.find((b) => b.categoryId === filterModal.categoryId)
  if (!block) return
  block.selectedBrandIds = brandIds
  block.selectedMinPrice = minPrice
  block.selectedMaxPrice = maxPrice
  block.selectedValueIds = valueIds
  block.page = 0
  fetchProducts(block)
}

function createBlock(cate) {
  return reactive({
    categoryId: cate.categoryId,
    categoryName: cate.categoryName,
    categorySlug: cate.slug,
    brands: [],
    selectedBrandId: null,
    selectedBrandName: null,
    sectionTitle: 'Sắp xếp theo',
    sortBy: 'popular',
    page: 0,
    size: 8,
    totalPages: 0,
    totalElements: 0,
    products: [],
    loadingProducts: false,
    productError: null,
    // Filter fields
    selectedMinPrice: null,
    selectedMaxPrice: null,
    selectedValueIds: [],
    selectedBrandIds: [],
    loadingMore: false,
  })
}

async function fetchMainCategories() {
  loadingCategories.value = true
  try {
    const res = await api.get('/client/categories/main')
    mainCategories.value = res.data?.data ?? []
    blocks.value = mainCategories.value.map(createBlock)

    // Fetch song song: brand bar + sản phẩm mặc định cho từng block
    await Promise.all(
      blocks.value.map((block) => Promise.all([fetchBrands(block), fetchProducts(block)])),
    )
  } catch (err) {
    console.error('Lỗi khi tải danh mục chính trang Home:', err)
    mainCategories.value = []
    blocks.value = []
  } finally {
    loadingCategories.value = false
  }
}

// ---- BRAND BAR ----
async function fetchBrands(block) {
  try {
    const res = await api.get(`/client/categories/${block.categoryId}/children`)
    block.brands = res.data?.data ?? []
    console.log(
      `[brand] cate=${block.categoryName} (id=${block.categoryId}) -> ${block.brands.length} brands`,
      block.brands,
    )
  } catch (err) {
    console.error('Lỗi khi tải danh sách hãng:', err)
    block.brands = []
  }
}

function onBrandClick(block, brand) {
  // Bấm lại đúng logo đang chọn -> bỏ chọn, về lại tất cả sản phẩm của category cha
  if (block.selectedBrandId === brand.categoryId) {
    resetBrand(block)
    return
  }
  block.selectedBrandId = brand.categoryId
  block.selectedBrandName = brand.categoryName
  block.sectionTitle = `${block.categoryName}: ${brand.categoryName}`
  block.page = 0
  fetchProducts(block)
}

function resetBrand(block) {
  block.selectedBrandId = null
  block.selectedBrandName = null
  block.sectionTitle = 'Sắp xếp theo'
  block.page = 0
  fetchProducts(block)
}

// ---- FETCH PRODUCTS (theo từng block) ----
async function fetchProducts(block) {
  block.loadingProducts = true
  block.productError = null
  try {
    // Gộp brand click trực tiếp (single) + brand chọn trong filter modal (multi)
    const allBrandIds = [
      ...(block.selectedBrandId != null ? [block.selectedBrandId] : []),
      ...(block.selectedBrandIds ?? []),
    ]
    const uniqueBrandIds = [...new Set(allBrandIds)]

    const res = await api.get('/home', {
      params: {
        sortBy: block.sortBy,
        categoryId: block.selectedBrandId,
        parentCategoryId: block.categoryId,
        page: block.page,
        size: block.size,
        minPrice: block.selectedMinPrice,
        maxPrice: block.selectedMaxPrice,
        valueIds: block.selectedValueIds?.length ? block.selectedValueIds.join(',') : null,
        brandIds: uniqueBrandIds.length ? uniqueBrandIds.join(',') : null,
      },
    })
    const pageData = res.data?.data
    block.products = pageData?.content ?? []
    block.totalPages = pageData?.totalPages ?? 0
    block.totalElements = pageData?.totalElements ?? 0
  } catch (err) {
    console.error('Lỗi khi tải sản phẩm trang home:', err)
    block.productError = 'Không thể tải sản phẩm, vui lòng thử lại.'
  } finally {
    block.loadingProducts = false
  }
}

function onSortChange(block, newSortBy) {
  block.sortBy = newSortBy
  block.page = 0
  fetchProducts(block)
}

function loadMore(block) {
  if (block.loadingMore) return
  block.loadingMore = true
  block.page += 1
  fetchProductsAppend(block)
}

async function fetchProductsAppend(block) {
  block.loadingMore = true
  block.productError = null
  try {
    const allBrandIds = [
      ...(block.selectedBrandId != null ? [block.selectedBrandId] : []),
      ...(block.selectedBrandIds ?? []),
    ]
    const uniqueBrandIds = [...new Set(allBrandIds)]

    const res = await api.get('/home', {
      params: {
        sortBy: block.sortBy,
        categoryId: block.selectedBrandId,
        parentCategoryId: block.categoryId,
        page: block.page,
        size: block.size,
        minPrice: block.selectedMinPrice,
        maxPrice: block.selectedMaxPrice,
        valueIds: block.selectedValueIds?.length ? block.selectedValueIds.join(',') : null,
        brandIds: uniqueBrandIds.length ? uniqueBrandIds.join(',') : null,
      },
    })
    const pageData = res.data?.data
    const newProducts = pageData?.content ?? []
    block.products.push(...newProducts)
    block.totalPages = pageData?.totalPages ?? 0
    block.totalElements = pageData?.totalElements ?? 0
  } catch (err) {
    console.error('Lỗi khi tải thêm sản phẩm:', err)
    block.productError = 'Không thể tải sản phẩm, vui lòng thử lại.'
    block.page -= 1
  } finally {
    block.loadingMore = false
  }
}

function remainingCount(block) {
  const remain = (block.totalElements ?? 0) - (block.products?.length ?? 0)
  return remain > 0 ? remain : 0
}

// ---- FETCH CATEGORY BY ID (filter mode - CategoryProducts page) ----
async function fetchCategoryById(categoryId) {
  loadingCategories.value = true
  try {
    const res = await api.get(`/client/categories/${categoryId}`)
    const cate = res.data?.data
    if (cate) {
      const block = createBlock(cate)
      blocks.value = [block]
      await Promise.all([fetchBrands(block), fetchProducts(block)])
    }
  } catch (err) {
    console.error('Lỗi khi tải danh mục:', err)
    blocks.value = []
  } finally {
    loadingCategories.value = false
  }
}

onMounted(() => {
  if (props.mode === 'filter') {
    fetchCategoryById(props.categoryId)
  } else {
    fetchMainCategories()
  }
})

watch(
  () => [props.categoryId, props.mode],
  ([newId, newMode]) => {
    if (newMode === 'filter') {
      fetchCategoryById(newId)
    } else {
      fetchMainCategories()
    }
  },
)

// ---- WISHLIST (chỉ FE, chưa nối BE) ----
const wishlistMap = reactive({})

function toggleWishlist(productId) {
  wishlistMap[productId] = !wishlistMap[productId]
  // TODO: nối API wishlist sau (POST/DELETE /api/wishlist/:productId)
}

// ---- ADD TO CART (chỉ FE, chưa nối BE) ----
function addToCart(product) {
  // TODO: nối API giỏ hàng sau (POST /api/cart/items { skuId: product.skuId, quantity: 1 })
  console.log('Thêm vào giỏ:', product.productName)
}

// ---- UTILS ----
function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN').format(value) + 'đ'
}
</script>

<style scoped>
/* ===== Sort bar header ===== */
.block-header {
  border-bottom: 2px solid var(--cps-red);
  padding-bottom: 10px;
}
.block-title {
  font-size: 1.15rem;
  font-weight: 800;
  font-family: var(--font-display);
  color: var(--cps-dark);
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
  transition:
    background 0.15s ease,
    color 0.15s ease;
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

@media (min-width: 1200px) {
  .col-xl-2-4 {
    flex: 0 0 auto;
    width: 20%;
  }
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
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
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

/* ===== Action icons: wishlist + cart, góc phải trên ===== */
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
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
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
  transition:
    fill 0.15s ease,
    stroke 0.15s ease;
}
.wishlist-btn.active .heart-icon {
  fill: #d70018;
  stroke: #d70018;
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
  margin: 0 0 4px;
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
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    color 0.15s ease;
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
  transition:
    background 0.15s ease,
    color 0.15s ease;
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
