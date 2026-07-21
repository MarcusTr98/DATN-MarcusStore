<template>
  <section class="new-products-section">
    <!-- Login Required Modal -->
    <LoginRequiredModal
      :visible="loginModal.visible"
      :title="loginModal.title"
      :message="loginModal.message"
      @close="loginModal.visible = false"
    />

    <!-- Toast Notification -->
    <Teleport to="body">
      <Transition name="toast">
        <div v-if="toast.show" class="cart-toast" :class="toast.type">
          <i :class="toast.type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'"></i>
          <span>{{ toast.message }}</span>
        </div>
      </Transition>
    </Teleport>
    <!-- ============ HEADER ============ -->
    <div class="section-header">
      <div class="header-content">
        <span class="kicker">
          <i class="fas fa-bolt"></i>
          Vừa cập nhật
        </span>
        <h2 class="section-title">Sản phẩm MỚI</h2>
        <p class="section-subtitle">Những thiết bị công nghệ mới nhất vừa ra mắt</p>
      </div>
    </div>

    <!-- ============ LOADING STATE ============ -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>Đang tải sản phẩm...</p>
    </div>

    <!-- ============ ERROR STATE ============ -->
    <div v-else-if="error" class="error-container">
      <i class="fas fa-exclamation-triangle"></i>
      <p>{{ error }}</p>
    </div>

    <!-- ============ EMPTY STATE ============ -->
    <div v-else-if="products.length === 0" class="empty-container">
      <i class="fas fa-box-open"></i>
      <p>Chưa có sản phẩm mới</p>
    </div>

    <!-- ============ CAROUSEL ============ -->
    <div v-else class="carousel-container">
      <!-- Navigation Arrows -->
      <button
        class="carousel-nav carousel-nav--prev"
        @click="prevSlide"
        :disabled="currentIndex === 0"
        aria-label="Sản phẩm trước"
      >
        <i class="fas fa-chevron-left"></i>
      </button>

      <button
        class="carousel-nav carousel-nav--next"
        @click="nextSlide"
        :disabled="currentIndex >= maxIndex"
        aria-label="Sản phẩm tiếp theo"
      >
        <i class="fas fa-chevron-right"></i>
      </button>

      <!-- Products Track -->
      <div class="carousel-track-wrapper">
        <div
          class="carousel-track"
          :style="{ transform: `translateX(-${currentIndex * slideWidth}px)` }"
        >
          <div
            v-for="product in products"
            :key="product.id"
            class="product-slide"
          >
            <div class="product-card-wrapper">
              <router-link :to="`/product/${product.slug || product.id}`" class="product-card">
                <!-- Image Container -->
                <div class="product-image-container">
                  <!-- Wishlist Icon -->
                  <button
                    class="wishlist-btn"
                    :class="{ active: wishlist.isWished(product.id), loading: wishlistToggling.has(product.id) }"
                    @click.prevent="toggleWishlist(product)"
                    aria-label="Thêm vào wishlist"
                  >
                    <i :class="wishlist.isWished(product.id) ? 'fas' : 'far'" class="fa-heart"></i>
                  </button>

                  <!-- NEW Badge -->
                  <span class="product-badge">
                    <i class="fas fa-star"></i> MỚI
                  </span>

                  <!-- Product Image -->
                  <img
                    :src="product.imageUrl"
                    :alt="product.name"
                    class="product-image"
                    loading="lazy"
                  />

                  <!-- Quick Actions Overlay -->
                  <div class="product-overlay">
                    <button class="action-btn action-btn--primary" @click.prevent="addToCart(product)">
                      <i class="fas fa-shopping-cart"></i>
                      Thêm vào giỏ
                    </button>
                    <button class="action-btn action-btn--secondary">
                      <i class="fas fa-eye"></i>
                      Xem nhanh
                    </button>
                  </div>
                </div>

                <!-- Product Info - Flex grow to fill space -->
                <div class="product-info">
                  <span class="product-category">{{ product.category }}</span>
                  <h3 class="product-name">{{ product.name }}</h3>

                  <!-- Rating -->
                  <div class="product-rating">
                    <div class="stars">
                      <i
                        v-for="n in 5"
                        :key="n"
                        :class="n <= Math.round(product.rating) ? 'fas fa-star' : 'far fa-star'"
                      ></i>
                    </div>
                    <span class="rating-count">({{ formatSoldCount(product.soldCount) }} đã bán)</span>
                  </div>

                  <!-- Price - Always at bottom -->
                  <div class="product-price">
                    <span class="price-current">{{ formatPrice(product.price) }}</span>
                    <span v-if="product.originalPrice" class="price-original">
                      {{ formatPrice(product.originalPrice) }}
                    </span>
                    <span v-if="product.discount" class="price-discount">
                      -{{ product.discount }}%
                    </span>
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ PAGINATION DOTS ============ -->
    <div class="carousel-dots">
      <button
        v-for="n in totalDots"
        :key="n"
        class="dot"
        :class="{ active: currentIndex === (n - 1) * productsPerPage }"
        @click="goToSlide((n - 1) * productsPerPage)"
        :aria-label="`Trang ${n}`"
      ></button>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import api from '@/utils/api'
