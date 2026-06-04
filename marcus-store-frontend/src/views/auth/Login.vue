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
          <router-link to="/auth/register" class="tab-link">Đăng ký</router-link>
        </div>

        <div class="title">
          <h2>Chào mừng trở lại!</h2>
          <p>Đăng nhập để tiếp tục mua sắm nhé</p>
        </div>

        <div class="social-login">
          <button class="social-btn social-fb">Facebook</button>
          <button class="social-btn social-gg">Google</button>
        </div>

        <div class="divider"><span>hoặc đăng nhập bằng tài khoản</span></div>

        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label>TÊN ĐĂNG NHẬP HOẶC EMAIL</label>
            <input
              type="text"
              v-model="loginForm.username"
              placeholder="Nhập tên đăng nhập của bạn"
              required
            />
          </div>

          <div class="form-group">
            <div class="password-header">
              <label>MẬT KHẨU</label>
              <router-link to="/auth/forgot-password">QUÊN MẬT KHẨU?</router-link>
            </div>
            <div class="password-input">
              <input
                :type="showLoginPassword ? 'text' : 'password'"
                v-model="loginForm.password"
                placeholder="Nhập mật khẩu của bạn"
                required
              />
              <span class="eye" @click="showLoginPassword = !showLoginPassword">{{
                showLoginPassword ? '🙈' : '👁'
              }}</span>
            </div>
          </div>

          <div class="remember">
            <input type="checkbox" v-model="loginForm.rememberMe" />
            <span>Nhớ tài khoản</span>
          </div>

          <button type="submit" class="main-btn">Đăng nhập</button>
        </form>

        <div class="bottom-link">
          Chưa có tài khoản? <router-link to="/auth/register">Đăng ký ngay</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const showLoginPassword = ref(false)

// ĐÃ SỬA: Binding dữ liệu Form Đăng nhập
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
})

const handleLogin = () => {
  console.log('Dữ liệu Đăng nhập gửi lên Backend:', loginForm)
  // Thực hiện tích hợp Jwt Token lưu vào LocalStorage/Pinia tại đây
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
.auth-page {
  display: flex;
  min-height: 100vh;
  width: 100%; /* BẮT BUỘC: Ép component giãn hết chiều ngang hệ thống */
  background: #fff7fa;
}
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
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  top: -150px;
  left: -120px;
}

.left-panel::after {
  content: '';
  position: absolute;
  width: 350px;
  height: 350px;
  background: rgba(255, 255, 255, 0.06);
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
  background: rgba(255, 255, 255, 0.15);
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
  box-shadow: 0 0 0 4px rgba(255, 105, 160, 0.12);
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
  box-shadow: 0 8px 20px rgba(255, 105, 160, 0.2);
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
