<template>
  <div class="about-page">
    <!-- ============ HERO ============ -->
    <section class="hero">
      <div class="hero-glow"></div>
      <div class="container hero-inner py-5">
        <div class="row align-items-center gy-5">
          <!-- Copy -->
          <div class="col-lg-6">
            <span class="badge-eyebrow"> Định hình tương lai bán lẻ công nghệ </span>
            <h1 class="hero-title">Về <span class="text-accent-red">Marcus</span> Store</h1>
            <p class="hero-lead">
              Chuyên điện thoại, iPad và phụ kiện chính hãng — được vận hành bởi một nền tảng công
              nghệ hiện đại phía sau mỗi đơn hàng, để trải nghiệm mua sắm luôn nhanh, minh bạch và
              an toàn.
            </p>
            <div class="hero-cta d-flex gap-3 flex-wrap">
              <router-link to="/products" class="btn btn-primary-red">
                Khám phá gian hàng <i class="fas fa-arrow-right ms-2"></i>
              </router-link>
              <a href="#story" class="btn btn-ghost-light"> Câu chuyện của chúng tôi </a>
            </div>

            <!-- Brand strip: thương hiệu đang phân phối -->
            <div class="brand-marquee mt-5">
              <div class="brand-track">
                <span v-for="n in 2" :key="n" class="brand-set">
                  <span class="brand-chip">Apple</span>
                  <span class="brand-chip">Samsung</span>
                  <span class="brand-chip">Xiaomi</span>
                  <span class="brand-chip">OPPO</span>
                  <span class="brand-chip">realme</span>
                  <span class="brand-chip">Anker</span>
                </span>
              </div>
            </div>
          </div>

          <!-- Signature visual: phone lockscreen với thông báo hệ thống -->
          <div class="col-lg-6">
            <div class="phone-stage">
              <div class="tablet-echo">
                <div class="tablet-cam"></div>
              </div>
              <div class="phone-shell">
                <div class="phone-notch"></div>
                <div class="phone-screen">
                  <p class="phone-time">21:04</p>
                  <p class="phone-date">Thứ Bảy, 05 tháng 07</p>

                  <div class="notif-card notif-1">
                    <i class="fas fa-bolt"></i>
                    <div>
                      <strong>Flash Sale vừa mở</strong>
                      <span>Giảm đến 3.000.000đ · còn 09:41</span>
                    </div>
                  </div>
                  <div class="notif-card notif-2">
                    <i class="fas fa-truck-fast"></i>
                    <div>
                      <strong>Đơn hàng #MS2214 đã giao</strong>
                      <span>Trợ giá vận chuyển -60.000đ</span>
                    </div>
                  </div>
                  <div class="notif-card notif-3">
                    <i class="fas fa-shield-alt"></i>
                    <div>
                      <strong>Thanh toán thành công</strong>
                      <span>Qua VNPAY · đã đối soát</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="phone-reflection"></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ HỆ SINH THÁI ============ -->
    <section class="ecosystem py-5">
      <div class="container py-4">
        <div class="section-head text-center mb-5">
          <span class="kicker">04 GIÁ TRỊ CỐT LÕI</span>
          <h2 class="section-title">Chúng tôi mang đến những gì?</h2>
          <p class="section-sub">
            Marcus Store chuyên cung cấp các dòng thiết bị công nghệ chính hãng — từ điện thoại
            thông minh, laptop cho đến phụ kiện cao cấp. Vượt lên trên một gian hàng vật lý, đây là
            nền tảng mua sắm được vận hành bằng công nghệ.
          </p>
        </div>

        <div class="row g-4">
          <div class="col-md-6 col-lg-3">
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-mobile-alt"></i></div>
              <h5>Thiết bị đỉnh cao</h5>
              <p>
                Smartphone, iPad và phụ kiện chính hãng, nguồn gốc rõ ràng, chế độ bảo hành 1 đổi 1
                minh bạch.
              </p>
            </div>
          </div>
          <div class="col-md-6 col-lg-3">
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-bolt"></i></div>
              <h5>Săn deal Flash Sale</h5>
              <p>
                Hệ thống Flash Sale tự động hóa theo thời gian thực, đảm bảo công bằng và mức giá
                hủy diệt mỗi ngày.
              </p>
            </div>
          </div>
          <div class="col-md-6 col-lg-3">
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-truck-fast"></i></div>
              <h5>Vận chuyển thông minh</h5>
              <p>
                Thuật toán phân tầng, tính phí tự động và trợ giá linh hoạt đến 60.000đ cho đơn hàng
                đạt ngưỡng.
              </p>
            </div>
          </div>
          <div class="col-md-6 col-lg-3">
            <div class="feature-card">
              <div class="feature-icon"><i class="fas fa-shield-alt"></i></div>
              <h5>Thanh toán an toàn</h5>
              <p>
                Tích hợp sâu VNPAY và mã QR PayOS, tự động đối soát dòng tiền và bảo mật dữ liệu cá
                nhân.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ DANH MỤC NỔI BẬT ============ -->
    <section class="categories py-5">
      <div class="container py-4">
        <div class="section-head text-center mb-5">
          <span class="kicker">GIAN HÀNG CỦA CHÚNG TÔI</span>
          <h2 class="section-title">Một điểm đến, đủ hệ sinh thái thiết bị</h2>
          <p class="section-sub">
            Từ chiếc điện thoại chính, chiếc iPad thứ hai, đến những phụ kiện nhỏ nhất — tất cả đều
            được chọn lọc và bảo hành theo cùng một tiêu chuẩn.
          </p>
        </div>

        <div class="category-grid">
          <div v-for="cat in categories" :key="cat.name" class="category-item">
            <div class="category-icon"><i :class="cat.icon"></i></div>
            <span class="category-name">{{ cat.name }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ CÂU CHUYỆN / SỨ MỆNH ============ -->
    <section id="story" class="story py-5">
      <div class="container py-4">
        <div class="row align-items-center g-5">
          <div class="col-lg-5">
            <span class="kicker">CÂU CHUYỆN CỦA CHÚNG TÔI</span>
            <h2 class="section-title text-start">
              Bán lẻ công nghệ, <br />vận hành bằng công nghệ
            </h2>
            <p class="story-text">
              Marcus Store ra đời từ một câu hỏi đơn giản: vì sao mua một chiếc điện thoại lại phải
              phức tạp đến vậy? Chúng tôi xây dựng lại toàn bộ trải nghiệm — từ danh mục sản phẩm,
              giỏ hàng, đến vận chuyển và thanh toán — thành một hệ thống liền mạch, để mỗi đơn hàng
              đều nhanh, đúng và đáng tin cậy.
            </p>
            <ul class="story-list">
              <li>
                <i class="fas fa-check"></i> Dữ liệu sản phẩm minh bạch, cập nhật theo thời gian
                thực
              </li>
              <li><i class="fas fa-check"></i> Quy trình đặt hàng tối giản, ít bước, ít sai sót</li>
              <li><i class="fas fa-check"></i> Đội ngũ kỹ thuật đứng sau từng luồng nghiệp vụ</li>
            </ul>
          </div>
          <div class="col-lg-7">
            <div class="story-panel">
              <div class="story-stat">
                <span class="stat-num">4</span>
                <span class="stat-label">nhóm nghiệp vụ<br />được số hóa</span>
              </div>
              <div class="story-stat">
                <span class="stat-num">2</span>
                <span class="stat-label">cổng thanh toán<br />tích hợp trực tiếp</span>
              </div>
              <div class="story-stat">
                <span class="stat-num">1</span>
                <span class="stat-label">nền tảng duy nhất<br />cho toàn bộ trải nghiệm</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ ĐỘI NGŨ ============ -->
    <section class="team-section py-5">
      <div class="container py-4">
        <div class="text-center mb-5">
          <span class="kicker">MARCUS STORE TEAM</span>
          <h2 class="section-title">Những kỹ sư đứng sau hệ thống</h2>
          <p class="section-sub">
            Vận hành một nền tảng thương mại điện tử với hàng ngàn luồng dữ liệu đòi hỏi một đội ngũ
            kỹ thuật không chỉ am hiểu công nghệ mà còn thấu hiểu nghiệp vụ kinh doanh.
          </p>
        </div>

        <div class="row justify-content-center g-4">
          <div class="col-6 col-md-4 col-lg-2" v-for="member in teamMembers" :key="member.id">
            <div class="team-card">
              <div class="card-body p-4">
                <div class="avatar-wrapper mb-3 mx-auto">
                  <img :src="member.avatar" :alt="member.name" class="rounded-circle img-fluid" />
                </div>
                <h5 class="fw-bold mb-1">{{ member.name }}</h5>
                <p class="team-role mb-2">{{ member.role }}</p>
                <div class="border-top pt-2 mt-2">
                  <p class="team-task mb-0">{{ member.task }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ CTA ============ -->
    <section class="cta-section py-5 text-center">
      <div class="container py-5">
        <h3 class="cta-title">Sẵn sàng trải nghiệm mua sắm thế hệ mới?</h3>
        <p class="cta-sub mb-4">
          Hãy để hệ thống của chúng tôi phục vụ bạn bằng những sản phẩm công nghệ tuyệt vời nhất.
        </p>
        <router-link to="/products" class="btn btn-primary-red btn-lg px-5 py-3">
          Khám phá gian hàng <i class="fas fa-arrow-right ms-2"></i>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// Danh mục sản phẩm chính đang kinh doanh
const categories = ref([
  { name: 'Điện thoại', icon: 'fas fa-mobile-alt' },
  { name: 'iPad & Máy tính bảng', icon: 'fas fa-tablet-alt' },
  { name: 'Âm thanh', icon: 'fas fa-headphones' },
  { name: 'Đồng hồ thông minh', icon: 'fas fa-clock' },
  { name: 'Sạc & Pin', icon: 'fas fa-bolt' },
  { name: 'Ốp lưng & Bảo vệ', icon: 'fas fa-shield-alt' },
])

// Dữ liệu đội ngũ — giữ nguyên theo yêu cầu
const teamMembers = ref([
  {
    id: 1,
    name: 'Marcus',
    role: 'Project Manager / Tech Lead',
    task: 'Kiến trúc hệ thống, điều phối dự án & phát triển các luồng nghiệp vụ cốt lõi',
    avatar: 'https://ui-avatars.com/api/?name=Marcus&background=d70018&color=fff&size=150',
  },
  {
    id: 2,
    name: 'Đức',
    role: 'Fullstack Developer',
    task: 'Phát triển module Sản phẩm, danh mục & quản lý thuộc tính',
    avatar: 'https://ui-avatars.com/api/?name=Đức&background=212529&color=fff&size=150',
  },
  {
    id: 3,
    name: 'Ngọc',
    role: 'Fullstack Developer',
    task: 'Thiết lập hệ thống Xác thực & Phân quyền bảo mật',
    avatar: 'https://ui-avatars.com/api/?name=Ngọc&background=d70018&color=fff&size=150',
  },
  {
    id: 4,
    name: 'Đạt',
    role: 'Fullstack Developer',
    task: 'Xử lý luồng Giỏ hàng, Đơn hàng, Voucher & Chiến dịch Flash Sale',
    avatar: 'https://ui-avatars.com/api/?name=Đạt&background=212529&color=fff&size=150',
  },
  {
    id: 5,
    name: 'Huy',
    role: 'Fullstack Developer',
    task: 'Triển khai Audit, hệ thống Tracking Log & module Nội dung/Tin tức',
    avatar: 'https://ui-avatars.com/api/?name=Huy&background=d70018&color=fff&size=150',
  },
])
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&display=swap');

.about-page {
  --clr-bg: #0b0b10;
  --clr-surface: #ffffff;
  --clr-surface-alt: #f6f5f3;
  --clr-red: #e1121c;
  --clr-red-dark: #b80e17;
  --clr-amber: #ffb627;
  --clr-ink: #14151a;
  --clr-muted: #6b7280;

  font-family: 'Be Vietnam Pro', system-ui, sans-serif;
  background-color: var(--clr-surface);
  color: var(--clr-ink);
}

.about-page h1,
.about-page h2,
.about-page h3,
.about-page .stat-num,
.about-page .kicker,
.about-page .badge-eyebrow {
  font-family: 'Space Grotesk', 'Be Vietnam Pro', sans-serif;
}

.text-accent-red {
  color: var(--clr-red);
}

/* ============ HERO ============ */
.hero {
  position: relative;
  background: var(--clr-bg);
  color: #fff;
  overflow: hidden;
  padding-top: 3rem;
}

.hero-glow {
  position: absolute;
  top: -30%;
  right: -15%;
  width: 55%;
  height: 160%;
  background: radial-gradient(circle at center, rgba(225, 18, 28, 0.45) 0%, transparent 70%);
  filter: blur(10px);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
}

.badge-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  margin-bottom: 1.5rem;
}

.badge-eyebrow .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--clr-amber);
  box-shadow: 0 0 0 3px rgba(255, 182, 39, 0.25);
}

