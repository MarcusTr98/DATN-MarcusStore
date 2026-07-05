<template>
  <!-- ===== TOP PROMO BANNER ===== -->
  <div class="top-promo-banner">
    <div class="container-fluid text-center py-1">
      <span class="marquee-text">
        🔥 Siêu sale tháng 7 – Giảm đến 50% | Free ship toàn quốc đơn từ 299k | Trả góp 0% lãi suất
        mọi sản phẩm 🔥
      </span>
    </div>
  </div>

  <!-- ===== MAIN CONTENT ===== -->
  <main class="home-page-upgraded">
    <!-- ============ HERO SLIDER ============ -->
    <section class="hero">
      <div class="hero-glow"></div>
      <div class="container hero-inner py-5">
        <div class="row align-items-center gy-5">
          <div class="col-lg-6">
            <span class="badge-eyebrow">Cập nhật máy hot nhất 07/2026</span>
            <h1 class="hero-title">Đổi mới. <span class="text-accent-red">Trả góp 0%.</span></h1>
            <p class="hero-lead">
              Sở hữu ngay iPhone, iPad, Samsung Galaxy chính hãng — trả góp 0% lãi suất, thu cũ đổi
              mới trợ giá đến 3.000.000đ, giao hàng trong 2 giờ.
            </p>
            <div class="hero-cta d-flex gap-3 flex-wrap">
              <router-link to="/category/dien-thoai" class="btn btn-primary-red">
                Mua điện thoại <i class="fas fa-arrow-right ms-2"></i>
              </router-link>
              <router-link to="/category/may-tinh-bang" class="btn btn-ghost-light">
                Xem iPad
              </router-link>
            </div>

            <div class="hero-dots mt-5">
              <span
                v-for="(s, i) in heroSlides"
                :key="i"
                class="dot-btn"
                :class="{ active: i === activeHeroSlide }"
                @click="activeHeroSlide = i"
              ></span>
            </div>
          </div>

          <!-- Signature-->
          <div class="col-lg-6">
            <div class="phone-stage">
              <!-- iPad mờ phía sau, gợi ý dải sản phẩm rộng hơn (điện thoại + tablet) -->
              <div class="tablet-echo">
                <div class="tablet-cam"></div>
              </div>

              <div class="phone-shell">
                <div class="phone-notch"></div>
                <div class="phone-screen">
                  <transition name="fade" mode="out-in">
                    <div :key="activeHeroSlide" class="slide-content">
                      <p class="slide-kicker">{{ heroSlides[activeHeroSlide].kicker }}</p>
                      <p class="slide-name">{{ heroSlides[activeHeroSlide].name }}</p>
                      <p class="slide-price">{{ heroSlides[activeHeroSlide].price }}</p>
                      <div class="slide-badge">
                        <i class="fas fa-tag"></i> {{ heroSlides[activeHeroSlide].tag }}
                      </div>
                    </div>
                  </transition>
                </div>
              </div>
              <div class="phone-reflection"></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="container-xxl py-3">
      <!-- Section Quick Categories-->
      <section class="quick-cats py-4 my-3 rounded-4 shadow-sm">
        <div class="row g-3 g-md-4 text-center justify-content-center">
          <div class="col-4 col-md-2" v-for="cat in categories" :key="cat.name">
            <router-link :to="cat.to" class="cat-tile">
              <div class="cat-icon"><i :class="cat.icon"></i></div>
              <span class="fw-semibold mt-2 d-block">{{ cat.name }}</span>
            </router-link>
          </div>
        </div>
      </section>
      <HeroBanner />
      <FlashSaleSection />

      <!-- ============ BANNER THƯƠNG HIỆU ============ -->
      <section class="brand-banners py-5">
        <div class="row g-4">
          <div class="col-md-6" v-for="b in brandBanners" :key="b.name">
            <router-link :to="b.to" class="brand-banner" :style="{ background: b.bg }">
              <div>
                <span class="brand-kicker">{{ b.kicker }}</span>
                <h4>{{ b.name }}</h4>
                <span class="brand-cta">Khám phá ngay <i class="fas fa-arrow-right"></i></span>
              </div>
              <i :class="b.icon" class="brand-icon"></i>
            </router-link>
          </div>
        </div>
      </section>

      <!-- ============ SẢN PHẨM NỔI BẬT (TABS) ============ -->
      <section class="featured py-5">
        <div class="section-head text-center mb-4">
          <span class="kicker">ĐƯỢC MUA NHIỀU NHẤT</span>
          <h2 class="section-title">Sản phẩm nổi bật</h2>
        </div>

        <div class="tab-bar mb-4">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="row g-4">
          <div class="col-6 col-md-4 col-lg-3" v-for="p in filteredProducts" :key="p.id">
            <div class="product-card static">
              <div class="product-thumb" :style="{ background: p.color }">
                <i :class="p.icon"></i>
              </div>
              <h6 class="product-name">{{ p.name }}</h6>
              <div class="product-rating">
                <i class="fas fa-star" v-for="n in 5" :key="n"></i>
                <span>({{ p.sold }} đã bán)</span>
              </div>
              <div class="product-price">
                <span class="price-now">{{ formatPrice(p.price) }}</span>
                <span v-if="p.oldPrice" class="price-old">{{ formatPrice(p.oldPrice) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ BANNER ĐÔI ============ -->
      <section class="promo-duo py-5">
        <div class="row g-4">
          <div class="col-md-6">
            <div class="promo-card promo-red">
              <span class="kicker-light">THU CŨ ĐỔI MỚI</span>
              <h4>Trợ giá đến 3.000.000đ</h4>
              <p>Định giá máy cũ ngay tại quầy, lên đời máy mới chỉ trong 30 phút.</p>
              <router-link to="/thu-cu-doi-moi" class="btn btn-outline-light-pill">
                Định giá ngay
              </router-link>
            </div>
          </div>
          <div class="col-md-6">
            <div class="promo-card promo-dark">
              <span class="kicker-light">TRẢ GÓP 0%</span>
              <h4>Duyệt hồ sơ trong 15 phút</h4>
              <p>Liên kết trực tiếp các đối tác tài chính, không cần chứng minh thu nhập.</p>
              <router-link to="/tra-gop" class="btn btn-outline-light-pill">
                Tìm hiểu thêm
              </router-link>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ SẢN PHẨM (component của team) ============ -->
      <ProductCard mode="standalone" />

      <NewsAndInfoSection />
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import HeroBanner from '@/layouts/home/HomeBanner.vue'
import FlashSaleSection from '@/layouts/home/HomeFlashSale.vue'
import NewsAndInfoSection from '@/layouts/home/HomeNewAndInfo.vue'
import ProductCard from '@/components/client/ProductCard.vue'

// Data mượn từ Landing Page
const categories = ref([
  { name: 'Điện thoại', icon: 'fas fa-mobile-alt', to: '/category/dien-thoai' },
  { name: 'iPad / Tablet', icon: 'fas fa-tablet-alt', to: '/category/may-tinh-bang' },
  { name: 'Âm thanh', icon: 'fas fa-headphones', to: '/category/am-thanh' },
  { name: 'Đồng hồ TM', icon: 'far fa-clock', to: '/category/dong-ho-thong-minh' },
  { name: 'Sạc & Pin', icon: 'fas fa-plug', to: '/category/sac-pin' },
  { name: 'Ốp lưng', icon: 'fas fa-shield-alt', to: '/category/op-lung' },
])

const heroSlides = ref([
  {
    kicker: 'FLAGSHIP 2026',
    name: 'Samsung Galaxy S26 Ultra',
    price: '26.990.000đ',
    tag: 'Trả góp 0%',
  },
  {
    kicker: 'BÁN CHẠY NHẤT',
    name: 'iPhone 17 Pro Max',
    price: '32.990.000đ',
    tag: 'Thu cũ trợ giá 5.000.000đ',
  },
  {
    kicker: 'ĐÁNG MUA',
    name: 'iPad Air M3',
    price: '16.990.000đ',
    tag: 'Tặng bút Apple Pencil',
  },
])
const activeHeroSlide = ref(0)
let heroSliderTimer = null

onMounted(() => {
  heroSliderTimer = setInterval(() => {
    activeHeroSlide.value = (activeHeroSlide.value + 1) % heroSlides.value.length
  }, 4000)
})

onUnmounted(() => {
  clearInterval(heroSliderTimer)
})

/* ---------- Sản phẩm nổi bật (tabs) ---------- */
function formatPrice(v) {
  return v.toLocaleString('vi-VN') + 'đ'
}

const allProducts = ref([
  {
    id: 11,
    name: 'iPhone 17 Pro Max',
    price: 32990000,
    oldPrice: null,
    sold: 245,
    category: 'phone',
    icon: 'fas fa-mobile-alt',
    color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
  },
  {
    id: 12,
    name: 'Samsung Galaxy S26 Ultra',
    price: 26990000,
    oldPrice: 29990000,
    sold: 158,
    category: 'phone',
    icon: 'fas fa-mobile-alt',
    color: 'linear-gradient(135deg,#14151a,#3a3c46)',
  },
  {
    id: 13,
    name: 'Xiaomi 17',
    price: 15990000,
    oldPrice: 17490000,
    sold: 96,
    category: 'phone',
    icon: 'fas fa-mobile-alt',
    color: 'linear-gradient(135deg,#ffb627,#c98600)',
  },
  {
    id: 14,
    name: 'iPad Pro M4 11-inch',
    price: 26990000,
    oldPrice: null,
    sold: 74,
    category: 'tablet',
    icon: 'fas fa-tablet-alt',
    color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
  },
  {
    id: 15,
    name: 'iPad Air M3',
    price: 16990000,
    oldPrice: 18490000,
    sold: 112,
    category: 'tablet',
    icon: 'fas fa-tablet-alt',
    color: 'linear-gradient(135deg,#14151a,#3a3c46)',
  },
  {
    id: 16,
    name: 'Apple Watch Series 10',
    price: 10990000,
    oldPrice: null,
    sold: 88,
    category: 'accessory',
    icon: 'far fa-clock',
    color: 'linear-gradient(135deg,#ffb627,#c98600)',
  },
  {
    id: 17,
    name: 'AirPods Max',
    price: 12990000,
    oldPrice: 13990000,
    sold: 41,
    category: 'audio',
    icon: 'fas fa-headphones',
    color: 'linear-gradient(135deg,#e1121c,#7a0d13)',
  },
  {
    id: 18,
    name: 'Ốp lưng MagSafe chính hãng',
    price: 690000,
    oldPrice: null,
    sold: 203,
    category: 'accessory',
    icon: 'fas fa-shield-alt',
    color: 'linear-gradient(135deg,#14151a,#3a3c46)',
  },
])

const tabs = [
  { key: 'all', label: 'Tất cả' },
  { key: 'phone', label: 'Điện thoại' },
  { key: 'tablet', label: 'iPad / Tablet' },
  { key: 'audio', label: 'Âm thanh' },
  { key: 'accessory', label: 'Phụ kiện' },
]
const activeTab = ref('all')
const filteredProducts = computed(() =>
  activeTab.value === 'all'
    ? allProducts.value
    : allProducts.value.filter((p) => p.category === activeTab.value),
)

/* ---------- Banner thương hiệu ---------- */
const brandBanners = ref([
  {
    name: 'iPhone chính hãng',
    kicker: 'APPLE VN/A',
    icon: 'fab fa-apple',
    to: '/category/dien-thoai',
    bg: 'linear-gradient(135deg,#14151a,#2a2b33)',
  },
  {
    name: 'Samsung Galaxy',
    kicker: 'GALAXY AI',
    icon: 'fas fa-mobile-alt',
    to: '/category/dien-thoai',
    bg: 'linear-gradient(135deg,#e1121c,#8c0e15)',
  },
])
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&display=swap');

.home-page-upgraded {
  --clr-bg: #0b0b10;
  --clr-surface: #ffffff;
  --clr-surface-alt: #f6f5f3;
  --clr-red: #e1121c;
  --clr-red-dark: #b80e17;
  --clr-amber: #ffb627;
  --clr-ink: #14151a;
  --clr-muted: #6b7280;
  background: var(--clr-surface-alt);
}

.top-promo-banner {
  background: #ffffff;
  color: #e1121c;
  font-size: 0.78rem;
  font-weight: 700;
  overflow: hidden;
  letter-spacing: 0.3px;
  border-bottom: 1px solid #f0e3e3;
}

.marquee-text {
  display: inline-block;
  animation: marquee 22s linear infinite;
  white-space: nowrap;
}

.text-accent-red {
  color: var(--clr-red);
}

.hero {
  position: relative;
  background: var(--clr-bg);
  color: #fff;
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  top: -30%;
  right: -15%;
  width: 55%;
  height: 160%;
  background: radial-gradient(circle at center, rgba(225, 18, 28, 0.45) 0%, transparent 70%);
  filter: blur(10px);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
}

.badge-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  margin-bottom: 1.5rem;
  font-family: 'Space Grotesk', 'Be Vietnam Pro', sans-serif;
}

.badge-eyebrow .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--clr-amber);
  box-shadow: 0 0 0 3px rgba(255, 182, 39, 0.25);
}

