<template>
  <div class="row g-3 mb-4">
    <!-- ==================== BANNER CHÍNH BÊN TRÁI (~68%) - SLIDER ==================== -->
    <div class="col-lg-8 col-md-7">
      <div class="hero-banner-main rounded-3 overflow-hidden position-relative">
        <!-- Slider Background -->
        <transition name="slide-fade" mode="out-in">
          <div
            :key="currentSlideIndex"
            class="hero-banner-slide"
            :style="{ background: slides[currentSlideIndex]?.bgGradient || defaultSlide.bgGradient }"
          >
            <a :href="slides[currentSlideIndex]?.link || '#'" class="hero-banner-link"></a>
            <div class="hero-banner-img">
              <img
                :src="slides[currentSlideIndex]?.image || defaultSlide.image"
                :alt="slides[currentSlideIndex]?.title || 'Banner'"
                class="hero-product-image float-animation-hero"
                @error="handleImageError"
              />
            </div>
          </div>
        </transition>

        <!-- Slider Controls -->
        <template v-if="slides.length > 1">
          <button class="slider-btn slider-prev" @click="prevSlide">
            <i class="bi bi-chevron-left"></i>
          </button>
          <button class="slider-btn slider-next" @click="nextSlide">
            <i class="bi bi-chevron-right"></i>
          </button>
          <div class="slider-indicators">
            <span
              v-for="(slide, index) in slides"
              :key="index"
              class="slider-dot"
              :class="{ active: index === currentSlideIndex }"
              @click="goToSlide(index)"
            ></span>
          </div>
        </template>
      </div>
    </div>

    <!-- ==================== 2 BANNER PHỤ BÊN PHẢI (~32%) - GLASSMORPHISM ==================== -->
    <div class="col-lg-4 col-md-5 d-flex flex-column gap-3">

      <!-- Ô phụ TRÊN - Glassmorphism Premium với Floating Shapes -->
      <div
        class="side-banner glass-banner glass-banner-top"
        :style="{ '--glass-gradient': topBanner.glassGradient || 'linear-gradient(135deg, rgba(139, 92, 246, 0.4) 0%, rgba(59, 130, 246, 0.3) 100%)' }"
      >
        <!-- Floating Decorative Shapes -->
        <div class="glass-shapes">
          <div class="glass-shape glass-shape-1"></div>
          <div class="glass-shape glass-shape-2"></div>
          <div class="glass-shape glass-shape-3"></div>
        </div>

        <!-- Glow Effect -->
        <div class="glass-glow glass-glow-top"></div>

        <div class="glass-content">
          <span class="glass-badge glass-badge-pulse" v-if="topBanner.tag">
            <span class="badge-icon">⚡</span>{{ topBanner.tag }}
          </span>
          <h6 class="glass-title">{{ topBanner.title }}</h6>

          <a :href="topBanner.link || '#'" class="glass-cta">
            <span>Xem ngay</span>
            <span class="cta-arrow">→</span>
          </a>
        </div>
        <div class="glass-img-wrapper">
          <img
            :src="topBanner.image"
            :alt="topBanner.title || 'Banner'"
            class="glass-product-image"
            @error="handleImageError"
          />
        </div>
      </div>

      <!-- Ô phụ DƯỚI - Glassmorphism với Shimmer Effect -->
      <div
        class="side-banner glass-banner glass-banner-bottom"
        :style="{ '--glass-gradient': bottomBanner.glassGradient || 'linear-gradient(135deg, rgba(236, 72, 153, 0.4) 0%, rgba(249, 115, 22, 0.3) 100%)' }"
      >
        <!-- Floating Decorative Shapes -->
        <div class="glass-shapes">
          <div class="glass-shape glass-shape-4"></div>
          <div class="glass-shape glass-shape-5"></div>
          <div class="glass-shape glass-shape-6"></div>
        </div>

        <!-- Glow Effect -->
        <div class="glass-glow glass-glow-bottom"></div>

        <div class="glass-content">
          <span class="glass-badge glass-badge-hot" v-if="bottomBanner.tag">
            <span class="badge-icon">🔥</span>{{ bottomBanner.tag }}
          </span>
          <h6 class="glass-title">{{ bottomBanner.title }}</h6>


          <a :href="bottomBanner.link || '#'" class="glass-cta glass-cta-warm">
            <span>Khám phá</span>
            <span class="cta-arrow">→</span>
          </a>
        </div>
        <div class="glass-img-wrapper">
          <img
            :src="bottomBanner.image"
            :alt="bottomBanner.title || 'Banner'"
            class="glass-product-image"
            @error="handleImageError"
          />
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { bannerApi } from '@/api/BannerApi'

