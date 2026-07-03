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
            <span class="title-line">MARCUS</span>
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
        <span class="card-tag" :style="{ animationDelay: (index * 0.35) + 's' }">
          <svg viewBox="0 0 24 24" class="lightning-icon"><path d="M13 2 3 14h7l-1 8 11-14h-7z"/></svg>
          SALE
        </span>
        <span class="card-discount">-{{ product.discount }}%</span>
        <div class="thumb">
          <span class="product-emoji">{{ product.emoji }}</span>

        </div>
        <div class="card-name">{{ product.name }}</div>
        <div class="card-spec">{{ product.spec }}</div>
        <div class="price-row">
          <span class="price-now">{{ formatPrice(product.displayPrice) }}</span>
          <span class="price-old">{{ formatPrice(product.originalPrice) }}</span>
        </div>
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
        <button type="button" class="cta" @click.stop="handleBuyClick($event, product)">
          Mua ngay
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import '@/assets/css/FlashSalePage.css'
const router = useRouter()

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

// Products
const flashSaleProducts = ref([
  { id: 1, name: 'iPhone 15 Pro Max 256GB', slug: 'iphone-15-pro-max', spec: 'Titan tự nhiên · 5G', emoji: '📱', price: 27990000, originalPrice: 34990000, discount: 20, soldPercent: 85, left: 6, displayPrice: 0, ripples: [] },
  { id: 2, name: 'Samsung Galaxy S24 Ultra', slug: 'samsung-galaxy-s24-ultra', spec: '12GB/256GB · Galaxy AI', emoji: '📱', price: 22490000, originalPrice: 29990000, discount: 25, soldPercent: 92, left: 3, displayPrice: 0, ripples: [] },
  { id: 3, name: 'MacBook Air M2 13"', slug: 'macbook-air-m2', spec: '8GB/256GB · Midnight', emoji: '💻', price: 21990000, originalPrice: 28990000, discount: 24, soldPercent: 78, left: 9, displayPrice: 0, ripples: [] },
  { id: 4, name: 'Laptop Gaming ROG Strix', slug: 'rog-strix', spec: 'RTX 4060 · 16GB/512GB', emoji: '💻', price: 26990000, originalPrice: 35990000, discount: 25, soldPercent: 64, left: 12, displayPrice: 0, ripples: [] },
  { id: 5, name: 'Apple Watch Series 9', slug: 'apple-watch-series-9', spec: 'GPS · 45mm', emoji: '⌚', price: 8490000, originalPrice: 11990000, discount: 29, soldPercent: 97, left: 2, displayPrice: 0, ripples: [] },
  { id: 6, name: 'Samsung Galaxy Watch 6', slug: 'galaxy-watch-6', spec: 'Bluetooth · 44mm', emoji: '⌚', price: 5290000, originalPrice: 7490000, discount: 29, soldPercent: 70, left: 14, displayPrice: 0, ripples: [] },
  { id: 7, name: 'Loa JBL Flip 6', slug: 'jbl-flip-6', spec: 'Chống nước IP67', emoji: '🔊', price: 2190000, originalPrice: 3290000, discount: 33, soldPercent: 99, left: 3, displayPrice: 0, ripples: [] },
  { id: 8, name: 'AirPods Pro 2', slug: 'airpods-pro-2', spec: 'Chống ồn chủ động', emoji: '🎧', price: 4990000, originalPrice: 6490000, discount: 23, soldPercent: 88, left: 7, displayPrice: 0, ripples: [] },
])

const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

const goToProduct = (product) => {
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

onMounted(() => {
  timerInterval = setInterval(tickTimer, 1000)
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
  clearInterval(timerInterval)
  clearInterval(fomoTimer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>

</style>


<style>

</style>
