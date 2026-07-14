<template>
  <div class="flash-sale-page">
    <!-- Timeline Bar -->
    <section class="flash-timeline">
      <span class="timeline-label">Flash Sale hôm nay</span>
      <div
        v-for="(slot, index) in flashSlots"
        :key="slot.slotId || index"
        class="slot"
        :class="{ live: slot.isLive }"
      >
        <span class="dot"></span>
        {{ slot.time }}
        <span v-if="!slot.isLive" class="bell">🔔</span>
      </div>
    </section>

    <!-- Hero Banner Section -->
    <section class="flash-hero" :class="{ 'has-banner': activeSlot?.bannerImageUrl }">
      <!-- Banner image làm background khi có -->
      <div v-if="activeSlot?.bannerImageUrl" class="hero-banner-bg">
        <img :src="activeSlot.bannerImageUrl" alt="Flash Sale Banner" />
        <div class="hero-banner-overlay"></div>
      </div>

      <div class="hero-bg">
        <!-- Wave shapes -->
        <div class="wave wave-1"></div>
        <div class="wave wave-2"></div>
        <div class="wave wave-3"></div>

        <!-- Decorative circles -->
        <div class="deco-circle deco-1"></div>
        <div class="deco-circle deco-2"></div>

        <!-- Lightning decorations -->
        <div class="lightning-deco lightning-1">
          <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
        </div>
        <div class="lightning-deco lightning-2">
          <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
        </div>

        <!-- Falling Dots Container - inside hero-bg for proper positioning -->
        <div class="falling-dots" ref="fallingDotsContainer"></div>
      </div>

      <div class="hero-content">
        <div class="hero-left">
          <div class="hero-badge">
            <span class="badge-icon">⚡</span>
            <span>FLASH SALE</span>
          </div>
          <h1 class="hero-title">
            <span class="title-line accent">FLASH SALE</span>
            <span class="title-line">{{ slotName }}</span>

          </h1>


          <div class="countdown-wrapper">
            <span class="countdown-label">{{ countdownLabel || 'Kết thúc sau' }}</span>
            <div class="countdown" id="countdown">
              <div class="unit">
                <div class="digits">
                  <span class="digit">{{ timer.hours[0] }}</span>
                  <span class="digit">{{ timer.hours[1] }}</span>
                </div>
                <div class="unit-label">Giờ</div>
              </div>
              <div class="colon">:</div>
              <div class="unit">
                <div class="digits">
                  <span class="digit">{{ timer.minutes[0] }}</span>
                  <span class="digit">{{ timer.minutes[1] }}</span>
                </div>
                <div class="unit-label">Phút</div>
              </div>
              <div class="colon">:</div>
              <div class="unit">
                <div class="digits">
                  <span class="digit">{{ timer.seconds[0] }}</span>
                  <span class="digit">{{ timer.seconds[1] }}</span>
                </div>
                <div class="unit-label">Giây</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Stats chỉ hiện khi không có banner -->
        <div v-if="!activeSlot?.bannerImageUrl" class="hero-right">
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-number">{{ stats.totalProducts || '200+' }}</span>
              <span class="stat-label">Sản phẩm</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">{{ stats.maxDiscount || '50%' }}</span>
              <span class="stat-label">Giảm giá</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">{{ stats.liveLabel || '24h' }}</span>
              <span class="stat-label">{{ stats.liveSubLabel || 'Chỉ hôm nay' }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Product Section -->
    <div class="section-head">
      <h2>Đang <span>cháy</span> hàng</h2>
      <router-link to="/" class="see-all">← Quay lại trang chủ</router-link>
    </div>

     <section class="product-grid">
      <article
        v-for="(product, index) in flashSaleProducts"
        :key="product.id"
        class="flash-card"
        :class="{ 'low-stock': product.left <= 3 }"
        :style="{ '--enter-delay': (index * 80) + 'ms' }"
        @click="goToProduct(product)"
      >
        <!-- Khối badge góc trên trái: SALE + % giảm -->
        <div class="card-badges">
          <span class="card-tag" :style="{ animationDelay: (index * 0.35) + 's' }">
            <svg viewBox="0 0 24 24" class="lightning-icon"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
            SALE
          </span>
          <span class="card-discount">Giảm {{ product.discount }}%</span>
        </div>

        <!-- Nút góc trên phải: tim + giỏ hàng -->
        <div class="card-actions" @click.stop>
          <button
            type="button"
            class="icon-btn"
            :class="{ active: product.favorited }"
            :title="product.favorited ? 'Đã yêu thích' : 'Yêu thích'"
            @click="toggleFavorite(product)"
          >
            <svg viewBox="0 0 24 24" class="icon-svg">
              <path
                :fill="product.favorited ? 'currentColor' : 'none'"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
              />
            </svg>
          </button>
          <button
            type="button"
            class="icon-btn"
            :class="{ adding: product.addingToCart }"
            title="Thêm vào giỏ hàng"
            @click.stop="addToCart(product)"
          >
            <svg viewBox="0 0 24 24" class="icon-svg">
              <path
                v-if="!product.addingToCart"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm-8 2a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"
              />
              <path
                v-else
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M5 13l4 4L19 7"
              />
            </svg>
          </button>
        </div>

        <!-- Ảnh sản phẩm -->
        <div class="thumb">
          <img
            v-if="product.image"
            :src="product.image"
            :alt="product.name"
            class="product-image"
            @error="(e) => { console.warn('[FlashSalePage] image failed:', product.image, e); e.target.style.display='none' }"
          />
          <span v-else class="product-emoji">{{ product.emoji }}</span>
        </div>

        <!-- Tên + mô tả -->
        <div class="card-name">{{ product.name }}</div>
        <div class="card-spec">{{ product.spec }}</div>

        <!-- Thanh tiến trình kho -->
        <div class="scarcity">
          <div class="bar-row">
            <span>Đã bán {{ product.soldPercent }}%</span>
            <span class="blink">Sắp cháy · Còn {{ product.left }} SP</span>
          </div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: product.soldPercent + '%' }">
              <span class="bar-shimmer"></span>
            </div>
          </div>
        </div>

        <!-- Giá -->
        <div class="price-row">
          <span class="price-now">{{ formatPrice(product.displayPrice) }}</span>
          <span class="price-old">{{ formatPrice(product.originalPrice) }}</span>
        </div>

        <!-- Chip biến thể -->
        <div v-if="product.variants && product.variants.length" class="variant-chips">
          <span
            v-for="(variant, vIdx) in product.variants"
            :key="vIdx"
            class="variant-chip"
          >
            {{ variant }}
          </span>
        </div>

        <!-- Tag khuyến mãi đi kèm -->
        <ul v-if="product.promos && product.promos.length" class="promo-tags">
          <li v-for="(promo, pIdx) in product.promos" :key="pIdx" class="promo-tag">
            <span class="promo-icon" v-html="promo.icon"></span>
            <span class="promo-text">{{ promo.text }}</span>
          </li>
        </ul>

        <!-- CTA lớn: MUA NGAY -->
        <button type="button" class="cta" @click.stop="handleBuyClick($event, product)">
          <span class="cta-text">MUA NGAY</span>
          <span class="cta-icon">
            <svg viewBox="0 0 24 24"><path
              fill="none" stroke="currentColor" stroke-width="2.5"
              stroke-linecap="round" stroke-linejoin="round"
              d="M5 12h14M13 6l6 6-6 6"
            /></svg>
          </span>
          <span
            v-for="ripple in product.ripples"
            :key="ripple.id"
            class="ripple"
            :style="{ left: ripple.x + 'px', top: ripple.y + 'px' }"
          ></span>
        </button>
      </article>
    </section>

    <!-- Promo Banners -->
    <section class="promo-grid">
      <div class="promo promo-b">
        <div class="tag">Ưu đãi thêm</div>
        <h3>Thu cũ đổi mới</h3>
        <p>Trợ giá đến 3 triệu khi lên đời máy mới.</p>
      </div>
      <div class="promo promo-b">
        <div class="tag">Thanh toán linh hoạt</div>
        <h3>Trả góp 0%</h3>
        <p>Duyệt nhanh 5 phút, không cần chứng minh thu nhập.</p>
      </div>
      <div class="promo promo-b">
        <div class="tag">Giao hàng siêu tốc</div>
        <h3>SHIP HỎA TỐC </h3>
        <p>Nhận hàng ngay trong 2 giờ tại nội thành. Miễn phí vận chuyển cho đơn hàng Flash Sale.</p>
      </div>
      <div class="promo promo-b">
        <div class="tag">Độc quyền MarcusStore
        </div>
        <h3>BẢO HÀNH 1 ĐỔI 1</h3>
        <p>Lỗi là đổi mới trong 30 ngày đầu tiên. Bảo hành chính hãng lên đến 24 tháng.</p>
      </div>
    </section>

    <!-- Footer -->
    <footer class="flash-footer">
      <div class="footer-left">
        <router-link to="/" class="footer-logo">
          <i class="fas fa-mobile-alt"></i>
          <span>Marcus<strong>STORE</strong></span>
        </router-link>
        <p>Hệ thống cửa hàng công nghệ hàng đầu Việt Nam</p>
      </div>
      <div class="footer-right">
        <router-link to="/" class="back-home">
          <i class="fas fa-home"></i>
          Quay về trang chủ
        </router-link>
      </div>
    </footer>

    <!-- FOMO Popup -->
    <div class="fomo" :class="{ show: fomoVisible }" role="status" aria-live="polite">
      <button class="fomo-close" @click="closeFomo">✕</button>
      <div class="fomo-dot">
        <svg viewBox="0 0 24 24"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
      </div>
      <div class="fomo-text">
        <b>{{ fomoMessage }}</b>
        <span>{{ fomoSub }}</span>
      </div>
      <div class="fomo-progress-track">
        <div v-if="fomoVisible" :key="fomoKey" class="fomo-progress-bar"></div>
      </div>
    </div>

    <!-- Toast thông báo — Teleport ra body để tránh clip bởi overflow/transform -->
    <Teleport to="body">
      <Transition name="fsp-toast">
        <div v-if="toast.show" class="fsp-toast-alert">
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import { useFlashSaleCountdown } from '@/composables/useFlashSaleCountdown'
import { useCartStore } from '@/stores/cartStore'
import '@/assets/css/FlashSalePage.css'

