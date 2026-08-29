<template>
  <!-- ============ MODE: standalone (trang chủ - điều phối nhiều block) ============ -->
  <div v-if="mode === 'standalone'" class="category-blocks-wrapper">
    <div v-if="loadingCategories" class="text-center py-5 text-muted">
      Đang tải danh mục...
    </div>

    <div v-else-if="mainCategories.length === 0" class="text-center py-5 text-muted">
      Chưa có danh mục nào có sản phẩm.
    </div>

    <template v-else>
      <ProductBlockSection
        v-for="cate in mainCategories"
        :key="cate.categoryId"
        :ref="(el) => setBlockRef(cate.categoryId, el)"
        :parent-category-id="cate.categoryId"
        :parent-category-name="cate.categoryName"
        :parent-category-slug="cate.slug"
        mode="standalone"
      />
    </template>
  </div>

  <!-- ============ MODE: list (trang /search?q=...) ============ -->
  <div v-else-if="mode === 'list'" class="product-list-wrapper">
    <div v-if="loading" class="text-center py-5 text-muted">Đang tải...</div>
    <div v-else-if="error" class="text-center py-5 text-danger">{{ error }}</div>
    <div v-else-if="!products.length" class="text-center py-5 text-muted">
      Không tìm thấy sản phẩm nào cho từ khóa "<strong>{{ keyword }}</strong>"
    </div>
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
                :class="{ active: isWished(product.productId) }"
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
                class="icon-btn compare-btn"
                :class="{ active: compareBar.isInCompare(product.productId) }"
                :disabled="!compareBar.isInCompare(product.productId) && !compareBar.canAddMore"
                title="So sánh"
                @click.stop.prevent="onToggleCompare(product)"
              >
                <svg viewBox="0 0 24 24" class="action-icon compare-icon">
                  <path d="M9 3v18M15 3v18" />
                  <path d="M3 9h6M15 9h6M3 15h6M15 15h6" />
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

      <nav v-if="totalPages > 1 && !externalPage" class="d-flex justify-content-center mt-4">
        <ul class="pagination">
          <li
            v-for="p in totalPages"
            :key="p"
            class="page-item"
            :class="{ active: p - 1 === page }"
          >
            <button class="page-link" @click="goPage(p - 1)">{{ p }}</button>
          </li>
        </ul>
      </nav>
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

  <!-- Login Required Modal (mode list) -->
  <LoginRequiredModal
    :visible="loginModal.visible"
    :title="loginModal.title"
    :message="loginModal.message"
    @close="loginModal.visible = false"
  />
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import VoucherCard from '@/layouts/home/VoucherCard.vue'
import ProductBlockSection from '@/components/client/ProductBlockSection.vue'
import { useCartStore } from '@/stores/cartStore'
import BaseModal from '@/components/BaseModal.vue'
import wishlist from '@/composables/useWishlistShared'
import { searchApi } from '@/composables/useSearchBox'
import LoginRequiredModal from '../LoginRequiredModal.vue'
import { useCompareBar } from '@/composables/useCompareBar'

const props = defineProps({
  mode: {
    type: String,
    default: 'standalone', // 'standalone' | 'list'
  },
  // Props cho mode 'list'
  keyword: { type: String, default: '' },
  size: { type: Number, default: 12 },
  parentCategorySlug: { type: String, default: null },
  brandSlug: { type: String, default: null },
  sortBy: { type: String, default: 'price_desc' },
  page: { type: Number, default: 0 },
})

// ---- STATE cho mode 'list' ----
const listProducts = ref([])
const listLoading = ref(false)
const listError = ref(null)
const listTotalPages = ref(0)
const listTotalElements = ref(0)
const listPage = ref(0)

defineExpose({
  totalElements: listTotalElements,
  totalPages: listTotalPages,
})

async function fetchList() {
  const kw = (props.keyword || '').trim()
  const isAccessoryFilter = props.parentCategorySlug === 'phu-kien'
  if (!kw && !isAccessoryFilter) {
    listProducts.value = []
    listTotalPages.value = 0
    listTotalElements.value = 0
    return
  }
  listLoading.value = true
  listError.value = null
  try {
    const { data } = await searchApi.search(kw, {
      parentCategorySlug: props.parentCategorySlug,
      brandSlug: props.brandSlug,
      sortBy: props.sortBy,
      page: listPage.value,
      size: props.size,
    })
    const pageData = data?.data || {}
    listProducts.value = pageData.content || []
    listTotalPages.value = pageData.totalPages || 0
    listTotalElements.value = pageData.totalElements || 0
  } catch (e) {
    console.warn('search error', e)
    listError.value = 'Không thể tải kết quả, vui lòng thử lại.'
    listProducts.value = []
  } finally {
    listLoading.value = false
  }
}

