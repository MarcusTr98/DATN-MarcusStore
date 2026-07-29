<template>
  <Teleport to="body">
  <transition name="rdm-fade">
    <div
      v-if="visible"
      class="rdm-overlay"
      @mousedown="onOverlayMousedown"
      @click="onOverlayClick"
    >
      <!-- ============================================================
           TỐI ƯU BỐ CỤC MODAL: 3 vùng rõ ràng
           - Header: cố định, không cuộn
           - Body: overflow-y:auto, chứa toàn bộ nội dung dài
           - Footer: cố định, chứa nút hành động (Đóng / Gửi phản hồi)
           max-height đổi thành 90vh theo yêu cầu.
           ============================================================ -->
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

          <!-- ============================================================
               TỐI ƯU: chuyển từ 1 cột dọc (phải cuộn rất nhiều) sang
               bố cục 2 CỘT giống modal đánh giá của khách hàng:
               - Cột trái: Khách hàng + Sản phẩm + Đánh giá (gọn, gộp lại)
               - Cột phải: Phản hồi (chip gợi ý + textarea)
               Nhờ vậy phần lớn nội dung hiển thị được trong 1 màn hình,
               không phải vuốt lên vuốt xuống nhiều lần mới đọc hết.
               (rdm-body vẫn giữ overflow-y:auto để phòng trường hợp
               review có nhiều ảnh/bình luận quá dài.)
               ============================================================ -->
          <div class="rdm-grid">

            <!-- CỘT TRÁI -->
            <div class="rdm-col-left">

              <!-- Khách hàng -->
              <div class="rdm-block">
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
              <div class="rdm-block">
                <h4><i class="fas fa-mobile-alt"></i> Sản phẩm</h4>

                <div class="rdm-row">
                  <span>Tên sản phẩm</span>
                  <b>{{ review?.productName || "-" }}</b>
                </div>
              </div>

              <!-- Đánh giá -->
              <div class="rdm-block">
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

            </div>

            <!-- CỘT PHẢI: Phản hồi -->
            <div class="rdm-col-right">

              <div class="rdm-block rdm-block-reply">

                <h4><i class="fas fa-comment-dots"></i> Phản hồi</h4>

                <!-- Gợi ý phản hồi nhanh theo số sao -->
                <div class="rdm-quick-replies">

                  <button
                      v-for="(text, idx) in quickReplies"
                      :key="idx"
                      type="button"
                      class="rdm-chip"
                      :class="{ 'rdm-chip-active': selectedQuickReply === text }"
                      @click="applyQuickReply(text)"
                  >
                    {{ text }}
                  </button>

                </div>

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
                        rows="9"
                        @input="onReplyManualInput"
                    ></textarea>

                </div>

                <div v-else class="rdm-reply-plain">

                    <textarea
                        v-model="replyText"
                        class="rdm-textarea"
                        rows="9"
                        placeholder="Nhập phản hồi cho khách hàng..."
                        @input="onReplyManualInput"
                    ></textarea>

                </div>

              </div>

            </div>

          </div>

        </div>

        <!-- Footer cố định: gộp cả nút "Đóng" và nút gửi/cập nhật phản hồi -->
        <div class="rdm-footer">

          <button
            class="rdm-btn-close"
            @click="onFooterCloseClick"
          >
            Đóng
          </button>

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
  </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from "vue";
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

// Mẫu phản hồi được chọn gần nhất (để biết nên THAY THẾ hay NỐI THÊM
// khi admin bấm chip khác) - cùng cơ chế với "quick comment" bên khách hàng
const selectedQuickReply = ref("")

