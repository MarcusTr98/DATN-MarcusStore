<template>
  <div class="pd-reviews">
    <div class="pd-reviews__header">
      <h3 class="pd-reviews__title">Đánh giá {{ productName }}</h3>
    </div>

    <div v-if="reviewCount > 0" class="pd-reviews__summary">
      <!-- Cột 1: Đánh giá / sao + số lượt -->
      <div class="pd-reviews__col pd-reviews__col-left">
        <div class="pd-reviews__rating-line">
          <span class="pd-reviews__rating-num">{{ rating > 0 ? rating.toFixed(1) : '0' }}</span>
          <span class="pd-reviews__rating-out">/5</span>
        </div>
        <div class="pd-reviews__stars">
          <span
            v-for="n in 5"
            :key="n"
            class="pd-reviews__star"
            :class="getStarClass(n)"
          >★</span>
        </div>
        <div class="pd-reviews__count">{{ formatReviewCount(reviewCount) }}</div>
      </div>

      <!-- Cột 2: Số + khách hài lòng -->
      <div class="pd-reviews__col pd-reviews__col-mid">
        <div class="pd-reviews__satisfied">
          <div class="pd-reviews__satisfied-row">
            <span class="pd-reviews__satisfied-num">{{ satisfiedCount }}</span>
            <span class="pd-reviews__satisfied-suffix">khách hài lòng</span>
          </div>
          <div class="pd-reviews__satisfied-desc">
            {{ formatSatisfiedDesc(satisfiedCount, reviewCount) }}
          </div>
        </div>
      </div>

      <!-- Cột 3: Bar chart -->
      <div class="pd-reviews__col pd-reviews__col-right">
        <div class="pd-reviews__bars">
          <div
            v-for="n in [5, 4, 3, 2, 1]"
            :key="n"
            class="pd-reviews__bar-row"
          >
            <span class="pd-reviews__bar-label">{{ n }} <span class="pd-reviews__bar-star">★</span></span>
            <div class="pd-reviews__bar-track">
              <div
                class="pd-reviews__bar-fill"
                :style="{ width: getBarPercent(n) + '%' }"
              />
            </div>
            <span class="pd-reviews__bar-pct">{{ getBarPercent(n) }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Placeholder khi chưa có đánh giá -->
    <div v-else class="pd-reviews__empty">
      <i class="ti ti-message-circle" aria-hidden="true" />
      <p>Chưa có đánh giá nào cho sản phẩm này.</p>
      <p class="pd-reviews__empty-hint">Hãy là người đầu tiên đánh giá!</p>
    </div>

    <!-- Action: đọc đánh giá -->
    <div v-if="reviewCount > 0" class="pd-reviews__actions">
      <button type="button" class="pd-reviews__read-btn" @click="toggleReviews">
        <i class="ti ti-message-dots" aria-hidden="true" />
        {{ showReviews ? 'Ẩn đánh giá' : 'Đọc đánh giá' }}
        <i
          class="ti ti-chevron-down pd-reviews__read-btn-chevron"
          :class="{ 'is-open': showReviews }"
          aria-hidden="true"
        />
      </button>
    </div>

    <!-- Danh sách đánh giá chi tiết -->
    <div v-if="showReviews && reviewCount > 0" class="pd-reviews__list">
      <div v-if="reviews.length === 0" class="pd-reviews__list-empty">
        <i class="ti ti-loader-2" aria-hidden="true" />
        <span>Chưa tải được nội dung đánh giá.</span>
      </div>

      <div v-else class="pd-reviews__list-items">
        <div v-for="(rv, idx) in reviews" :key="rv.reviewId || idx" class="pd-review-item">
          <div class="pd-review-item__header">
            <div class="pd-review-item__avatar">
              {{ (rv.userName || 'K').charAt(0).toUpperCase() }}
            </div>
            <div class="pd-review-item__meta">
              <div class="pd-review-item__name">{{ rv.userName || 'Khách hàng' }}</div>
              <div class="pd-review-item__stars">
                <span
                  v-for="n in 5"
                  :key="n"
                  class="pd-review-item__star"
                  :class="{ full: (rv.rating || 0) >= n }"
                >★</span>
              </div>
            </div>
            <div v-if="rv.createdAt" class="pd-review-item__date">
              {{ formatDate(rv.createdAt) }}
            </div>
          </div>
          <p v-if="rv.comment" class="pd-review-item__comment">{{ rv.comment }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  productName: { type: String, default: '' },
  rating: { type: Number, default: 0 },
  reviewCount: { type: Number, default: 0 },
  reviewDistribution: { type: Array, default: () => [] },
  reviews: { type: Array, default: () => [] },
})

const showReviews = ref(false)

function toggleReviews() {
  showReviews.value = !showReviews.value
}

function getStarClass(n) {
  const r = props.rating || 0
  if (r >= n) return 'full'
  if (r >= n - 0.5) return 'half'
  return 'empty'
}

function getBarPercent(star) {
  const dist = props.reviewDistribution || []
  const entry = dist.find((d) => d.star === star)
  if (!entry || !props.reviewCount) return 0
  return Math.round((entry.count / props.reviewCount) * 100)
}

function formatReviewCount(n) {
  if (!n) return '0 đánh giá'
  if (n >= 1000) return `${(n / 1000).toFixed(1).replace(/\.0$/, '')}k+ đánh giá`
  return `${n} đánh giá`
}

