<template>
  <section class="usp-slider-section">
    <div class="container-xxl">
      <div class="usp-header">
        <span class="usp-eyebrow">Tại sao chọn Marcus Store</span>
        <h3 class="usp-title">Trải nghiệm dịch vụ đỉnh cao</h3>
      </div>

      <div class="usp-track-wrapper">
        <button type="button" class="usp-edge-btn usp-edge-left" @click="scrollPage(-1)">
          <i class="fas fa-chevron-left"></i>
        </button>

        <div class="usp-track" ref="trackRef">
          <article v-for="(item, index) in uspItems" :key="index" class="usp-card">
            <div class="usp-icon"><i :class="item.icon"></i></div>
            <span class="usp-card-eyebrow">{{ item.eyebrow }}</span>
            <h4 class="usp-card-title">{{ item.title }}</h4>
            <p class="usp-card-desc">{{ item.desc }}</p>
            <div class="usp-card-footer">
              <span class="usp-more">Tìm hiểu thêm <i class="fas fa-arrow-right"></i></span>
            </div>
          </article>
        </div>

        <button type="button" class="usp-edge-btn usp-edge-right" @click="scrollPage(1)">
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const uspItems = [
  {
    icon: 'fas fa-recycle',
    eyebrow: 'Thu cũ đổi mới',
    title: 'Trợ giá 3.000.000đ',
    desc: 'Định giá máy cũ nhanh chóng trong 30 phút tại quầy. Lên đời máy mới dễ dàng hơn bao giờ hết.',
  },
  {
    icon: 'fas fa-credit-card',
    eyebrow: 'Trả góp 0%',
    title: 'Duyệt trong 15 phút',
    desc: 'Liên kết trực tiếp đối tác tài chính, không cần chứng minh thu nhập phức tạp, duyệt ngay tại chỗ.',
  },
  {
    icon: 'fas fa-shipping-fast',
    eyebrow: 'Ship hỏa tốc',
    title: 'Giao sau 2 giờ',
    desc: 'Đặc quyền cho đơn nội thành. Nhận hàng thần tốc, kiểm tra tại chỗ, hoàn tiền nếu không ưng ý.',
  },
  {
    icon: 'fas fa-shield-alt',
    eyebrow: 'Bảo hành vàng',
    title: '1 đổi 1 trong 30 ngày',
    desc: 'Lỗi là đổi mới, không sửa chữa. An tâm trải nghiệm sản phẩm chính hãng với bảo hành 24 tháng.',
  },
  {
    icon: 'fas fa-tools',
    eyebrow: 'Hỗ trợ kỹ thuật',
    title: 'Cài đặt miễn phí',
    desc: 'Hỗ trợ sao chép dữ liệu, cài đặt ứng dụng bản quyền trọn đời cho mọi khách hàng.',
  },
  {
    icon: 'fas fa-hand-holding-usd',
    eyebrow: 'Giá tốt nhất',
    title: 'Hoàn tiền nếu rẻ hơn',
    desc: 'Marcus Store cam kết hoàn tiền chênh lệch nếu bạn tìm thấy sản phẩm cùng tình trạng có giá tốt hơn.',
  },
]

const trackRef = ref(null)

const scrollPage = (dir) => {
  const el = trackRef.value
  if (!el) return
  // Tính độ rộng của 1 card + khoảng cách (gap là 24px)
  const cardWidth = el.querySelector('.usp-card').offsetWidth + 24
  el.scrollBy({ left: cardWidth * dir, behavior: 'smooth' })
}
</script>

<style scoped>
.usp-slider-section {
  padding: 4rem 0;
  background: #fafafa;
}
.usp-header {
  margin-bottom: 2rem;
  text-align: center;
}
.usp-eyebrow {
  display: block;
  color: #e1121c;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 0.75rem;
  margin-bottom: 8px;
}
.usp-title {
  font-size: 2rem;
  font-weight: 800;
  color: #111;
}

.usp-track-wrapper {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
}

/* Flexbox thay vì Grid để slider mượt mà */
.usp-track {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding: 1rem 0;
  scrollbar-width: none;
}
.usp-track::-webkit-scrollbar {
  display: none;
}

.usp-card {
  /* Tỉ lệ 33.333% đảm bảo hiện đúng 3 thẻ trên desktop */
  flex: 0 0 calc(33.333% - 16px);
  padding: 2.5rem;
  border-radius: 28px;
  background: linear-gradient(150deg, #1f1f1f 0%, #000 100%);
  color: #fff;
  scroll-snap-align: start;
  border: 1px solid #333;
  transition: 0.4s;
  display: flex;
  flex-direction: column;
}

.usp-card:hover {
  transform: translateY(-10px);
  border-color: #e1121c;
  box-shadow: 0 20px 30px rgba(225, 18, 28, 0.2);
}

/* Các thành phần nội dung giữ nguyên */
.usp-icon {
  width: 50px;
  height: 50px;
  background: #e1121c;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 1.2rem;
  margin-bottom: 20px;
}
.usp-card-eyebrow {
  color: #e1121c;
  font-weight: 800;
  text-transform: uppercase;
  font-size: 0.7rem;
  letter-spacing: 1.5px;
}
.usp-card-title {
  font-size: 1.4rem;
  font-weight: 800;
  margin: 0.8rem 0;
}
.usp-card-desc {
  font-size: 0.9rem;
  color: #a3a3a3;
  line-height: 1.6;
  flex-grow: 1;
  margin-bottom: 20px;
}
.usp-more {
  font-weight: 700;
  font-size: 0.85rem;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

/* Nút điều hướng */
.usp-edge-btn {
  position: absolute;
  top: 40%;
  z-index: 10;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: 0.3s;
}
.usp-edge-btn:hover {
  background: #e1121c;
  color: #fff;
  border-color: #e1121c;
}
.usp-edge-left {
  left: -60px;
}
.usp-edge-right {
  right: -60px;
}

/* Mobile: Hiện 1 thẻ */
@media (max-width: 768px) {
  .usp-edge-btn {
    display: none;
  }
  .usp-card {
    flex: 0 0 90%;
  }
}
/* Tablet: Hiện 2 thẻ */
@media (min-width: 769px) and (max-width: 1024px) {
  .usp-card {
    flex: 0 0 calc(50% - 12px);
  }
}
</style>
