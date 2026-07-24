<template>
  <!-- ===== TOP PROMO BANNER ===== -->
  <div class="top-promo-banner">
    <div class="container-fluid text-center py-1">
      <span class="marquee-text">{{
        sysSettings.PROMO_TEXT ||
        '🔥 Siêu sale tháng 7 – Giảm đến 50% | Free ship toàn quốc đơn từ 299k | Trả góp 0% lãi suất mọi sản phẩm 🔥'
      }}</span>
    </div>
  </div>

  <!-- ===== MAIN CONTENT ===== -->
  <main class="home-page-upgraded">
    <!-- ============ HERO SLIDER ============ -->
    <section class="hero">
      <div class="hero-grid" aria-hidden="true"></div>
      <div class="hero-glow"></div>
      <div class="hero-orb hero-orb-one" aria-hidden="true"></div>
      <div class="hero-orb hero-orb-two" aria-hidden="true"></div>
      <div class="container hero-inner py-5">
        <div class="row align-items-center gy-5">
          <div class="col-lg-6">
            <span class="badge-eyebrow"
              ><span class="badge-pulse"></span
              >{{ sysSettings.HOME_HERO_BADGE || 'Cập nhật máy hot nhất 07/2026' }}</span
            >
            <h1 class="hero-title">
              {{ sysSettings.HOME_HERO_TITLE || 'Đổi mới.' }}
              <span class="text-accent-red">{{
                sysSettings.HOME_HERO_TITLE_ACCENT || 'Trả góp 0%.'
              }}</span>
            </h1>
            <p class="hero-lead">
              {{
                sysSettings.HOME_HERO_LEAD ||
                'Sở hữu ngay iPhone, iPad, Samsung Galaxy chính hãng — trả góp 0% lãi suất, thu cũ đổi mới trợ giá đến 3.000.000đ, giao hàng trong 2 giờ.'
              }}
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
              <div class="stage-ring stage-ring-one" aria-hidden="true"></div>
              <div class="stage-ring stage-ring-two" aria-hidden="true"></div>
              <div
                v-for="(benefit, index) in heroBenefits"
                :key="benefit.text"
                class="floating-chip"
                :class="benefit.position"
                :style="{ '--chip-delay': `${index * 0.18}s`, '--sparkle-delay': `${index * 0.7}s` }"
              >
                <span class="chip-icon"><i :class="benefit.icon"></i></span>
                <span>{{ benefit.text }}</span>
                <span class="chip-sparkle" aria-hidden="true">✦</span>
              </div>
              <!-- iPad mờ phía sau, gợi ý dải sản phẩm rộng hơn (điện thoại + tablet) -->
              <div class="tablet-echo">
                <div class="tablet-cam"></div>
              </div>

              <div class="phone-shell">
                <div class="phone-notch"></div>
                <div class="phone-screen">
                  <p class="phone-time">{{ currentTime }}</p>
                  <p class="phone-date">{{ currentDate }}</p>
                  <transition name="fade" mode="out-in">
                    <div :key="activeHeroSlide" class="slide-content">
                      <p class="slide-kicker">{{ currentSlide.kicker }}</p>
                      <p class="slide-name">{{ currentSlide.name }}</p>
                      <p class="slide-price">{{ currentSlide.price }}</p>
                      <div class="slide-badge">
                        <i class="fas fa-tag"></i> {{ currentSlide.tag }}
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

      <!-- ============ SẢN PHẨM HOT ============ -->
      <HomeHotProducts />

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

      <!-- ============ SẢN PHẨM ============ -->
      <ProductCard mode="standalone" />

      <!-- ============ BÀI VIẾT  ============ -->
      <NewsAndInfoSection />

      <!-- ============ tHÔNG TIN ============ -->
      <ClientSlider />
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/utils/api' // Bổ sung import API trực tiếp
import HeroBanner from '@/layouts/home/HomeBanner.vue'
import FlashSaleSection from '@/layouts/home/HomeFlashSale.vue'
import NewsAndInfoSection from '@/layouts/home/HomeNewAndInfo.vue'
import HomeHotProducts from '@/layouts/home/HomeNewProducts.vue'
import ProductCard from '@/components/client/ProductCard.vue'
import ClientSlider from '@/components/client/ClientSlider.vue'

// Khởi tạo state nội bộ để reactivity 100%
const sysSettings = ref({})

