<template>
  <section class="admin-welcome-page">
    <div class="welcome-card">
      <span class="welcome-greeting">{{ greeting }}</span>
      <h1 class="welcome-name">
        Xin chào, <span class="welcome-name__highlight">{{ displayName }}</span>
      </h1>
      <p class="welcome-message">
        {{ welcomeMessage }}
        <br />
        Hãy chọn một chức năng trong menu bên trái để bắt đầu làm việc.
      </p>
      <div v-if="roleLabel" class="welcome-role">
        <i class="bi bi-shield-check"></i>
        <span>{{ roleLabel }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const userInfo = (() => {
  try {
    return JSON.parse(localStorage.getItem('USER_INFO') || 'null')
  } catch {
    return null
  }
})()

const userRoles = (() => {
  try {
    return JSON.parse(localStorage.getItem('USER_ROLE') || '[]')
  } catch {
    return []
  }
})()

const displayName =
  userInfo?.fullName || userInfo?.username || userInfo?.name || 'bạn'

const isAdmin = userRoles.includes('ROLE_ADMIN')
const isStaff = userRoles.includes('ROLE_STAFF')

const roleLabel = computed(() => {
  if (isAdmin) return 'Quản trị viên (Admin)'
  if (isStaff) return 'Nhân viên (Staff)'
  return ''
})

const welcomeMessage = computed(() => {
  if (isAdmin) return 'Chào mừng bạn đến với trang quản trị Marcus Store.'
  if (isStaff) return 'Chào mừng bạn đến với trang quản lý nhân viên Marcus Store.'
  return 'Chào mừng bạn đến với trang quản trị Marcus Store.'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return 'Chào buổi sáng'
  if (hour < 14) return 'Chào buổi trưa'
  if (hour < 18) return 'Chào buổi chiều'
  return 'Chào buổi tối'
})
</script>

<style scoped>
.admin-welcome-page {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: transparent;
}

.welcome-card {
  max-width: 720px;
  width: 100%;
  text-align: center;
  padding: 56px 32px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.welcome-greeting {
  display: inline-block;
  font-size: 14px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: #6b7280;
  margin-bottom: 12px;
}

.welcome-name {
  font-size: 36px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 16px;
}

.welcome-name__highlight {
  background: linear-gradient(120deg, #6366f1 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

.welcome-message {
  font-size: 16px;
  line-height: 1.7;
  color: #475569;
  margin: 0 auto 24px;
  max-width: 540px;
}

.welcome-role {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  font-size: 14px;
  font-weight: 600;
}

.welcome-role i {
  font-size: 16px;
}

@media (max-width: 600px) {
  .welcome-card {
    padding: 40px 20px;
  }
  .welcome-name {
    font-size: 26px;
  }
}
</style>