.hero-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: clamp(2.4rem, 5vw, 3.6rem);
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 1.25rem;
}

.hero-lead {
  font-size: 1.05rem;
  color: rgba(255, 255, 255, 0.72);
  max-width: 32rem;
  margin-bottom: 2rem;
}

.btn-primary-red {
  background: var(--clr-red);
  border: none;
  color: #fff;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  border-radius: 999px;
  padding: 0.85rem 1.75rem;
  display: inline-flex;
  align-items: center;
  transition: all 0.25s ease;
  text-decoration: none;
}

.btn-primary-red:hover {
  background: var(--clr-red-dark);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(225, 18, 28, 0.35);
}

.btn-ghost-light {
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  background: transparent;
  border-radius: 999px;
  padding: 0.85rem 1.75rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  transition: all 0.25s ease;
  text-decoration: none;
}

.btn-ghost-light:hover {
  border-color: #fff;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.hero-dots {
  display: flex;
  gap: 8px;
}

.dot-btn {
  width: 26px;
  height: 4px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.25);
  cursor: pointer;
  transition: all 0.25s ease;
}

.dot-btn.active {
  background: var(--clr-red);
  width: 40px;
}

/* Phone mockup */
.phone-stage {
  display: flex;
  justify-content: center;
  position: relative;
}