const FALLBACK_IMAGE = 'https://res.cloudinary.com/dyeb3lju6/image/upload/v1785215934/marcus-store/izcsoamwd4ho2ipab896.png'

// ===== SLIDES DATA - Dynamic Banner Data =====
const slides = ref([
  {
    id: 1,
    tag: 'RA MẮT CHÍNH THỨC',
    title: 'iPhone 16 Pro Max',
    specs: 'Camera 48MP | Chip A18 Pro',
    price: '28.990.000đ',
    image: '/images/banner-iphone-full.png',
    bgGradient: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
    link: '/products/iphone-16-pro-max'
  },
  {
    id: 2,
    tag: 'SIÊU SALE',
    title: 'Samsung Galaxy S24 Ultra',
    specs: 'AI Camera | Snapdragon 8 Gen 3',
    price: '22.990.000đ',
    image: '/images/banner-s24ultra.png',
    bgGradient: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
    link: '/products/samsung-s24-ultra'
  }
])

// ===== DEFAULT FALLBACK DATA =====
const defaultSlide = {
  tag: 'RA MẮT CHÍNH THỨC',
  title: 'iPhone 16 Pro Max',
  specs: 'Camera 48MP | Chip A18 Pro',
  price: '28.990.000đ',
  image: '/images/banner-iphone-full.png',
  bgGradient: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
  link: '/products/iphone-16-pro-max'
}

const defaultTop = {

  title: 'Samsung Galaxy S24 Ultra',
  specs: 'AI Camera | Snapdragon 8 Gen 3',
  price: '22.990.000đ',
  image: '/images/banner-s24ultra.png',
  bgGradient: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
  glassGradient: 'linear-gradient(135deg, rgba(30, 58, 138, 0.6) 0%, rgba(59, 130, 246, 0.4) 50%, rgba(99, 102, 241, 0.3) 100%)',
  link: '/products/samsung-s24-ultra'
}

const defaultBottom = {
  tag: 'DEAL HOT',
  title: 'Phụ Kiện Chính Hãng',
  specs: 'Tai nghe | Sạc dự phòng | Ốp lưng',
  price: 'Từ 199.000đ',
  image: '/images/banner-phukien.png',
  bgGradient: 'linear-gradient(135deg, #2d2d2d 0%, #1a1a1a 100%)',
  glassGradient: 'linear-gradient(135deg, rgba(190, 24, 93, 0.5) 0%, rgba(249, 115, 22, 0.4) 50%, rgba(236, 72, 153, 0.3) 100%)',
  link: '/accessories'
}

// ===== API DATA STATE =====
const topBanner = ref(defaultTop)
const bottomBanner = ref(defaultBottom)

// Slider state
const currentSlideIndex = ref(0)
let sliderInterval = null

function nextSlide() {
  if (slides.value.length > 1) {
    currentSlideIndex.value = (currentSlideIndex.value + 1) % slides.value.length
  }
}

function prevSlide() {
  if (slides.value.length > 1) {
    currentSlideIndex.value = (currentSlideIndex.value - 1 + slides.value.length) % slides.value.length
  }
}

function goToSlide(index) {
  currentSlideIndex.value = index
  resetAutoSlide()
}

function startAutoSlide() {
  stopAutoSlide()
  if (slides.value.length > 1) {
    sliderInterval = setInterval(nextSlide, 5000)
  }
}

function resetAutoSlide() {
  startAutoSlide()
}

function stopAutoSlide() {
  if (sliderInterval) {
    clearInterval(sliderInterval)
    sliderInterval = null
  }
}

const handleImageError = (e) => {
  e.target.src = FALLBACK_IMAGE
}

// Chuẩn hóa positionCode để tránh lệch do khoảng trắng/hoa-thường từ BE
function normalizeCode(code) {
  return String(code || '').trim().toUpperCase()
}