import { useCartStore } from '@/stores/cartStore'
import LoginRequiredModal from '@/components/LoginRequiredModal.vue'
import wishlist from '@/composables/useWishlistShared'

const cartStore = useCartStore()

/* ---------- State ---------- */
const products = ref([])
const loading = ref(true)
const error = ref(null)
const currentIndex = ref(0)
const slideWidth = ref(300)
const productsPerPage = ref(4)
const wishlistToggling = ref(new Set())

/* ---------- Login Modal ---------- */
const loginModal = reactive({
  visible: false,
  title: '',
  message: '',
})

/* ---------- Toast ---------- */
const toast = reactive({
  show: false,
  type: 'Thêm thành công vào sản phẩm yêu thích',
  message: '',
})
let toastTimer = null

function showToast(type, message) {
  clearTimeout(toastTimer)
  toast.type = type
  toast.message = message
  toast.show = true
  toastTimer = setTimeout(() => {
    toast.show = false
  }, 2800)
}

/* ---------- Auth Helpers ---------- */
function isLoggedIn() {
  return !!localStorage.getItem('ACCESS_TOKEN')
}

function openLoginModal(title, message) {
  loginModal.title = title
  loginModal.message = message
  loginModal.visible = true
}

/* ---------- Fetch API ---------- */
async function fetchNewestProducts() {
  try {
    loading.value = true
    const response = await api.get('/home/newest')
    if (response.data && response.data.data) {
      products.value = response.data.data.map(item => ({
        id: item.productId,
        skuId: item.skuId,
        name: item.productName,
        imageUrl: item.thumbnailUrl,
        price: item.price,
        originalPrice: item.originalPrice,
        discount: item.discountPercent,
        rating: item.rating || 5.0,
        soldCount: 0,
        slug: item.slug,
        category: item.categoryName || '',
      }))
    }
  } catch (err) {
    console.error('Error fetching newest products:', err)
    error.value = 'Không thể tải sản phẩm mới'
  } finally {
    loading.value = false
  }
}

function updateResponsive() {
  const width = window.innerWidth
  if (width < 576) {
    productsPerPage.value = 1.5
    slideWidth.value = (window.innerWidth - 32) / 1.5
  } else if (width < 768) {
    productsPerPage.value = 2
    slideWidth.value = (window.innerWidth - 64) / 2
  } else if (width < 1024) {
    productsPerPage.value = 3
    slideWidth.value = 260
  } else {
    productsPerPage.value = 4
    slideWidth.value = 280
  }
}

/* ---------- Computed Properties ---------- */
const maxIndex = computed(() => {
  return Math.max(0, products.value.length - productsPerPage.value)
})

const totalDots = computed(() => {
  return Math.ceil(products.value.length / productsPerPage.value)
})

