<template>
  <div class="auth-page">
    <div class="left-panel">
      <div class="content">
        <div class="brand">
          <div class="menu-icon">☰</div>

          <div>
            <h1>MarcusStore</h1>
            <p>Chào mừng bạn đến với MarcusStore!</p>
          </div>
        </div>

        <div class="features">
          <div class="feature-item">
            <div class="icon">🛡</div>
            <span>Bảo mật tuyệt đối - Tin cậy mọi giao dịch</span>
          </div>

          <div class="feature-item">
            <div class="icon">🕒</div>
            <span>Giao hàng siêu tốc - Toàn quốc trong 24h</span>
          </div>

          <div class="feature-item">
            <div class="icon">♡</div>
            <span>Nơi khách hàng gửi trọn niềm tin</span>
          </div>

          <div class="feature-item">
            <div class="icon">📞</div>
            <span>Hỗ trợ 24/7 - Luôn sẵn sàng phục vụ</span>
          </div>

          <div class="feature-item">
            <div class="icon">🔁</div>
            <span>Đổi trả dễ dàng - Cam kết hài lòng</span>
          </div>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="auth-card">
        <div class="tabs">
          <button class="active">Đăng nhập</button>

          <a class="tab-link" href="/dang-ky">
            Đăng ký
          </a>
        </div>

        <div class="title">
          <h2>Chào mừng trở lại!</h2>
          <p>Đăng nhập để tiếp tục mua sắm nhé</p>
        </div>

        <div class="social-login">
          <button class="social-btn social-fb">
            <span class="social-icon" aria-hidden>
              <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg">
                <path fill="#1877F2" d="M24 12.073C24 5.405 18.627 0 12 0S0 5.405 0 12.073C0 18.09 4.388 23.092 10.125 24v-8.486H7.078v-3.441h3.047V9.356c0-3.012 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953h-1.513c-1.49 0-1.953.927-1.953 1.876v2.25h3.328l-.532 3.441h-2.796V24C19.612 23.092 24 18.09 24 12.073z"/>
              </svg>
            </span>
            <span>Facebook</span>
          </button>

          <button class="social-btn social-gg">
            <span class="social-icon" aria-hidden>
              <svg width="20" height="20" viewBox="0 0 533.5 544.3" xmlns="http://www.w3.org/2000/svg">
                <path fill="#4285F4" d="M533.5 278.4c0-17.4-1.4-34.1-4.1-50.3H272v95h147.4c-6.4 34.7-25.9 64.1-55.2 83.8v69.6h89.2c52.4-48.2 82.1-119.3 82.1-198.1z"/>
                <path fill="#34A853" d="M272 544.3c74 0 136-24.6 181.3-66.8l-89.2-69.6c-24.8 16.7-56.6 26.6-92.1 26.6-70.8 0-130.8-47.8-152.2-112.1H29.1v70.4C74.8 492.5 167.2 544.3 272 544.3z"/>
                <path fill="#FBBC05" d="M119.8 325.4c-10.6-31.1-10.6-64.6 0-95.7V159.3H29.1c-39.3 78.8-39.3 170.7 0 249.5l90.7-83.4z"/>
                <path fill="#EA4335" d="M272 107.7c39.9 0 75.8 13.7 104.1 40.6l78.1-78.1C407.8 24.7 345.8 0 272 0 167.2 0 74.8 51.8 29.1 128.9l90.7 70.4C141.2 155.5 201.2 107.7 272 107.7z"/>
              </svg>
            </span>
            <span>Google</span>
          </button>
        </div>

        <div class="divider">
          <span>hoặc đăng nhập bằng tài khoản</span>
        </div>

        <form>
          <div class="form-group">
            <label>TÊN ĐĂNG NHẬP HOẶC EMAIL</label>

            <input
              type="text"
              placeholder="Nhập tên đăng nhập của bạn"
            />
          </div>

          <div class="form-group">
            <div class="password-header">
              <label>MẬT KHẨU</label>
              <a href="#">QUÊN MẬT KHẨU?</a>
            </div>

            <div class="password-input">
              <input
                :type="showLoginPassword ? 'text' : 'password'"
                placeholder="Nhập mật khẩu của bạn"
              />

              <span
                class="eye"
                role="button"
                tabindex="0"
                @click="showLoginPassword = !showLoginPassword"
                @keydown.enter="showLoginPassword = !showLoginPassword"
              >
                {{ showLoginPassword ? '🙈' : '👁' }}
              </span>
            </div>
          </div>

          <div class="remember">
            <input type="checkbox" />
            <span>Nhớ tài khoản</span>
          </div>

          <button class="main-btn">
            Đăng nhập
          </button>
        </form>

        <div class="bottom-link">
          Chưa có tài khoản?
          <a href="/dang-ky">
            Đăng ký ngay
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const showLoginPassword = ref(false)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Inter', sans-serif;
}