.hero-title {
  font-size: clamp(2.4rem, 5vw, 3.6rem);
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 1.25rem;
}

.hero-lead {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.72);
  max-width: 32rem;
  margin-bottom: 2rem;
}

.btn-primary-red {
  background: var(--clr-red);
  border: none;
  color: #fff;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  border-radius: 999px;
  padding: 0.85rem 1.75rem;
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
}

.btn-primary-red:hover {
  background: var(--clr-red-dark);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(225, 18, 28, 0.35);
}

.btn-ghost-light {
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  background: transparent;
  border-radius: 999px;
  padding: 0.85rem 1.75rem;
  font-weight: 600;
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
}

.btn-ghost-light:hover {
  border-color: #fff;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

/* Brand marquee */
.brand-marquee {
  overflow: hidden;
  mask-image: linear-gradient(90deg, transparent, #000 10%, #000 90%, transparent);
}

.brand-track {
  display: flex;
  width: max-content;
  animation: scroll-left 18s linear infinite;
}

.brand-set {
  display: flex;
  gap: 2.5rem;
  padding-right: 2.5rem;
}

.brand-chip {
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.45);
  white-space: nowrap;
}

@keyframes scroll-left {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

/* Signature: phone lockscreen */
.phone-stage {
  display: flex;
  justify-content: center;
  position: relative;
}

/* iPad mờ phía sau, gợi ý dải sản phẩm rộng hơn (điện thoại + tablet) */
.tablet-echo {
  position: absolute;
  top: 6%;
  right: 6%;
  width: 300px;
  height: 400px;
  background: linear-gradient(160deg, #24252e, #0b0b10);
  border: 5px solid #2a2b36;
  border-radius: 28px;
  opacity: 0.55;
  transform: rotate(6deg);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.4);
}

.tablet-cam {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #454652;
}

.phone-shell {
  width: 260px;
  height: 530px;
  background: linear-gradient(160deg, #1c1d24, #0b0b10);
  border-radius: 42px;
  border: 6px solid #232430;
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.55),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04);
  position: relative;
  z-index: 1;
  padding: 22px 16px;
}

.phone-notch {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 18px;
  background: #0b0b10;
  border-radius: 12px;
}

.phone-screen {
  margin-top: 46px;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: calc(100% - 46px);
}

.phone-time {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 2.6rem;
  font-weight: 600;
  color: #fff;
  margin-bottom: 0;
  line-height: 1;
}

.phone-date {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 1.75rem;
}

.notif-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  opacity: 0;
  animation: notif-in 0.6s ease forwards;
}