function formatDate(dateStr) {
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('vi-VN')
  } catch {
    return ''
  }
}

const satisfiedCount = Math.round((props.reviewCount || 0) * ((props.rating || 0) / 5))

function formatSatisfiedDesc(satisfied, total) {
  if (!total) return ''
  const pct = Math.round((satisfied / total) * 100)
  if (pct >= 90) return `Tuyệt vời! ${pct}% khách hàng hài lòng`
  if (pct >= 70) return `${pct}% khách hàng hài lòng`
  if (pct >= 50) return `${pct}% khách hàng đánh giá tốt`
  return `${pct}% khách hàng phản hồi tích cực`
}
</script>

<style scoped>
.pd-reviews {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  overflow: hidden;
}

.pd-reviews__header {
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}
.pd-reviews__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.pd-reviews__summary {
  display: grid;
  grid-template-columns: 1fr 1.1fr 1.2fr;
  gap: 24px;
  padding: 28px 24px;
  align-items: center;
}

.pd-reviews__col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* Cột 1: số lớn + sao + lượt */
.pd-reviews__col-left {
  align-items: center;
  text-align: center;
  border-right: 1px solid #eee;
  padding-right: 16px;
}
.pd-reviews__rating-line {
  display: flex;
  align-items: baseline;
  gap: 2px;
}
.pd-reviews__rating-num {
  font-size: 40px;
  font-weight: 800;
  color: #e11d1d;
  line-height: 1;
}
.pd-reviews__rating-out {
  font-size: 16px;
  color: #999;
  font-weight: 500;
}
.pd-reviews__stars {
  display: flex;
  gap: 2px;
}
.pd-reviews__star {
  font-size: 16px;
  line-height: 1;
}
.pd-reviews__star.full { color: #ffb800; }
.pd-reviews__star.half {
  background: linear-gradient(90deg, #ffb800 50%, #ccc 50%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.pd-reviews__star.empty { color: #ccc; }
.pd-reviews__count {
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

/* Cột 2: số khách hài lòng */
.pd-reviews__col-mid {
  align-items: center;
  text-align: center;
  border-right: 1px solid #eee;
  padding-right: 16px;
}
.pd-reviews__satisfied {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}
.pd-reviews__satisfied-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.pd-reviews__satisfied-num {
  font-size: 36px;
  font-weight: 800;
  color: #e11d1d;
  line-height: 1;
}
.pd-reviews__satisfied-suffix {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}
.pd-reviews__satisfied-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  max-width: 220px;
}

/* Cột 3: bar chart */
.pd-reviews__col-right {
  padding-left: 8px;
}
.pd-reviews__bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pd-reviews__bar-row {
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  gap: 10px;
  align-items: center;
  font-size: 13px;
}
.pd-reviews__bar-label {
  color: #666;
  text-align: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1px;
}
.pd-reviews__bar-star {
  color: #ffb800;
  font-size: 12px;
}
.pd-reviews__bar-track {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.pd-reviews__bar-fill {
  height: 100%;
  background: #ffb800;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.pd-reviews__bar-pct {
  color: #666;
  font-size: 12px;
}

/* Empty */
.pd-reviews__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 20px;
  color: #aaa;
}
.pd-reviews__empty i {
  font-size: 48px;
}
.pd-reviews__empty p {
  font-size: 14px;
  color: #666;
  text-align: center;
  margin: 0;
}
.pd-reviews__empty-hint {
  color: #e11d1d !important;
  font-weight: 600;
}

/* Action button */
.pd-reviews__actions {
  padding: 0 24px 24px;
  display: flex;
  justify-content: center;
}
.pd-reviews__read-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 28px;
  background: #fff;
  border: 1px solid #e11d1d;
  color: #e11d1d;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.pd-reviews__read-btn:hover {
  background: #e11d1d;
  color: #fff;
}
.pd-reviews__read-btn i {
  font-size: 16px;
}
.pd-reviews__read-btn-chevron {
  transition: transform 0.2s ease;
  font-size: 14px !important;
}
.pd-reviews__read-btn-chevron.is-open {
  transform: rotate(180deg);
}

/* Danh sách đánh giá */
.pd-reviews__list {
  border-top: 1px solid #eee;
  padding: 20px 24px 24px;
}
.pd-reviews__list-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: #999;
  font-size: 14px;
}
.pd-reviews__list-items {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pd-review-item {
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f0f0;
}
.pd-review-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.pd-review-item__header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pd-review-item__avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e11d1d;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pd-review-item__meta {
  flex: 1;
  min-width: 0;
}
.pd-review-item__name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}
.pd-review-item__stars {
  display: flex;
  gap: 1px;
  margin-top: 2px;
}
.pd-review-item__star {
  font-size: 13px;
  color: #ccc;
}
.pd-review-item__star.full {
  color: #ffb800;
}
.pd-review-item__date {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
}
.pd-review-item__comment {
  margin: 8px 0 0;
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .pd-reviews__summary {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 20px 16px;
  }
  .pd-reviews__col-left,
  .pd-reviews__col-mid {
    border-right: none;
    border-bottom: 1px solid #eee;
    padding-right: 0;
    padding-bottom: 16px;
  }
}
</style>