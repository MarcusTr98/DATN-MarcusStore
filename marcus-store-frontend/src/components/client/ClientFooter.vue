<template>
  <footer class="ms-footer">
    <!-- Support Badges -->
    <div class="support-bar">
      <div class="container">
        <div class="support-grid">
          <div class="support-item">
            <div class="support-icon"><i class="fas fa-shield-alt"></i></div>
            <div>
              <p class="support-title">Hàng chính hãng 100%</p>
              <p class="support-sub">Cam kết từ nhà sản xuất</p>
            </div>
          </div>
          <div class="support-item">
            <div class="support-icon"><i class="fas fa-undo-alt"></i></div>
            <div>
              <p class="support-title">Đổi trả trong 30 ngày</p>
              <p class="support-sub">Miễn phí, không cần lý do</p>
            </div>
          </div>
          <div class="support-item">
            <div class="support-icon"><i class="fas fa-truck"></i></div>
            <div>
              <p class="support-title">Giao hàng toàn quốc</p>
              <p class="support-sub">Nhanh chóng - An toàn</p>
            </div>
          </div>
          <div class="support-item">
            <div class="support-icon"><i class="fas fa-headset"></i></div>
            <div>
              <p class="support-title">Hỗ trợ 24/7</p>
              <p class="support-sub">Tổng đài: 1800 6975</p>
            </div>
          </div>
          <div class="support-item">
            <div class="support-icon"><i class="fas fa-tag"></i></div>
            <div>
              <p class="support-title">Giá tốt nhất thị trường</p>
              <p class="support-sub">Cam kết hoàn tiền chênh lệch</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Footer -->
    <div class="footer-main">
      <div class="container">
        <div class="footer-grid">
          <!-- Brand Column -->
          <div class="footer-col footer-brand-col">
            <div class="footer-logo">
              <div class="footer-logo-icon"><i class="fas fa-mobile-alt"></i></div>
              <div>
                <span class="footer-logo-brand">Marcus</span>
                <span class="footer-logo-store">STORE</span>
              </div>
            </div>
            <p class="footer-about">
              Hệ thống bán lẻ điện thoại, phụ kiện chính hãng hàng đầu Việt Nam. Cam kết giá tốt
              nhất, bảo hành uy tín, hậu mãi dài lâu.
            </p>
            <div class="contact-list">
              <div class="contact-item">
                <i class="fas fa-map-marker-alt"></i>
                <span>{{ sysSettings.ADDRESS || 'Đang cập nhật địa chỉ' }}</span>
              </div>
              <div class="contact-item">
                <i class="fas fa-phone-alt"></i>
                <a :href="'tel:' + sysSettings.HOTLINE">(+84) {{ sysSettings.HOTLINE }}</a>
              </div>
              <div class="contact-item">
                <i class="fas fa-envelope"></i>
                <a :href="'mailto:' + sysSettings.EMAIL">{{
                  sysSettings.EMAIL || 'support@marcus.com'
                }}</a>
              </div>
              <div class="contact-item">
                <i class="fas fa-clock"></i>
                <span>{{ sysSettings.WORKING_HOURS || '08:00 – 21:00' }}</span>
              </div>
            </div>
            <div class="social-row">
              <a
                class="social-btn facebook"
                :href="sysSettings.FACEBOOK_URL || '#'"
                target="_blank"
                aria-label="Facebook"
              >
                <i class="fab fa-facebook-f"></i>
              </a>
              <a
                class="social-btn youtube"
                :href="sysSettings.YOUTUBE_URL || '#'"
                target="_blank"
                aria-label="YouTube"
              >
                <i class="fab fa-youtube"></i>
              </a>
              <a
                class="social-btn tiktok"
                :href="sysSettings.TIKTOK_URL || '#'"
                target="_blank"
                aria-label="TikTok"
              >
                <i class="fab fa-tiktok"></i>
              </a>
              <a
                class="social-btn instagram"
                :href="sysSettings.INSTAGRAM_URL || '#'"
                target="_blank"
                aria-label="Instagram"
              >
                <i class="fab fa-instagram"></i>
              </a>
            </div>
          </div>

          <!-- Danh mục sản phẩm -->
          <div class="footer-col">
            <h5 class="footer-col-title">Danh mục sản phẩm</h5>
            <ul class="footer-links">
              <li><router-link to="/category/dien-thoai">Điện thoại iPhone</router-link></li>
              <li>
                <router-link to="/category/dien-thoai-samsung">Điện thoại Samsung</router-link>
              </li>
              <li><router-link to="/category/dien-thoai-xiaomi">Điện thoại Xiaomi</router-link></li>
              <li><router-link to="/category/may-tinh-bang">Máy tính bảng</router-link></li>
              <li>
                <router-link to="/category/dong-ho-thong-minh">Đồng hồ thông minh</router-link>
              </li>
              <li><router-link to="/category/am-thanh">Tai nghe & Loa</router-link></li>
              <li><router-link to="/category/sac-pin">Sạc & Pin dự phòng</router-link></li>
              <li><router-link to="/category/op-lung">Ốp lưng & Dán màn hình</router-link></li>
              <li><router-link to="/category/phu-kien">Phụ kiện chính hãng</router-link></li>
            </ul>
          </div>

          <!-- Chính sách -->
          <div class="footer-col">
            <h5 class="footer-col-title">Chính sách & Hỗ trợ</h5>
            <ul class="footer-links">
              <li><router-link to="/bao-hanh">Chính sách bảo hành</router-link></li>
              <li><router-link to="/doi-tra">Chính sách đổi trả</router-link></li>
              <li><router-link to="/giao-hang">Chính sách giao hàng</router-link></li>
              <li><router-link to="/thanh-toan">Hướng dẫn thanh toán</router-link></li>
              <li><router-link to="/bao-mat">Bảo mật thông tin</router-link></li>
              <li><router-link to="/faq">Câu hỏi thường gặp</router-link></li>
              <li><router-link to="/lien-he">Liên hệ với chúng tôi</router-link></li>
              <li><router-link to="/he-thong-cua-hang">Hệ thống cửa hàng</router-link></li>
            </ul>
          </div>

          <!-- Newsletter -->
          <div class="footer-col">
            <h5 class="footer-col-title">Nhận ưu đãi độc quyền</h5>
            <p class="footer-newsletter-desc">
              Đăng ký nhận bản tin để nhận ngay <strong>voucher 100K</strong> và cập nhật những ưu
              đãi mới nhất mỗi ngày.
            </p>
            <div class="newsletter-form">
              <input
                type="email"
                class="newsletter-input"
                placeholder="Nhập địa chỉ email của bạn..."
              />
              <button class="newsletter-btn"><i class="fas fa-paper-plane me-2"></i>Đăng ký</button>
            </div>

            <h5 class="footer-col-title mt-4">Phương thức thanh toán</h5>
            <div class="payment-methods">
              <div class="payment-badge"><i class="fab fa-cc-visa"></i></div>
              <div class="payment-badge"><i class="fab fa-cc-mastercard"></i></div>
              <div class="payment-badge"><i class="fab fa-cc-jcb"></i></div>
              <div class="payment-badge pay-text">MoMo</div>
              <div class="payment-badge pay-text">ZaloPay</div>
              <div class="payment-badge pay-text">COD</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Copyright -->
    <div class="footer-bottom">
      <div class="container">
        <div class="footer-bottom-inner">
          <span>
            &copy; 2026
            <router-link to="/" class="footer-bottom-brand">Marcus Store</router-link>
            — Bản quyền thuộc về Marcus Store.
          </span>
          <span>
            Thiết kế bởi
            <a href="#" class="footer-bottom-brand">Marcus Store Team</a>
          </span>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { onMounted } from 'vue'
import { useSettings } from '@/composables/useSettings'

const { sysSettings, fetchSettings } = useSettings()

onMounted(() => {
  fetchSettings() // Đọi API. Header đã gọi rồi, footer chỉ lấy data từ Cache
})
</script>
