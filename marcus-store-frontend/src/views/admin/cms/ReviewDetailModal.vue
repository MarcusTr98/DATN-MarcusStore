<template>
  <Teleport to="body">
  <transition name="rdm-fade">
    <div
      v-if="visible"
      class="rdm-overlay"
      @mousedown="onOverlayMousedown"
      @click="onOverlayClick"
    >
      <div class="rdm-box">

        <div class="rdm-header">
          <h3>
            <i class="fas fa-star"></i>
            Chi tiết đánh giá
          </h3>

          <button
            class="rdm-close-btn"
            @click="onCloseBtnClick"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="rdm-body">

          <!-- Khách hàng -->
          <div class="rdm-section">
            <h4><i class="fas fa-user"></i> Khách hàng</h4>

            <div class="rdm-row">
              <span>Họ tên</span>
              <b>{{ review?.fullName || "-" }}</b>
            </div>

            <div class="rdm-row">
              <span>Tài khoản</span>
              <b>{{ review?.username || "-" }}</b>
            </div>
          </div>

          <!-- Sản phẩm -->
          <div class="rdm-section">
            <h4><i class="fas fa-mobile-alt"></i> Sản phẩm</h4>

            <div class="rdm-row">
              <span>Tên sản phẩm</span>
              <b>{{ review?.productName || "-" }}</b>
            </div>
          </div>

          <!-- Đánh giá -->
          <div class="rdm-section">
            <h4><i class="fas fa-star"></i> Đánh giá</h4>

            <div class="rdm-stars">
              <i
                v-for="n in 5"
                :key="n"
                class="fas fa-star"
                :class="{ active: n <= (review?.rating || 0) }"
                :style="{ '--i': n }"
              ></i>

              <span class="rdm-rating-num">{{ review?.rating || 0 }}/5</span>
            </div>

            <div class="rdm-comment">
              <i class="fas fa-quote-left rdm-quote-icon"></i>
              {{ review?.commentText || "Không có nội dung" }}
            </div>
<div
  v-if="review?.images && review.images.length"
  class="rdm-images"
>
    <img
        v-for="(image,index) in review.images"
        :key="index"
        :src="image"
        class="rdm-image"
        @click="previewImage(image)"
    >
</div>

            <div class="rdm-date">
              <i class="far fa-clock"></i>
              {{ formatDate(review?.createdAt) }}
            </div>
          </div>

          <!-- Phản hồi -->
          <div class="rdm-section">

            <h4><i class="fas fa-comment-dots"></i> Phản hồi</h4>

            <div
                v-if="review.reply"
                class="rdm-reply-box"
            >
                <div class="rdm-reply-meta">
                  <strong>{{ review.reply.staffName }}</strong>
                  <small>{{ formatDate(review.reply.createdAt) }}</small>
                </div>

                <textarea
                    v-model="replyText"
                    class="rdm-textarea"
                    rows="5"
                ></textarea>

            </div>

            <div v-else>

                <textarea
                    v-model="replyText"
                    class="rdm-textarea"
                    rows="5"
                    placeholder="Nhập phản hồi cho khách hàng..."
                ></textarea>

            </div>

            <div class="rdm-action">

                <button
                    class="rdm-save-btn"
                    @click="saveReply"
                    :disabled="loading"
                >

                    <i class="fas fa-paper-plane"></i>

                    {{ review.reply ? "Cập nhật phản hồi" : "Gửi phản hồi" }}

                </button>

            </div>

          </div>

        </div>

        <div class="rdm-footer">

          <button
            class="rdm-btn-close"
            @click="onFooterCloseClick"
          >
            Đóng
          </button>

        </div>

      </div>
    </div>
  </transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from "vue";
import api from "@/utils/api";

const emit = defineEmits([
  "close",
  "saved"
])

const props = defineProps({
  visible: Boolean,
  review: {
    type: Object,
    default: () => ({})
  }
})

const replyText = ref("")
const loading = ref(false)

watch(
    () => props.review,
    (val) => {
        console.log("Review =", val)
        console.log("Images =", val?.images)
      replyText.value = val?.reply?.replyText || ""
    },
    { immediate: true }
)