// Gợi ý phản hồi nhanh cho nhân viên, chia theo số sao của đánh giá
// để khớp giọng điệu (xin lỗi/cảm ơn) với cảm xúc khách hàng đã để lại
const quickRepliesByRating = {
  1: [
    "Shop xin lỗi vì trải nghiệm chưa tốt, mong bạn để lại thông tin liên hệ để được hỗ trợ ngay.",
    "Rất tiếc về sự cố này, chúng tôi sẽ liên hệ trực tiếp để khắc phục cho bạn trong thời gian sớm nhất.",
    "Cảm ơn góp ý, shop sẽ kiểm tra và cải thiện chất lượng sản phẩm/dịch vụ."
  ],
  2: [
    "Cảm ơn phản hồi của bạn, shop sẽ cải thiện để phục vụ tốt hơn trong lần sau.",
    "Rất tiếc vì trải nghiệm chưa như mong đợi, mong bạn thông cảm và tiếp tục ủng hộ shop.",
    "Shop xin ghi nhận góp ý và sẽ khắc phục sớm nhất có thể."
  ],
  3: [
    "Cảm ơn đánh giá của bạn, shop sẽ tiếp tục cải thiện sản phẩm và dịch vụ.",
    "Cảm ơn góp ý, mong bạn tiếp tục ủng hộ shop trong thời gian tới.",
    "Shop rất trân trọng nhận xét của bạn và sẽ cố gắng hoàn thiện hơn."
  ],
  4: [
    "Cảm ơn bạn đã tin tưởng và ủng hộ shop!",
    "Rất vui vì bạn hài lòng với sản phẩm, hẹn gặp lại bạn ở những đơn hàng sau!",
    "Cảm ơn đánh giá của bạn, chúc bạn sử dụng sản phẩm thật vui vẻ!"
  ],
  5: [
    "Cảm ơn bạn rất nhiều vì đánh giá 5 sao, shop rất vui vì bạn hài lòng!",
    "Cảm ơn bạn đã tin tưởng và ủng hộ shop, hẹn gặp lại bạn ở những đơn hàng tiếp theo!",
    "Rất vui khi sản phẩm làm bạn hài lòng, chúc bạn một ngày tốt lành!"
  ]
}

// Chưa xác định được sao (trường hợp hiếm) thì tạm dùng bộ trung tính (3 sao)
const quickReplies = computed(() =>
    quickRepliesByRating[props.review?.rating] || quickRepliesByRating[3]
)

watch(
    () => props.review,
    (val) => {
      replyText.value = val?.reply?.replyText || ""
      selectedQuickReply.value = ""
    },
    { immediate: true }
)

// Nếu admin tự gõ tay khác với mẫu đang áp dụng, bỏ trạng thái "đang dùng mẫu"
// để lần bấm chip tiếp theo NỐI THÊM thay vì thay thế nhầm nội dung đã gõ.
const onReplyManualInput = () => {
  if (
      selectedQuickReply.value &&
      replyText.value.trim() !== selectedQuickReply.value
  ) {
    selectedQuickReply.value = ""
  }
}

const applyQuickReply = (text) => {

  const current = replyText.value.trim()

  // Nội dung hiện tại đúng bằng mẫu vừa chọn trước đó -> THAY THẾ bằng mẫu mới
  if (selectedQuickReply.value && current === selectedQuickReply.value) {

    replyText.value = text

  }
  // Admin có gõ thêm nội dung riêng, mẫu cũ nằm ở cuối -> bỏ mẫu cũ, nối mẫu mới
  else if (
      selectedQuickReply.value &&
      current.endsWith(selectedQuickReply.value)
  ) {

    const base = current
        .slice(0, current.length - selectedQuickReply.value.length)
        .trim()

    replyText.value = base ? base + " " + text : text

  }
  // Chưa từng chọn mẫu nào -> nối vào cuối nội dung hiện có (nếu có)
  else {

    replyText.value = current ? current + " " + text : text

  }

  selectedQuickReply.value = text

}

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
  width: 860px !important;
  max-width: 94vw !important;
  /* Yêu cầu: giới hạn chiều cao modal ở 90vh để không tràn màn hình,
     phần thân (rdm-body) sẽ tự cuộn khi nội dung dài */
  max-height: 90vh !important;
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

/* ============================================================
   Scrollbar tùy chỉnh cho phần thân modal: mỏng, tông hồng nhạt,
   thay cho thanh cuộn mặc định thô của trình duyệt.
   - Chrome/Edge/Safari: dùng ::-webkit-scrollbar
   - Firefox: dùng scrollbar-width + scrollbar-color
   ============================================================ */
