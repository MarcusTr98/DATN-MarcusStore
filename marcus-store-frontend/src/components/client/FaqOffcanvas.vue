<template>
  <div>
    <!-- Nút Floating FAQ (Dính mép giữa bên phải) -->
    <button
      class="btn-floating-faq shadow-lg"
      type="button"
      data-bs-toggle="offcanvas"
      data-bs-target="#faqOffcanvas"
      aria-controls="faqOffcanvas"
    >
      <i class="fas fa-question-circle fs-5 mb-1"></i>
      <span>Trợ giúp</span>
    </button>

    <!-- Offcanvas FAQ Sidebar -->
    <div
      class="offcanvas offcanvas-end"
      tabindex="-1"
      id="faqOffcanvas"
      aria-labelledby="faqOffcanvasLabel"
    >
      <div class="offcanvas-header bg-light border-bottom">
        <h5 class="offcanvas-title fw-bold text-dark" id="faqOffcanvasLabel">
          <i class="fas fa-headset text-danger me-2"></i> Câu Hỏi Thường Gặp
        </h5>
        <button
          type="button"
          class="btn-close text-reset"
          data-bs-dismiss="offcanvas"
          aria-label="Close"
        ></button>
      </div>

      <div class="offcanvas-body p-4">
        <p class="text-secondary mb-4 small">
          Marcus Store luôn sẵn sàng hỗ trợ bạn. Dưới đây là các câu hỏi phổ biến nhất về dịch vụ
          của chúng tôi.
        </p>

        <!-- Accordion Bootstrap 5 -->
        <div class="accordion accordion-flush" id="faqAccordion">
          <div
            class="accordion-item border rounded mb-3 shadow-sm"
            v-for="item in faqs"
            :key="item.id"
          >
            <h2 class="accordion-header" :id="'heading' + item.id">
              <button
                class="accordion-button collapsed fw-semibold text-dark rounded"
                type="button"
                data-bs-toggle="collapse"
                :data-bs-target="'#collapse' + item.id"
                aria-expanded="false"
                :aria-controls="'collapse' + item.id"
              >
                {{ item.question }}
              </button>
            </h2>
            <div
              :id="'collapse' + item.id"
              class="accordion-collapse collapse"
              :aria-labelledby="'heading' + item.id"
              data-bs-parent="#faqAccordion"
            >
              <div class="accordion-body text-secondary small" style="line-height: 1.6">
                {{ item.answer }}
              </div>
            </div>
          </div>
        </div>

        <!-- Nút liên hệ thêm -->
        <div class="mt-5 text-center">
          <p class="small text-muted mb-2">Không tìm thấy câu trả lời?</p>
          <button
            class="btn btn-outline-danger btn-sm rounded-pill px-4"
            data-bs-dismiss="offcanvas"
          >
            Chat với Admin
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const faqs = ref([
  {
    id: 1,
    question: 'Chính sách trợ giá vận chuyển hoạt động như thế nào?',
    answer:
      'Thuật toán của chúng tôi tự động tính toán dựa trên khối lượng và khoảng cách qua hệ thống GHN. Marcus Store sẽ trợ giá lên tới 60.000 VNĐ cho các đơn hàng đạt điều kiện tối thiểu.',
  },
  {
    id: 2,
    question: 'Tôi có thể thanh toán qua những hình thức nào?',
    answer:
      'Hệ thống hỗ trợ thanh toán khi nhận hàng (COD), cổng thanh toán VNPAY an toàn và quét mã QR PayOS tự động đối soát.',
  },
  {
    id: 3,
    question: 'Flash Sale diễn ra vào khung giờ nào?',
    answer:
      'Các khung giờ Flash Sale được hệ thống mở tự động. Vui lòng theo dõi biểu tượng đếm ngược trên trang chủ để không bỏ lỡ các deal giảm giá sâu.',
  },
  {
    id: 4,
    question: 'Làm sao để tôi theo dõi đơn hàng?',
    answer:
      'Ngay khi hàng được đóng gói, hệ thống sẽ cấp mã Tracking Code của Giao Hàng Nhanh. Bạn có thể tra cứu trực tiếp tình trạng kiện hàng theo thời gian thực.',
  },
])
</script>

<style scoped>
/* Nút FAQ dính mép dọc bên phải */
.btn-floating-faq {
  position: fixed;
  top: 50%;
  right: -5px;
  transform: translateY(-50%);
  background-color: #212529;
  color: #ffffff;
  border: none;
  border-radius: 8px 0 0 8px;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1040;
  transition: all 0.3s ease;
  width: 50px;
}

.btn-floating-faq:hover {
  background-color: #d70018;
  right: 0;
  padding-right: 15px;
}

.btn-floating-faq span {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-top: 8px;
  text-transform: uppercase;
}

/* Custom Accordion */
.accordion-item {
  overflow: hidden;
  border-color: #f8fafc !important;
}

.accordion-button {
  background-color: #ffffff;
  box-shadow: none !important;
}

.accordion-button:not(.collapsed) {
  background-color: #fde8e9;
  color: #d70018 !important;
}

.accordion-button::after {
  filter: grayscale(1);
  transition: all 0.2s ease;
}

.accordion-button:not(.collapsed)::after {
  filter: invert(15%) sepia(95%) saturate(6011%) hue-rotate(352deg) brightness(97%) contrast(106%);
}

.offcanvas {
  border-left: none;
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.1);
}
</style>
