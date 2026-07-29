<template>
  <div class="pd-reviews">
    <div class="pd-reviews__header">
      <h3 class="pd-reviews__title">
        Đánh giá {{ productName }}
      </h3>
    </div>

    <!-- ================= TÓM TẮT ================= -->
    <div
      v-if="reviewCount > 0"
      class="pd-reviews__summary"
    >
      <!-- Cột trái -->
      <div class="pd-reviews__col pd-reviews__col-left">
        <div class="pd-reviews__rating-line">
          <span class="pd-reviews__rating-num">
            {{ rating > 0 ? rating.toFixed(1) : "0" }}
          </span>

          <span class="pd-reviews__rating-out">/5</span>
        </div>

        <div class="pd-reviews__stars">
          <span
            v-for="n in 5"
            :key="n"
            class="pd-reviews__star"
            :class="getStarClass(n)"
          >
            ★
          </span>
        </div>

        <div class="pd-reviews__count">
          {{ formatReviewCount(reviewCount) }}
        </div>
      </div>

      <!-- Cột giữa -->
      <div class="pd-reviews__col pd-reviews__col-mid">
        <div class="pd-reviews__satisfied">
          <div class="pd-reviews__satisfied-row">
            <span class="pd-reviews__satisfied-num">
              {{ satisfiedCount }}
            </span>

            <span class="pd-reviews__satisfied-suffix">
              khách hài lòng
            </span>
          </div>

          <div class="pd-reviews__satisfied-desc">
            {{ formatSatisfiedDesc(satisfiedCount, reviewCount) }}
          </div>
        </div>
      </div>

      <!-- Cột phải -->
      <div class="pd-reviews__col pd-reviews__col-right">
        <div class="pd-reviews__bars">
          <div
            v-for="star in [5,4,3,2,1]"
            :key="star"
            class="pd-reviews__bar-row"
          >
            <span class="pd-reviews__bar-label">
              {{ star }}
              <span class="pd-reviews__bar-star">★</span>
            </span>

            <div class="pd-reviews__bar-track">
              <div
                class="pd-reviews__bar-fill"
                :style="{ width: getBarPercent(star) + '%' }"
              />
            </div>

            <span class="pd-reviews__bar-pct">
              {{ getBarPercent(star) }}%
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= CHƯA CÓ ĐÁNH GIÁ ================= -->

    <div
      v-else
      class="pd-reviews__empty"
    >
      <i class="ti ti-message-circle"></i>

      <p>Chưa có đánh giá nào cho sản phẩm này.</p>

      <p class="pd-reviews__empty-hint">
        Hãy là người đầu tiên đánh giá!
      </p>
    </div>

    <!-- ================= BUTTON ================= -->

    <div
      v-if="reviewCount > 0"
      class="pd-reviews__actions"
    >
      <button
        class="pd-reviews__read-btn"
        @click="toggleReviews"
      >
        <i class="ti ti-message-dots"></i>

        {{ showReviews ? "Ẩn đánh giá" : "Đọc đánh giá" }}

        <i
          class="ti ti-chevron-down pd-reviews__read-btn-chevron"
          :class="{ 'is-open': showReviews }"
        ></i>
      </button>
    </div>

    <!-- ================= DANH SÁCH ĐÁNH GIÁ ================= -->

    <div
      v-if="showReviews"
      class="pd-reviews__list"
    >
      <!-- Loading -->

      <div
        v-if="loadingReviews"
        class="pd-reviews__list-empty"
      >
        <i class="ti ti-loader-2"></i>

        <span>Đang tải đánh giá...</span>
      </div>

      <!-- Không có -->

      <div
        v-else-if="reviews.length===0"
        class="pd-reviews__list-empty"
      >
        Chưa có đánh giá nào.
      </div>

      <!-- Có dữ liệu -->

      <div
        v-else
        class="pd-reviews__list-items"
      >
        <div
          v-for="review in reviews"
          :key="review.reviewId"
          class="pd-review-item"
        >
          <div class="pd-review-item__header">

            <div class="pd-review-item__avatar">
              {{ (review.fullName || "K").charAt(0).toUpperCase() }}
            </div>

            <div class="pd-review-item__meta">

              <div class="pd-review-item__name">
                {{ review.fullName }}
              </div>

              <div class="pd-review-item__stars">

                <span
                  v-for="star in 5"
                  :key="star"
                  class="pd-review-item__star"
                  :class="{ full: review.rating >= star }"
                >
                  ★
                </span>

              </div>

            </div>

            <div class="pd-review-item__date">
              {{ formatDate(review.createdAt) }}
            </div>

          </div>

          <div
            v-if="review.commentText"
            class="pd-review-item__comment"
          >
            {{ review.commentText }}
          </div>
          <div
  v-if="review.images && review.images.length"
  class="pd-review-item__images"
