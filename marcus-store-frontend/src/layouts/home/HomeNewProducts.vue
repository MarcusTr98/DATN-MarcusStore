<template>
  <section class="new-products-section">
    <LoginRequiredModal
      :visible="loginModal.visible"
      :title="loginModal.title"
      :message="loginModal.message"
      @close="loginModal.visible = false"
    />

    <Teleport to="body">
      <Transition name="toast">
        <div v-if="toast.show" class="cart-toast" :class="toast.type">
          <i :class="toast.type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'"></i>
          <span>{{ toast.message }}</span>
        </div>
      </Transition>
    </Teleport>

    <div class="tech-panel">


      <header class="section-header section-header--center">
        <div class="fire-header-wrapper">
          <!-- Floating ember particles -->
          <span class="ember ember-1"></span>
          <span class="ember ember-2"></span>
          <span class="ember ember-3"></span>
          <span class="ember ember-4"></span>
          <span class="ember ember-5"></span>
          <span class="ember ember-6"></span>
          <span class="ember ember-7"></span>
          <span class="ember ember-8"></span>
          <h2 class="section-title">
            <i class="fas fa-bolt title-bolt"></i>
            <strong> SẢN PHẨM MỚI 🔥</strong>
          </h2>
        </div>
        <p class="section-subtitle">Những thiết bị công nghệ mới nhất vừa ra mắt</p>
      </header>

      <div v-if="loading" class="state-container">
        <div class="loading-spinner"></div>
        <p>Đang tải sản phẩm...</p>
      </div>

      <div v-else-if="error" class="state-container state-container--error">
        <i class="fas fa-exclamation-triangle"></i>
        <p>{{ error }}</p>
      </div>

      <div v-else-if="products.length === 0" class="state-container">
        <i class="fas fa-box-open"></i>
        <p>Chưa có sản phẩm mới</p>
      </div>

      <div v-else class="carousel-container">
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

        <div class="carousel-track-wrapper">
          <div class="carousel-track" :style="{ transform: `translateX(-${currentIndex * slideStep}px)` }">
            <article v-for="product in products" :key="product.id" class="product-slide">
              <router-link :to="`/product/${product.slug || product.id}`" class="product-card">
                <!-- Image container: aspect 1/1, fallback placeholder if image missing/broken -->
                <div class="product-image-container">
                  <span class="product-badge product-badge--hot" aria-label="Sản phẩm mới">
                    <i class="fas fa-fire"></i><span>MỚI</span>
                  </span>
                  <button
                    class="wishlist-btn"
                    :class="{ active: wishlist.isWished(product.id), loading: wishlistToggling.has(product.id) }"
                    @click.prevent="toggleWishlist(product)"
                    aria-label="Thêm vào danh sách yêu thích"
                  >
                    <i :class="wishlist.isWished(product.id) ? 'fas' : 'far'" class="fa-heart"></i>
                  </button>

                  <div class="image-skeleton" :class="{ hidden: imageLoaded[product.id] || imageErrored[product.id] }" aria-hidden="true"></div>
                  <img
                    v-if="product.imageUrl && !imageErrored[product.id]"
                    :src="product.imageUrl"
                    :alt="product.name"
                    class="product-image"
                    :class="{ loaded: imageLoaded[product.id] }"
                    loading="lazy"
                    @load="onImageLoad(product.id)"
                    @error="onImageError(product.id)"
                  />
                  <div
                    v-else
                    class="image-fallback"
                    role="img"
                    :aria-label="`Ảnh minh họa cho ${product.name}`"
                  >
                    <i class="fas fa-image"></i>
                    <span class="image-fallback-text">{{ product.name }}</span>
                  </div>

                  <div class="quick-actions">
                    <button class="qa-btn qa-btn--quick" aria-label="Xem nhanh">
                      <i class="fas fa-eye"></i>
                    </button>
                    <button class="qa-btn qa-btn--cart" @click.prevent="addToCart(product)" aria-label="Thêm vào giỏ">
                      <i class="fas fa-shopping-bag"></i>
                    </button>
                  </div>
                </div>

                <div class="product-info">
                  <div class="product-label-row">
                    <span class="authentic-label"><i class="fas fa-shield-alt"></i> CHÍNH HÃNG</span>
                    <span class="product-category">{{ product.category }}</span>
                  </div>
                  <h3 class="product-name">{{ product.name }}</h3>

                  <div class="product-rating">
                    <div class="stars">
                      <i
                        v-for="n in 5"
                        :key="n"
                        :class="n <= Math.round(product.rating) ? 'fas fa-star' : 'far fa-star'"
                      ></i>
                    </div>
                    <span v-if="product.soldCount > 0" class="rating-count">({{ formatSoldCount(product.soldCount) }} đã bán)</span>
                    <span v-else class="rating-count rating-count--new">Hàng mới về</span>
                  </div>

                  <div class="card-bottom">
                    <div class="product-price">
                      <span class="price-current">{{ formatPrice(product.price) }}</span>
                      <div class="old-price-row">
                        <span v-if="product.originalPrice" class="price-original price-original--burning">{{ formatPrice(product.originalPrice) }}</span>
                        <span v-if="product.discount" class="price-discount price-discount--hot">-{{ product.discount }}%</span>
                      </div>
                    </div>
                    <button class="cart-btn" @click.prevent="addToCart(product)" aria-label="Thêm vào giỏ hàng">
                      <i class="fas fa-shopping-bag"></i>
                    </button>
                  </div>
                </div>
              </router-link>
            </article>
          </div>
        </div>
      </div>

      <div v-if="products.length > 0" class="carousel-dots">
        <button
          v-for="n in totalDots"
          :key="n"
          class="dot"
          :class="{ active: currentIndex === Math.min((n - 1) * productsPerPage, maxIndex) }"
          @click="goToSlide((n - 1) * productsPerPage)"
          :aria-label="`Trang ${n}`"
        ></button>
      </div>

      <p class="footer-banner">
        Chỉ áp dụng đơn hàng online thành công <span>—</span> Giao hàng 2H trong nội thành <span>—</span> Số lượng có hạn
      </p>
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
const products = ref([])
const loading = ref(true)
const error = ref(null)
const currentIndex = ref(0)
const slideStep = ref(296)
const productsPerPage = ref(4)
const wishlistToggling = ref(new Set())
const imageLoaded = reactive({})
const imageErrored = reactive({})

