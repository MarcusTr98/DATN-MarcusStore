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
        <button class="header-btn notif-btn" @click.stop="toggleNotif">
          <i class="fa-regular fa-bell"></i>
          <span v-if="unreadCount > 0" class="badge-pulse">{{ displayUnreadCount }}</span>
        </button>

        <div v-if="showNotifDropdown" class="notif-dropdown card-shadow">
          <div class="notif-header">
            <strong>Thông báo hệ thống</strong>
            <button @click="markAllAsRead" class="btn-read-all">Đánh dấu đã đọc</button>
          </div>
          <div class="notif-body">
            <div v-if="isLoadingNotif" class="empty-notif">Đang tải dữ liệu...</div>

            <div v-else-if="displayNotifications.length === 0" class="empty-notif">
              Bạn không còn thông báo nào cần xử lý.
            </div>

            <div
              v-for="item in displayNotifications"
              :key="item.id"
              class="notif-item"
              @click="handleNotifClick(item)"
            >
              <div class="notif-icon-wrapper">
                <div class="notif-icon" :class="item.type">
                  <i v-if="item.type === 'ORDER'" class="fa-solid fa-box"></i>
                  <i v-else-if="item.type === 'CONTACT'" class="fa-solid fa-envelope-open-text"></i>
                  <i v-else class="fa-solid fa-bell"></i>
                </div>
                <span v-if="!item.isRead" class="unread-dot"></span>
              </div>

              <div class="notif-content">
                <p class="notif-title" :class="{ 'font-bold': !item.isRead }">{{ item.title }}</p>
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

// LỌC ra những cái chưa đọc trước, rồi mới lấy 5 cái trên cùng
const displayNotifications = computed(() => {
  return notifications.value.filter((item) => !item.isRead).slice(0, 5)
})

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

    //FIX: Thêm .data.data vì bọc qua ApiResponse của Backend
    notifications.value = res.data.data.list || []
    unreadCount.value = res.data.data.unreadCount || 0
  } catch (error) {
    console.error('Lỗi hệ thống tải thông báo:', error)
  } finally {
    isLoadingNotif.value = false
  }
}

const connectWebSocket = () => {
  const token = localStorage.getItem('ACCESS_TOKEN')

  stompClient = new Client({
    webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws-endpoint`),
    reconnectDelay: 5000,
    connectHeaders: {
      Authorization: token ? `Bearer ${token}` : '',
    },
    onConnect: () => {
      stompClient.subscribe('/topic/admin/notifications', (message) => {
        handleNewNotification(JSON.parse(message.body))
      })
    },
  })
  stompClient.activate()
}

const handleNewNotification = (data) => {
  // Có tin mới qua WebSocket => Đẩy vào đầu danh sách lịch sử
  notifications.value.unshift({ ...data, isRead: false })

  // Giới hạn mảng client-side 20 tin cho nhẹ bộ nhớ trình duyệt
  if (notifications.value.length > 20) {
    notifications.value = notifications.value.slice(0, 20)
  }

  // Tăng số lượng CHƯA ĐỌC ở quả chuông lên 1
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
    console.error('Thao tác thất bại:', error)
  }
}

const handleNotifClick = async (item) => {
  if (!item.isRead) {
    try {
      await api.put(`/admin/notifications/${item.id}/read`)
      item.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      if (unreadCount.value > 0) {
        fetchNotifications()
      }
    } catch (error) {
      console.error('Lỗi thực thi:', error)
    }
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
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.05); /* Shadow xám sang trọng */
  margin: 16px;
  border-radius: 16px;
  position: relative;
  z-index: 50;
  border: 1px solid #e2e8f0;
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
  color: #0f172a; /* Midnight Blue */
}

.greeting-title span {
  color: #3b82f6; /* Đổi text span sang tông xanh Công nghệ quyền lực */
}

.greeting-sub {
  margin: 0;
  font-size: 13px;
  color: #64748b; /* Slate Gray */
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 14px;
  align-items: center;
}

.header-btn {
  padding: 8px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.header-btn:hover {
  background: #f8fafc;
  color: #0f172a;
  border-color: #94a3b8;
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

/* Chuông & Badge thông báo */
.notification-wrapper {
  position: relative;
}
.notif-btn {
  padding: 8px 14px;
  font-size: 18px;
  position: relative;
}
/* Chuông & Badge thông báo */
.notif-btn i {
  color: #f59e0b; /* Màu vàng cam (Amber) rực rỡ, quyền lực */
  filter: drop-shadow(0 2px 4px rgba(245, 158, 11, 0.3)); /* Thêm độ bóng nhẹ cho chuông sáng lên */
  transition: all 0.2s ease;
}

.notif-btn:hover i {
  color: #d97706; /* Sậm màu lại một chút khi trỏ chuột vào */
  transform: scale(1.1); /* Hơi to lên khi trỏ chuột */
}

.badge-pulse {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444; /* Đỏ Crimson */
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
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
  }
}

/* Dropdown Container */
.notif-dropdown {
  position: absolute;
  top: 50px;
  right: 0;
  width: 360px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow:
    0 10px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.notif-header {
  padding: 14px 16px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc; /* Nền Header xám sạch sẽ */
}
.notif-header strong {
  color: #1e293b;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.btn-read-all {
  border: none;
  background: none;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
  font-weight: 700;
}
.btn-read-all:hover {
  text-decoration: underline;
  color: #1d4ed8;
}

.notif-body {
  max-height: 380px;
  overflow-y: auto;
}
.empty-notif {
  padding: 32px 24px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

/* Dropdown Item */
.notif-item {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s ease;
}
.notif-item:hover {
  background: #f8fafc;
}

.notif-icon-wrapper {
  position: relative;
  display: inline-flex;
  align-self: flex-start;
}

.unread-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background-color: #ef4444; /* Chấm đỏ rực khi chưa đọc */
  border: 2px solid #fff;
  border-radius: 50%;
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
  font-size: 14px;
}

/* Tông màu icon hiện đại */
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
  font-weight: 500;
  color: #475569;
}
.notif-title.font-bold {
  color: #0f172a;
  font-weight: 800; /* Đậm vượt trội khẳng định quyền lực */
}

.notif-desc {
  margin: 4px 0 6px;
  font-size: 13px;
  color: #334155;
  line-height: 1.4;
}
.notif-time {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}
</style>
