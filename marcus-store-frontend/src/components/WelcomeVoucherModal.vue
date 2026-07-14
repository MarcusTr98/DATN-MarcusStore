<template>
  <transition name="fade">
    <div
      v-if="visible"
      class="voucher-overlay"
      @click.self="closeModal"
    >
      <div class="voucher-modal">

        <!-- Close -->
        <button class="btn-close-modal" @click="closeModal" aria-label="Đóng">
          <i class="fa-solid fa-xmark"></i>
        </button>

        <!-- Header -->
        <div class="voucher-header">
          <p class="eyebrow">Ưu đãi thành viên mới</p>
          <h2>Quà chào mừng<br>dành cho bạn</h2>
          <p class="header-sub">Đăng ký tài khoản bằng Gmail chỉ trong 5 giây</p>
        </div>

        <!-- Ticket seam: notches + stamp -->
        <div class="ticket-seam">
          <span class="notch notch-left"></span>
          <span class="notch notch-right"></span>
          <div class="stamp">
            <span class="stamp-eyebrow">voucher</span>
            <span class="stamp-amount">100K</span>
          </div>
        </div>

        <!-- Body -->
        <div class="voucher-body">

          <ul class="benefits">
            <li>
              <span class="check"><i class="fa-solid fa-check"></i></span>
              Đăng ký nhanh chỉ với 1 lần nhấn
            </li>

            <li>
              <span class="check"><i class="fa-solid fa-check"></i></span>
              Voucher tự động lưu vào tài khoản
            </li>

            <li>
              <span class="check"><i class="fa-solid fa-check"></i></span>
              Không cần nhập mã giảm giá
            </li>
          </ul>

          <button class="btn-google" @click="registerNow">
            <i class="fa-brands fa-google"></i>
            Đăng ký ngay với Google
          </button>

          <button class="btn-later" @click="closeModal">
            Để sau
          </button>

        </div>

      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'

const visible = ref(false)

onMounted(() => {
  const token = localStorage.getItem('ACCESS_TOKEN')

  // Đã đăng nhập thì không hiện
  if (token) return

  // Chưa đăng nhập thì hiện
  setTimeout(() => {
    visible.value = true
  }, 1500)
})

function closeModal() {
  visible.value = false
}

function registerNow() {
  closeModal()

  setTimeout(() => {
    const footer = document.getElementById('newsletter')

    if (footer) {
      footer.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      })
    }

    nextTick(() => {
      setTimeout(() => {
        document.getElementById('newsletterEmail')?.focus()
      }, 500)
    })
  }, 200)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Baloo+2:wght@600;800&family=Inter:wght@400;500;600&display=swap');

.voucher-overlay {
  position: fixed;
  inset: 0;
  background: rgba(23, 17, 13, .6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 99999;
  padding: 20px;
  backdrop-filter: blur(4px);
  font-family: 'Inter', sans-serif;
}

.voucher-modal {
  width: 420px;
  max-width: 100%;
  background: #FFFDF9;
  border-radius: 22px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 24px 60px rgba(30, 15, 5, .35);
  animation: popup .35s cubic-bezier(.2, .8, .2, 1);
}

.btn-close-modal {
  position: absolute;
  right: 14px;
  top: 14px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, .22);
  color: #fff;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
  cursor: pointer;
  transition: background .2s, transform .2s;
}

.btn-close-modal:hover {
  background: rgba(255, 255, 255, .35);
  transform: rotate(90deg);
}

.voucher-header {
  background: linear-gradient(135deg, #E8483B 0%, #FF8A3D 100%);
  padding: 34px 32px 46px;
  text-align: center;
  color: #fff;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: .12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, .85);
}

.voucher-header h2 {
  margin: 0 0 10px;
  font-family: 'Baloo 2', 'Inter', sans-serif;
  font-weight: 800;
  font-size: 26px;
  line-height: 1.25;
}

.header-sub {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, .92);
}

.ticket-seam {
  position: relative;
  height: 0;
}

.notch {
  position: absolute;
  top: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(23, 17, 13, .6);
  backdrop-filter: blur(4px);
  transform: translateY(-50%);
}

.notch-left { left: -11px; }
.notch-right { right: -11px; }

.stamp {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-6deg);
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #FFF6E9;
  border: 2px dashed #E8483B;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(30, 15, 5, .18);
}

.stamp-eyebrow {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: .1em;
  text-transform: uppercase;
  color: #C9631E;
}

.stamp-amount {
  font-family: 'Baloo 2', 'Inter', sans-serif;
  font-weight: 800;
  font-size: 22px;
  color: #C9331E;
  line-height: 1.2;
}

.voucher-body {
  padding: 46px 32px 30px;
  border-top: 2px dashed #F0DCC8;
}

.benefits {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
}

.benefits li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 13px;
  font-size: 14px;
  color: #4A3F38;
}

.check {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #E1F5EE;
  color: #0F6E56;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}

.btn-google {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #E6DED4;
  border-radius: 12px;
  padding: 13px;
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #2B211C;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
  margin-bottom: 10px;
}

.btn-google i { color: #E8483B; font-size: 16px; }

.btn-google:hover {
  border-color: #D8CBBB;
  box-shadow: 0 2px 8px rgba(30, 15, 5, .08);
}

.btn-later {
  width: 100%;
  background: none;
  border: none;
  color: #9A8F84;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 500;
  padding: 6px;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.btn-later:hover { color: #6B6058; }

.fade-enter-active,
.fade-leave-active {
  transition: .3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes popup {
  from { transform: scale(.85); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}
</style>