const fetchSettings = async () => {
  try {
    const res = await api.get('/public/settings')
    sysSettings.value = res.data
  } catch (error) {
    console.error('Lỗi khi tải cấu hình trang chủ:', error)
  }
}

// Data mượn từ Landing Page
const categories = ref([
  { name: 'Điện thoại', icon: 'fas fa-mobile-alt', to: '/category/dien-thoai' },
  { name: 'iPad / Tablet', icon: 'fas fa-tablet-alt', to: '/category/may-tinh-bang' },
  { name: 'Âm thanh', icon: 'fas fa-headphones', to: '/category/am-thanh' },
  { name: 'Đồng hồ TM', icon: 'far fa-clock', to: '/category/dong-ho-thong-minh' },
  { name: 'Sạc & Pin', icon: 'fas fa-plug', to: '/category/sac-pin' },
  { name: 'Ốp lưng', icon: 'fas fa-shield-alt', to: '/category/op-lung' },
])

const defaultHeroSlides = [
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
  { kicker: 'ĐÁNG MUA', name: 'iPad Air M3', price: '16.990.000đ', tag: 'Tặng bút Apple Pencil' },
]

const heroSlides = computed(() => {
  const slidesData = sysSettings.value.HOME_HERO_SLIDES
  if (!slidesData) return defaultHeroSlides
  try {
    const parsed = JSON.parse(slidesData)
    return Array.isArray(parsed) && parsed.length ? parsed : defaultHeroSlides
  } catch {
    return defaultHeroSlides
  }
})

const activeHeroSlide = ref(0)
let heroSliderTimer = null

const heroBenefits = [
  { text: 'Giao nhanh 2h', icon: 'fas fa-bolt', position: 'chip-top-right' },
  { text: 'Chính hãng 100%', icon: 'fas fa-shield-alt', position: 'chip-bottom-left' },
  { text: 'Bảo hành uy tín', icon: 'fas fa-award', position: 'chip-top-left' },
  { text: 'Đổi trả dễ dàng', icon: 'fas fa-sync-alt', position: 'chip-bottom-right' },
]

const currentSlide = computed(() => {
  const slides = heroSlides.value
  return slides[activeHeroSlide.value % slides.length] || slides[0]
})

onMounted(() => {
  fetchSettings() // Gọi API ngay khi mount component

  heroSliderTimer = setInterval(() => {
    if (heroSlides.value.length > 0) {
      activeHeroSlide.value = (activeHeroSlide.value + 1) % heroSlides.value.length
    }
  }, 4000)
})

onUnmounted(() => {
  clearInterval(heroSliderTimer)
})

/*  Đồng hồ hiện tại trên màn hình điện thoại  */
const now = ref(new Date())
let clockTimer = null

const currentTime = computed(() =>
  now.value.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false }),
)

const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']

const currentDate = computed(() => {
  const d = now.value
  const weekday = weekdays[d.getDay()]
  const day = String(d.getDate()).padStart(2, '0')
  const month = String(d.getMonth() + 1).padStart(2, '0')
  return `${weekday}, ${day} tháng ${month}`
})

onMounted(() => {
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  clearInterval(clockTimer)
})

/*  Banner thương hiệu  */
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
  min-height: 650px;
  display: flex;
  align-items: center;
  isolation: isolate;
  background:
    radial-gradient(circle at 78% 42%, rgba(225, 18, 28, 0.14), transparent 28%),
    linear-gradient(135deg, #08080c 0%, #101018 55%, #09090e 100%);
  color: #fff;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 120px;
  background: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.28));
  pointer-events: none;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: linear-gradient(to right, rgba(0, 0, 0, 0.85), transparent 78%);
  animation: grid-drift 18s linear infinite;
  pointer-events: none;
}

.hero-glow {
  position: absolute;
  top: -30%;
  right: -15%;
  width: 55%;
  height: 160%;
  background: radial-gradient(circle at center, rgba(225, 18, 28, 0.45) 0%, transparent 70%);
  filter: blur(18px);
  animation: glow-breathe 6s ease-in-out infinite;
  pointer-events: none;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(1px);
  pointer-events: none;
}

.hero-orb-one {
  width: 9px;
  height: 9px;
  top: 18%;
  left: 7%;
  background: var(--clr-red);
  box-shadow: 0 0 24px 8px rgba(225, 18, 28, 0.32);
  animation: orb-float 7s ease-in-out infinite;
}

