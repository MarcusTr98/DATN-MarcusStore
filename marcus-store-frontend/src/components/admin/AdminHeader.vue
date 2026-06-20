<template>
  <div class="header">
    <div class="header-left">
      <div class="header-greeting">
        <h1 class="greeting-title">
          Chào mừng <span>{{ username }}</span> trở lại hệ thống 👋
        </h1>
        <h2 class="greeting-sub">
          Hôm nay là {{ currentDate }}. Chúc bạn một ngày làm việc hiệu quả!
        </h2>
      </div>
    </div>

    <div class="header-right">
      <div class="notification-wrapper" ref="notifWrapperRef">
        <button class="header-btn notif-btn" @click="toggleNotif">
          <i class="fa-regular fa-bell"></i>
          <span v-if="unreadCount > 0" class="badge-pulse">{{ displayUnreadCount }}</span>
        </button>

        <div v-if="showNotifDropdown" class="notif-dropdown card-shadow">
          <div class="notif-header">
            <strong>Thông báo mới</strong>
            <button @click="markAllAsRead" class="btn-read-all">Đánh dấu đã đọc</button>
          </div>
          <div class="notif-body">
            <div v-if="isLoadingNotif" class="empty-notif">Đang tải...</div>
            <div v-else-if="notifications.length === 0" class="empty-notif">
              Không có thông báo nào.
            </div>
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notif-item"
              :class="{ unread: !item.isRead }"
              @click="handleNotifClick(item)"
            >
              <div class="notif-icon" :class="item.type">
                <i v-if="item.type === 'ORDER'" class="fa-solid fa-box"></i>
                <i v-else-if="item.type === 'CONTACT'" class="fa-solid fa-envelope-open-text"></i>
                <i v-else class="fa-solid fa-bell"></i>
              </div>
              <div class="notif-content">
                <p class="notif-title">{{ item.title }}</p>
                <p class="notif-desc">{{ item.message }}</p>
                <span class="notif-time">{{ item.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <router-link to="/admin/profile" class="header-btn profile-btn">
        <img :src="personCircleIcon" alt="Avatar" class="avatar" />
        <span>Hồ sơ cá nhân</span>
      </router-link>

      <button class="header-btn logout-btn" @click="handleLogout">
        <img :src="logoutIcon" alt="Logout" class="logout-icon" />
        <span>Đăng xuất</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import api from '@/utils/api'

import personCircleIcon from '/src/assets/icons/person-circle.svg'
import logoutIcon from '/src/assets/icons/logout.svg'

const router = useRouter()
const username = ref('')
const currentDate = ref('')

const showNotifDropdown = ref(false)
const isLoadingNotif = ref(false)
const unreadCount = ref(0)
const notifications = ref([])
const notifWrapperRef = ref(null)
let stompClient = null

const displayUnreadCount = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value))

const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080'

const loadUser = () => {
  username.value = localStorage.getItem('USERNAME') || 'Admin'
  currentDate.value = new Date().toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

const fetchNotifications = async () => {
  isLoadingNotif.value = true
  try {
    const res = await api.get('/admin/notifications')
    notifications.value = res.data || []
    unreadCount.value = notifications.value.filter((n) => !n.isRead).length
  } catch (error) {
    console.error('Không thể tải thông báo:', error)
  } finally {
    isLoadingNotif.value = false
  }
}

const connectWebSocket = () => {
  const socket = new SockJS(`${WS_BASE_URL}/ws-endpoint`)
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
      stompClient.subscribe('/topic/admin/notifications', (message) => {
        handleNewNotification(JSON.parse(message.body))
      })
    },
  })
  stompClient.activate()
}

const handleNewNotification = (data) => {
  notifications.value.unshift({ ...data, isRead: false })
  unreadCount.value++
}

const toggleNotif = () => {
  showNotifDropdown.value = !showNotifDropdown.value
}

const closeNotifOnOutsideClick = (event) => {
  if (notifWrapperRef.value && !notifWrapperRef.value.contains(event.target)) {
    showNotifDropdown.value = false
  }
}