// Parse metadata JSON từ API (nếu có)
function enrichBanner(b, defaults) {
  let meta = {}
  try {
    if (b.metadata) {
      meta = typeof b.metadata === 'string' ? JSON.parse(b.metadata) : b.metadata
    }
  } catch (e) {
    console.warn('[HomeBanner] Không parse được metadata cho banner:', b.bannerId || b.title, e)
  }

  return {
    // FIX: b.title có thể là null/undefined/'' từ BE -> dùng fallback đầy đủ
    tag: meta.tag || meta.discountTag || defaults.tag || '',
    title: b.title || defaults.title || '',
    specs: meta.specs || defaults.specs || '',
    price: meta.price || defaults.price || '',
    image: b.imageUrl || defaults.image,
    link: b.targetUrl || b.linkUrl || defaults.link || '#',
    bgGradient: meta.bg || meta.bgGradient || defaults.bgGradient,
    glassGradient: meta.glassGradient || defaults.glassGradient
  }
}

// ===== API LOADING =====
async function loadBanners() {
  try {
    const positionsRaw = await bannerApi.getPublicPositions()
    const allBannersRaw = await bannerApi.getPublicBanners()

    // Phòng trường hợp BE bọc data trong { data: [...] } thay vì trả mảng thẳng
    const positions = Array.isArray(positionsRaw) ? positionsRaw : (positionsRaw?.data || [])
    const allBanners = Array.isArray(allBannersRaw) ? allBannersRaw : (allBannersRaw?.data || [])

    console.log('[HomeBanner] positions từ API:', positions)
    console.log('[HomeBanner] banners từ API:', allBanners)

    if (!Array.isArray(positions) || positions.length === 0) {
      console.warn('[HomeBanner] Không lấy được danh sách vị trí (positions rỗng hoặc sai định dạng)')
    }
    if (!Array.isArray(allBanners) || allBanners.length === 0) {
      console.warn('[HomeBanner] Không lấy được danh sách banner (banners rỗng hoặc sai định dạng)')
    }

    const heroPos = positions.find(p => normalizeCode(p.positionCode) === 'HOME_HERO_SLIDER')
    const topPos = positions.find(p => normalizeCode(p.positionCode) === 'HOME_MIDDLE')
    const bottomPos = positions.find(p => normalizeCode(p.positionCode) === 'CATEGORY_TOP')

    if (!heroPos) console.warn('[HomeBanner] Không tìm thấy position code HOME_HERO_SLIDER trong dữ liệu positions')
    if (!topPos) console.warn('[HomeBanner] Không tìm thấy position code HOME_MIDDLE trong dữ liệu positions')
    if (!bottomPos) console.warn('[HomeBanner] Không tìm thấy position code CATEGORY_TOP trong dữ sách banner')

    // Position 1: Slider - tối đa 5 ảnh
    if (heroPos) {
      const matched = allBanners.filter(b => String(b.positionId) === String(heroPos.positionId))
      console.log('[HomeBanner] Banner khớp HOME_HERO_SLIDER (trước khi lọc isActive):', matched)

      const heroList = matched
        .filter(b => !!b.isActive)
        .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
        .slice(0, 5)
        .map(b => enrichBanner(b, defaultSlide))

      slides.value = heroList
      if (heroList.length > 0) {
        startAutoSlide()
      } else {
        console.warn('[HomeBanner] Có banner khớp position nhưng không cái nào isActive=true, hoặc không có banner nào khớp positionId')
      }
    }

    // Position 2: Banner phụ trên
    if (topPos) {
      const matched = allBanners.filter(b => String(b.positionId) === String(topPos.positionId))
      const top = matched.find(b => !!b.isActive)
      console.log('[HomeBanner] Banner khớp HOME_MIDDLE:', matched, '-> chọn:', top)
      if (top) topBanner.value = enrichBanner(top, defaultTop)
    }

    // Position 3: Banner phụ dưới
    if (bottomPos) {
      const matched = allBanners.filter(b => String(b.positionId) === String(bottomPos.positionId))
      const bottom = matched.find(b => !!b.isActive)
      console.log('[HomeBanner] Banner khớp CATEGORY_TOP:', matched, '-> chọn:', bottom)
      if (bottom) bottomBanner.value = enrichBanner(bottom, defaultBottom)
    }
  } catch (err) {
    console.error('[HomeBanner] Lỗi load banners:', err)
  }
}

onMounted(loadBanners)
onUnmounted(stopAutoSlide)
</script>