.auth-page {
  display: flex;
  min-height: 100vh;
  background: #fff7fa;
}

.left-panel {
  width: 35%;
  background: linear-gradient(135deg, #ff9ec2, #ff5d99);
  padding: 50px;
  display: flex;
  align-items: center;
  color: white;
  position: relative;
  overflow: hidden;
}

.left-panel::before {
  content: '';
  position: absolute;
  width: 450px;
  height: 450px;
  background: rgba(255,255,255,0.08);
  border-radius: 50%;
  top: -150px;
  left: -120px;
}

.left-panel::after {
  content: '';
  position: absolute;
  width: 350px;
  height: 350px;
  background: rgba(255,255,255,0.06);
  border-radius: 50%;
  bottom: -120px;
  right: -100px;
}

.content {
  position: relative;
  z-index: 2;
}

.brand {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 70px;
}

.menu-icon {
  font-size: 28px;
}

.brand h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.brand p {
  opacity: 0.9;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: rgba(255,255,255,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.right-panel {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
}

.auth-card {
  width: 100%;
  max-width: 700px;
  background: white;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 10px 35px rgba(255, 105, 160, 0.12);
}

.tabs {
  display: flex;
  background: #fff0f6;
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 28px;
}

.tabs button,
.tab-link {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  color: #777;
  transition: 0.3s;
  text-align: center;
  text-decoration: none;
}

.tabs .active {
  background: white;
  color: #ff4d94;
  box-shadow: 0 4px 12px rgba(255, 105, 160, 0.15);
}

.title h2 {
  font-size: 32px;
  margin-bottom: 8px;
}

.title p {
  color: #777;
  margin-bottom: 24px;
}

.social-login {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.social-btn {
  flex: 1;
  height: 48px;
  border: 1px solid #ffd4e4;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  color: #273044;
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
  transition: 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.social-btn:hover {
  background: #fff3f8;
  color: #111827;
}

.social-icon {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 24px;
}

.social-icon svg {
  width: 24px;
  height: 24px;
  display: block;
}

.divider {
  position: relative;
  text-align: center;
  margin-bottom: 28px;
  color: #ff6ca7;
  font-size: 13px;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  width: 32%;
  height: 1px;
  background: #f2d8e3;
  top: 50%;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.form-group {
  margin-bottom: 18px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #555;
}

input[type='text'],
input[type='email'],
input[type='password'] {
  width: 100%;
  height: 48px;
  border: 1px solid #f2d8e3;
  border-radius: 12px;
  padding: 0 16px;
  outline: none;
  transition: 0.3s;
}

input:focus {
  border-color: #ff6ca7;
  box-shadow: 0 0 0 4px rgba(255,105,160,0.12);
}

.password-input {
  position: relative;
}

.eye {
  position: absolute;
  right: 16px;
  top: 13px;
  opacity: 0.5;
  cursor: pointer;
  user-select: none;
}

.password-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.password-header a {
  color: #ff4d94;
  text-decoration: none;
  font-size: 12px;
  font-weight: 600;
}

.remember {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 22px;
  font-size: 14px;
  color: #666;
}

.main-btn {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff76ae, #ff4d94);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.3s;
  box-shadow: 0 8px 20px rgba(255,105,160,0.2);
}

.main-btn:hover {
  transform: translateY(-2px);
}

.bottom-link {
  text-align: center;
  margin-top: 24px;
  color: #777;
}

.bottom-link a {
  color: #ff4d94;
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 992px) {
  .auth-page {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
  }
}

@media (max-width: 600px) {
  .social-login {
    flex-direction: column;
  }

  .auth-card {
    padding: 24px;
  }

  .title h2 {
    font-size: 26px;
  }
}
</style>
