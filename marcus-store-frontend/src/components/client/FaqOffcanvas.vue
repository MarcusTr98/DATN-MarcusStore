<template>
  <div>
    <!-- Nút Floating FAQ -->
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
      class="offcanvas offcanvas-end custom-offcanvas-zindex"
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

      <div class="offcanvas-body p-4 d-flex flex-column">
        <p class="text-secondary mb-3 small">
          Marcus Store luôn sẵn sàng hỗ trợ bạn. Tìm nhanh câu trả lời bên dưới hoặc liên hệ trực
          tiếp với đội ngũ CSKH.
        </p>

        <!-- Ô tìm kiếm -->
        <div class="faq-search mb-3">
          <i class="fas fa-search"></i>
          <input
            v-model="searchQuery"
            type="text"
            class="form-control"
            placeholder="Tìm câu hỏi... (VD: bảo hành, IMEI, trả góp)"
          />
          <button
            v-if="searchQuery"
            class="btn-clear-search"
            type="button"
            @click="searchQuery = ''"
            aria-label="Xóa tìm kiếm"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>

        <!-- Bộ lọc theo danh mục -->
        <div class="faq-categories mb-4">
          <button
            v-for="cat in categories"
            :key="cat.key"
            type="button"
            class="faq-cat-btn"
            :class="{ active: activeCategory === cat.key }"
            @click="activeCategory = cat.key"
          >
            <i :class="cat.icon"></i>
            {{ cat.label }}
          </button>
        </div>

        <!-- Accordion Bootstrap 5 -->
        <div
          v-if="filteredFaqs.length"
          class="accordion accordion-flush flex-grow-1"
          id="faqAccordion"
        >
          <div
            class="accordion-item border rounded mb-3 shadow-sm"
            v-for="item in filteredFaqs"
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
                <span class="faq-cat-badge me-2">
                  <i :class="categoryIcon(item.category)"></i>
                </span>
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

        <!-- Trạng thái không có kết quả -->
        <div v-else class="faq-empty text-center py-5">
          <i class="fas fa-folder-open fs-2 text-muted mb-3 d-block"></i>
          <p class="text-muted small mb-0">
            Không tìm thấy câu hỏi phù hợp với "<strong>{{ searchQuery }}</strong
            >"
          </p>
        </div>

        <!-- Nút liên hệ thêm (Đã nâng cấp) -->
        <div class="mt-4 pt-3 border-top text-center">
          <p class="small text-muted mb-3">Vẫn chưa giải quyết được vấn đề?</p>
          <div class="d-flex gap-2 justify-content-center">
            <!-- Nút Hotline giữ nguyên vì tính cấp bách -->
            <a
              href="tel:19001234"
              class="btn btn-outline-dark btn-sm rounded-pill px-3 border-2 fw-semibold"
            >
              <i class="fas fa-phone-alt me-1"></i> Gọi Hotline
            </a>

            <!-- Đổi nút Zalo thành nút gọi Chat Live hệ thống -->
            <button
              type="button"
              class="btn btn-danger btn-sm rounded-pill px-3 fw-semibold shadow-sm"
              @click="triggerLiveChat"
            >
              <i class="fas fa-comments me-1"></i> Chat với CSKH
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const searchQuery = ref('')
const activeCategory = ref('all')

const categories = [
  { key: 'all', label: 'Tất cả', icon: 'fas fa-th-large' },
  { key: 'shipping', label: 'Vận chuyển', icon: 'fas fa-truck' },
  { key: 'payment', label: 'Thanh toán', icon: 'fas fa-credit-card' },
  { key: 'warranty', label: 'Bảo hành', icon: 'fas fa-shield-alt' },
  { key: 'device', label: 'Sản phẩm', icon: 'fas fa-mobile-alt' },
  { key: 'promotion', label: 'Khuyến mãi', icon: 'fas fa-tags' },
]

const categoryIcon = (key) => categories.find((c) => c.key === key)?.icon || 'fas fa-question'