const router = useRouter()
const flashSaleStore = useFlashSaleStore()
const cartStore = useCartStore()
const { clientSlots, clientLoading, displaySlots, bannerStats } = storeToRefs(flashSaleStore)
const stats = computed(() => bannerStats.value || {})

// ==== Lấy dữ liệu Flash Sale từ BE ====
async function fetchFlashSales(silent = false) {
  await flashSaleStore.fetchClientSlots(20)
}

// Tên slot Flash Sale đang hiển thị (ưu tiên ACTIVE, rồi SCHEDULED).
// Rơi về 'FLASH SALE' nếu BE chưa có dữ liệu.
const activeSlot = computed(() => {
  if (!Array.isArray(clientSlots.value) || clientSlots.value.length === 0) return null
  return (
    clientSlots.value.find((s) => Number(s.status) === 2) ||
    clientSlots.value.find((s) => Number(s.status) === 1) ||
    clientSlots.value[0]
  )
})
const slotName = computed(() => activeSlot.value?.name || 'FLASH SALE')

// ==== Bộ đếm ngược động (chạy mỗi giây) ====
// Hết giờ -> tự gọi lại API để cập nhật danh sách mà không cần F5.
const { label: countdownLabel, timer } = useFlashSaleCountdown(
  () => flashSaleStore.clientSlots,
  () => fetchFlashSales(true),
)