const saveReply = async () => {

  if (!replyText.value.trim()) {
    alert("Vui lòng nhập phản hồi")
    return
  }

  loading.value = true

  try {

    if (props.review.reply) {

      await api.put(
          `/admin/reviews/${props.review.reviewId}/reply`,
          {
            replyText: replyText.value
          }
      )

    } else {

      await api.post(
          `/admin/reviews/${props.review.reviewId}/reply`,
          {
            replyText: replyText.value
          }
      )

    }

    emit("saved")
    emit("close")

  } catch (e) {

    console.error(e)
    alert("Lưu phản hồi thất bại")

  } finally {

    loading.value = false

  }

}

let mousedownOnOverlay = false

const onOverlayMousedown = (e) => {
  mousedownOnOverlay = e.target === e.currentTarget
}

const onOverlayClick = (e) => {

  if (mousedownOnOverlay && e.target === e.currentTarget) {
    emit("close")
  }

  mousedownOnOverlay = false
}

const onCloseBtnClick = () => emit("close")

const onFooterCloseClick = () => emit("close")

const formatDate = (date) => {

  if (!date) return "-"

  return new Date(date).toLocaleString("vi-VN")

}
const previewImage = (url) => {

    window.open(url, "_blank")

}
</script>

<style scoped>

.rdm-fade-enter-active,
.rdm-fade-leave-active{
  transition: opacity .25s !important;
}

.rdm-fade-enter-from,
.rdm-fade-leave-to{
  opacity: 0 !important;
}

.rdm-overlay{
  position: fixed !important;
  inset: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  background: rgba(0,0,0,.45) !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  z-index: 99999 !important;
  margin: 0 !important;
  padding: 0 !important;
}

.rdm-box{
  display: flex !important;
  flex-direction: column !important;
  width: 700px !important;
  max-width: 92vw !important;
  max-height: 88vh !important;
  background: white !important;
  border-radius: 24px !important;
  box-shadow: 0 20px 60px rgba(0,0,0,.35) !important;
  overflow: hidden !important;
  font-family: -apple-system, "Segoe UI", Roboto, Inter, sans-serif;
}

