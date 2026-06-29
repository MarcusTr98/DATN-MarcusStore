<template>
  <div class="row g-3 mb-4">
    <!-- Main Carousel -->
    <div class="col-lg-8 col-md-7">
      <div id="heroCarousel" class="carousel slide hero-carousel" data-bs-ride="carousel">
        <div class="carousel-indicators">
          <button
            v-for="(_, i) in heroBanners"
            :key="i"
            type="button"
            :data-bs-target="'#heroCarousel'"
            :data-bs-slide-to="i"
            :class="{ active: i === 0 }"
          ></button>
        </div>
        <div class="carousel-inner rounded-3 overflow-hidden">
          <div
            v-for="(banner, i) in heroBanners"
            :key="i"
            :class="['carousel-item', { active: i === 0 }]"
          >
            <div class="hero-banner-slide" :style="{ background: banner.bg }">
              <div class="hero-banner-content">
                <p class="hero-sub">{{ banner.sub }}</p>
                <h2 class="hero-title">{{ banner.title }}</h2>
                <p class="hero-desc">{{ banner.desc }}</p>
                <a href="#" class="hero-cta-btn">Mua ngay</a>
              </div>
              <div class="hero-banner-img">
                <div class="product-placeholder-lg">
                  <span>{{ banner.emoji }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <button
          class="carousel-control-prev"
          type="button"
          data-bs-target="#heroCarousel"
          data-bs-slide="prev"
        >
          <span class="carousel-control-prev-icon"></span>
        </button>
        <button
          class="carousel-control-next"
          type="button"
          data-bs-target="#heroCarousel"
          data-bs-slide="next"
        >
          <span class="carousel-control-next-icon"></span>
        </button>
      </div>
    </div>

    <!-- Side Banners -->
    <div class="col-lg-4 col-md-5 d-flex flex-column gap-3">
      <div
        v-for="banner in sideBanners"
        :key="banner.title"
        class="side-banner rounded-3 d-flex align-items-center justify-content-between p-3"
        :style="{ background: banner.bg }"
      >
        <div>
          <p class="side-banner-sub mb-1">{{ banner.sub }}</p>
          <h6 class="side-banner-title mb-2">{{ banner.title }}</h6>
          <a href="#" class="side-banner-link">Xem ngay →</a>
        </div>
        <div class="side-banner-emoji">{{ banner.emoji }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// ---- HERO BANNERS (giữ fix cứng tạm thời) ----
const heroBanners = ref([
  {
    sub: 'Ra mắt chính thức',
    title: 'iPhone 16 Pro Max',
    desc: 'Camera 48MP | Chip A18 Pro | Giá từ 28.990.000đ',
    emoji: '📱',
    bg: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
  },
  {
    sub: 'Deal khủng tháng 6',
    title: 'Samsung Galaxy S25 Ultra',
    desc: 'Bút S Pen | AI Galaxy | Giảm đến 5 triệu',
    emoji: '📱',
    bg: 'linear-gradient(135deg, #0d1b2a 0%, #1b2838 50%, #2d4a7a 100%)',
  },
  {
    sub: 'Laptop Gaming Hot',
    title: 'ASUS ROG Strix G16',
    desc: 'RTX 4070 | i9-14900HX | 240Hz | Trả góp 0%',
    emoji: '💻',
    bg: 'linear-gradient(135deg, #1a0a0a 0%, #3d0000 50%, #6b0000 100%)',
  },
])

const sideBanners = ref([
  {
    sub: 'Giảm đến',
    title: 'iPad Pro M4\n-3.000.000đ',
    emoji: '📟',
    bg: 'linear-gradient(135deg, #e8f4fd, #bee3f8)',
  },
  {
    sub: 'Chỉ hôm nay',
    title: 'AirPods Pro 2\nChỉ 4.490.000đ',
    emoji: '🎧',
    bg: 'linear-gradient(135deg, #f0fdf4, #bbf7d0)',
  },
])
</script>

<style scoped>
.hero-carousel {
  border-radius: var(--radius-md);
  overflow: hidden;
}
.hero-banner-slide {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2rem 2.5rem;
  position: relative;
  overflow: hidden;
}
.hero-banner-slide::before {
  content: '';
  position: absolute;
  right: -60px;
  top: -60px;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 50%;
}
.hero-banner-content {
  z-index: 1;
  max-width: 55%;
}
.hero-sub {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-bottom: 8px;
}
.hero-title {
  color: #fff;
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 10px;
}
.hero-desc {
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.82rem;
  margin-bottom: 18px;
}
.hero-cta-btn {
  display: inline-block;
  background: var(--cps-red);
  color: #fff;
  text-decoration: none;
  padding: 9px 22px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.hero-cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(215, 0, 24, 0.5);
}
.hero-banner-img {
  z-index: 1;
}
.product-placeholder-lg {
  font-size: 7rem;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.4));
  user-select: none;
}

.side-banner {
  flex: 1;
  border: 1px solid var(--cps-border);
  cursor: pointer;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.side-banner:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}
.side-banner-sub {
  font-size: 0.72rem;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.side-banner-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--cps-dark);
  white-space: pre-line;
}
.side-banner-link {
  font-size: 0.78rem;
  color: var(--cps-red);
  font-weight: 600;
  text-decoration: none;
}
.side-banner-link:hover {
  text-decoration: underline;
}
.side-banner-emoji {
  font-size: 3.5rem;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
}

@media (max-width: 768px) {
  .hero-banner-slide {
    height: 220px;
    padding: 1.2rem 1.5rem;
  }
  .hero-title {
    font-size: 1.1rem;
  }
  .product-placeholder-lg {
    font-size: 5rem;
  }
}

@media (max-width: 576px) {
  .hero-banner-img {
    display: none;
  }
  .hero-banner-content {
    max-width: 100%;
  }
}
</style>