.hero-orb-two {
  width: 5px;
  height: 5px;
  right: 10%;
  bottom: 18%;
  background: var(--clr-amber);
  box-shadow: 0 0 20px 6px rgba(255, 182, 39, 0.24);
  animation: orb-float 9s ease-in-out 1s infinite reverse;
}

.hero-inner {
  position: relative;
  z-index: 2;
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

.badge-pulse {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--clr-amber);
  box-shadow: 0 0 0 0 rgba(255, 182, 39, 0.5);
  animation: badge-pulse 2s ease-out infinite;
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
  letter-spacing: -0.045em;
  text-wrap: balance;
  animation: hero-reveal 0.75s 0.1s both;
}

.hero-lead {
  font-size: 1.05rem;
  color: rgba(255, 255, 255, 0.72);
  max-width: 32rem;
  margin-bottom: 2rem;
  line-height: 1.75;
  animation: hero-reveal 0.75s 0.2s both;
}

.hero-cta {
  animation: hero-reveal 0.75s 0.3s both;
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
  min-height: 520px;
  align-items: center;
  perspective: 1200px;
}

.stage-ring {
  position: absolute;
  left: 50%;
  top: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
  transform: translate(-50%, -50%) rotateX(68deg);
  pointer-events: none;
}

.stage-ring-one {
  width: 480px;
  height: 480px;
  animation: ring-spin 18s linear infinite;
}

.stage-ring-two {
  width: 390px;
  height: 390px;
  border-style: dashed;
  border-color: rgba(225, 18, 28, 0.28);
  animation: ring-spin 13s linear infinite reverse;
}

.floating-chip {
  position: absolute;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  background: rgba(18, 18, 26, 0.68);
  box-shadow: 0 14px 35px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(14px);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.74rem;
  font-weight: 600;
  white-space: nowrap;
  opacity: 0;
  animation:
    chip-pop-in 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) var(--chip-delay) forwards,
    chip-glow 3.2s ease-in-out var(--sparkle-delay) infinite;
}

.chip-icon {
  display: grid;
  place-items: center;
  width: 27px;
  height: 27px;
  border-radius: 9px;
  background: rgba(255, 182, 39, 0.12);
}

.chip-icon i {
  color: var(--clr-amber);
  animation: icon-ting 3.2s ease-in-out var(--sparkle-delay) infinite;
}

.chip-sparkle {
  position: absolute;
  top: -9px;
  right: -5px;
  color: #fff3b0;
  font-size: 0.85rem;
  opacity: 0;
  filter: drop-shadow(0 0 5px rgba(255, 182, 39, 0.9));
  animation: sparkle-ting 3.2s ease-out var(--sparkle-delay) infinite;
}

.chip-top-right {
  top: 12%;
  right: 1%;
}

.chip-bottom-left {
  left: 0;
  bottom: 14%;
}

.chip-top-left {
  left: 2%;
  top: 28%;
}

.chip-bottom-right {
  right: -1%;
  bottom: 29%;
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
  transform: rotateY(-7deg) rotateX(2deg);
  animation: phone-float 6s ease-in-out infinite;
  transition: transform 0.45s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.phone-stage:hover .phone-shell {
  transform: rotateY(0) rotateX(0) translateY(-6px);
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
  margin-top: 46px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  overflow: hidden;
  min-height: 360px;
  border-radius: 25px;
  background:
    radial-gradient(circle at 50% 85%, rgba(225, 18, 28, 0.18), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), transparent);
}

.phone-screen::after {
  content: '';
  position: absolute;
  top: -35%;
  left: -70%;
  width: 45%;
  height: 180%;
  transform: rotate(18deg);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  animation: screen-shine 6s ease-in-out infinite;
  pointer-events: none;
}

.phone-time {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 2.6rem;
  font-weight: 600;
  color: #fff;
  margin-bottom: 0;
  line-height: 1;
}