<style scoped>
/* ===== BANNER CHÍNH - Hero Banner ===== */
.hero-banner-main {
  /* FIX: dùng height 100% để tự co giãn khớp với chiều cao thực tế của cột 2 banner phụ bên phải,
     thay vì set cứng 540px khiến 2 bên bị lệch chiều cao khi nội dung/kích thước banner phụ thay đổi.
     Bootstrap .row mặc định là flex + align-items: stretch nên cột bên trái (col-lg-8)
     sẽ tự được kéo giãn bằng chiều cao cột bên phải (col-lg-4) - ta chỉ cần cho phần tử con bên trong
     lấp đầy 100% chiều cao đó. */
  height: 100%;
  min-height: 460px; /* fallback an toàn, tránh co về 0 nếu trình duyệt không stretch được */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  position: relative;
  overflow: hidden;
}

/* ===== HERO SLIDE TRANSITION ===== */
.hero-banner-slide {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  padding: 0;
}

.hero-banner-link {
  position: absolute;
  inset: 0;
  z-index: 3;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: opacity 0.5s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
}

/* ===== SLIDER CONTROLS ===== */
.slider-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #333;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.slider-btn:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  transform: translateY(-50%) scale(1.1);
}

.slider-prev {
  left: 12px;
}

.slider-next {
  right: 12px;
}

.slider-indicators {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  gap: 8px;
}

.slider-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
  /* FIX: thêm viền + shadow tối để dot luôn nổi bật kể cả trên nền sáng (ảnh biển, ảnh trắng...) */
  border: 1px solid rgba(0, 0, 0, 0.25);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
}

.slider-dot:hover {
  background: rgba(255, 255, 255, 0.8);
}

.slider-dot.active {
  background: #fff;
  width: 28px;
  border-radius: 5px;
  /* FIX: tăng viền/shadow khi active để càng dễ thấy */
  border: 1px solid rgba(0, 0, 0, 0.3);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
}

/* ===== HERO IMAGE - Full Cover ===== */
.hero-banner-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* FIX: lớp phủ gradient tối ở đáy để dots/nút điều hướng luôn nổi bật, bất kể ảnh nền sáng hay tối */
.hero-banner-slide::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.35) 0%,
    rgba(0, 0, 0, 0.12) 15%,
    transparent 35%
  );
}

.hero-product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.5s ease;
}

.hero-banner-img:hover .hero-product-image {
  transform: scale(1.03);
}

.hero-banner-main:hover .hero-product-image {
  transform: scale(1.03);
}

/* ===== ANIMATION FLOAT CHO HERO ===== */
.float-animation-hero {
  animation: floatHero 6s ease-in-out infinite;
}

@keyframes floatHero {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.03);
  }
}

/* ================================================================
   ===== GLASSMORPHISM SIDE BANNERS - MODERN PASTEL DESIGN =====
   ================================================================ */

/* Base Glass Banner - Soft Pastel Gradient */
.glass-banner {
  flex: 1;
  min-height: 300px;
  border-radius: 24px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2rem;
  gap: 1.5rem;
  background: var(--glass-gradient, linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.05) 100%));
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.15),
    0 4px 16px rgba(139, 92, 246, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  transform-style: preserve-3d;
}

/* Glass Banner Hover */
.glass-banner:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.2),
    0 8px 24px rgba(139, 92, 246, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.4);
}

/* Top Banner - Blue/Pink Pastel Theme */
.glass-banner-top {
  background: linear-gradient(145deg, 
    rgba(147, 197, 253, 0.4) 0%, 
    rgba(196, 181, 253, 0.3) 50%,
    rgba(233, 213, 255, 0.2) 100%);
}

/* Bottom Banner - Peach/Coral Pastel Theme */
.glass-banner-bottom {
  background: linear-gradient(145deg, 
    rgba(253, 186, 116, 0.35) 0%, 
    rgba(252, 165, 165, 0.3) 50%,
    rgba(254, 215, 170, 0.2) 100%);
}

/* Smooth Light Overlay */
.glass-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.2) 0%,
    transparent 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
  pointer-events: none;
  border-radius: inherit;
}

/* ================================================================
   ===== DECORATIVE ORBS - SOFT PASTEL =====
   ================================================================ */
.glass-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.glass-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
}

.glass-banner-top .glass-shape-1 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(147, 197, 253, 0.6) 0%, transparent 70%);
  top: -60px;
  right: 10%;
  animation: floatOrb1 12s ease-in-out infinite;
}

.glass-banner-top .glass-shape-2 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(196, 181, 253, 0.5) 0%, transparent 70%);
  bottom: -40px;
  right: 25%;
  animation: floatOrb2 10s ease-in-out infinite;
}

