<template>
  <div class="header">
    <div class="header-left">
      <div class="header-logo">
        <img :src="boltIcon" alt="Logo" class="header-logo-img" />
      </div>
      <h1>MarcusStore Admin</h1>
    </div>

    <div class="header-right">
      <div class="notification-wrapper">
        <button class="header-btn notif-btn" @click="toggleNotif">
          <i class="fa-regular fa-bell"></i>
          <span v-if="unreadCount > 0" class="badge-pulse">{{ unreadCount }}</span>
        </button>

        <div v-if="showNotifDropdown" class="notif-dropdown card-shadow">
          <div class="notif-header">
            <strong>Thông báo mới</strong>
            <button @click="markAllAsRead" class="btn-read-all">Đánh dấu đã đọc</button>
          </div>
          <div class="notif-body">
            <div v-if="notifications.length === 0" class="empty-notif">Không có thông báo nào.</div>
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notif-item"
              :class="{ unread: !item.isRead }"
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

import boltIcon from '/src/assets/icons/lightning.svg'
import personCircleIcon from '/src/assets/icons/person-circle.svg'
import logoutIcon from '/src/assets/icons/logout.svg'

const router = useRouter()
const username = ref('')

// --- State Thông báo ---
const showNotifDropdown = ref(false)
const unreadCount = ref(0)
const notifications = ref([])
let stompClient = null

const loadUser = () => {
  username.value = localStorage.getItem('USERNAME') || 'Admin'
}

// Logic WebSocket
const connectWebSocket = () => {
  // Thay đổi URL theo cấu hình WebSocket của Spring Boot
  const socket = new SockJS('http://localhost:8080/ws-endpoint')
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('Đã kết nối WebSocket!')
      // kênh thông báo chung của Admin
      stompClient.subscribe('/topic/admin/notifications', (message) => {
        const notifData = JSON.parse(message.body)
        handleNewNotification(notifData)
      })
    },
    onStompError: (frame) => {
      console.error('Lỗi Stomp: ' + frame.headers['message'])
    },
  })
  stompClient.activate()
}

const handleNewNotification = (data) => {
  // data = { id, title, message, type: 'ORDER' | 'CONTACT', time }
  notifications.value.unshift({ ...data, isRead: false })
  unreadCount.value++

  // const audio = new Audio('/ting.mp3'); audio.play();
}

const toggleNotif = () => {
  showNotifDropdown.value = !showNotifDropdown.value
}

const markAllAsRead = () => {
  notifications.value.forEach((n) => (n.isRead = true))
  unreadCount.value = 0
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
  window.addEventListener('auth-changed', loadUser)

  // Tạm tạo vài data ảo để ông test giao diện trước khi nối Backend
  notifications.value = [
    {
      id: 1,
      type: 'ORDER',
      title: 'Đơn hàng mới!',
      message: 'Khách hàng Nguyễn Văn A vừa đặt đơn ORD-123',
      time: 'Vừa xong',
      isRead: false,
    },
    {
      id: 2,
      type: 'CONTACT',
      title: 'Yêu cầu hỗ trợ',
      message: 'Trần B vừa gửi form liên hệ',
      time: '10 phút trước',
      isRead: false,
    },
  ]
  unreadCount.value = 2
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) stompClient.deactivate()
  window.removeEventListener('auth-changed', loadUser)
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
  box-shadow: 0 4px 12px rgba(255, 105, 160, 0.15);
  margin: 16px;
  border-radius: 16px;
  position: relative;
  z-index: 50; /* Để dropdown không bị đè */
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
  margin: 0;
  font-size: 20px;
  font-weight: 800;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.header-btn {
  padding: 8px 16px;
  border: 1px solid #ffd4e4;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #5e4a54;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}
.header-btn:hover {
  background: #fff0f5;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}
.logout-icon {
  width: 16px;
  height: 16px;
}

/* CSS cho Quả chuông và Dropdown */
.notification-wrapper {
  position: relative;
}
.notif-btn {
  padding: 8px 12px;
  font-size: 18px;
  position: relative;
}
.notif-btn i {
  color: #ff4d94;
}

.badge-pulse {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #d70018;
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
    box-shadow: 0 0 0 0 rgba(215, 0, 24, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(215, 0, 24, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(215, 0, 24, 0);
  }
}

.notif-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 320px;
  background: white;
  border-radius: 12px;
  border: 1px solid #eee;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.notif-header {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fdfdfd;
}
.btn-read-all {
  border: none;
  background: none;
  color: #0984e3;
  font-size: 12px;
  cursor: pointer;
  font-weight: 600;
}
.btn-read-all:hover {
  text-decoration: underline;
}

.notif-body {
  max-height: 350px;
  overflow-y: auto;
}
.empty-notif {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.notif-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: 0.2s;
}
.notif-item:hover {
  background: #f9fafb;
}
.notif-item.unread {
  background: #fff0f5;
}

.notif-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}
.notif-icon.ORDER {
  background: #27ae60;
}
.notif-icon.CONTACT {
  background: #f39c12;
}

.notif-content {
  flex: 1;
}
.notif-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #333;
}
.notif-desc {
  margin: 2px 0 4px;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}
.notif-time {
  font-size: 11px;
  color: #999;
}
</style>
