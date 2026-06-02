<template>
  <div class="header">
    <div class="header-left">
      <div class="header-logo">
        <img :src="boltIcon" alt="Logo" class="header-logo-img" />
      </div>
      <h1>MarcusStore</h1>
    </div>

    <div class="header-right">
      <button
        class="header-btn profile-btn"
        @click="showProfileMenu = !showProfileMenu"
      >
        <img :src="personCircleIcon" alt="Profile" class="avatar" />
        <span>Profile</span>
      </button>

      <button class="header-btn logout-btn" @click="handleLogout">
        <img :src="logoutIcon" alt="Logout" class="logout-icon" />
        <span>Logout</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

import boltIcon from '/src/assets/icons/lightning.svg'
import personCircleIcon from '/src/assets/icons/person-circle.svg'
import logoutIcon from '/src/assets/icons/logout.svg'

const showProfileMenu = ref(false)

const handleLogout = () => {
  alert('Logging out...')
}
</script>

<style scoped>
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