.glass-banner-top .glass-shape-3 {
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(233, 213, 255, 0.4) 0%, transparent 70%);
  top: 20%;
  right: 40%;
  animation: floatOrb3 8s ease-in-out infinite;
}

.glass-banner-bottom .glass-shape-4 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(253, 186, 116, 0.5) 0%, transparent 70%);
  top: -50px;
  left: 15%;
  animation: floatOrb1 11s ease-in-out infinite reverse;
}

.glass-banner-bottom .glass-shape-5 {
  width: 140px;
  height: 140px;
  background: radial-gradient(circle, rgba(252, 165, 165, 0.5) 0%, transparent 70%);
  bottom: -30px;
  right: 20%;
  animation: floatOrb2 9s ease-in-out infinite;
}

.glass-banner-bottom .glass-shape-6 {
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, rgba(254, 215, 170, 0.4) 0%, transparent 70%);
  top: 25%;
  left: 5%;
  animation: floatOrb3 7s ease-in-out infinite reverse;
}

@keyframes floatOrb1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, 25px) scale(1.1); }
}

@keyframes floatOrb2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-15px, -20px); }
}

@keyframes floatOrb3 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(12px, -8px); }
  66% { transform: translate(-8px, 12px); }
}

/* ================================================================
   ===== SOFT GLOW EFFECTS =====
   ================================================================ */
.glass-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(50px);
  opacity: 0.4;
  transition: opacity 0.4s ease;
}

.glass-banner:hover .glass-glow {
  opacity: 0.6;
}

.glass-glow-top {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(147, 197, 253, 0.5) 0%, rgba(196, 181, 253, 0.3) 50%, transparent 70%);
  top: -60px;
  right: -30px;
  animation: softGlow1 6s ease-in-out infinite;
}

.glass-glow-bottom {
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(253, 186, 116, 0.5) 0%, rgba(252, 165, 165, 0.3) 50%, transparent 70%);
  bottom: -50px;
  left: -20px;
  animation: softGlow2 7s ease-in-out infinite;
}

@keyframes softGlow1 {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.15); opacity: 0.5; }
}

@keyframes softGlow2 {
  0%, 100% { transform: scale(1.1); opacity: 0.35; }
  50% { transform: scale(0.95); opacity: 0.45; }
}

/* ================================================================
   ===== GLASS CONTENT =====
   ================================================================ */
.glass-content {
  z-index: 10;
  flex: 0 0 48%;
  max-width: 48%;
  flex-shrink: 0;
  position: relative;
  text-align: left;
}