.notif-1 {
  animation-delay: 0.2s;
}
.notif-2 {
  animation-delay: 0.5s;
}
.notif-3 {
  animation-delay: 0.8s;
}

.notif-card i {
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 9px;
  background: var(--clr-red);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
}

.notif-card div {
  display: flex;
  flex-direction: column;
  text-align: left;
  line-height: 1.3;
}

.notif-card strong {
  font-size: 0.78rem;
  color: #fff;
}

.notif-card span {
  font-size: 0.68rem;
  color: rgba(255, 255, 255, 0.55);
}

@keyframes notif-in {
  from {
    opacity: 0;
    transform: translateY(-12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.phone-reflection {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 40px;
  background: radial-gradient(ellipse at center, rgba(225, 18, 28, 0.35), transparent 70%);
  filter: blur(8px);
}

@media (prefers-reduced-motion: reduce) {
  .brand-track,
  .notif-card {
    animation: none !important;
    opacity: 1;
  }
}

/* ============ SECTION HEAD (shared) ============ */
.kicker {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--clr-red);
  margin-bottom: 0.75rem;
}

.section-title {
  font-weight: 700;
  color: var(--clr-ink);
  margin-bottom: 1rem;
}

.section-sub {
  color: var(--clr-muted);
  max-width: 42rem;
  margin: 0 auto;
}

.text-start.section-title {
  margin: 0 0 1rem;
}

/* ============ ECOSYSTEM ============ */
.ecosystem {
  background: var(--clr-surface);
}

.feature-card {
  height: 100%;
  background: var(--clr-surface-alt);
  border-radius: 20px;
  padding: 2rem 1.5rem;
  text-align: center;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-6px);
  background: #fff;
  box-shadow: 0 16px 32px rgba(20, 21, 26, 0.08);
}

.feature-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 1.1rem;
  border-radius: 16px;
  background: #fff;
  color: var(--clr-red);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;
}