/* iPad mờ phía sau, gợi ý dải sản phẩm rộng hơn (điện thoại + tablet) */
.tablet-echo {
  position: absolute;
  top: 6%;
  right: 6%;
  width: 300px;
  height: 400px;
  background: linear-gradient(160deg, #24252e, #0b0b10);
  border: 5px solid #2a2b36;
  border-radius: 28px;
  opacity: 0.55;
  transform: rotate(6deg);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.4);
}

.tablet-cam {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #454652;
}

.phone-shell {
  width: 260px;
  height: 500px;
  background: linear-gradient(160deg, #1c1d24, #0b0b10);
  border-radius: 42px;
  border: 6px solid #232430;
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.55),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04);
  position: relative;
  z-index: 1;
  padding: 22px 18px;
}

.phone-notch {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 18px;
  background: #0b0b10;
  border-radius: 12px;
}

.phone-screen {
  margin-top: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.slide-kicker {
  color: var(--clr-amber);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 0.6rem;
  font-family: 'Space Grotesk', 'Be Vietnam Pro', sans-serif;
}

.slide-name {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.4rem;
}

.slide-price {
  font-size: 1.3rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 1rem;
  font-family: 'Space Grotesk', 'Be Vietnam Pro', sans-serif;
}

.slide-badge {
  background: rgba(225, 18, 28, 0.15);
  border: 1px solid rgba(225, 18, 28, 0.4);
  color: #ff5a63;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.35s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.phone-reflection {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 40px;
  background: radial-gradient(ellipse at center, rgba(225, 18, 28, 0.35), transparent 70%);
  filter: blur(8px);
}

/* Style cho Danh mục nhanh */
.quick-cats {
  background: var(--clr-surface);
}

.cat-tile {
  text-decoration: none;
  color: var(--clr-ink);
  transition: all 0.25s ease;
}

.cat-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: var(--clr-surface-alt);
  color: var(--clr-red);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin: 0 auto;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.cat-tile:hover .cat-icon {
  background: var(--clr-red);
  color: #fff;
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(225, 18, 28, 0.2);
}

@keyframes marquee {
  0% {
    transform: translateX(100vw);
  }
  100% {
    transform: translateX(-100%);
  }
}

/* ============ RESPONSIVE (Hero Slider) ============ */
@media (max-width: 992px) {
  .phone-shell {
    width: 220px;
    height: 440px;
  }
  .tablet-echo {
    width: 250px;
    height: 340px;
  }
}

@media (max-width: 576px) {
  .tablet-echo {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.1rem;
  }
}

/* ============ BRAND BANNERS ============ */
.brand-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 22px;
  padding: 2.5rem;
  color: #fff;
  text-decoration: none;
  min-height: 180px;
  transition: all 0.3s ease;
}