const loginModal = reactive({ visible: false, title: '', message: '' })
const toast = reactive({ show: false, type: 'success', message: '' })
let toastTimer = null

function showToast(type, message) {
  clearTimeout(toastTimer)
  toast.type = type
  toast.message = message
  toast.show = true
  toastTimer = setTimeout(() => { toast.show = false }, 2800)
}

function isLoggedIn() {
  return !!localStorage.getItem('ACCESS_TOKEN')
}

function openLoginModal(title, message) {
  loginModal.title = title
  loginModal.message = message
  loginModal.visible = true
}

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
    productsPerPage.value = 1
    slideStep.value = Math.max(250, width - 76) + 14
  } else if (width < 768) {
    productsPerPage.value = 2
    slideStep.value = 250
  } else if (width < 1100) {
    productsPerPage.value = 3
    slideStep.value = 260
  } else {
    productsPerPage.value = 4
    slideStep.value = 296
  }
  currentIndex.value = Math.min(currentIndex.value, maxIndex.value)
}

const maxIndex = computed(() => Math.max(0, products.value.length - productsPerPage.value))
const totalDots = computed(() => Math.ceil(products.value.length / productsPerPage.value))

function nextSlide() {
  if (currentIndex.value < maxIndex.value) currentIndex.value += 1
}

function prevSlide() {
  if (currentIndex.value > 0) currentIndex.value -= 1
}

function goToSlide(index) {
  currentIndex.value = Math.min(index, maxIndex.value)
}

function formatPrice(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
  }).format(value)
}