.feature-card:hover .feature-icon {
  background: var(--clr-red);
  color: #fff;
  transform: scale(1.08) rotate(-4deg);
}

.feature-card h5 {
  font-weight: 700;
  margin-bottom: 0.6rem;
}

.feature-card p {
  color: var(--clr-muted);
  font-size: 0.9rem;
  margin: 0;
}

/* ============ CATEGORIES ============ */
.categories {
  background: var(--clr-surface-alt);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 1.25rem;
}

.category-item {
  background: #fff;
  border-radius: 18px;
  padding: 1.75rem 1rem;
  text-align: center;
  transition: all 0.3s ease;
  border: 1px solid rgba(20, 21, 26, 0.06);
}

.category-item:hover {
  transform: translateY(-6px);
  border-color: transparent;
  box-shadow: 0 16px 30px rgba(20, 21, 26, 0.1);
}

.category-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 0.85rem;
  border-radius: 14px;
  background: var(--clr-surface-alt);
  color: var(--clr-red);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.3s ease;
}

.category-item:hover .category-icon {
  background: var(--clr-red);
  color: #fff;
  transform: scale(1.08);
}

.category-name {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--clr-ink);
  line-height: 1.3;
}

@media (max-width: 992px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 480px) {
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ============ STORY ============ */
.story {
  background: var(--clr-surface-alt);
}

.story-text {
  color: var(--clr-muted);
  margin-bottom: 1.5rem;
}

.story-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.story-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 0.85rem;
  color: var(--clr-ink);
  font-weight: 500;
  font-size: 0.95rem;
}