.rdm-body{
  scrollbar-width: thin;
  scrollbar-color: #f7b8d1 transparent;
}

.rdm-body::-webkit-scrollbar{
  width:6px;
}

.rdm-body::-webkit-scrollbar-track{
  background: transparent;
}

.rdm-body::-webkit-scrollbar-thumb{
  background-color: #f7b8d1;
  border-radius: 10px;
}

.rdm-body::-webkit-scrollbar-thumb:hover{
  background-color: #f55d9b;
}

/* ============================================================
   Bố cục 2 cột: cột trái (thông tin khách hàng/sản phẩm/đánh giá)
   gọn theo chiều rộng cố định, cột phải (phản hồi) giãn hết phần
   còn lại và cao bằng cột trái -> tận dụng chiều ngang thay vì
   xếp chồng theo chiều dọc, giảm mạnh nhu cầu phải cuộn.
   ============================================================ */
.rdm-grid{
  display:flex;
  gap:24px;
  padding:22px 28px;
  align-items:stretch;
}

.rdm-col-left{
  flex:0 0 260px;
  display:flex;
  flex-direction:column;
  gap:16px;
  min-width:0;
}

.rdm-col-right{
  flex:1;
  min-width:0;
  display:flex;
}

.rdm-block{
  background:#fafafa;
  border:1px solid #f2f2f2;
  border-radius:16px;
  padding:18px 20px;
  transition: background .2s ease, border-color .2s ease;
}

.rdm-block:hover{
  background:#fffbfd;
  border-color:#ffe3ee;
}

.rdm-block-reply{
  flex:1;
  display:flex;
  flex-direction:column;
  width:100%;
}

.rdm-block h4{
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

.rdm-block h4 i{
  font-size:13px;
  color:#ffb400;
}

/* Trên màn hình hẹp: xếp lại thành 1 cột như trước, tránh vỡ layout */
@media (max-width: 760px){
  .rdm-grid{
    flex-direction:column;
    padding:18px 20px;
  }
  .rdm-col-left{
    flex:none;
  }
  .rdm-col-right{
    flex:none;
  }
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
  color:#E0E0E0;
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

/* ============================================================
   Gợi ý phản hồi nhanh: dùng tông trung tính (xám) chứ không dùng
   hồng, để không đổi màu của khối phản hồi admin vốn có (giữ nguyên
   theo yêu cầu) - chip chỉ đóng vai trò gợi ý, tách biệt về mặt màu.
   ============================================================ */
.rdm-quick-replies{
  display:flex;
  flex-wrap:wrap;
  gap:8px;
  margin-bottom:14px;
}

.rdm-chip{
  padding:7px 13px;
  border-radius:20px;
  border:1px solid #e2e8f0;
  background:#f8fafc;
  color:#475569;
  font-size:12.5px;
  font-weight:600;
  cursor:pointer;
  transition: background .2s ease, border-color .2s ease, color .2s ease;
  white-space:normal;
  text-align:left;
  line-height:1.4;
}

.rdm-chip:hover{
  background:#eef2ff;
  color:#3b4b6b;
  border-color:#c7d2fe;
}

.rdm-chip-active{
  background:#e0e7ff;
  color:#3730a3;
  border-color:#a5b4fc;
}

.rdm-reply-box{
  background: linear-gradient(135deg, #f8f9ff, #fff);
  border-left:4px solid #f55d9b;
  border-radius:16px;
  padding:16px 18px;
  box-shadow: 0 4px 14px rgba(245,93,155,.08);
  flex:1;
  display:flex;
  flex-direction:column;
}

.rdm-reply-plain{
  flex:1;
  display:flex;
  flex-direction:column;
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
  align-items:center;
  gap:12px;
  padding:18px 28px;
  border-top:1px solid #f2f2f2;
  flex-shrink:0;
  background: #fff;
}

.rdm-btn-close{
  border:none;
  background:#f8f8f8;
  color:#4A5568;
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
    flex:1;
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