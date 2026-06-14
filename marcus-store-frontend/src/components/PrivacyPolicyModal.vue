<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="overlay"
        @click.self="$emit('close')"
        role="dialog"
        aria-modal="true"
        aria-labelledby="privacy-title"
      >
        <div class="modal">
 
          <!-- Header -->
          <div class="modal-header">
            <div class="header-icon icon-blue">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </div>
            <div class="header-text">
              <h2 id="privacy-title">Chính sách bảo mật thông tin</h2>
              <p>Marcus Store cam kết bảo vệ quyền riêng tư của bạn</p>
            </div>
            <button class="modal-close" aria-label="Đóng" @click="$emit('close')">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
 
          <!-- Body -->
          <div class="modal-body">
 
            <div class="intro-card">
              <p>Marcus Store cam kết tôn trọng và bảo vệ quyền riêng tư của khách hàng. Chính sách này mô tả cách chúng tôi thu thập, sử dụng và bảo vệ thông tin cá nhân của bạn.</p>
            </div>
 
            <!-- Section: Thu thập -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>
                Thông tin chúng tôi thu thập
              </div>
              <ul class="item-list">
                <li v-for="item in collectItems" :key="item.text" class="item">
                  <span class="dot dot-blue"></span>{{ item.text }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Mục đích -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>
                Mục đích sử dụng
              </div>
              <ul class="item-list">
                <li v-for="item in purposeItems" :key="item" class="item">
                  <span class="dot dot-blue"></span>{{ item }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Chia sẻ bên thứ ba -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
                Chia sẻ thông tin với bên thứ ba
              </div>
              <div class="intro-card">
                <p>Chúng tôi chỉ chia sẻ dữ liệu trong các trường hợp cần thiết: <strong>đơn vị vận chuyển</strong> phục vụ giao hàng, <strong>đối tác thanh toán</strong> điện tử, và <strong>cơ quan nhà nước</strong> theo quy định pháp luật. Chúng tôi <u>không bán</u> dữ liệu khách hàng cho bất kỳ bên nào.</p>
              </div>
            </div>
 
            <!-- Section: Cam kết bảo mật -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                Cam kết bảo mật
              </div>
              <ul class="item-list">
                <li v-for="item in securityItems" :key="item" class="item">
                  <svg class="item-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  {{ item }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Quyền của bạn -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                Quyền của bạn
              </div>
              <ul class="item-list">
                <li v-for="item in rightItems" :key="item" class="item">
                  <svg class="item-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
                  {{ item }}
                </li>
              </ul>
            </div>
 
            <!-- Section: Cookie & Trẻ em -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M8.56 2.75c4.37 6.03 6.02 9.42 8.03 17.72m2.54-15.38c-3.72 4.35-8.94 5.66-16.88 5.85m19.5 1.9c-3.5-.93-6.63-.82-8.94 0-2.58.92-5.01 2.86-7.44 6.32"/></svg>
                Cookie &amp; Bảo vệ trẻ em
              </div>
              <div class="intro-card">
                <p>Website sử dụng Cookie để ghi nhớ tùy chọn và cải thiện trải nghiệm. Bạn có thể tắt Cookie qua cài đặt trình duyệt. Chúng tôi không thu thập dữ liệu của trẻ em dưới 13 tuổi; nếu phát hiện, dữ liệu sẽ được xóa ngay lập tức.</p>
              </div>
            </div>
 
            <div class="divider"></div>
 
            <!-- Contact -->
            <div class="section">
              <div class="section-label">
                <svg class="label-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.4 2 2 0 0 1 3.6 1.22h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L7.91 8.84a16 16 0 0 0 6.29 6.29l.95-.95a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                Liên hệ về quyền riêng tư
              </div>
              <div class="contact-grid">
                <div class="contact-item">
                  <span class="contact-badge badge-blue">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.4 2 2 0 0 1 3.6 1.22h3a2 2 0 0 1 2 1.72c.17.96.47 1.9.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.84a16 16 0 0 0 6.29 6.29l.95-.95a2 2 0 0 1 2.11-.45c.91.23 1.85.53 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                  </span>
                  <div>
                    <div class="contact-label">Hotline</div>
                    <div class="contact-value">1900 6098</div>
                  </div>
                </div>
                <div class="contact-item">
                  <span class="contact-badge badge-blue">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                  </span>
                  <div>
                    <div class="contact-label">Email</div>
                    <div class="contact-value">privacy@marcusstore.vn</div>
                  </div>
                </div>
                <div class="contact-item contact-full">
                  <span class="contact-badge badge-blue">
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
            <p class="footer-note">Việc tiếp tục sử dụng dịch vụ đồng nghĩa bạn chấp thuận chính sách bảo mật này.</p>
            <button class="confirm-btn btn-blue" @click="$emit('close')">
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
 
const collectItems = [
  { text: 'Họ tên, số điện thoại và địa chỉ email.' },
  { text: 'Địa chỉ giao hàng và lịch sử giao dịch, mua hàng.' },
  { text: 'Thông tin đăng nhập tài khoản (được mã hóa bảo mật).' },
]
 
const purposeItems = [
  'Xử lý đơn hàng, giao hàng và xác minh danh tính khách hàng.',
  'Hỗ trợ chăm sóc khách hàng và giải quyết khiếu nại.',
  'Thông báo chương trình khuyến mãi và cải thiện trải nghiệm mua sắm.',
]
 
const securityItems = [
  'Sử dụng công nghệ mã hóa để bảo vệ dữ liệu trong quá trình truyền tải và lưu trữ.',
  'Giới hạn quyền truy cập thông tin nội bộ theo từng vai trò.',
  'Thường xuyên kiểm tra và nâng cấp hệ thống bảo mật.',
]
 
const rightItems = [
  'Kiểm tra, cập nhật hoặc chỉnh sửa thông tin cá nhân.',
  'Yêu cầu khóa hoặc xóa tài khoản và toàn bộ dữ liệu liên quan.',
  'Từ chối nhận email quảng cáo bất kỳ lúc nào.',
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
.intro-card strong { font-weight: 600; color: #222; }
.intro-card u { text-decoration-color: #1d6fa4; }
 
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
 
.item-check {
  width: 14px;
  height: 14px;
  stroke: #1d6fa4;
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

  display: grid;
  place-items: center;

  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  z-index: 99999;
  padding: 20px;
}

.modal {
  position: relative;
  width: min(680px, 100%);
  max-height: 88vh;
  margin: auto;

  background: #fff;
  border-radius: 20px;

  display: flex;
  flex-direction: column;
  overflow: hidden;

  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.15);
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
.confirm-btn:hover  { opacity: 0.88; }
.confirm-btn:active { transform: scale(0.97); }
.btn-red  { background: #e11d48; }
.btn-blue { background: #1d6fa4; }
 
/* ── Transition ── */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-enter-from, .modal-leave-to       { opacity: 0; transform: scale(0.96); }
</style>