function goListPage(n) {
  listPage.value = Math.max(n, 0)
}

watch(
  () => [props.keyword, props.parentCategorySlug, props.brandSlug, props.mode],
  ([kw, , , m]) => {
    if (m === 'list') {
      listPage.value = 0
      fetchList()
    }
  },
  { immediate: true },
)

watch(
  () => [props.sortBy, props.mode],
  ([, m]) => {
    if (m === 'list') {
      listPage.value = 0
      fetchList()
    }
  },
)

watch(
  () => props.page,
  (newPage) => {
    if (props.mode === 'list' && newPage != null) {
      listPage.value = Math.max(newPage, 0)
      fetchList()
    }
  },
)

const products = listProducts
const loading = listLoading
const error = listError
const totalPages = listTotalPages
const totalElements = listTotalElements
const page = listPage
const externalPage = computed(() => props.page > 0)
function goPage(n) {
  goListPage(n)
}

// ---- STATE cho mode 'standalone' ----
const mainCategories = ref([])
const loadingCategories = ref(false)
const blockRefs = ref({})

function setBlockRef(categoryId, el) {
  if (el) blockRefs.value[categoryId] = el
}

async function fetchMainCategories() {
  loadingCategories.value = true
  try {
    const res = await api.get('/client/categories/main')
    mainCategories.value = res.data?.data ?? []
  } catch (err) {
    console.error('Lỗi khi tải danh mục chính trang Home:', err)
    mainCategories.value = []
  } finally {
    loadingCategories.value = false
  }
}

onMounted(() => {
  wishlist.fetchIds()
  if (props.mode === 'standalone') {
    fetchMainCategories()
  }
})

watch(
  () => props.mode,
  (newMode) => {
    if (newMode === 'standalone') {
      fetchMainCategories()
    }
  },
)

// ---- WISHLIST ----
function isWished(productId) {
  return wishlist.isWished(productId)
}

const togglingIds = ref(new Set())

const loginModal = reactive({ visible: false, title: '', message: '' })

function isLoggedIn() {
  return !!localStorage.getItem('ACCESS_TOKEN')
}

async function toggleWishlist(productId) {
  if (!isLoggedIn()) {
    loginModal.title = 'Lưu sản phẩm yêu thích'
    loginModal.message = 'Vui lòng đăng nhập để lưu và quản lý các sản phẩm yêu thích của bạn.'
    loginModal.visible = true
    return
  }
  if (togglingIds.value.has(productId)) return
  togglingIds.value.add(productId)
  try {
    const result = await wishlist.toggle(productId)
    if (!result.success) {
      showNotify('error', 'Lỗi', result.message)
    }
  } finally {
    togglingIds.value.delete(productId)
  }
}

// ---- ADD TO CART ----
const cartStore = useCartStore()
const router = useRouter()

// ---- COMPARE BAR (so sánh sản phẩm) ----
const compareBar = useCompareBar()
compareBar.canAddMore = computed(() => compareBar.state.items.length < compareBar.MAX_ITEMS)

function onToggleCompare(product) {
  compareBar.toggleCompare(product)
}

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

async function addToCart(product) {
  if (!isLoggedIn()) {
    loginModal.title = 'Thêm vào giỏ hàng'
    loginModal.message = 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng và tiến hành thanh toán.'
    loginModal.visible = true
    return
  }
  const skuId = product?.defaultSkuId ?? product?.skuId
  if (!skuId) {
    if (product?.slug) {
      router.push(`/product/${product.slug}`)
    } else {
      showNotify('info', 'Sản phẩm tạm hết hàng', 'Phiên bản này hiện không còn hàng. Vui lòng chọn sản phẩm khác.')
    }
    return
  }
  const ok = await cartStore.addToCart(skuId, 1)
  if (ok) {
    showNotify('success', 'Thêm vào giỏ hàng', `Đã thêm "${product.productName}" vào giỏ hàng`)
  } else {
    showNotify('error', 'Thêm thất bại', cartStore.error || 'Thêm vào giỏ hàng thất bại')
  }
}

// ---- UTILS ----
function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN').format(value) + 'đ'
}
</script>

<style scoped>
.category-blocks-wrapper {
  width: 100%;
}
.product-list-wrapper {
  width: 100%;
}

/* Reuse styles for product-card (mode list) */
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


.compare-btn .compare-icon {
  stroke: #6b7280;
}
.compare-btn:hover .compare-icon {
  stroke: #d70018;
}
.compare-btn.active {
  background: #d70018;
}
.compare-btn.active .compare-icon {
  stroke: #fff;
}
.compare-btn:disabled:not(.active) {
  opacity: 0.35;
  cursor: not-allowed;
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
</style>