// Timeline: lấy từ store getter (đã chuẩn hoá ở store), fallback hiển thị placeholder
const flashSlots = computed(() => {
  const list = displaySlots.value
  if (Array.isArray(list) && list.length > 0) return list
  // Fallback tĩnh khi BE chưa có dữ liệu để UI không bị trống
  return [
    { time: 'Đang diễn ra · 09:00–12:00', isLive: true },
    { time: '12:00 SA', isLive: false },
    { time: '16:00 CH', isLive: false },
    { time: '20:00 CH', isLive: false },
  ]
})

// ==== Sản phẩm hiển thị: lấy từ tất cả items của các slot trong clientSlots ====
// BE trả về nhiều slot; gộp items[] của tất cả slot, lấy tối đa 12 SP đầu tiên.
function mapItemToCard(item, idx) {
  const totalQty = Number(item.flashSaleQuantity ?? 0)
  const soldQty = Number(item.soldQuantity ?? 0)
  const soldPercent = totalQty > 0 ? Math.min(100, Math.round((soldQty / totalQty) * 100)) : 0
  const remaining = Math.max(0, Number(item.remainingQuantity ?? totalQty - soldQty))
  return {
    id: item.skuId ?? `fs-${idx}`,
    skuId: item.skuId ?? item.id,
    name: item.productName || item.skuCode || `Sản phẩm Flash Sale #${idx + 1}`,
    slug: item.skuCode || `flash-sale-${item.skuId ?? idx}`,
    spec: item.skuCode ? `Mã: ${item.skuCode}` : 'Sản phẩm chính hãng',
    emoji: '🛍️',
    image: item.skuImageUrl || null,
    price: Number(item.flashSalePrice ?? item.originalPrice ?? 0),
    originalPrice: Number(item.originalPrice ?? item.flashSalePrice ?? 0),
    discount: item.discountPercent ?? 0,
    soldPercent,
    left: remaining,
    displayPrice: 0,
    ripples: [],
    favorited: false,
    addingToCart: false,
    variants: deriveVariantsFromSku(item.skuCode),
    promos: derivePromoTags(item),
    // Thêm thông tin slot để gửi khi thêm vào giỏ
    slotId: item._slotId || null,
    slotName: item._slotName || null,
  }
}