.rdm-header{
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:22px 28px;
  border-bottom:1px solid #eee;
  flex-shrink:0;
  background: linear-gradient(135deg, #fff5f9, #fff);
}

.rdm-header h3{
  background: linear-gradient(135deg, #f55d9b, #c24f83);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size:24px;
  font-weight:800;
  letter-spacing: -.3px;
  display:flex;
  align-items:center;
  gap:10px;
}

.rdm-header h3 i{
  color:#ffb400;
  -webkit-text-fill-color:#ffb400;
}

.rdm-close-btn{
  width:36px;
  height:36px;
  display:flex;
  align-items:center;
  justify-content:center;
  border:none;
  background:#fff0f6;
  color:#c24f83;
  border-radius:50%;
  cursor:pointer;
  transition: all .2s ease;
}

.rdm-close-btn:hover{
  background:#f55d9b;
  color:white;
  transform: rotate(90deg);
}

.rdm-body{
  overflow-y:auto;
  flex:1;
}

.rdm-section{
  padding:20px 28px;
  border-bottom:1px solid #f2f2f2;
  transition: background .2s ease;
}

.rdm-section:last-child{
  border-bottom:none;
}

.rdm-section:hover{
  background:#fffbfd;
}

.rdm-section h4{
  color:#f55d9b;
  margin-bottom:15px;
  font-size:15px;
  font-weight:700;
  text-transform:uppercase;
  letter-spacing:.6px;
  display:flex;
  align-items:center;
  gap:8px;
}

.rdm-section h4 i{
  font-size:13px;
  color:#ffb400;
}

.rdm-row{
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:10px 14px;
  margin-bottom:8px;
  background:#fafafa;
  border-radius:12px;
  transition: background .2s ease, transform .2s ease;
}

.rdm-row:hover{
  background:#fff0f6;
  transform: translateX(2px);
}

.rdm-row:last-child{
  margin-bottom:0;
}

.rdm-row span{
  color:#999;
  font-size:14px;
  font-weight:500;
}

.rdm-row b{
  color:#333;
  font-size:15px;
  font-weight:700;
}

.rdm-stars{
  margin-bottom:15px;
  display:flex;
  align-items:center;
  gap:2px;
}

.rdm-stars i{
  color:#ddd;
  margin-right:3px;
  font-size:20px;
  transition: transform .2s ease;
  animation: rdm-pop .35s ease backwards;
  animation-delay: calc(var(--i) * .05s);
}

.rdm-stars i.active{
  color:#ffb400;
  text-shadow: 0 2px 6px rgba(255,180,0,.35);
}

.rdm-stars:hover i.active{
  transform: scale(1.15) rotate(-8deg);
}

@keyframes rdm-pop{
  from{ opacity:0; transform: scale(0) rotate(-40deg); }
  to{ opacity:1; transform: scale(1) rotate(0); }
}

.rdm-rating-num{
  margin-left:8px;
  font-weight:800;
  color:#f55d9b;
  font-size:16px;
}

.rdm-comment{
  position:relative;
  background: linear-gradient(135deg, #fff7fb, #fff);
  border:1px solid #ffd8e7;
  border-radius:16px;
  padding:18px 20px 18px 42px;
  line-height:1.7;
  margin-bottom:12px;
  font-size:15px;
  color:#444;
  font-style:italic;
}

.rdm-quote-icon{
  position:absolute;
  top:14px;
  left:16px;
  color:#ffd8e7;
  font-size:18px;
  font-style:normal;
}

.rdm-date{
  color:#aaa;
  font-size:13px;
  display:flex;
  align-items:center;
  gap:6px;
}

.rdm-reply-box{
  background: linear-gradient(135deg, #f8f9ff, #fff);
  border-left:4px solid #f55d9b;
  border-radius:16px;
  padding:16px 18px;
  box-shadow: 0 4px 14px rgba(245,93,155,.08);
}

.rdm-reply-meta{
  display:flex;
  align-items:baseline;
  justify-content:space-between;
  margin-bottom:6px;
}

.rdm-reply-meta strong{
  color:#333;
  font-size:15px;
  font-weight:700;
}

.rdm-reply-box small{
  color:#aaa;
  font-size:12.5px;
}

.rdm-footer{
  display:flex;
  justify-content:flex-end;
  padding:20px 28px;
  border-top:1px solid #f2f2f2;
  flex-shrink:0;
}

.rdm-btn-close{
  border:none;
  background:#f8f8f8;
  color:#666;
  padding:12px 25px;
  border-radius:14px;
  cursor:pointer;
  font-weight:600;
  transition: background .2s ease;
}

.rdm-btn-close:hover{
  background:#eee;
}

.rdm-textarea{
    width:100%;
    margin-top:12px;
    min-height:120px;
    resize:vertical;
    border:1.5px solid #ffd7e5;
    border-radius:16px;
    padding:16px;
    outline:none;
    font-size:15px;
    line-height:1.6;
    color:#333;
    font-family: inherit;
    transition: border-color .2s ease, box-shadow .2s ease;
}

.rdm-textarea::placeholder{
    color:#c9c9c9;
}

.rdm-textarea:focus{
    border-color:#f55d9b;
    box-shadow:0 0 0 4px rgba(245,93,155,.12);
}

.rdm-action{
    margin-top:18px;
    display:flex;
    justify-content:flex-end;
}

.rdm-save-btn{
    border:none;
    background: linear-gradient(135deg, #f55d9b, #ef3f89);
    color:white;
    padding:13px 26px;
    border-radius:14px;
    cursor:pointer;
    font-weight:700;
    font-size:14.5px;
    letter-spacing:.2px;
    box-shadow: 0 6px 16px rgba(245,93,155,.3);
    transition: transform .15s ease, box-shadow .15s ease;
    display:flex;
    align-items:center;
    gap:8px;
}

.rdm-save-btn:hover:not(:disabled){
    transform: translateY(-2px);
    box-shadow: 0 10px 22px rgba(245,93,155,.4);
}

.rdm-save-btn:active:not(:disabled){
    transform: translateY(0);
}

.rdm-save-btn:disabled{
    opacity:.6;
    cursor:not-allowed;
    box-shadow:none;
}


.rdm-images{

    display:flex;

    flex-wrap:wrap;

    gap:12px;

    margin:18px 0;

}

.rdm-image{

    width:110px;

    height:110px;

    object-fit:cover;

    border-radius:12px;

    border:1px solid #ececec;

    cursor:pointer;

    transition:.25s;

    background:white;

}

.rdm-image:hover{

    transform:scale(1.06);

    box-shadow:0 8px 22px rgba(0,0,0,.18);

}
</style>