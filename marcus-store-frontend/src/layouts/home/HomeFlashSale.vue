<template>
  <div class="flash-sale-page">


    <!-- Hero Banner Section -->
    <section class="flash-hero">
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
            <span class="title-line">{{ featuredSlotName }}</span>
            <span class="title-line accent">FLASH SALE</span>
          </h1>
          <p class="hero-subtitle">Giảm đến <strong>50%++</strong> cho hàng ngàn sản phẩm công nghệ</p>

          <div class="countdown-wrapper">
            <span class="countdown-label">Kết thúc sau</span>
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

        <div class="hero-right">
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-number">200+</span>
              <span class="stat-label">Sản phẩm</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">50%</span>
              <span class="stat-label">Giảm giá</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">24h</span>
              <span class="stat-label">Chỉ hôm nay</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Product Section -->
    <div class="section-head">
      <h2>Đang <span>cháy</span> hàng</h2>
      <router-link to="/khuyen-mai" class="see-all">Đi đến trang khuyến mãi</router-link>
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
            @error="(e) => { console.warn('[HomeFlashSale] image failed:', product.image, e); e.target.style.display='none' }"
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

        <!-- Chip biến thể (màu sắc, dung lượng...) -->
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useFlashSaleStore } from '@/stores/FlashSaleStore'
import '@/assets/css/FlashSalePage.css'

const router = useRouter()
const flashSaleStore = useFlashSaleStore()
const { clientSlots, clientLoading, featuredSlot } = storeToRefs(flashSaleStore)

// Tên slot Flash Sale đang hiển thị (ưu tiên ACTIVE, rồi SCHEDULED).
const featuredSlotName = computed(() => {
  const slot = featuredSlot.value
  if (slot && slot.name) return slot.name
  if (Array.isArray(clientSlots.value) && clientSlots.value.length > 0) {
    const upcoming = clientSlots.value.find((s) => Number(s.status) === 1)
    if (upcoming?.name) return upcoming.name
  }
  return 'FLASH SALE'
})

// Countdown Timer
const timer = ref({ hours: '05', minutes: '32', seconds: '47' })
let timerInterval = null

const tickTimer = () => {
  let h = parseInt(timer.value.hours)
  let m = parseInt(timer.value.minutes)
  let s = parseInt(timer.value.seconds)
  s--
  if (s < 0) { s = 59; m-- }
  if (m < 0) { m = 59; h-- }
  if (h < 0) { h = 0; m = 0; s = 0 }
  timer.value = {
    hours: String(h).padStart(2, '0'),
    minutes: String(m).padStart(2, '0'),
    seconds: String(s).padStart(2, '0'),
  }
}

// Timeline slots
const flashSlots = ref([
  { time: 'Đang diễn ra · 09:00–12:00', isLive: true },
  { time: '12:00 SA', isLive: false },
  { time: '16:00 CH', isLive: false },
  { time: '20:00 CH', isLive: false },
])