/* ---------- Navigation Methods ---------- */
function nextSlide() {
  if (currentIndex.value < maxIndex.value) {
    currentIndex.value += 1
  }
}

function prevSlide() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
  }
}

function goToSlide(index) {
  currentIndex.value = Math.min(index, maxIndex.value)
}

/* ---------- Utility Functions ---------- */
function formatPrice(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
  }).format(value)
}

function formatSoldCount(count) {
  if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count.toString()
}

function addToCart(product) {
  if (!isLoggedIn()) {
    openLoginModal(
      'Thêm vào giỏ hàng',
      'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng và tiến hành thanh toán.'
    )
    return
  }

  if (!product?.skuId) {
    console.warn('Sản phẩm không có skuId:', product)
    showToast('error', 'Sản phẩm chưa có thông tin SKU')
    return
  }

  cartStore.addToCart(product.skuId, 1).then((ok) => {
    if (ok) {
      showToast('success', `Đã thêm "${product.name}" vào giỏ hàng`)
    } else {
      showToast('error', cartStore.error || 'Thêm vào giỏ hàng thất bại')
    }
  })
}

async function toggleWishlist(product) {
  if (!isLoggedIn()) {
    openLoginModal(
      'Lưu sản phẩm yêu thích',
      'Vui lòng đăng nhập để lưu và quản lý các sản phẩm yêu thích của bạn.'
    )
    return
  }

  const productId = product.id
  if (!productId) {
    showToast('error', 'Không tìm thấy sản phẩm')
    return
  }

  if (wishlistToggling.value.has(productId)) return
  wishlistToggling.value.add(productId)

  const result = await wishlist.toggle(productId)
  wishlistToggling.value.delete(productId)

  if (result.success) {
    product.isWishlist = wishlist.isWished(productId)
    showToast('success', result.message)
  } else {
    showToast('error', result.message)
  }
}

/* ---------- Lifecycle ---------- */
onMounted(() => {
  updateResponsive()
  window.addEventListener('resize', updateResponsive)
  fetchNewestProducts()
  wishlist.fetchIds()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateResponsive)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
/* ============ CSS VARIABLES ============ */
.new-products-section {
  --primary-color: #e1121c;
  --primary-dark: #b80e16;
  --text-primary: #14151a;
  --text-secondary: #6b7280;
  --text-muted: #9ca3af;
  --bg-surface: #f8f9fa;
  --bg-card: #ffffff;
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 8px 24px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 12px 40px rgba(0, 0, 0, 0.12);
  --shadow-hover: 0 12px 32px rgba(0, 0, 0, 0.12);
  --shadow-nav: 0 4px 20px rgba(0, 0, 0, 0.1);
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --transition-fast: 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-bounce: 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

  padding: 60px 0 80px;
  background: linear-gradient(180deg, var(--bg-surface) 0%, #fff 100%);
}

/* ============ HEADER ============ */
.section-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 0 20px;
}

.kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--primary-color);
  margin-bottom: 12px;
}

.kicker i {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

.section-title {
  font-size: clamp(1.75rem, 4vw, 2.5rem);
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.section-subtitle {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 0;
  max-width: 400px;
  margin: 0 auto;
}

/* ============ LOADING STATE ============ */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ============ ERROR STATE ============ */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--primary-color);
}

.error-container i {
  font-size: 2.5rem;
  margin-bottom: 12px;
}

/* ============ EMPTY STATE ============ */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.empty-container i {
  font-size: 3rem;
  margin-bottom: 12px;
}

/* ============ CAROUSEL ============ */
.carousel-container {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 80px;
}

/* Navigation Buttons */
.carousel-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 1.1rem;
  cursor: pointer;
  box-shadow: var(--shadow-nav);
  transition: all var(--transition-bounce);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.06);
  will-change: transform, box-shadow, background, color;
}

.carousel-nav:hover:not(:disabled) {
  background: var(--primary-color);
  color: #fff;
  transform: translateY(-50%) scale(1.08);
  box-shadow: 0 8px 30px rgba(225, 18, 28, 0.3);
}