>
  <img
    v-for="(image,index) in review.images"
    :key="index"
    :src="image"
    class="pd-review-item__image"
  />
</div>
<div
  v-if="review.replyContent"
  class="pd-review-item__reply"
>

  <div class="pd-review-item__reply-title">

    <i class="fa-solid fa-store"></i>

    Cửa hàng phản hồi

  </div>

  <div class="pd-review-item__reply-content">

    {{ review.replyContent }}

  </div>

  <div class="pd-review-item__reply-info">

    {{ review.replyStaffName }}

    •

    {{ formatDate(review.replyCreatedAt) }}

  </div>

</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed } from "vue";
import reviewService from "@/stores/reviewService";

const props = defineProps({
  productId: {
    type: Number,
    required: true,
  },
  productName: {
    type: String,
    default: "",
  },
  rating: {
    type: Number,
    default: 0,
  },
  reviewCount: {
    type: Number,
    default: 0,
  },
  reviewDistribution: {
    type: Array,
    default: () => [],
  },
});

const showReviews = ref(false);
const loadingReviews = ref(false);
const reviews = ref([]);

async function toggleReviews() {
  showReviews.value = !showReviews.value

  if (
    showReviews.value &&
    reviews.value.length === 0 &&
    props.productId
  ) {
    try {
      console.log("ProductId:", props.productId)

      const res = await reviewService.getProductReviews(props.productId)

      console.log("Response:", res.data)

      reviews.value = res.data.data
      console.log("Reviews:", reviews.value)
      console.log(JSON.stringify(reviews.value, null, 2))

    } catch (e) {
      console.error(e)
    }
  }
}

function getStarClass(n) {
  const r = props.rating || 0;

  if (r >= n) return "full";
  if (r >= n - 0.5) return "half";

  return "empty";
}

function getBarPercent(star) {
  const item = props.reviewDistribution.find(
    (x) => x.star === star
  );

  if (!item || props.reviewCount === 0) return 0;

  return Math.round((item.count / props.reviewCount) * 100);
}

function formatReviewCount(count) {
  if (!count) return "0 đánh giá";

  if (count >= 1000) {
    return `${(count / 1000).toFixed(1).replace(".0", "")}k+ đánh giá`;
  }

  return `${count} đánh giá`;
}