function deriveVariantsFromSku(skuCode) {
  if (!skuCode) return []
  const parts = String(skuCode).split('-').filter(Boolean)
  const tail = parts.slice(-2)
  return tail.map((p) => p.replace(/_/g, ' ').trim()).filter((p) => p.length > 0)
}

function derivePromoTags(item) {
  const tags = []
  const price = Number(item.originalPrice ?? 0)
  if (price > 10000000) {
    tags.push({ icon: '🎓', text: 'Học sinh/Sinh viên giảm đến 500.000đ' })
  }
  tags.push({ icon: '🛡️', text: 'Bảo hành chính hãng 12 tháng' })
  return tags
}

// Tổng hợp sản phẩm từ mọi slot đang/sắp diễn ra
const flashSaleProducts = computed(() => {
  if (!Array.isArray(clientSlots.value) || clientSlots.value.length === 0) return []
  const allItems = []
  for (const slot of clientSlots.value) {
    if (Array.isArray(slot.items)) {
      for (const it of slot.items) {
        // Thêm slotId vào mỗi item để biết nó thuộc slot nào
        allItems.push({ ...it, _slotId: slot.slotId })
      }
    }
  }
  return allItems.slice(0, 12).map(mapItemToCard)
})

// formatPrice, goToProduct, animateCountUp, ripple, favorite, add-to-cart, FOMO
const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

// ==== Toast ====
const toast = reactive({
  show: false,
  type: 'warning',
  title: '',
  message: '',
})
let toastTimer = null
function showToast({ type = 'warning', title, message }) {
  clearTimeout(toastTimer)
  toast.type = type
  toast.title = title
  toast.message = message
  toast.show = true
  toastTimer = setTimeout(() => {
    toast.show = false
  }, 2800)
}