// === Sản phẩm hiển thị trên trang chủ ===
// Lấy từ featuredSlot.items khi có dữ liệu từ BE; fallback về dữ liệu mẫu nếu BE rỗng.
const fallbackProducts = [
  {
    id: 'fs-1',
    name: 'iPhone 15 Pro Max 256GB',
    slug: 'iphone-15-pro-max',
    spec: 'Titan tự nhiên · 5G',
    emoji: '📱',
    image: null,
    price: 27990000,
    originalPrice: 34990000,
    discount: 20,
    soldPercent: 85,
    left: 6,
    displayPrice: 0,
    ripples: [],
    favorited: false,
    addingToCart: false,
    variants: ['Titan Tự Nhiên', '256GB'],
    promos: [
      { icon: '🎓', text: 'Học sinh/Sinh viên giảm đến 500.000đ' },
      { icon: '🎁', text: 'Quà tặng kèm: Tai nghe 300k' },
      { icon: '🛡️', text: 'Bảo hành chính hãng 12 tháng' },
    ],
  },
  {
    id: 'fs-2',
    name: 'Samsung Galaxy S24 Ultra',
    slug: 'samsung-galaxy-s24-ultra',
    spec: '12GB/256GB · Galaxy AI',
    emoji: '📱',
    image: null,
    price: 22490000,
    originalPrice: 29990000,
    discount: 25,
    soldPercent: 92,
    left: 3,
    displayPrice: 0,
    ripples: [],
    favorited: false,
    addingToCart: false,
    variants: ['Titan Gray', '256GB'],
    promos: [
      { icon: '🎓', text: 'Học sinh/Sinh viên giảm đến 400.000đ' },
      { icon: '🎁', text: 'Quà tặng kèm: Sạc nhanh 25W' },
      { icon: '🛡️', text: 'Bảo hành chính hãng 12 tháng' },
    ],
  },
  {
    id: 'fs-3',
    name: 'MacBook Air M2 13"',
    slug: 'macbook-air-m2',
    spec: '8GB/256GB · Midnight',
    emoji: '💻',
    image: null,
    price: 21990000,
    originalPrice: 28990000,
    discount: 24,
    soldPercent: 78,
    left: 9,
    displayPrice: 0,
    ripples: [],
    favorited: false,
    addingToCart: false,
    variants: ['Midnight', '8GB / 256GB'],
    promos: [
      { icon: '🎓', text: 'Sinh viên giảm thêm đến 5%' },
      { icon: '🛡️', text: 'Bảo hành chính hãng 12 tháng' },
    ],
  },
  {
    id: 'fs-4',
    name: 'Laptop Gaming ROG Strix',
    slug: 'rog-strix',
    spec: 'RTX 4060 · 16GB/512GB',
    emoji: '💻',
    image: null,
    price: 26990000,
    originalPrice: 35990000,
    discount: 25,
    soldPercent: 64,
    left: 12,
    displayPrice: 0,
    ripples: [],
    favorited: false,
    addingToCart: false,
    variants: ['Eclipse Gray', '16GB / 512GB'],
    promos: [
      { icon: '🎁', text: 'Tặng kèm: Chuột gaming + Túi' },
      { icon: '🛡️', text: 'Bảo hành chính hãng 24 tháng' },
    ],
  },
]

// Map item từ FlashSaleStore (BE) sang shape card UI
function mapItemToCard(item, idx) {
  const totalQty = Number(item.flashSaleQuantity ?? 0)
  const soldQty = Number(item.soldQuantity ?? 0)
  const soldPercent = totalQty > 0 ? Math.min(100, Math.round((soldQty / totalQty) * 100)) : 0
  const remaining = Math.max(0, Number(item.remainingQuantity ?? totalQty - soldQty))

  return {
    id: item.skuId ?? `fs-${idx}`,
    name: item.productName || item.skuCode || `Sản phẩm Flash Sale #${idx + 1}`,
    slug: item.skuCode || `flash-sale-${item.skuId ?? idx}`,
    spec: item.skuCode ? `Mã: ${item.skuCode}` : 'Sản phẩm chính hãng',
    emoji: '🛍️',
    image: item.skuImageUrl || null,
    price: Number(item.flashSalePrice ?? 0),
    originalPrice: Number(item.originalPrice ?? item.flashSalePrice ?? 0),
    discount: item.discountPercent ?? 0,
    soldPercent,
    left: remaining,
    displayPrice: 0,
    ripples: [],
    favorited: false,
    addingToCart: false,
    // Tách phần biến thể từ skuCode (vd "IP15PM-256-TIT") — fallback hiển thị mã SKU gọn
    variants: item.skuCode ? deriveVariantsFromSku(item.skuCode) : [],
    // Các tag khuyến mãi đi kèm hiển thị cố định (BE chưa trả) — dùng helper này khi API chưa có
    promos: derivePromoTags(item),
  }
}