function formatDate(date) {
  if (!date) return "";

  return new Date(date).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

const satisfiedCount = computed(() => {
  return Math.round(
    (props.reviewCount || 0) *
      ((props.rating || 0) / 5)
  );
});

function formatSatisfiedDesc(satisfied, total) {
  if (!total) return "";

  const percent = Math.round((satisfied / total) * 100);

  if (percent >= 90)
    return `Tuyệt vời! ${percent}% khách hàng hài lòng`;

  if (percent >= 70)
    return `${percent}% khách hàng hài lòng`;

  if (percent >= 50)
    return `${percent}% khách hàng đánh giá tốt`;

  return `${percent}% khách hàng phản hồi tích cực`;
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

/* ===== Summary ===== */

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

/* Left */

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
}

.pd-reviews__rating-out {
  color: #999;
  font-size: 16px;
}

.pd-reviews__stars {
  display: flex;
  gap: 2px;
}

.pd-reviews__star {
  font-size: 16px;
}

.pd-reviews__star.full {
  color: #ffb800;
}

.pd-reviews__star.empty {
  color: #ccc;
}

.pd-reviews__star.half {
  background: linear-gradient(90deg, #ffb800 50%, #ccc 50%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.pd-reviews__count {
  color: #888;
  font-size: 12px;
}

/* Mid */

.pd-reviews__col-mid {
  align-items: center;
  text-align: center;
  border-right: 1px solid #eee;
  padding-right: 16px;
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
}

.pd-reviews__satisfied-suffix {
  font-weight: 600;
}

.pd-reviews__satisfied-desc {
  color: #666;
  font-size: 13px;
}

/* Right */

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
}

.pd-reviews__bar-label {
  text-align: right;
}

.pd-reviews__bar-star {
  color: #ffb800;
}

.pd-reviews__bar-track {
  height: 8px;
  background: #eee;
  border-radius: 999px;
  overflow: hidden;
}

.pd-reviews__bar-fill {
  height: 100%;
  background: #ffb800;
}

.pd-reviews__bar-pct {
  font-size: 12px;
  color: #666;
}

/* ===== Empty ===== */

.pd-reviews__empty {
  padding: 40px;
  text-align: center;
  color: #777;
}

.pd-reviews__empty i {
  font-size: 42px;
  margin-bottom: 10px;
}

.pd-reviews__empty-hint {
  color: #e11d1d;
  font-weight: 600;
}

/* ===== Button ===== */

.pd-reviews__actions {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.pd-reviews__read-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 26px;
  border: 1px solid #e11d1d;
  border-radius: 8px;
  background: white;
  color: #e11d1d;
  cursor: pointer;
  transition: .2s;
}

.pd-reviews__read-btn:hover {
  background: #e11d1d;
  color: white;
}

.pd-reviews__read-btn-chevron {
  transition: .25s;
}

.pd-reviews__read-btn-chevron.is-open {
  transform: rotate(180deg);
}

/* ===== List ===== */

.pd-reviews__list {
  border-top: 1px solid #eee;
  padding: 20px 24px;
}

.pd-reviews__list-empty {
  text-align: center;
  color: #888;
}

.pd-reviews__list-items {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pd-review-item {
  padding-bottom: 18px;
  border-bottom: 1px solid #eee;
}

.pd-review-item:last-child {
  border-bottom: none;
}

.pd-review-item__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pd-review-item__avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #e11d1d;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 700;
}

.pd-review-item__meta {
  flex: 1;
}

.pd-review-item__name {
  font-weight: 600;
}

.pd-review-item__stars {
  display: flex;
  gap: 2px;
  margin-top: 3px;
}

.pd-review-item__star {
  color: #ccc;
}

.pd-review-item__star.full {
  color: #ffb800;
}

.pd-review-item__date {
  font-size: 12px;
  color: #999;
}

.pd-review-item__comment {
  margin-top: 10px;
  line-height: 1.6;
  color: #444;
}

/* ===== Responsive ===== */

@media (max-width:768px) {

  .pd-reviews__summary {
    grid-template-columns: 1fr;
  }

  .pd-reviews__col-left,
  .pd-reviews__col-mid {
    border-right: none;
    border-bottom: 1px solid #eee;
    padding-right: 0;
    padding-bottom: 16px;
  }

}

/* ================= Ảnh đánh giá ================= */

.pd-review-item__images{

display:flex;

gap:10px;

flex-wrap:wrap;

margin-top:12px;

}

.pd-review-item__image{

width:90px;

height:90px;

object-fit:cover;

border-radius:8px;

border:1px solid #eee;

cursor:pointer;

transition:.2s;

}

.pd-review-item__image:hover{

transform:scale(1.05);

}

/* ================= Reply ================= */

.pd-review-item__reply{

margin-top:15px;

background:#fafafa;

border-left:4px solid #e11d1d;

padding:12px;

border-radius:8px;

}

.pd-review-item__reply-title{

font-weight:700;

color:#e11d1d;

display:flex;

align-items:center;

gap:6px;

margin-bottom:8px;

}

.pd-review-item__reply-content{

line-height:1.6;

color:#333;

}

.pd-review-item__reply-info{

margin-top:8px;

font-size:12px;

color:#999;

}
</style>