.brand-banner:hover {
  transform: translateY(-4px);
  color: #fff;
}

.brand-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--clr-amber);
}

.brand-banner h4 {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 1.6rem;
  font-weight: 700;
  margin: 0.4rem 0 1rem;
}

.brand-cta {
  font-size: 0.85rem;
  font-weight: 600;
}

.brand-icon {
  font-size: 3.2rem;
  opacity: 0.25;
}

/* ============ SẢN PHẨM NỔI BẬT ============ */
.section-head .kicker {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--clr-red);
  margin-bottom: 0.5rem;
}

.section-title {
  font-weight: 700;
}

.tab-bar {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  justify-content: center;
}

.tab-btn {
  border: 1px solid #e5e2dd;
  background: #fff;
  color: var(--clr-muted);
  padding: 0.5rem 1.2rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  transition: all 0.2s ease;
}

.tab-btn.active,
.tab-btn:hover {
  background: var(--clr-red);
  border-color: var(--clr-red);
  color: #fff;
}

.product-card {
  position: relative;
  background: #fff;
  border-radius: 18px;
  padding: 1.25rem;
  transition: all 0.25s ease;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 14px 28px rgba(20, 21, 26, 0.08);
}

.product-card.static {
  width: 100%;
  background: var(--clr-surface-alt);
}

