<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="overlay"
        @click.self="$emit('close')"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tos-title"
      >
        <div class="modal">
 
          <!-- Header -->
          <div class="modal-header">
            <div class="header-icon icon-red">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
              </svg>
            </div>
            <div class="header-text">
              <h2 id="tos-title">Điều khoản dịch vụ</h2>
              <p>Marcus Store — Cập nhật lần cuối: tháng 6, 2025</p>
            </div>
            <button class="modal-close" aria-label="Đóng" @click="$emit('close')">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
 
          <!-- Body -->
          <div class="modal-body">
 
            <div class="intro-card">
              <p>Chào mừng Quý khách đến với Marcus Store. Khi truy cập hoặc thực hiện giao dịch trên website, Quý khách đồng ý tuân thủ các điều khoản và điều kiện dưới đây.</p>
            </div>
 
            <!-- Section: Phạm vi & Tài khoản -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                Phạm vi áp dụng &amp; Tài khoản
              </div>
              <ul class="item-list">
                <li v-for="item in accountItems" :key="item" class="item">
                  <span class="dot dot-red"></span>{{ item }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Đặt hàng & Thanh toán -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
                Quy trình đặt hàng &amp; Thanh toán
              </div>
              <ul class="item-list">
                <li v-for="item in orderItems" :key="item" class="item">
                  <span class="dot dot-red"></span>{{ item }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Hành vi bị nghiêm cấm -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>
                Hành vi bị nghiêm cấm
              </div>
              <ul class="item-list">
                <li v-for="item in prohibitedItems" :key="item" class="item item-danger">
                  <svg class="item-icon-x" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  {{ item }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Giới hạn trách nhiệm -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                Giới hạn trách nhiệm
              </div>
              <div class="intro-card">
                <p>Marcus Store không chịu trách nhiệm với thiệt hại phát sinh do lỗi từ phía người dùng như cung cấp sai thông tin, để lộ mật khẩu hoặc sử dụng website trái quy định. Điều khoản có thể được cập nhật bất kỳ lúc nào và có hiệu lực ngay khi đăng tải.</p>
              </div>
            </div>
 
            <div class="divider"></div>
 
            <!-- Contact -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.4 2 2 0 0 1 3.6 1.22h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L7.91 8.84a16 16 0 0 0 6.29 6.29l.95-.95a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                Thông tin liên hệ
              </div>
              <div class="contact-grid">
                <div class="contact-item">
                  <span class="contact-badge badge-red">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.4 2 2 0 0 1 3.6 1.22h3a2 2 0 0 1 2 1.72c.17.96.47 1.9.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.84a16 16 0 0 0 6.29 6.29l.95-.95a2 2 0 0 1 2.11-.45c.91.23 1.85.53 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                  </span>
                  <div>
                    <div class="contact-label">Hotline</div>
                    <div class="contact-value">1900 6098</div>
                  </div>
                </div>
                <div class="contact-item">
                  <span class="contact-badge badge-red">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                  </span>
                  <div>
                    <div class="contact-label">Email</div>
                    <div class="contact-value">support@marcusstore.vn</div>
                  </div>
                </div>
                <div class="contact-item contact-full">
                  <span class="contact-badge badge-red">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  </span>
                  <div>
                    <div class="contact-label">Giờ hỗ trợ</div>
                    <div class="contact-value">08:00 – 22:00, tất cả các ngày trong tuần</div>
                  </div>
                </div>
              </div>
            </div>
 
          </div>
 
          <!-- Footer -->
          <div class="modal-footer">
            <p class="footer-note">Bằng cách nhấn "Đã hiểu", bạn xác nhận đã đọc và đồng ý với các điều khoản trên.</p>
            <button class="confirm-btn btn-red" @click="$emit('close')">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
              Đã hiểu
            </button>
          </div>
 
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
 
<script setup>
defineProps({ show: Boolean })
defineEmits(['close'])
 
const accountItems = [
  'Áp dụng với tất cả khách tham quan, khách đã đăng ký và tổ chức sử dụng dịch vụ mua sắm trực tuyến.',
  'Cung cấp đầy đủ, chính xác và trung thực thông tin cá nhân khi đăng ký tài khoản.',
  'Tự chịu trách nhiệm bảo mật tài khoản và mật khẩu; không chia sẻ cho bên thứ ba.',
  'Marcus Store có quyền tạm khóa hoặc chấm dứt tài khoản nếu phát hiện hành vi vi phạm.',
]
 
const orderItems = [
  'Đơn hàng chỉ hợp lệ khi khách hàng nhận được xác nhận từ hệ thống.',
  'Giá sản phẩm hiển thị bằng Việt Nam Đồng (VNĐ) và có thể thay đổi theo chương trình khuyến mãi.',
  'Hỗ trợ nhiều phương thức: COD, chuyển khoản ngân hàng, ví điện tử và cổng thanh toán trực tuyến.',
  'Marcus Store có quyền từ chối đơn hàng nếu phát hiện thông tin không chính xác hoặc dấu hiệu bất thường.',
]
 
const prohibitedItems = [
  'Can thiệp trái phép vào hệ thống hoặc phát tán mã độc, phần mềm gây hại.',
  'Thu thập dữ liệu khách hàng trái phép hoặc thực hiện gian lận trong giao dịch.',
  'Giả mạo thương hiệu Marcus Store dưới bất kỳ hình thức nào.',
]
</script>
 
<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 9999;
  padding: 1rem;
}
 
.modal {
  width: 100%;
  max-width: 680px;
  max-height: 88vh;
  background: #fff;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.15);
}
 
/* ── Header ── */
.modal-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px 28px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
 
.header-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-red  { background: #fff1f3; color: #e11d48; }
.icon-blue { background: #eff6ff; color: #1d6fa4; }
 
.header-text { flex: 1; min-width: 0; }
.header-text h2 { font-size: 17px; font-weight: 600; color: #111; margin: 0 0 3px; }
.header-text p  { font-size: 13px; color: #888; margin: 0; }
 
.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.modal-close:hover { background: #f5f5f5; color: #333; }
 
/* ── Body ── */
.modal-body {
  padding: 20px 28px;
  overflow-y: auto;
  flex: 1;
}
.modal-body::-webkit-scrollbar { width: 4px; }
.modal-body::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }
 
.intro-card {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 20px;
}
.intro-card p { font-size: 14px; color: #444; line-height: 1.7; margin: 0; }
 
.section { margin-bottom: 22px; }
 
.section-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  margin-bottom: 10px;
}
.label-icon { width: 14px; height: 14px; }
 
.item-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
 
.item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  background: #f9f9f9;
  border-radius: 10px;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}
 
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}
.dot-red  { background: #e11d48; }
.dot-blue { background: #1d6fa4; }
 
.item-danger { background: #fff8f9; }
 
.item-icon-x {
  width: 14px;
  height: 14px;
  stroke: #e11d48;
  flex-shrink: 0;
  margin-top: 3px;
}
 
.divider { height: 1px; background: #f0f0f0; margin: 20px 0; }
 
/* ── Contact ── */
.contact-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #f9f9f9;
  border-radius: 10px;
}
.contact-full { grid-column: span 2; }
 
.contact-badge {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.badge-red  { background: #fff1f3; color: #e11d48; }
.badge-blue { background: #eff6ff; color: #1d6fa4; }
 
.contact-label { font-size: 11px; color: #aaa; margin-bottom: 1px; }
.contact-value { font-size: 13px; font-weight: 600; color: #222; }
 
/* ── Footer ── */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 28px 20px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.footer-note { font-size: 12px; color: #bbb; line-height: 1.5; flex: 1; }
 
.confirm-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 22px;
  border-radius: 10px;
  border: none;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.15s, transform 0.1s;
}
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;

  display: flex;
  justify-content: center;
  align-items: center;

  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  z-index: 99999;
  padding: 20px;
}
.modal {
  position: relative;
  margin: auto;
}
@media (max-width: 768px) {
  .overlay {
    padding: 12px;
  }

  .modal {
    max-height: 95vh;
    border-radius: 16px;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding-left: 18px;
    padding-right: 18px;
  }
}
.confirm-btn:hover   { opacity: 0.88; }
.confirm-btn:active  { transform: scale(0.97); }
.btn-red  { background: #e11d48; }
.btn-blue { background: #1d6fa4; }
 
/* ── Transition ── */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-enter-from, .modal-leave-to       { opacity: 0; transform: scale(0.96); }
</style>
 