<template>
  <section class="flash-sale-section mb-4">
    <div
      class="flash-sale-header d-flex align-items-center justify-content-between px-3 py-2 rounded-top"
    >
      <div class="d-flex align-items-center gap-3">
        <span class="flash-icon">⚡</span>
        <h3 class="flash-title mb-0">FLASH SALE</h3>
        <div class="countdown d-flex align-items-center gap-1">
          <span class="countdown-label">Kết thúc trong:</span>
          <span class="countdown-block">{{ timer.hours }}</span>
          <span class="countdown-sep">:</span>
          <span class="countdown-block">{{ timer.minutes }}</span>
          <span class="countdown-sep">:</span>
          <span class="countdown-block">{{ timer.seconds }}</span>
        </div>
      </div>
      <a href="#" class="flash-view-all">Xem tất cả →</a>
    </div>

    <div class="flash-sale-body px-2 py-3">
      <div class="horizontal-scroll-wrapper">
        <div class="d-flex gap-3 pb-2">
          <div
            v-for="product in flashSaleProducts"
            :key="product.id"
            class="flash-card flex-shrink-0"
          >
            <div class="discount-badge">-{{ product.discount }}%</div>
            <div class="product-img-wrapper">
              <div class="product-emoji-img">{{ product.emoji }}</div>
            </div>
            <div class="product-card-body">
              <p class="product-name">{{ product.name }}</p>
              <div class="price-row d-flex align-items-center gap-2 mb-1">
                <span class="price-current">{{ formatPrice(product.price) }}</span>
                <span class="price-original">{{ formatPrice(product.originalPrice) }}</span>
              </div>
              <div class="sold-bar-wrapper">
                <div class="progress sold-progress" style="height: 6px">
                  <div
                    class="progress-bar bg-danger"
                    :style="{ width: (product.sold / product.total) * 100 + '%' }"
                  ></div>
                </div>
                <small class="sold-text">Đã bán {{ product.sold }}/{{ product.total }}</small>
              </div>
            </div>
          </div>  
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// ---- COUNTDOWN TIMER (giữ fix cứng tạm thời) ----
const timer = ref({ hours: '05', minutes: '32', seconds: '47' })
let timerInterval = null

const tickTimer = () => {
  let h = parseInt(timer.value.hours)
  let m = parseInt(timer.value.minutes)
  let s = parseInt(timer.value.seconds)
  s--
  if (s < 0) {
    s = 59
    m--
  }
  if (m < 0) {
    m = 59
    h--
  }
  if (h < 0) {
    h = 0
    m = 0
    s = 0
  }
  timer.value = {
    hours: String(h).padStart(2, '0'),
    minutes: String(m).padStart(2, '0'),
    seconds: String(s).padStart(2, '0'),
  }
}

onMounted(() => {
  timerInterval = setInterval(tickTimer, 1000)
})
onUnmounted(() => {
  clearInterval(timerInterval)
})

// ---- FLASH SALE PRODUCTS (giữ fix cứng tạm thời) ----
const flashSaleProducts = ref([
  {
    id: 1,
    name: 'iPhone 15 128GB',
    emoji: '📱',
    price: 19990000,
    originalPrice: 24990000,
    discount: 20,
    sold: 78,
    total: 100,
  },
  {
    id: 2,
    name: 'Samsung S24 FE 256GB',
    emoji: '📱',
    price: 12490000,
    originalPrice: 16990000,
    discount: 26,
    sold: 55,
    total: 80,
  },
  {
    id: 3,
    name: 'OPPO Find X8 256GB',
    emoji: '📱',
    price: 14990000,
    originalPrice: 19990000,
    discount: 25,
    sold: 40,
    total: 60,
  },
  {
    id: 4,
    name: 'AirPods 4 (ANC)',
    emoji: '🎧',
    price: 3990000,
    originalPrice: 5290000,
    discount: 25,
    sold: 90,
    total: 100,
  },
  {
    id: 5,
    name: 'Apple Watch Series 10',
    emoji: '⌚',
    price: 9990000,
    originalPrice: 13490000,
    discount: 26,
    sold: 35,
    total: 50,
  },
  {
    id: 6,
    name: 'iPad Air M2 11-inch WiFi',
    emoji: '📟',
    price: 15990000,
    originalPrice: 20990000,
    discount: 24,
    sold: 22,
    total: 40,
  },
  {
    id: 7,
    name: 'Xiaomi 14T Pro 512GB',
    emoji: '📱',
    price: 13490000,
    originalPrice: 17990000,
    discount: 25,
    sold: 60,
    total: 100,
  },
  {
    id: 8,
    name: 'Laptop ASUS Vivobook 15',
    emoji: '💻',
    price: 11990000,
    originalPrice: 15990000,
    discount: 25,
    sold: 18,
    total: 30,
  },
])

const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
</script>

<style scoped>
.flash-sale-section {
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--cps-border);
}
.flash-sale-header {
  background: linear-gradient(120deg, #ff2d3a 0%, var(--cps-red) 50%, var(--cps-red-dark) 100%);
  color: #fff;
  padding-top: 12px;
  padding-bottom: 12px;
}
.flash-icon {
  font-size: 1.6rem;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.25));
}
.flash-title {
  font-size: 1.35rem;
  font-weight: 800;
  font-family: var(--font-display);
  letter-spacing: 1.5px;
  color: #fb4141;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}
.countdown {
  gap: 6px;
}
.countdown-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #fb4141;
  opacity: 0.95;
}
.countdown-block {
  background: #1a1a1a;
  color: #fffefc;
  font-size: 0.95rem;
  font-weight: 800;
  padding: 4px 9px;
  border-radius: 6px;
  min-width: 34px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}
.countdown-sep {
  font-weight: 800;
  font-size: 1.1rem;
  color: #fff;
}
.flash-view-all {
  color: #fff;
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 700;
  white-space: nowrap;
}
.flash-view-all:hover {
  text-decoration: underline;
}
.flash-sale-body {
  background: #fff9fa;
}
.horizontal-scroll-wrapper {
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: #ddd transparent;
}
.flash-card {
  width: 170px;
  background: #fff;
  border: 1px solid var(--cps-border);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
  position: relative;
}
.flash-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--cps-red-light);
}
.discount-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: var(--cps-red);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  font-family: var(--font-display);
  padding: 3px 8px;
  border-radius: 4px;
  z-index: 1;
  letter-spacing: 0.2px;
}
.product-img-wrapper {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  height: 150px;
}
.product-actual-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.flash-slot-banner-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 56px;
  height: 56px;
  object-fit: contain;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.35));
  pointer-events: none;
  z-index: 2;
}
.product-emoji-img {
  font-size: 3rem;
  user-select: none;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.08));
}
.product-card-body {
  padding: 0 14px 16px;
}
.product-name {
  font-size: 0.86rem;
  font-weight: 600;
  font-family: var(--font-body);
  color: var(--cps-text);
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.price-current {
  font-size: 1.02rem;
  font-weight: 800;
  font-family: var(--font-display);
  color: var(--cps-red);
  letter-spacing: -0.2px;
}
.price-original {
  font-size: 0.78rem;
  color: var(--cps-text-light);
  text-decoration: line-through;
}
.sold-bar-wrapper {
  margin-top: 8px;
}
.sold-progress {
  border-radius: 10px;
  background: var(--cps-red-tint);
}
.sold-text {
  font-size: 0.66rem;
  color: var(--cps-text-light);
}

@media (max-width: 768px) {
  .flash-title {
    font-size: 0.95rem;
  }
  .countdown-label {
    display: none;
  }
}
</style>