const faqs = ref([
  {
    id: 1,
    category: 'shipping',
    question: 'Chính sách trợ giá vận chuyển hoạt động như thế nào?',
    answer:
      'Thuật toán của chúng tôi tự động tính toán dựa trên khối lượng và khoảng cách qua hệ thống GHN. Marcus Store sẽ trợ giá lên tới 60.000 VNĐ cho các đơn hàng đạt điều kiện tối thiểu.',
  },
  {
    id: 2,
    category: 'shipping',
    question: 'Làm sao để tôi theo dõi đơn hàng?',
    answer:
      'Ngay khi hàng được đóng gói, hệ thống sẽ cấp mã Tracking Code của Giao Hàng Nhanh. Bạn có thể tra cứu trực tiếp tình trạng kiện hàng theo thời gian thực trong mục "Đơn hàng của tôi".',
  },
  {
    id: 3,
    category: 'payment',
    question: 'Tôi có thể thanh toán qua những hình thức nào?',
    answer:
      'Hệ thống hỗ trợ thanh toán khi nhận hàng (COD), cổng thanh toán VNPAY an toàn và chuyển khoản ngân hàng với đối soát tự động.',
  },
  {
    id: 4,
    category: 'payment',
    question: 'Marcus Store có hỗ trợ trả góp không?',
    answer:
      'Có. Với các sản phẩm từ 3 triệu VNĐ trở lên, bạn có thể chọn trả góp 0% lãi suất qua thẻ tín dụng hoặc công ty tài chính liên kết ngay tại bước thanh toán.',
  },
  {
    id: 5,
    category: 'warranty',
    question: 'Chính sách bảo hành áp dụng như thế nào?',
    answer:
      'Tất cả máy chính hãng được bảo hành 12 tháng tại hãng, cùng 1 đổi 1 trong 30 ngày đầu nếu có lỗi phần cứng từ nhà sản xuất.',
  },
  {
    id: 6,
    category: 'warranty',
    question: 'Tôi có thể đổi trả sản phẩm trong bao lâu?',
    answer:
      'Bạn có 7 ngày kể từ khi nhận hàng để đổi trả nếu sản phẩm còn nguyên hộp, đầy đủ phụ kiện và chưa kích hoạt bảo hành điện tử.',
  },
  {
    id: 7,
    category: 'device',
    question: 'Làm sao để kiểm tra IMEI trước khi nhận máy?',
    answer:
      'Mã IMEI được in trên vỏ hộp và hiển thị trong đơn hàng của bạn. Bạn có thể đối chiếu với máy thực tế và tra cứu trên trang chủ Bộ TT&TT để xác minh nguồn gốc chính hãng.',
  },
  {
    id: 8,
    category: 'device',
    question: 'Marcus Store có chương trình thu cũ đổi mới không?',
    answer:
      'Có. Bạn có thể mang máy cũ đến để được định giá và trừ thẳng vào hóa đơn mua máy mới, áp dụng cho hầu hết các dòng điện thoại phổ biến.',
  },
  {
    id: 9,
    category: 'promotion',
    question: 'Flash Sale diễn ra vào khung giờ nào?',
    answer:
      'Các khung giờ Flash Sale được hệ thống mở tự động. Vui lòng theo dõi biểu tượng đếm ngược trên trang chủ để không bỏ lỡ các deal giảm giá sâu.',
  },
])

const filteredFaqs = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return faqs.value.filter((item) => {
    const matchesCategory = activeCategory.value === 'all' || item.category === activeCategory.value
    const matchesQuery =
      !query ||
      item.question.toLowerCase().includes(query) ||
      item.answer.toLowerCase().includes(query)
    return matchesCategory && matchesQuery
  })
})

// Hàm Mới: Xử lý kích hoạt Chat Live
const triggerLiveChat = () => {
  // 1. Tự động đóng Offcanvas FAQ
  const closeBtn = document.querySelector('#faqOffcanvas .btn-close')
  if (closeBtn) closeBtn.click()

  // 2. Kích hoạt nút Chat Live của hệ thống (delay nhẹ để chờ Offcanvas đóng xong)
  setTimeout(() => {
    const chatBtn = document.querySelector('.chat-trigger-btn')
    if (chatBtn) {
      chatBtn.click()
    } else {
      console.warn('Chưa tìm thấy Widget Chat. CSKH có thể đang Offline.')
    }
  }, 350)
}
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

/* Ép z-index của bản thân Offcanvas cao hơn mọi Widget khác (1050 của Chat) */
.custom-offcanvas-zindex {
  z-index: 1060 !important;
}

/* Ô tìm kiếm */
.faq-search {
  position: relative;
  display: flex;
  align-items: center;
}

.faq-search i.fa-search {
  position: absolute;
  left: 14px;
  color: #adb5bd;
  font-size: 13px;
}

.faq-search .form-control {
  padding-left: 36px;
  padding-right: 32px;
  border-radius: 10px;
  background-color: #f8fafc;
  border-color: #e9ecef;
  font-size: 0.875rem;
}

.faq-search .form-control:focus {
  border-color: #d70018;
  box-shadow: 0 0 0 0.2rem rgba(215, 0, 24, 0.1);
  background-color: #fff;
}

.btn-clear-search {
  position: absolute;
  right: 10px;
  border: none;
  background: transparent;
  color: #adb5bd;
  font-size: 12px;
  padding: 4px;
}

.btn-clear-search:hover {
  color: #d70018;
}

/* Bộ lọc danh mục */
.faq-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.faq-cat-btn {
  border: 1px solid #e9ecef;
  background-color: #ffffff;
  color: #495057;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.faq-cat-btn i {
  margin-right: 4px;
  font-size: 0.7rem;
}

.faq-cat-btn:hover {
  border-color: #d70018;
  color: #d70018;
}

.faq-cat-btn.active {
  background-color: #d70018;
  border-color: #d70018;
  color: #ffffff;
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

.faq-cat-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background-color: #fde8e9;
  color: #d70018;
  font-size: 10px;
  flex-shrink: 0;
}

.faq-empty {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.offcanvas {
  border-left: none;
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.1);
}
</style>

<!-- Tác động Global để lớp nền (backdrop) đen của Bootstrap cũng đè lên Chat Widget -->
<style>
.offcanvas-backdrop {
  z-index: 1055 !important;
}
</style>