.phone-date {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 1.75rem;
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

@keyframes hero-reveal {
  from {
    opacity: 0;
    transform: translateY(22px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes grid-drift {
  to {
    background-position: 64px 64px;
  }
}

@keyframes glow-breathe {
  0%,
  100% {
    opacity: 0.72;
    transform: scale(0.96);
  }
  50% {
    opacity: 1;
    transform: scale(1.06);
  }
}

@keyframes badge-pulse {
  70% {
    box-shadow: 0 0 0 7px rgba(255, 182, 39, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 182, 39, 0);
  }
}

@keyframes phone-float {
  0%,
  100% {
    transform: rotateY(-7deg) rotateX(2deg) translateY(0);
  }
  50% {
    transform: rotateY(-4deg) rotateX(1deg) translateY(-12px);
  }
}

@keyframes chip-pop-in {
  from {
    opacity: 0;
    transform: scale(0.72);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes chip-glow {
  0%, 60%, 100% {
    border-color: rgba(255, 255, 255, 0.14);
    box-shadow: 0 14px 35px rgba(0, 0, 0, 0.3);
  }
  70% {
    border-color: rgba(255, 182, 39, 0.48);
    box-shadow:
      0 14px 35px rgba(0, 0, 0, 0.3),
      0 0 20px rgba(255, 182, 39, 0.28);
  }
}

@keyframes icon-ting {
  0%,
  60%,
  84%,
  100% {
    transform: rotate(0) scale(1);
  }
  68% {
    transform: rotate(-14deg) scale(1.28);
  }
  75% {
    transform: rotate(12deg) scale(1.1);
  }
}

@keyframes sparkle-ting {
  0%,
  62%,
  100% {
    opacity: 0;
    transform: scale(0.2) rotate(0);
  }
  68% {
    opacity: 1;
    transform: scale(1.35) rotate(90deg);
  }
  78% {
    opacity: 0;
    transform: scale(0.6) rotate(160deg) translateY(-5px);
  }
}

@keyframes orb-float {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(28px, -20px);
  }
}

@keyframes ring-spin {
  from {
    transform: translate(-50%, -50%) rotateX(68deg) rotateZ(0);
  }
  to {
    transform: translate(-50%, -50%) rotateX(68deg) rotateZ(360deg);
  }
}

@keyframes screen-shine {
  0%,
  55% {
    left: -70%;
  }
  80%,
  100% {
    left: 135%;
  }
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
  .hero {
    min-height: auto;
  }
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

  .stage-ring {
    display: none;
  }

  .floating-chip {
    padding: 7px 9px;
    gap: 6px;
    border-radius: 11px;
    font-size: 0.64rem;
  }

  .chip-icon {
    width: 22px;
    height: 22px;
  }

  .phone-stage {
    min-height: 460px;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.1rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero *,
  .hero *::before,
  .hero *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
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

/* ============ ĐẶC QUYỀN / ƯU ĐÃI - slider đôi ============ */
.perk-slider {
  --perk-accent: #f2711c;
}

.perk-slider-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.perk-slider-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--clr-ink);
  margin: 0;
}

.perk-slider-nav {
  display: flex;
  gap: 10px;
}

.perk-nav-btn {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  background: #ffffff;
  color: #6b7280;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  transition: all 0.2s ease;
}

.perk-nav-btn:hover {
  border-color: var(--perk-accent);
  color: var(--perk-accent);
  transform: translateY(-2px);
}

.perk-viewport {
  overflow: hidden;
}

.perk-track {
  display: flex;
  transition: transform 0.45s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.perk-page {
  flex: 0 0 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.5rem;
}

.perk-card {
  position: relative;
  border-radius: 18px;
  border-left: 4px solid var(--perk-accent);
  background: linear-gradient(135deg, #fff6ee 0%, #ffffff 55%);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  padding: 1.75rem 1.75rem 1.6rem;
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;
}

.perk-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
}

.perk-eyebrow {
  display: block;
  color: var(--perk-accent);
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

.perk-title {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 1.7rem;
  font-weight: 800;
  font-style: italic;
  color: var(--clr-ink);
  margin: 0 0 0.6rem;
  text-transform: uppercase;
}

.perk-desc {
  margin: 0;
  color: var(--clr-muted);
  font-size: 0.92rem;
  line-height: 1.5;
}

.perk-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 1.5rem;
}

.perk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d8dee8;
  cursor: pointer;
  transition: all 0.25s ease;
}

.perk-dot.active {
  width: 22px;
  border-radius: 4px;
  background: var(--perk-accent);
}

@media (max-width: 768px) {
  .perk-page {
    grid-template-columns: 1fr;
  }

  .perk-title {
    font-size: 1.4rem;
  }

  .perk-card {
    padding: 1.4rem;
  }
}
</style>
