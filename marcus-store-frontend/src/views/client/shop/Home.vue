<template>
  <!-- ===== TOP PROMO BANNER ===== -->
  <div class="top-promo-banner">
    <div class="container-fluid text-center py-1">
      <span class="marquee-text">
        🔥 Siêu sale tháng 6 – Giảm đến 50% | Free ship toàn quốc đơn từ 299k | Trả góp 0% lãi suất
        mọi sản phẩm 🔥
      </span>
    </div>
  </div>

  <!-- ===== MAIN CONTENT ===== -->
  <main>
    <div class="container-xxl py-3">
      <HeroBanner />
      <FlashSaleSection />
      <ProductCard mode="standalone" />
      <NewsAndInfoSection />
    </div>
  </main>
</template>

<script setup>
import HeroBanner from '@/layouts/home/HomeBanner.vue'
import FlashSaleSection from '@/layouts/home/HomeFlashSale.vue'
import NewsAndInfoSection from '@/layouts/home/HomeNewAndInfo.vue'
import ProductCard from '@/components/client/ProductCard.vue'
</script>

<style scoped>
.top-promo-banner {
  background: var(--cps-red);
  color: #000000;
  font-size: 0.78rem;
  font-weight: 600;
  overflow: hidden;
  letter-spacing: 0.2px;
}
.marquee-text {
  display: inline-block;
  animation: marquee 30s linear infinite;
  white-space: nowrap;
}
@keyframes marquee {
  0% {
    transform: translateX(100vw);
  }
  100% {
    transform: translateX(-100%);
  }
}
</style>
