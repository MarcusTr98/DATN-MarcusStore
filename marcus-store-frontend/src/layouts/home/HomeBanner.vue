<template>
  <div class="row g-3 mb-4">
    <!-- Loading state -->
    <div v-if="loading" class="col-12 text-center py-5">
      <div class="spinner-border text-danger" role="status"></div>
    </div>

    <template v-else>
      <!-- Main Carousel -->
      <div class="col-lg-8 col-md-7">
        <div
          v-if="heroBanners.length"
          id="heroCarousel"
          class="carousel slide hero-carousel"
          data-bs-ride="carousel"
        >
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
              :key="banner.id"
              :class="['carousel-item', { active: i === 0 }]"
            >
              <a
                :href="banner.linkUrl || '#'"
                class="d-block hero-banner-slide"
                :style="{ backgroundImage: `url(${banner.imageUrl})` }"
              ></a>
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

        <!-- Fallback nếu không có banner slider -->
        <div v-else class="hero-placeholder rounded-3 d-flex align-items-center justify-content-center">
          <span class="text-muted">Chưa có banner</span>
        </div>
      </div>

      <!-- Side Banners -->
      <div class="col-lg-4 col-md-5 d-flex flex-column gap-3">
        <!-- Banner Home 1 -->
        <a
          v-if="banner1"
          :href="banner1.linkUrl || '#'"
          class="side-banner rounded-3 d-block"
        >
          <img
            :src="banner1.imageUrl"
            :alt="banner1.title"
            class="side-banner-img"
          />
        </a>
        <div v-else class="side-banner-placeholder rounded-3"></div>

        <!-- Banner Home 2 -->
        <a
          v-if="banner2"
          :href="banner2.linkUrl || '#'"
          class="side-banner rounded-3 d-block"
        >
          <img
            :src="banner2.imageUrl"
            :alt="banner2.title"
            class="side-banner-img"
          />
        </a>
        <div v-else class="side-banner-placeholder rounded-3"></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

const loading = ref(true)
const heroBanners = ref([])  // HOME_SLIDER — nhiều ảnh, chạy tuần tự theo displayOrder
const banner1 = ref(null)    // HOME_BANNER_1 — 1 ảnh cố định, bên phải trên
const banner2 = ref(null)    // HOME_BANNER_2 — 1 ảnh cố định, bên phải dưới

async function fetchBanners() {
  try {
    // Gọi song song 3 vị trí cùng lúc, không cần chờ tuần tự
    const [sliderRes, banner1Res, banner2Res] = await Promise.all([
      api.get('/public/banners', { params: { position: 'HOME_SLIDER' }, skipGlobalLoading: true }),
      api.get('/public/banners', { params: { position: 'HOME_BANNER_1' }, skipGlobalLoading: true }),
      api.get('/public/banners', { params: { position: 'HOME_BANNER_2' }, skipGlobalLoading: true }),
    ])

    // Backend đã filter isActive + ngày + sort theo displayOrder sẵn rồi,
    // FE chỉ cần dùng trực tiếp, không cần tính toán thêm
    heroBanners.value = (sliderRes.data?.data || []).map(b => ({
      id: b.id,
      title: b.title,
      imageUrl: b.imageUrl,
      linkUrl: b.targetUrl || null,
    }))

    // Mỗi vị trí chỉ lấy banner đầu tiên (index 0) — admin chỉ được tạo 1 banner/vị trí
    // nhưng phòng hờ trường hợp có nhiều hơn 1 thì lấy cái mới nhất (API trả về đầu tiên)
    const b1List = banner1Res.data?.data || []
    banner1.value = b1List.length ? {
      id: b1List[0].id,
      title: b1List[0].title,
      imageUrl: b1List[0].imageUrl,
      linkUrl: b1List[0].targetUrl || null,
    } : null

    const b2List = banner2Res.data?.data || []
    banner2.value = b2List.length ? {
      id: b2List[0].id,
      title: b2List[0].title,
      imageUrl: b2List[0].imageUrl,
      linkUrl: b2List[0].targetUrl || null,
    } : null

  } catch (err) {
    console.error('Lỗi tải banner trang chủ:', err)
    // Không throw — lỗi banner không nên làm crash toàn bộ trang
  } finally {
    loading.value = false
  }
}

onMounted(fetchBanners)
</script>

<style scoped>
/* Hero Carousel */
.hero-carousel {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.hero-banner-slide {
  height: 460px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: block;
}

.hero-placeholder {
  height: 460px;
  background: #f3f4f6;
  border: 1px dashed #d1d5db;
}

/* Side Banners */
.side-banner {
  flex: 1;
  overflow: hidden;
  border: 1px solid var(--cps-border);
  transition: transform 0.2s, box-shadow 0.2s;
  text-decoration: none;
  min-height: 220px;
}

.side-banner:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.side-banner-img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.side-banner-placeholder {
  flex: 1;
  min-height: 220px;
  background: #f3f4f6;
  border: 1px dashed #d1d5db;
}

@media (max-width: 768px) {
  .hero-banner-slide {
    height: 220px;
  }
}
</style>