// ==== Điều hướng sản phẩm ====
// Chỉ cho phép click khi có slot ACTIVE (status=2); nếu không thì chặn + hiển thị toast.
const isFlashSaleActive = computed(() => {
  if (!Array.isArray(clientSlots.value) || clientSlots.value.length === 0) return false
  return clientSlots.value.some((s) => Number(s.status) === 2)
})

const goToProduct = (product) => {
  if (!isFlashSaleActive.value) {
    showToast({
      type: 'warning',
      title: 'Oops!',
      message: 'Flash Sale chưa bắt đầu, hãy chờ thêm nhé!',
    })
    return
  }
  router.push(`/product/${product.slug}`)
}

// --- Hiệu ứng "đếm số" cho giá khi card xuất hiện ---
const animateCountUp = (product, delayMs = 0) => {
  const duration = 900
  const to = product.price
  let startTs = null

  const step = (now) => {
    if (startTs === null) startTs = now + delayMs
    if (now < startTs) {
      requestAnimationFrame(step)
      return
    }
    const elapsed = now - startTs
    const t = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - t, 3) // easeOutCubic
    product.displayPrice = Math.round(to * eased)
    if (t < 1) requestAnimationFrame(step)
    else product.displayPrice = to
  }
  requestAnimationFrame(step)
}

// --- Hiệu ứng ripple khi bấm "Mua ngay" ---
const handleBuyClick = (event, product) => {
  const btn = event.currentTarget
  const rect = btn.getBoundingClientRect()
  const ripple = {
    id: Date.now() + Math.random(),
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
  product.ripples.push(ripple)
  setTimeout(() => {
    product.ripples = product.ripples.filter((r) => r.id !== ripple.id)
  }, 650)

  // TODO: gắn logic thêm vào giỏ hàng thật ở đây
  addToCart(product)
}

// Toggle yêu thích (UI-only — chưa gắn API wishlist)
const toggleFavorite = (product) => {
  product.favorited = !product.favorited
}

// Thêm vào giỏ hàng - kết nối API thật với Flash Sale
const addToCart = async (product) => {
  if (product.addingToCart) return
  product.addingToCart = true
  
  try {
    // Kiểm tra xem Flash Sale có đang active không
    if (!isFlashSaleActive.value) {
      showToast({
        type: 'warning',
        title: 'Oops!',
        message: 'Flash Sale chưa bắt đầu, hãy chờ thêm nhé!',
      })
      return
    }

    // Tìm slot đang active để lấy slotId
    const activeSlot = clientSlots.value.find(s => Number(s.status) === 2)
    
    if (!activeSlot || !product.slotId) {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: 'Không tìm thấy thông tin Flash Sale',
      })
      return
    }

    // Kiểm tra số lượng còn lại
    if (product.left <= 0) {
      showToast({
        type: 'error',
        title: 'Hết hàng!',
        message: 'Sản phẩm đã hết hàng trong Flash Sale này',
      })
      return
    }

    // Gọi API thêm vào giỏ với thông tin Flash Sale
    const success = await cartStore.addToCartWithFlashSale(
      product.skuId,           // skuId
      1,                      // quantity
      activeSlot.slotId,      // flashSaleSlotId
      product.price           // flashSalePrice
    )
    
    if (success) {
      showToast({
        type: 'success',
        title: 'Thành công!',
        message: 'Đã thêm vào giỏ hàng',
      })
    } else {
      showToast({
        type: 'error',
        title: 'Lỗi!',
        message: cartStore.error || 'Không thể thêm vào giỏ',
      })
    }
  } catch (error) {
    console.error('Lỗi thêm vào giỏ:', error)
    showToast({
      type: 'error',
      title: 'Lỗi!',
      message: error.response?.data?.message || 'Không thể thêm vào giỏ hàng',
    })
  } finally {
    setTimeout(() => {
      product.addingToCart = false
    }, 700)
  }
}

