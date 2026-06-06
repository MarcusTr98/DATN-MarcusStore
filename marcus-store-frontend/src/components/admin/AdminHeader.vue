<template>
  <div class="header">
    <div class="header-left">
      <div class="header-logo">
        <img :src="boltIcon" alt="Logo" class="header-logo-img" />
      </div>
      <h1>MarcusStore Admin</h1>
    </div>

    <div class="header-right">
      <router-link to="/admin/profile" class="header-btn profile-btn">
        <img :src="personCircleIcon" alt="" class="avatar" />
        <span>{{ username }}</span>
      </router-link>

      <button class="header-btn logout-btn" @click="handleLogout">
        <img :src="logoutIcon" alt="Logout" class="logout-icon" />
        <span>Đăng xuất</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import boltIcon from '/src/assets/icons/lightning.svg'
import personCircleIcon from '/src/assets/icons/person-circle.svg'
import logoutIcon from '/src/assets/icons/logout.svg'

const router = useRouter()

const username = ref('')

const loadUser = () => {
  username.value =
    localStorage.getItem('USERNAME') || 'Admin'
}

onMounted(() => {
  loadUser()

  window.addEventListener('auth-changed', loadUser)
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('USERNAME')
  localStorage.removeItem('USER_ROLE')

  window.dispatchEvent(new Event('auth-changed'))

  router.push('/auth/login')
}
</script>

<style scoped>
.profile-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #5e4a54;
  font-size: 14px;
  font-weight: 500;
}
.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.logout-icon {
  width: 18px;
  height: 18px;
}
.header {
  background: white;
  padding: 20px 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 12px rgba(255, 105, 160, 0.15);
  margin: 16px;
  border-radius: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff7eb3, #ff4d94);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-logo-img {
  width: 24px;
  height: 24px;
}

.header-left h1 {
  color: #ff4d94;
}

.header-right {
  display: flex;
  gap: 12px;
}

.header-btn {
  padding: 10px 16px;
  border: 1px solid #ffd4e4;
  border-radius: 10px;
  background: white;
  cursor: pointer;
}

.avatar {
  width: 32px;
}
</style>