// Tách phân biệt mãu/dung lượng từ skuCode theo pattern "PREFIX-COLOR-STORAGE"
function deriveVariantsFromSku(skuCode) {
  if (!skuCode) return []
  const parts = String(skuCode).split('-').filter(Boolean)
  // Bỏ phần prefix model, lấy 2 đoạn cuối
  const tail = parts.slice(-2)
  return tail
    .map((p) => p.replace(/_/g, ' ').trim())
    .filter((p) => p.length > 0)
}

// Sinh promo tags mặc định (BE chưa có field promo thì vẫn hiển thị 3 tag quen thuộc)
function derivePromoTags(item) {
  const tags = []
  const price = Number(item.originalPrice ?? 0)
  if (price > 10000000) {
    tags.push({ icon: '🎓', text: 'Học sinh/Sinh viên giảm đến 500.000đ' })
  }
  tags.push({ icon: '🛡️', text: 'Bảo hành chính hãng 12 tháng' })
  return tags
}

const flashSaleProducts = computed(() => {
  const slot = featuredSlot.value
  if (slot && Array.isArray(slot.items) && slot.items.length) {
    return slot.items.slice(0, 8).map(mapItemToCard)
  }
  return fallbackProducts
})

const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

const goToProduct = (product) => {
  router.push(`/product/${product.slug}`)
}

// Đếm số tăng dần cho giá khi card xuất hiện
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

// Ripple khi bấm "MUA NGAY"
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

  // TODO: gắn logic thêm vào giỏ hàng + chuyển checkout thật ở đây
  addToCart(product)
}

// Toggle yêu thích (UI-only — chưa gắn API wishlist)
const toggleFavorite = (product) => {
  product.favorited = !product.favorited
}

// Thêm vào giỏ (UI-only — chưa gắn cart API; hiển thị tick xanh ~600ms)
const addToCart = (product) => {
  if (product.addingToCart) return
  product.addingToCart = true
  setTimeout(() => {
    product.addingToCart = false
  }, 700)
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
    return
  }

  const isMobile = window.innerWidth < 640
  const count = isMobile ? 60 : 120

  // Quãng đường rơi = đúng chiều cao thật của banner
  const fallDistance = container.offsetHeight || 340

  container.innerHTML = ''

  for (let i = 0; i < count; i++) {
    const dot = document.createElement('div')
    dot.classList.add('falling-dot')

    const x = Math.random() * 100
    const size = 3 + Math.random() * 8
    const duration = 2.5 + Math.random() * 4
    const delay = Math.random() * 6
    const isWhite = Math.random() > 0.4

    dot.style.position = 'absolute'
    dot.style.left = `${x}%`
    dot.style.top = '-20px'
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
    dot.style.opacity = '0'

    container.appendChild(dot)
  }
}

let resizeHandler = null

onMounted(async () => {
  timerInterval = setInterval(tickTimer, 1000)
  createFallingDots()

  resizeHandler = () => createFallingDots()
  window.addEventListener('resize', resizeHandler)

  // Kéo dữ liệu Flash Sale ACTIVE từ BE (nếu có) để đổ vào card
  try {
    if (!flashSaleStore.clientSlots || flashSaleStore.clientSlots.length === 0) {
      await flashSaleStore.fetchClientSlots(8)
    }
  } catch (e) {
    console.warn('[HomeFlashSale] không tải được dữ liệu flash sale:', e)
  }

  // Đếm số giá tăng dần sau khi card entrance animation gần xong
  flashSaleProducts.value.forEach((product, index) => {
    animateCountUp(product, 300 + index * 80)
  })

  setTimeout(showNextFomo, 1800)
  fomoTimer = setInterval(showNextFomo, 6000)
})

onUnmounted(() => {
  clearInterval(timerInterval)
  clearInterval(fomoTimer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>
</style>


<style>
</style>