function formatSoldCount(count) {
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

function addToCart(product) {
  if (!isLoggedIn()) {
    openLoginModal('Thêm vào giỏ hàng', 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng và tiến hành thanh toán.')
    return
  }
  if (!product?.skuId) {
    console.warn('Sản phẩm không có skuId:', product)
    showToast('error', 'Sản phẩm chưa có thông tin SKU')
    return
  }
  cartStore.addToCart(product.skuId, 1).then(ok => {
    if (ok) showToast('success', `Đã thêm "${product.name}" vào giỏ hàng`)
    else showToast('error', cartStore.error || 'Thêm vào giỏ hàng thất bại')
  })
}

function onImageLoad(id) {
  imageLoaded[id] = true
}

function onImageError(id) {
  imageErrored[id] = true
  imageLoaded[id] = true
}

async function toggleWishlist(product) {
  if (!isLoggedIn()) {
    openLoginModal('Lưu sản phẩm yêu thích', 'Vui lòng đăng nhập để lưu và quản lý các sản phẩm yêu thích của bạn.')
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
.new-products-section {
  --red: #ef4444;
  --red-bright: #f87171;
  --dark-red: #991b1b;
  --medium-red: #dc2626;
  --accent-red: #b91c1c;
  padding: 78px 20px 70px;
  background: #fff5f5;
}

.tech-panel {
  position: relative;
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 34px 24px 22px;
  border: 2px solid #ef4444;
  border-radius: 22px;
  background:
    radial-gradient(circle at 85% 10%, rgba(239, 68, 68, 0.25), transparent 28%),
    linear-gradient(135deg, #7f1d1d 0%, #991b1b 52%, #b91c1c 100%);
  box-shadow: 0 0 20px rgba(239, 68, 68, 0.4), 0 18px 45px rgba(127, 29, 29, 0.2);
}

.folder-tabs {
  position: absolute;
  z-index: 5;
  top: -48px;
  left: 20px;
  right: 20px;
  display: flex;
  align-items: flex-end;
  gap: 6px;
  overflow-x: auto;
  padding: 4px 2px 0;
  scrollbar-width: none;
}

.folder-tabs::-webkit-scrollbar { display: none; }

.folder-tab {
  position: relative;
  flex: 1 0 145px;
  min-height: 48px;
  padding: 12px 14px 11px;
  border: 1px solid rgba(252, 165, 165, 0.55);
  border-bottom: 0;
  border-radius: 13px 13px 3px 3px;
  background: linear-gradient(180deg, #991b1b, #7f1d1d);
  color: rgba(255, 255, 255, 0.83);
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.35px;
  cursor: pointer;
  transition: transform 0.2s ease, filter 0.2s ease;
  white-space: nowrap;
}

.folder-tab::before {
  content: '';
  position: absolute;
  top: -7px;
  left: 14px;
  width: 42px;
  height: 8px;
  border-radius: 7px 7px 0 0;
  background: inherit;
  border: inherit;
  border-bottom: 0;
}

.folder-tab i { margin-right: 8px; color: #fff; }
.folder-tab:hover { filter: brightness(1.15); transform: translateY(-2px); }
.folder-tab.active {
  min-height: 55px;
  color: #fff;
  border-color: var(--red-bright);
  background: linear-gradient(180deg, #dc2626, #991b1b);
  box-shadow: 0 -4px 14px rgba(239, 68, 68, 0.55), inset 0 1px rgba(255, 255, 255, 0.28);
}

.section-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
  padding: 0 6px;
}
.section-header--center {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 6px;
}
.section-header--center .view-all-link {
  position: absolute;
  top: 50%;
  right: 6px;
  transform: translateY(-50%);
}

.header-copy { min-width: 0; }
/* ===== FIRE HEADER WITH FLOATING EMBER PARTICLES ===== */
.fire-header-wrapper {
  position: relative;
  display: inline-block;
}

.section-title {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: #fff;
  font-size: clamp(1.6rem, 2.8vw, 2.1rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: 0.4px;
  text-shadow: 0 2px 12px rgba(255, 69, 0, 0.5);
}
.section-title strong { font-weight: 900; }

.ember {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff4500;
  box-shadow: 0 0 8px 2px rgba(255, 69, 0, 0.8), 0 0 16px 4px rgba(255, 183, 3, 0.4);
  animation: emberFloat 3s ease-in-out infinite;
  will-change: transform, opacity;
  z-index: 1;
  pointer-events: none;
}

.ember-1 { top: -8px; left: -5px; animation-delay: 0s; animation-duration: 2.8s; width: 5px; height: 5px; }
.ember-2 { top: -5px; right: -8px; animation-delay: 0.4s; animation-duration: 3.2s; width: 7px; height: 7px; }
.ember-3 { top: 50%; left: -12px; animation-delay: 0.8s; animation-duration: 2.5s; width: 4px; height: 4px; }
.ember-4 { top: 30%; right: -10px; animation-delay: 1.2s; animation-duration: 3.5s; width: 5px; height: 5px; }
.ember-5 { bottom: -6px; left: 10%; animation-delay: 0.3s; animation-duration: 2.9s; width: 6px; height: 6px; }
.ember-6 { bottom: -4px; right: 15%; animation-delay: 0.7s; animation-duration: 3.1s; width: 4px; height: 4px; }
.ember-7 { top: -2px; left: 40%; animation-delay: 1.5s; animation-duration: 2.6s; width: 5px; height: 5px; }
.ember-8 { top: 60%; left: -8px; animation-delay: 0.2s; animation-duration: 3.3s; width: 3px; height: 3px; }

@keyframes emberFloat {
  0% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
  50% {
    transform: translateY(-18px) translateX(6px) scale(0.7);
    opacity: 0.6;
  }
  100% {
    transform: translateY(-32px) translateX(-4px) scale(0.3);
    opacity: 0;
  }
}

/* ===== HOT BADGE GLOWING AURA ===== */
.product-badge--hot {
  background: linear-gradient(135deg, #ff4500, #e63946, #dc2626);
  box-shadow: 0 4px 12px rgba(255, 69, 0, 0.5);
  animation: badgeGlow 1.8s ease-in-out infinite;
  will-change: box-shadow;
}
.product-badge--hot::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: inherit;
  background: radial-gradient(ellipse at center, rgba(255, 69, 0, 0.4), transparent 70%);
  animation: badgeAura 1.8s ease-in-out infinite;
  z-index: -1;
  will-change: opacity;
}

@keyframes badgeGlow {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(255, 69, 0, 0.5), 0 0 20px rgba(255, 69, 0, 0.3);
  }
  50% {
    box-shadow: 0 4px 20px rgba(255, 69, 0, 0.8), 0 0 35px rgba(255, 183, 3, 0.5), 0 0 50px rgba(230, 57, 70, 0.3);
  }
}

@keyframes badgeAura {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.15); }
}

/* ===== BURNING PRICE TAG (STRIKETHROUGH) ===== */
.price-original--burning {
  position: relative;
  color: #94a3b8;
  font-size: 0.72rem;
  text-decoration: line-through;
  background: linear-gradient(90deg, #ff4500, #ff6b35, #ffb703, #ff4500);
  background-size: 200% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: heatFlicker 2s linear infinite;
  will-change: background-position;
}

@keyframes heatFlicker {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* ===== HOT DISCOUNT BADGE ===== */
.price-discount--hot {
  padding: 2px 6px;
  border-radius: 4px;
  background: linear-gradient(135deg, #ff4500, #e63946);
  color: #fff;
  font-size: 0.62rem;
  font-weight: 900;
  box-shadow: 0 2px 8px rgba(255, 69, 0, 0.4);
  animation: discountPulse 2s ease-in-out infinite;
  will-change: box-shadow;
}

@keyframes discountPulse {
  0%, 100% { box-shadow: 0 2px 8px rgba(255, 69, 0, 0.4); }
  50% { box-shadow: 0 2px 14px rgba(255, 69, 0, 0.7), 0 0 20px rgba(255, 183, 3, 0.3); }
}

/* ===== FIERY BORDER ANIMATION ===== */
@keyframes fieryGradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.title-bolt {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #f87171, #dc2626);
  color: #fff;
  font-size: 0.95rem;
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.5);
  animation: pulse 1.8s infinite;
}
.section-subtitle {
  margin: 4px 0 0;
  color: #fecaca;
  font-size: 0.94rem;
  text-align: center;
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border: 1px solid rgba(103, 232, 249, 0.55);
  border-radius: 999px;
  background: rgba(0, 58, 112, 0.5);
  color: #fecaca;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.3px;
  text-decoration: none;
  transition: background 0.2s ease, transform 0.2s ease, color 0.2s ease;
}
.view-all-link:hover {
  background: rgba(34, 211, 238, 0.18);
  color: #fff;
  transform: translateX(2px);
}
.view-all-link i { font-size: 0.65rem; transition: transform 0.2s ease; }
.view-all-link:hover i { transform: translateX(3px); }


.state-container {
  min-height: 290px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fee2e2;
}
.state-container > i { margin-bottom: 12px; font-size: 2.6rem; }
.state-container--error { color: #fecaca; }
.loading-spinner {
  width: 44px;
  height: 44px;
  margin-bottom: 14px;
  border: 4px solid rgba(255, 255, 255, 0.25);
  border-top-color: var(--red);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

.carousel-container { position: relative; }
.carousel-track-wrapper { overflow: hidden; padding: 5px; }
.carousel-track { display: flex; gap: 16px; transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1); }
.product-slide { flex: 0 0 calc((100% - 48px) / 4); min-width: 0; }

.carousel-nav {
  position: absolute;
  z-index: 12;
  top: 47%;
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border: 2px solid #fca5a5;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 0 18px rgba(239, 68, 68, 0.8), 0 7px 18px rgba(127, 29, 29, 0.35);
  transform: translateY(-50%);
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.carousel-nav:hover:not(:disabled) { transform: translateY(-50%) scale(1.09); }
.carousel-nav:disabled { opacity: 0.35; cursor: not-allowed; box-shadow: none; }
.carousel-nav--prev { left: -47px; }
.carousel-nav--next { right: -47px; }

.product-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 405px;
  height: 100%;
  overflow: hidden;
  border: 1px solid #fca5a5;
  border-radius: 14px;
  background: #fff;
  color: #152033;
  text-decoration: none;
  box-shadow: 0 5px 20px rgba(127, 29, 29, 0.18), 0 0 8px rgba(239, 68, 68, 0.2);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.product-card::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 16px;
  padding: 2px;
  background: linear-gradient(135deg, #e63946, #ff4500, #ffb703, #ff4500, #e63946);
  background-size: 300% 300%;
  opacity: 0;
  z-index: -1;
  transition: opacity 0.35s ease;
  will-change: opacity, background-position;
  animation: fieryGradient 2.5s ease infinite;
}
.product-card:hover { transform: translateY(-6px); box-shadow: 0 13px 28px rgba(127, 29, 29, 0.27), 0 0 16px rgba(239, 68, 68, 0.35); }
.product-card:hover::before { opacity: 1; }
.product-image-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 18px;
  overflow: hidden;
  background: linear-gradient(180deg, #fff, #fff5f5);
}
.product-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 0;
  transition: opacity 0.35s ease, transform 0.25s ease;
}
.product-image.loaded { opacity: 1; }
.product-card:hover .product-image { transform: scale(1.05); }

.image-skeleton {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, #fce7f3 0%, #fff5f5 50%, #fce7f3 100%);
  background-size: 200% 100%;
  animation: shimmer 1.2s linear infinite;
  transition: opacity 0.25s ease;
}
.image-skeleton.hidden { opacity: 0; pointer-events: none; }

/* Fallback shown when product image is missing or fails to load */
.image-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 18px;
  text-align: center;
  background: linear-gradient(135deg, #fce7f3 0%, #ffe4e6 100%);
  color: #64748b;
}
.image-fallback i {
  font-size: 2.4rem;
  color: #94a3b8;
  opacity: 0.8;
}
.image-fallback-text {
  display: -webkit-box;
  overflow: hidden;
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1.3;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: #475569;
}

/* "MỚI" badge pinned to top-left corner of every product card */
.product-badge {
  position: absolute;
  z-index: 4;
  top: 10px;
  left: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px 6px 9px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.5);
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.6px;
  line-height: 1;
  pointer-events: none;
  text-transform: uppercase;
  will-change: box-shadow;
}
.product-badge i { font-size: 0.7rem; }
.product-badge span { display: inline-block; }
.wishlist-btn {
  position: absolute;
  z-index: 5;
  top: 10px;
  right: 10px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid #fca5a5;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.94);
  color: #dc2626;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.1);
  transition: transform 0.2s ease, color 0.2s ease;
}
.wishlist-btn:hover { transform: scale(1.1); color: #ef4444; }
.wishlist-btn.active { color: #ef4444; }
.wishlist-btn.loading { opacity: 0.6; pointer-events: none; }

.quick-actions {
  position: absolute;
  z-index: 4;
  right: 10px;
  bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  opacity: 0;
  transform: translateY(10px);
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.product-card:hover .quick-actions { opacity: 1; transform: translateY(0); }
.qa-btn {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 5px 14px rgba(220, 38, 38, 0.45);
  font-size: 0.8rem;
  transition: transform 0.2s ease, background 0.2s ease;
}
.qa-btn--quick { background: rgba(15, 23, 42, 0.78); }
.qa-btn--quick:hover { background: #0f172a; transform: scale(1.08); }
.qa-btn--cart { background: linear-gradient(135deg, #ef4444, #dc2626); }
.qa-btn--cart:hover { transform: scale(1.08); }

/* Card baseline helpers - force equal vertical spacing so every card aligns
   price + cart button at the bottom edge regardless of title length */
.product-info { flex: 1; display: flex; flex-direction: column; padding: 14px 14px 15px; }
.product-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 21px;
  margin-bottom: 6px;
}
.authentic-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 7px;
  border-radius: 5px;
  background: #dcfce7;
  color: #15803d;
  font-size: 0.56rem;
  font-weight: 900;
  white-space: nowrap;
}
.product-category {
  overflow: hidden;
  color: #64748b;
  font-size: 0.58rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* Title is locked to exactly 2 lines so the bottom block of every card
   starts at the same vertical position */
.product-name {
  display: -webkit-box;
  min-height: 48px;
  margin: 0 0 10px;
  overflow: hidden;
  color: #14213d;
  font-size: 0.88rem;
  font-weight: 800;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.product-rating {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 22px;
  margin-bottom: 12px;
}
.stars { display: flex; gap: 1px; color: #fbbf24; font-size: 0.67rem; }
.rating-count { color: #94a3b8; font-size: 0.65rem; white-space: nowrap; }
.rating-count--new {
  padding: 2px 7px;
  border-radius: 5px;
  background: #fef2f2;
  color: #dc2626;
  font-weight: 800;
}
/* margin-top: auto pushes the price + cart button to the bottom of the card */
.card-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 4px;
}
.product-price { min-width: 0; flex: 1; }
.price-current { display: block; color: #c81624; font-size: 1.05rem; font-weight: 900; }
.old-price-row { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.price-original { color: #94a3b8; font-size: 0.72rem; text-decoration: line-through; }
.price-discount { padding: 2px 6px; border-radius: 4px; background: #ffe4e6; color: #e11d48; font-size: 0.62rem; font-weight: 900; }
.cart-btn {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin-bottom: 1px;
  border: 1px solid #fca5a5;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 0 13px rgba(239, 68, 68, 0.55);
  transition: transform 0.2s ease;
}
.cart-btn:hover { transform: scale(1.09) rotate(-5deg); }

.carousel-dots { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 20px; }
.dot { width: 7px; height: 7px; padding: 0; border: 0; border-radius: 999px; background: rgba(255,255,255,.4); cursor: pointer; transition: width .2s ease, background .2s ease; }
.dot.active { width: 25px; background: #f87171; box-shadow: 0 0 8px rgba(248, 113, 113, 0.7); }
.footer-banner { margin: 19px 0 0; color: rgba(255, 255, 255, 0.88); font-size: 0.72rem; text-align: center; }
.footer-banner span { margin: 0 7px; color: #f87171; }

.cart-toast {
  position: fixed;
  z-index: 9999;
  top: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  min-width: 280px;
  max-width: min(360px, calc(100vw - 32px));
  padding: 14px 16px;
  border: 1px solid #bfe7cc;
  border-radius: 12px;
  background: #f0fff5;
  color: #166534;
  box-shadow: 0 10px 28px rgba(26, 34, 51, 0.16);
  font-size: 14px;
  font-weight: 600;
  gap: 10px;
}
.cart-toast.error { border-color: #fecaca; background: #fef2f2; color: #991b1b; }
.cart-toast i { color: #16a34a; font-size: 22px; }
.cart-toast.error i { color: #dc2626; }
.toast-enter-active, .toast-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-12px); }

@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.2); } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

@media (max-width: 1100px) {
  .product-slide { flex-basis: calc((100% - 32px) / 3); }
  .carousel-nav--prev { left: -36px; }
  .carousel-nav--next { right: -36px; }
}

@media (max-width: 768px) {
  .new-products-section { padding: 72px 12px 45px; }
  .tech-panel { padding: 28px 14px 18px; }
  .folder-tabs { left: 12px; right: 12px; }
  .folder-tab { flex-basis: 135px; }
  .product-slide { flex-basis: calc((100% - 16px) / 2); }
  .carousel-nav { width: 40px; height: 40px; }
  .carousel-nav--prev { left: -29px; }
  .carousel-nav--next { right: -29px; }
}

@media (max-width: 575px) {
  .section-header { margin-bottom: 18px; padding: 0 2px; }
  .section-subtitle { font-size: 0.82rem; }
  .product-slide { flex-basis: 100%; }
  .product-card { min-height: 390px; }
  .carousel-nav--prev { left: -25px; }
  .carousel-nav--next { right: -25px; }
  .footer-banner { line-height: 1.7; }
  .footer-banner span { margin: 0 3px; }
}
</style>