// FOMO popup
const fomoVisible = ref(false)
const fomoMessage = ref('')
const fomoSub = ref('')
const fomoKey = ref(0)
const fomoMessages = [
  ['Anh T. vừa mua iPhone 15 Pro Max', 'TP. Hồ Chí Minh · 1 phút trước'],
  ['Chỉ còn 3 chiếc Loa JBL Flip 6 cuối cùng!', 'Kho Hà Nội · vừa xong'],
  ['Chị H. vừa đặt Samsung Galaxy S24 Ultra', 'Đà Nẵng · 3 phút trước'],
  ['Apple Watch Series 9 sắp hết size 45mm', 'Kho Hà Nội · vừa xong'],
  ['Anh K. vừa mua AirPods Pro 2', 'Hải Phòng · 4 phút trước'],
]
let fomoIdx = 0
let fomoTimer = null

const showNextFomo = () => {
  const [msg, sub] = fomoMessages[fomoIdx % fomoMessages.length]
  fomoMessage.value = msg
  fomoSub.value = sub
  fomoVisible.value = true
  fomoKey.value++ // ép progress-bar remount để chạy lại animation từ đầu
  fomoIdx++
  setTimeout(() => { fomoVisible.value = false }, 4200)
}

const closeFomo = () => {
  fomoVisible.value = false
  clearInterval(fomoTimer)
}

// Special Effects: Falling Dots
const fallingDotsContainer = ref(null)

const createFallingDots = () => {
  const container = fallingDotsContainer.value
  if (!container) {
    console.log('Container not found')
    return
  }

  const isMobile = window.innerWidth < 640
  const count = isMobile ? 60 : 120

  // Quãng đường rơi = đúng chiều cao thật của banner (thay vì số cứng 400px)
  const fallDistance = container.offsetHeight || 340

  container.innerHTML = '' // Clear existing

  for (let i = 0; i < count; i++) {
    const dot = document.createElement('div')
    dot.classList.add('falling-dot')

    // Random properties với distribution đều
    const x = Math.random() * 100 // 0% - 100% spread evenly
    const size = 3 + Math.random() * 8
    const duration = 2.5 + Math.random() * 4 // 2.5s - 6.5s
    const delay = Math.random() * 6 // 0s - 6s spread
    const isWhite = Math.random() > 0.4

    // Gán trực tiếp từng style property
    dot.style.position = 'absolute'
    dot.style.left = `${x}%`
    dot.style.top = '-20px' // Fixed start position above
    dot.style.width = `${size}px`
    dot.style.height = `${size}px`
    dot.style.background = isWhite ? '#fff' : '#fbbf24'
    dot.style.borderRadius = '50%'
    dot.style.setProperty('--fall-distance', `${fallDistance}px`)
    dot.style.animationName = 'fallDown'
    dot.style.animationDuration = `${duration}s`
    dot.style.animationDelay = `${delay}s`
    dot.style.animationTimingFunction = 'linear'
    dot.style.animationIterationCount = 'infinite'
    dot.style.boxShadow = `0 0 ${size}px ${isWhite ? 'rgba(255,255,255,0.9)' : 'rgba(251,191,36,0.9)'}`
    dot.style.opacity = '0' // Start invisible

    container.appendChild(dot)
  }
  console.log('Created', count, 'falling dots')
}

let resizeHandler = null

onMounted(async () => {
  // Tải dữ liệu Flash Sale ACTIVE + sắp diễn ra từ BE
  await fetchFlashSales(false)
  createFallingDots()

  // Tạo lại các chấm khi resize để quãng rơi luôn khớp chiều cao banner
  resizeHandler = () => createFallingDots()
  window.addEventListener('resize', resizeHandler)

  // Đếm số giá tăng dần, chạy sau khi card entrance animation gần xong, so le từng card
  flashSaleProducts.value.forEach((product, index) => {
    animateCountUp(product, 300 + index * 80)
  })

  setTimeout(showNextFomo, 1800)
  fomoTimer = setInterval(showNextFomo, 6000)
})

onUnmounted(() => {
  clearInterval(fomoTimer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>

</style>


<style>

</style>
