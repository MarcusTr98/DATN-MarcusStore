<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
        <div class="login-modal">
          <!-- Close -->
          <button class="close-btn" @click="$emit('close')">
            <i class="fa-solid fa-xmark"></i>
          </button>

          <!-- Logo -->
          <div class="logo-box">
            <div class="logo-icon" :class="{ 'has-site-logo': siteLogoUrl }">
              <img v-if="siteLogoUrl" :src="siteLogoUrl" :alt="siteName" class="site-logo-image" />
              <i v-else class="fa-solid fa-mobile-screen-button"></i>
            </div>

            <div class="logo-text">
              <div class="brand">{{ siteNameParts.primary }}</div>
              <div v-if="siteNameParts.secondary" class="store">{{ siteNameParts.secondary }}</div>
            </div>
          </div>

          <!-- Title -->
          <h3>{{ title }}</h3>

          <!-- Message -->
          <p>
            {{ message }}
          </p>

          <!-- Login -->
          <router-link
            to="/auth/login"
            class="login-btn"
            @click="$emit('close')"
          >
            Đăng nhập ngay
          </router-link>

          <!-- Register -->
          <div class="register">
            Chưa có tài khoản?
            <router-link
              to="/auth/register"
              @click="$emit('close')"
            >
              Đăng ký
            </router-link>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onMounted } from 'vue'
import { useSettings } from '@/composables/useSettings'

const { siteName, siteLogoUrl, siteNameParts, fetchSettings } = useSettings()
onMounted(fetchSettings)
defineProps({
  visible: {
    type: Boolean,
    default: false,
  },

  title: {
    type: String,
    default: 'Trải nghiệm tiện ích',
  },

  message: {
    type: String,
    default: 'Vui lòng đăng nhập để quản lý tài khoản của bạn.',
  },
})

defineEmits(['close'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.login-modal {
  width: 380px;
  background: #fff;
  border-radius: 24px;
  padding: 32px 30px 28px;
  text-align: center;
  position: relative;
  animation: popup .25s ease;
}

.close-btn {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #f3f4f6;
  color: #7b8794;
  cursor: pointer;
  transition: all .2s;
}

.close-btn:hover {
  background: #e9ecef;
}

.logo-box {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: #d70018;
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 26px;
}

/* Marcus sửa: ảnh nhận diện có nền trắng, fallback icon vẫn giữ nền đỏ. */
.logo-icon.has-site-logo {
  padding: 7px;
  background: #ffffff;
  border: 1px solid #fee2e2;
  box-shadow: 0 5px 14px rgba(215, 0, 24, 0.12);
}

.logo-icon.has-site-logo .site-logo-image {
  border-radius: 7px;
}

.logo-text {
  text-align: left;
  line-height: 1.1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand {
  font-size: 22px;
  font-weight: 800;
  color: #d70018;
  line-height: 1;
  margin: 0;
}

.store {
  font-size: 13px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: #d70018;
  line-height: 1;
  margin: 0;
}

h3 {
  font-size: 18px;
  font-weight: 700;
  color: #222;
  margin-bottom: 12px;
}

p {
  color: #666;
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 26px;
}

.login-btn {
  display: block;
  width: 100%;
  padding: 14px;
  border-radius: 14px;
  background: #d70018;
  color: #fff;
  text-decoration: none;
  font-weight: 700;
  transition: .2s;
}

.login-btn:hover {
  background: #b90015;
  color: #fff;
}

.register {
  margin-top: 18px;
  font-size: 14px;
  color: #666;
}

.register a {
  color: #d70018;
  text-decoration: none;
  font-weight: 700;
  margin-left: 4px;
}

.register a:hover {
  text-decoration: underline;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity .25s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes popup {
  from {
    transform: scale(.92);
    opacity: 0;
  }

  to {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 576px) {
  .login-modal {
    width: calc(100% - 32px);
    padding: 28px 22px;
  }

  .logo-icon {
    width: 50px;
    height: 50px;
    font-size: 22px;
  }

  .brand {
    font-size: 20px;
  }

  .store {
    font-size: 12px;
    letter-spacing: 2px;
  }
}
</style>
