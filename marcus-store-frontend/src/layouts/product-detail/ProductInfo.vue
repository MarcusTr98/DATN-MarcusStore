<template>
  <div class="pd-info">
    <!-- Brand + tên -->
    <div class="pd-info__brand" v-if="brand">{{ brand }}</div>
    <h1 class="pd-info__name">{{ productName }}</h1>

    <!-- Rating + đã bán -->
    <div class="pd-info__meta">
      <!-- Stars -->
      <div class="pd-info__stars">
        <span
          v-for="n in 5"
          :key="n"
          class="pd-info__star"
          :class="getStarClass(n)"
        >★</span>
      </div>
      <div class="pd-info__rating-val">{{ rating > 0 ? rating.toFixed(1) : '0' }} / 5</div>

      <div class="pd-info__divider" />

      <div class="pd-info__reviews">
        <span>{{ formatReviewCount(reviewCount) }}</span>
      </div>

      <div class="pd-info__divider" />

      <div class="pd-info__sold" v-if="totalSold > 0">
        Đã bán <strong>{{ formatSold(totalSold) }}</strong>
      </div>
    </div>

    <!-- Khối giá -->
    <div class="pd-info__price-box">
      <div v-if="currentSku" class="pd-info__price-row">
        <span class="pd-info__price-current">
          {{ formatPrice(currentSku.price) }}
        </span>
        <span v-if="currentSku.originalPrice" class="pd-info__price-original">
          {{ formatPrice(currentSku.originalPrice) }}
        </span>
        <span v-if="currentSku.discountPercent > 0" class="pd-info__price-badge">
          -{{ currentSku.discountPercent }}%
        </span>
      </div>
      <div v-else class="pd-info__price-row">
        <span class="pd-info__price-current">{{ formatPrice(minPrice) }}</span>
        <span v-if="maxPrice && maxPrice !== minPrice" class="pd-info__price-range">
          – {{ formatPrice(maxPrice) }}
        </span>
      </div>

      <div v-if="minOriginalPrice && minOriginalPrice > minPrice" class="pd-info__save">
        Tiết kiệm <strong>{{ formatPrice(minOriginalPrice - minPrice) }}</strong>
        so với giá gốc
      </div>
    </div>

    <!-- Wishlist + share -->
    <div class="pd-info__actions-top">
      <button
        type="button"
        class="pd-info__icon-btn"
        :class="{ active: isWished }"
        @click="$emit('toggle-wishlist')"
        :title="isWished ? 'Bỏ yêu thích' : 'Yêu thích'"
      >
        <i class="ti ti-heart" aria-hidden="true"></i>
        <span>{{ isWished ? 'Đã yêu thích' : 'Yêu thích' }}</span>
      </button>
      <button type="button" class="pd-info__icon-btn" @click="onShare" title="Chia sẻ">
        <i class="ti ti-share" aria-hidden="true"></i>
        <span>Chia sẻ</span>
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  productName: { type: String, default: '' },
  brand: { type: String, default: '' },
  totalSold: { type: Number, default: 0 },
  rating: { type: Number, default: 0 },
  reviewCount: { type: Number, default: 0 },
  minPrice: { type: [Number, String], default: 0 },
  maxPrice: { type: [Number, String], default: 0 },
  minOriginalPrice: { type: [Number, String], default: 0 },
  isWished: { type: Boolean, default: false },
  currentSku: { type: Object, default: null },
})

defineEmits(['toggle-wishlist'])

function getStarClass(n) {
  const r = props.rating || 0
  if (r >= n) return 'full'
  if (r >= n - 0.5) return 'half'
  return 'empty'
}

function formatReviewCount(n) {
  if (!n) return '0 đánh giá'
  if (n >= 1000) return `${(n / 1000).toFixed(1).replace(/\.0$/, '')}k+ đánh giá`
  return `${n} đánh giá`
}

function formatPrice(value) {
  if (value == null) return ''
  return new Intl.NumberFormat('vi-VN').format(Number(value)) + 'đ'
}

function formatSold(n) {
  n = Number(n) || 0
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k+'
  return n.toString()
}

function onShare() {
  if (navigator.share) {
    navigator
      .share({ title: props.productName, url: window.location.href })
      .catch(() => copyLink())
  } else {
    copyLink()
  }
}
function copyLink() {
  navigator.clipboard?.writeText(window.location.href)
}
</script>

<style scoped>
.pd-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pd-info__brand {
  font-size: 13px;
  font-weight: 600;
  color: #e11d1d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.pd-info__name {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.35;
  margin: 0;
}

.pd-info__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #555;
  flex-wrap: wrap;
}

.pd-info__stars {
  display: flex;
  gap: 2px;
}
.pd-info__star {
  font-size: 14px;
  line-height: 1;
}
.pd-info__star.full {
  color: #ffb800;
}
.pd-info__star.half {
  background: linear-gradient(90deg, #ffb800 50%, #ccc 50%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.pd-info__star.empty {
  color: #ccc;
}

.pd-info__rating-val {
  font-weight: 600;
  color: #444;
}

.pd-info__reviews {
  color: #0066cc;
  font-weight: 500;
  cursor: pointer;
  text-decoration: underline;
}
.pd-info__reviews:hover {
  color: #004a99;
}

.pd-info__sold strong {
  color: #1a1a1a;
}

.pd-info__divider {
  width: 1px;
  height: 14px;
  background: #ddd;
}

.pd-info__price-box {
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pd-info__price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.pd-info__price-current {
  color: #e11d1d;
  font-size: 28px;
  font-weight: 800;
}
.pd-info__price-original {
  color: #999;
  font-size: 15px;
  text-decoration: line-through;
}
.pd-info__price-badge {
  background: #e11d1d;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}
.pd-info__price-range {
  color: #e11d1d;
  font-size: 22px;
  font-weight: 800;
}
.pd-info__save {
  font-size: 13px;
  color: #16a34a;
}
.pd-info__save strong {
  font-weight: 700;
}

.pd-info__actions-top {
  display: flex;
  gap: 8px;
}
.pd-info__icon-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  transition: all 0.15s ease;
}
.pd-info__icon-btn:hover {
  border-color: #e11d1d;
  color: #e11d1d;
}
.pd-info__icon-btn.active {
  border-color: #e11d1d;
  background: #fff5f5;
  color: #e11d1d;
}
.pd-info__icon-btn i {
  font-size: 16px;
}
</style>