.product-thumb {
  width: 100%;
  height: 110px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 34px;
  margin-bottom: 0.9rem;
}

.product-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--clr-ink);
  margin-bottom: 0.5rem;
  min-height: 2.4em;
}

.product-rating {
  color: var(--clr-amber);
  font-size: 0.72rem;
  margin-bottom: 0.5rem;
}

.product-rating span {
  color: var(--clr-muted);
  margin-left: 4px;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 0.6rem;
}

.price-now {
  font-weight: 700;
  color: var(--clr-red);
  font-size: 1rem;
}

.price-old {
  font-size: 0.78rem;
  color: var(--clr-muted);
  text-decoration: line-through;
}

/* ============ BANNER ĐÔI ============ */
.promo-card {
  border-radius: 22px;
  padding: 2.5rem;
  color: #fff;
  height: 100%;
}

.promo-red {
  background: linear-gradient(135deg, var(--clr-red), #8c0e15);
}

.promo-dark {
  background: linear-gradient(135deg, var(--clr-bg), #2a2b33);
}

.kicker-light {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--clr-amber);
}

.promo-card h4 {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 1.5rem;
  margin: 0.5rem 0 0.75rem;
}

.promo-card p {
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 1.5rem;
}

.btn-outline-light-pill {
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
  border-radius: 999px;
  padding: 0.65rem 1.5rem;
  font-weight: 600;
  font-size: 0.88rem;
  text-decoration: none;
  display: inline-block;
  transition: all 0.25s ease;
}

.btn-outline-light-pill:hover {
  background: #fff;
  color: var(--clr-ink);
}

@media (max-width: 768px) {
  .brand-banner {
    padding: 1.75rem;
  }
}
</style>