.carousel-nav:active:not(:disabled) {
  transform: translateY(-50%) scale(0.95);
}

.carousel-nav:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.carousel-nav--prev {
  left: 10px;
}

.carousel-nav--next {
  right: 10px;
}

/* Track Wrapper */
.carousel-track-wrapper {
  overflow: hidden;
  border-radius: var(--radius-lg);
  padding-right: 10px;
}

.carousel-track {
  display: flex;
  gap: 20px;
  transition: transform var(--transition-bounce);
  will-change: transform;
}

/* ============ EQUAL HEIGHT SLIDES & CARDS ============ */
.product-slide {
  flex: 0 0 auto;
  width: 280px;
  height: 100%;
}

.product-card-wrapper {
  padding: 4px;
  height: 100%;
}

.product-card {
  /* Flexbox column layout - full height */
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 420px;

  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
  text-decoration: none;
  color: inherit;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

/* ============ IMAGE CONTAINER - FIXED HEIGHT ============ */
.product-image-container {
  position: relative;
  flex-shrink: 0;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform var(--transition-normal);
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

/* Wishlist & Badge - Z-index layering */
.wishlist-btn,
.product-badge {
  position: absolute;
  z-index: 5;
}

.wishlist-btn {
  top: 12px;
  right: 12px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  color: var(--text-secondary);
  font-size: 0.85rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-bounce);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  will-change: transform, box-shadow, color, background;
}

.wishlist-btn:hover {
  background: #fff;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.wishlist-btn:active {
  transform: scale(0.95);
}

.wishlist-btn.active {
  color: #ef4444;
}

.wishlist-btn.loading {
  opacity: 0.6;
  pointer-events: none;
}

.wishlist-btn.loading i {
  animation: wishlist-pulse 0.6s ease-in-out infinite;
}

@keyframes wishlist-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.product-badge {
  top: 12px;
  left: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: #fff;
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-radius: 999px;
  box-shadow: 0 3px 10px rgba(225, 18, 28, 0.35);
}

.product-badge i {
  font-size: 0.52rem;
}

/* Quick Actions Overlay */
.product-overlay {
  position: absolute;
  inset: 0;
  background: rgba(20, 21, 26, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  opacity: 0;
  visibility: hidden;
  transition: all var(--transition-fast);
}

@media (hover: hover) and (pointer: fine) {
  .product-card:hover .product-overlay {
    opacity: 1;
    visibility: visible;
  }
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 25px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-bounce);
  transform: translateY(10px);
  opacity: 0;
  will-change: transform, opacity, background;
}

@media (hover: hover) and (pointer: fine) {
  .product-card:hover .action-btn {
    transform: translateY(0);
    opacity: 1;
  }

  .product-card:hover .action-btn:nth-child(2) {
    transition-delay: 0.05s;
  }
}

.action-btn:active {
  transform: scale(0.95) translateY(0) !important;
}

.action-btn--primary {
  background: var(--primary-color);
  color: #fff;
}

.action-btn--primary:hover {
  background: var(--primary-dark);
}

.action-btn--secondary {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.action-btn--secondary:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* ============ PRODUCT INFO - FLEX GROW ============ */
.product-info {
  /* Push to fill available space */
  flex: 1;
  display: flex;
  flex-direction: column;

  padding: 16px 16px 18px;
  min-height: 0;
}

.product-category {
  display: inline-block;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.product-name {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 10px;
  line-height: 1.4;

  /* Line clamp - exactly 2 lines */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5em;
  max-height: 2.8em;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.stars {
  display: flex;
  gap: 2px;
  color: #ffc107;
  font-size: 0.75rem;
}

.rating-count {
  font-size: 0.72rem;
  color: var(--text-muted);
}

/* Price - Always at bottom with margin-top auto */
.product-price {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
}

.price-current {
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--primary-color);
}

.price-original {
  font-size: 0.82rem;
  color: var(--text-muted);
  text-decoration: line-through;
}

.price-discount {
  display: inline-block;
  padding: 2px 8px;
  background: linear-gradient(135deg, #fff1f2, #ffe4e6);
  color: var(--primary-color);
  font-size: 0.68rem;
  font-weight: 700;
  border-radius: 4px;
}

/* ============ PAGINATION DOTS ============ */
.carousel-dots {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 36px;
}

.dot {
  width: 10px;
  height: 10px;
  border: none;
  border-radius: 50%;
  background: #d1d5db;
  cursor: pointer;
  transition: all var(--transition-fast);
  padding: 0;
}

.dot:hover {
  background: #9ca3af;
  transform: scale(1.2);
}

.dot.active {
  width: 32px;
  border-radius: 5px;
  background: var(--primary-color);
}

/* ============ FOOTER CTA ============ */
.section-footer {
  text-align: center;
  margin-top: 44px;
}

.view-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 32px;
  background: transparent;
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 600;
  text-decoration: none;
  border: 2px solid var(--text-primary);
  border-radius: 30px;
  transition: all var(--transition-fast);
}

.view-all-btn:hover {
  background: var(--text-primary);
  color: #fff;
}

.view-all-btn i {
  transition: transform var(--transition-fast);
}

.view-all-btn:hover i {
  transform: translateX(4px);
}

/* ============ RESPONSIVE ============ */
@media (max-width: 1024px) {
  .carousel-container {
    padding: 0 50px;
  }

  .product-slide {
    width: 260px;
  }

  .product-card {
    min-height: 400px;
  }

  .product-image-container {
    height: 180px;
  }
}

@media (max-width: 768px) {
  .new-products-section {
    padding: 40px 0 60px;
  }

  .carousel-container {
    padding: 0 40px;
  }

  .carousel-nav {
    width: 44px;
    height: 44px;
    font-size: 1rem;
  }

  .product-slide {
    width: calc((100vw - 100px) / 2);
  }

  .product-card {
    min-height: 380px;
  }

  .product-image-container {
    height: 160px;
  }

  .product-info {
    padding: 14px 14px 16px;
  }

  .product-name {
    font-size: 0.85rem;
  }

  .price-current {
    font-size: 1rem;
  }
}

@media (max-width: 576px) {
  .carousel-container {
    padding: 0 20px;
  }

  .carousel-nav {
    width: 40px;
    height: 40px;
    font-size: 0.9rem;
  }

  .carousel-nav--prev {
    left: 0;
  }

  .carousel-nav--next {
    right: 0;
  }

  .product-slide {
    width: calc((100vw - 56px) / 1.5);
  }

  .carousel-track {
    gap: 14px;
  }

  .product-card {
    min-height: 360px;
  }

  .product-image-container {
    height: 140px;
    padding: 12px;
  }

  .wishlist-btn {
    width: 30px;
    height: 30px;
    font-size: 0.75rem;
    top: 10px;
    right: 10px;
  }

  .product-badge {
    top: 10px;
    left: 10px;
    padding: 4px 10px;
    font-size: 0.58rem;
  }

  .action-btn {
    padding: 8px 14px;
    font-size: 0.7rem;
  }

  .view-all-btn {
    padding: 12px 24px;
    font-size: 0.85rem;
  }
}

/* ============ TOAST NOTIFICATION ============ */
.cart-toast {
  position: fixed;
  z-index: 9999;
  top: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  min-width: 280px;
  max-width: min(360px, calc(100vw - 32px));
  border: 1px solid #bfe7cc;
  border-radius: 12px;
  background: #f0fff5;
  color: #166534;
  box-shadow: 0 10px 28px rgba(26, 34, 51, 0.16);
  font-size: 14px;
  font-weight: 600;
  gap: 10px;
  padding: 14px 16px;
  pointer-events: auto;
  transform: translateY(-12px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.cart-toast.error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

.cart-toast i {
  color: #16a34a;
  font-size: 22px;
}

.cart-toast.error i {
  color: #dc2626;
}

.cart-toast span {
  flex: 1;
}

/* Toast animation */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