.story-list i {
  color: var(--clr-red);
  margin-top: 4px;
}

.story-panel {
  background: var(--clr-bg);
  border-radius: 24px;
  padding: 3rem 2rem;
  display: flex;
  flex-wrap: wrap;
  gap: 2rem;
  justify-content: space-around;
}

.story-stat {
  text-align: center;
  color: #fff;
  min-width: 140px;
}

.stat-num {
  display: block;
  font-size: 3rem;
  font-weight: 700;
  color: var(--clr-amber);
  line-height: 1;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.4;
}

/* ============ TEAM (giữ dữ liệu, chỉ nâng cấp giao diện) ============ */
.team-section {
  background: var(--clr-surface);
}

.team-card {
  background: var(--clr-surface-alt);
  border-radius: 18px;
  height: 100%;
  text-align: center;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.team-card:hover {
  transform: translateY(-8px);
  background: #fff;
  box-shadow: 0 14px 32px rgba(20, 21, 26, 0.08);
}

.avatar-wrapper {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, var(--clr-red) 0%, var(--clr-bg) 100%);
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 3px solid var(--clr-surface-alt);
}

.team-role {
  color: var(--clr-red);
  font-size: 0.8rem;
  font-weight: 700;
  margin-bottom: 0;
}

.team-task {
  color: var(--clr-muted);
  font-size: 0.75rem;
  line-height: 1.5;
}

/* ============ CTA ============ */
.cta-section {
  background: var(--clr-bg);
  color: #fff;
}

.cta-title {
  font-weight: 700;
  font-size: 1.9rem;
  margin-bottom: 0.75rem;
}

.cta-sub {
  color: rgba(255, 255, 255, 0.65);
  font-size: 1.05rem;
}

/* ============ RESPONSIVE ============ */
@media (max-width: 992px) {
  .phone-shell {
    width: 220px;
    height: 460px;
  }
  .tablet-echo {
    width: 250px;
    height: 340px;
  }
}

@media (max-width: 576px) {
  .tablet-echo {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero {
    padding-top: 2rem;
  }
  .hero-title {
    font-size: 2.1rem;
  }
  .hero-lead {
    max-width: 100%;
  }
  .phone-stage {
    margin-top: 2rem;
  }
  .story-panel {
    padding: 2rem 1.25rem;
  }
}
</style>