/* Badge Styles */
.glass-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 10px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.glass-badge-pulse {
  background: linear-gradient(135deg, 
    rgba(147, 197, 253, 0.5) 0%, 
    rgba(196, 181, 253, 0.4) 100%);
  color: #4c1d95;
  box-shadow: 
    0 4px 20px rgba(147, 197, 253, 0.4),
    0 0 30px rgba(147, 197, 253, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.glass-badge-hot {
  background: linear-gradient(135deg, 
    rgba(253, 186, 116, 0.5) 0%, 
    rgba(252, 165, 165, 0.4) 100%);
  color: #9a3412;
  box-shadow: 
    0 4px 20px rgba(253, 186, 116, 0.4),
    0 0 30px rgba(253, 186, 116, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.badge-icon {
  font-size: 0.85rem;
}

/* Pulse Animation for Badge */
.glass-badge-pulse .badge-icon {
  animation: iconPulse 1.5s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

/* Modern Title Typography */
.glass-title {
  font-size: 1.4rem;
  font-weight: 800;
  color: #1e1b4b;
  margin-bottom: 16px;
  line-height: 1.25;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.glass-banner:hover .glass-title {
  text-shadow: 0 4px 12px rgba(147, 197, 253, 0.3);
}

.glass-banner-bottom .glass-title {
  color: #7c2d12;
}

.glass-banner-bottom:hover .glass-title {
  text-shadow: 0 4px 12px rgba(253, 186, 116, 0.3);
}

/* Specs */
.glass-specs {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 8px;
  line-height: 1.5;
}

/* Price */
.glass-price {
  margin-bottom: 12px;
}

.price-current {
  font-size: 1.2rem;
  font-weight: 800;
  color: #fbbf24;
  text-shadow: 0 2px 15px rgba(251, 191, 36, 0.4);
}

.glass-banner-bottom .price-current {
  background: linear-gradient(135deg, #fbbf24, #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Modern Pill CTA Button */
.glass-cta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 28px;
  border-radius: 50px;
  font-size: 0.85rem;
  font-weight: 700;
  text-decoration: none;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.7) 0%, rgba(147, 197, 253, 0.6) 100%);
  color: #1e1b4b;
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  box-shadow: 
    0 6px 24px rgba(99, 102, 241, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: hidden;
  position: relative;
}

.glass-cta::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.3) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.glass-cta:hover::before {
  opacity: 1;
}

.glass-cta:hover {
  transform: translateX(6px);
  box-shadow: 
    0 10px 32px rgba(99, 102, 241, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.cta-arrow {
  transition: transform 0.3s ease;
}

.glass-cta:hover .cta-arrow {
  transform: translateX(4px);
}

/* Warm CTA for Bottom Banner */
.glass-cta-warm {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.7) 0%, rgba(253, 186, 116, 0.6) 100%);
  color: #7c2d12;
  box-shadow: 
    0 6px 24px rgba(249, 115, 22, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.glass-cta-warm:hover {
  box-shadow: 
    0 10px 32px rgba(249, 115, 22, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

/* ================================================================
   ===== PRODUCT IMAGE - PREMIUM FINISH =====
   ================================================================ */
.glass-img-wrapper {
  z-index: 10;
  flex: 0 0 52%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  position: relative;
  max-width: 52%;
  overflow: visible;
  align-self: stretch;
  padding-top: 0.5rem;
}

.glass-product-image {
  max-height: 240px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
  object-position: bottom;
  filter: drop-shadow(0 15px 30px rgba(0, 0, 0, 0.15));
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  animation: productFloat 4s ease-in-out infinite;
}

.glass-banner-bottom .glass-product-image {
  animation: productFloatReverse 4.5s ease-in-out infinite;
  animation-delay: 0.5s;
}

@keyframes productFloat {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-12px); }
}

@keyframes productFloatReverse {
  0%, 100% { transform: translateY(-10px); }
  50% { transform: translateY(4px); }
}

.glass-banner:hover .glass-product-image {
  transform: translateY(-6px) scale(1.05);
  filter: drop-shadow(0 20px 40px rgba(0, 0, 0, 0.2));
  animation-play-state: paused;
}

/* ================================================================
   ===== RESPONSIVE =====
   ================================================================ */
@media (max-width: 992px) {
  .hero-banner-main {
    height: 100%;
    min-height: 380px;
  }
  .glass-banner {
    min-height: 230px;
    border-radius: 18px;
  }
  .glass-product-image {
    max-height: 220px;
  }
  .glass-content {
    max-width: 48%;
    flex: 0 0 48%;
  }
  .glass-img-wrapper {
    max-width: 52%;
    flex: 0 0 52%;
  }
  .glass-title {
    font-size: 1.25rem;
  }
}

@media (max-width: 768px) {
  .hero-banner-main {
    height: 320px;
  }
  .glass-banner {
    min-height: 180px;
    padding: 1rem 1.25rem;
    border-radius: 16px;
  }
  .glass-product-image {
    max-height: 160px;
  }
  .glass-content {
    max-width: 48%;
    flex: 0 0 48%;
  }
  .glass-img-wrapper {
    max-width: 52%;
    flex: 0 0 52%;
  }
  .glass-title {
    font-size: 1rem;
    margin-bottom: 10px;
  }
  .glass-badge {
    padding: 5px 12px;
    font-size: 0.6rem;
  }
  .glass-cta {
    padding: 8px 16px;
    font-size: 0.75rem;
  }
}

@media (max-width: 576px) {
  .hero-banner-main {
    height: 280px;
  }
  .glass-img-wrapper {
    display: none;
  }
  .glass-content {
    max-width: 100%;
    flex: 1;
    text-align: center;
  }
  .glass-banner {
    min-height: 140px;
    padding: 1.2rem;
    justify-content: center;
  }
  .glass-title {
    color: #1e1b4b;
    font-size: 1rem;
    background: none;
    -webkit-text-fill-color: #1e1b4b;
  }
  .glass-banner-bottom .glass-title {
    color: #7c2d12;
    -webkit-text-fill-color: #7c2d12;
  }
  .glass-shape {
    opacity: 0.15;
  }
}
</style>