const markAllAsRead = async () => {
  try {
    await api.put('/admin/notifications/mark-all-read')
    notifications.value.forEach((n) => (n.isRead = true))
    unreadCount.value = 0
  } catch (error) {
    console.error('Không thể đánh dấu đã đọc:', error)
  }
}

const handleNotifClick = (item) => {
  if (!item.isRead) {
    item.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  showNotifDropdown.value = false
  if (item.type === 'ORDER' && item.referenceId) {
    router.push(`/admin/order/${item.referenceId}`)
  } else if (item.type === 'CONTACT') {
    router.push('/admin/contact-management')
  }
}

const handleLogout = () => {
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('USERNAME')
  localStorage.removeItem('USER_ROLE')
  window.dispatchEvent(new Event('auth-changed'))
  if (stompClient) stompClient.deactivate()
  router.push('/auth/login')
}

onMounted(() => {
  loadUser()
  fetchNotifications()
  window.addEventListener('auth-changed', loadUser)
  document.addEventListener('click', closeNotifOnOutsideClick)
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) stompClient.deactivate()
  window.removeEventListener('auth-changed', loadUser)
  document.removeEventListener('click', closeNotifOnOutsideClick)
})
</script>

<style scoped>
.header {
  flex: 0 0 auto;
  background: white;
  padding: 16px 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 12px rgba(255, 105, 160, 0.08);
  margin: 16px;
  border-radius: 16px;
  position: relative;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-greeting {
  display: flex;
  flex-direction: column;
}

.greeting-title {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 800;
  color: #111827;
}

.greeting-title span {
  color: #ff4d94;
}

.greeting-sub {
  margin: 0;
  font-size: 13px;
  color: #374151;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 14px;
  align-items: center;
}

.header-btn {
  padding: 8px 16px;
  border: 1px solid #f3d6e3;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.header-btn:hover {
  background: #fff0f5;
  color: #d63384;
  border-color: #efbdd2;
}

.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
}
.logout-icon {
  width: 18px;
  height: 18px;
}

/* Chuông & Badge */
.notification-wrapper {
  position: relative;
}
.notif-btn {
  padding: 8px 14px;
  font-size: 18px;
  position: relative;
}
.notif-btn i {
  color: #f55d9b;
}

.badge-pulse {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #dc3545;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 99px;
  box-shadow: 0 0 0 2px white;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(220, 53, 69, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(220, 53, 69, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(220, 53, 69, 0);
  }
}

/* Dropdown Container */
.notif-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 340px;
  background: white;
  border-radius: 12px;
  border: 1px solid #f3d6e3;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.notif-header {
  padding: 14px 16px;
  border-bottom: 1px solid #f3d6e3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fffafd;
}
.notif-header strong {
  color: #b4557d;
  font-size: 14px;
  text-transform: uppercase;
}

.btn-read-all {
  border: none;
  background: none;
  color: #0984e3;
  font-size: 12px;
  cursor: pointer;
  font-weight: 700;
}
.btn-read-all:hover {
  text-decoration: underline;
}

.notif-body {
  max-height: 380px;
  overflow-y: auto;
}
.empty-notif {
  padding: 24px;
  text-align: center;
  color: #374151;
  font-size: 14px;
  font-weight: 500;
}

/* Dropdown Item */
.notif-item {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #f9fafb;
  cursor: pointer;
  transition: 0.2s;
}
.notif-item:hover {
  background: #f8fafc;
}
.notif-item.unread {
  background: #fff0f5;
}

.notif-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  font-size: 16px;
}
.notif-icon.ORDER {
  background: #10b981;
}
.notif-icon.CONTACT {
  background: #f59e0b;
}

.notif-content {
  flex: 1;
}
.notif-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}
.notif-desc {
  margin: 4px 0 6px;
  font-size: 13px;
  color: #374151;
  line-height: 1.4;
  font-weight: 500;
}
.notif-time {
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
}
</style>
