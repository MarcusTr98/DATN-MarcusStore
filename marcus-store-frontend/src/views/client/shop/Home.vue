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
              <button
                v-for="(s, i) in heroSlides"
                :key="`${s.name}-${i}`"
                type="button"
                class="dot-btn"
                :class="{ active: i === activeHeroSlide }"
                :aria-label="`Xem ưu đãi ${i + 1}: ${s.name}`"
                :aria-current="i === activeHeroSlide ? 'true' : undefined"
                @click="activeHeroSlide = i"
              ></button>
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
                :style="{
                  '--chip-delay': `${index * 0.18}s`,
                  '--sparkle-delay': `${index * 0.7}s`,
                }"
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
                    <div :key="activeHeroSlide" class="slide-content" aria-live="polite">
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
import HeroBanner from '@/layouts/home/HomeBanner.vue'
import FlashSaleSection from '@/layouts/home/HomeFlashSale.vue'
import NewsAndInfoSection from '@/layouts/home/HomeNewAndInfo.vue'
import HomeHotProducts from '@/layouts/home/HomeNewProducts.vue'
import ProductCard from '@/components/client/ProductCard.vue'
import ClientSlider from '@/components/client/ClientSlider.vue'
import { useHomePage } from '@/composables/useHomePage'

// Marcus refactor: Home.vue chỉ điều phối các section trang chủ.
const {
  sysSettings,
  categories,
  heroSlides,
  activeHeroSlide,
  heroBenefits,
  currentSlide,
  currentTime,
  currentDate,
  brandBanners,
} = useHomePage()
</script>

<style scoped src="@/assets/css/Home.css"></style>
