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
      <button
        class="header-btn theme-btn"
        type="button"
        :aria-label="isDark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'"
        :title="isDark ? 'Light mode' : 'Dark mode'"
        @click="toggleTheme"
      >
        <i :class="isDark ? 'fa-solid fa-sun' : 'fa-solid fa-moon'"></i>
        <span>{{ isDark ? 'Light' : 'Dark' }}</span>
      </button>

      <div class="notification-wrapper" ref="notifWrapperRef">
        <button
          class="header-btn notif-btn"
          type="button"
          aria-label="Mở trung tâm thông báo"
          :aria-expanded="showNotifDropdown"
          @click.stop="toggleNotif"
        >
          <i class="fa-regular fa-bell"></i>
          <span v-if="unreadCount > 0" class="badge-pulse">{{ displayUnreadCount }}</span>
        </button>

        <div v-if="showNotifDropdown" class="notif-dropdown card-shadow">
          <div class="notif-header">
            <div>
              <strong>Trung tâm thông báo</strong>
              <span class="realtime-status" :class="connectionState.toLowerCase()">
                <i class="fa-solid fa-circle"></i>
                {{ connectionState === 'CONNECTED' ? 'Đang realtime' : 'Đang kết nối lại' }}
              </span>
            </div>
            <button
              class="btn-read-all"
              type="button"
              :disabled="unreadCount === 0"
              @click="handleMarkAllAsRead"
            >
              Đọc tất cả
            </button>
          </div>
          <div class="notif-tabs">
            <button
              type="button"
              :class="{ active: activeFilter === 'ALL' }"
              @click="setFilter('ALL')"
            >
              Tất cả
            </button>
            <button
              type="button"
              :class="{ active: activeFilter === 'UNREAD' }"
              @click="setFilter('UNREAD')"
            >
              Chưa đọc <span v-if="unreadCount">{{ displayUnreadCount }}</span>
            </button>
          </div>
          <div class="notif-body">
            <div v-if="isLoading" class="notif-skeleton-list">
              <div v-for="index in 3" :key="index" class="notif-skeleton"></div>
            </div>

            <div v-else-if="errorMessage" class="empty-notif error-state">
              <i class="fa-solid fa-triangle-exclamation"></i>
              <span>{{ errorMessage }}</span>
              <button type="button" @click="fetchNotifications()">Thử lại</button>
            </div>

            <div v-else-if="notifications.length === 0" class="empty-notif">
              <span class="empty-notif__icon"><i class="fa-regular fa-bell-slash"></i></span>
              <strong>{{
                activeFilter === 'UNREAD' ? 'Bạn đã đọc hết thông báo' : 'Chưa có thông báo'
              }}</strong>
              <span>Thông báo đơn hàng và liên hệ mới sẽ xuất hiện tại đây.</span>
            </div>

            <template v-else>
              <button
                v-for="item in notifications"
                :key="item.id"
                type="button"
                class="notif-item"
                :class="{ unread: !item.isRead }"
                @click="handleNotifClick(item)"
              >
                <div class="notif-icon-wrapper">
                  <div class="notif-icon" :class="item.type">
                    <i :class="getNotificationIcon(item.type)"></i>
                  </div>
                  <span v-if="!item.isRead" class="unread-dot"></span>
                </div>

                <div class="notif-content">
                  <p class="notif-title">{{ item.title }}</p>
                  <p class="notif-desc">{{ item.message }}</p>
                  <span class="notif-time">{{ formatRelativeTime(item.createdAt) }}</span>
                </div>
                <i class="fa-solid fa-chevron-right notif-chevron"></i>
              </button>

              <button
                v-if="hasMore"
                type="button"
                class="btn-load-more"
                :disabled="isLoadingMore"
                @click="loadMore"
              >
                <i v-if="isLoadingMore" class="fa-solid fa-spinner fa-spin"></i>
                {{ isLoadingMore ? 'Đang tải...' : 'Xem thêm thông báo' }}
              </button>
            </template>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminNotifications } from '@/composables/useAdminNotifications'
import { useAdminTheme } from '@/composables/useAdminTheme'

import personCircleIcon from '/src/assets/icons/person-circle.svg'
import logoutIcon from '/src/assets/icons/logout.svg'

const router = useRouter()
const { isDark, toggleTheme } = useAdminTheme()
const username = ref('')
const currentDate = ref('')

const showNotifDropdown = ref(false)
const notifWrapperRef = ref(null)
const {
  notifications,
  unreadCount,
  displayUnreadCount,
  activeFilter,
  hasMore,
  isLoading,
  isLoadingMore,
  errorMessage,
  connectionState,
  fetchNotifications,
  setFilter,
  loadMore,
  markAsRead,
  markAllAsRead,
  connectRealtime,
  disconnectRealtime,
} = useAdminNotifications()

const loadUser = () => {
  username.value = localStorage.getItem('USERNAME') || 'Admin'
  currentDate.value = new Date().toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

const toggleNotif = () => {
  showNotifDropdown.value = !showNotifDropdown.value
  if (showNotifDropdown.value && notifications.value.length === 0) fetchNotifications()
}

const closeNotifOnOutsideClick = (event) => {
  if (notifWrapperRef.value && !notifWrapperRef.value.contains(event.target)) {
    showNotifDropdown.value = false
  }
}

const handleMarkAllAsRead = async () => {
  await markAllAsRead()
}

const handleNotifClick = async (item) => {
  await markAsRead(item)

  showNotifDropdown.value = false

  if (
    ['ORDER', 'ORDER_COMPLETED', 'ORDER_CANCELLED', 'ORDER_FAILED', 'REFUND'].includes(item.type) &&
    item.referenceId
  ) {
    router.push(`/admin/order/${item.referenceId}`)
  } else if (item.type === 'CONTACT') {
    router.push('/admin/contact-management')
  } else if (typeof item.type === 'string' && item.type.startsWith('WARRANTY_') && item.referenceId) {
    router.push(`/admin/warranty/${item.referenceId}`)
  }
}

const getNotificationIcon = (type) => {
  if (type === 'ORDER') return 'fa-solid fa-box'
  if (type === 'ORDER_COMPLETED') return 'fa-solid fa-circle-check'
  if (type === 'ORDER_CANCELLED') return 'fa-solid fa-ban'
  if (type === 'ORDER_FAILED') return 'fa-solid fa-triangle-exclamation'
  if (type === 'CONTACT') return 'fa-solid fa-envelope-open-text'
  if (type === 'REFUND') return 'fa-solid fa-money-bill-transfer'
  if (type === 'WARRANTY_REQUEST') return 'fa-solid fa-shield-halved'
  if (typeof type === 'string' && type.startsWith('WARRANTY_')) return 'fa-solid fa-shield-halved'
  return 'fa-solid fa-bell'
}

const formatRelativeTime = (value) => {
  if (!value) return 'Vừa xong'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Vừa xong'
  const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000))
  if (seconds < 60) return 'Vừa xong'
  if (seconds < 3600) return `${Math.floor(seconds / 60)} phút trước`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} giờ trước`
  if (seconds < 604800) return `${Math.floor(seconds / 86400)} ngày trước`
  return date.toLocaleDateString('vi-VN')
}

const handleLogout = () => {
  localStorage.removeItem('ACCESS_TOKEN')
  localStorage.removeItem('USERNAME')
  localStorage.removeItem('USER_ROLE')
  window.dispatchEvent(new Event('auth-changed'))
  disconnectRealtime()
  router.push('/auth/login')
}

onMounted(() => {
  loadUser()
  fetchNotifications()
  window.addEventListener('auth-changed', loadUser)
  document.addEventListener('click', closeNotifOnOutsideClick)
  connectRealtime()
})

onUnmounted(() => {
  disconnectRealtime()
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

.theme-btn i {
  width: 16px;
  color: #2563eb;
  text-align: center;
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
  width: min(420px, calc(100vw - 32px));
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
.notif-header > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.notif-header strong {
  color: #1e293b;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.realtime-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 600;
}
.realtime-status i {
  font-size: 6px;
}
.realtime-status.connected {
  color: #16a34a;
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
.btn-read-all:disabled {
  color: #94a3b8;
  cursor: default;
  text-decoration: none;
}

.notif-tabs {
  display: flex;
  gap: 6px;
  padding: 9px 14px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}
.notif-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.notif-tabs button.active {
  background: #eff6ff;
  color: #2563eb;
}
.notif-tabs button span {
  min-width: 18px;
  padding: 1px 5px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 9px;
  text-align: center;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
}
.empty-notif__icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 17px;
}
.empty-notif strong {
  color: #334155;
}
.empty-notif > span:last-child {
  font-size: 11px;
}
.error-state {
  color: #b91c1c;
}
.error-state button {
  padding: 6px 10px;
  border: 0;
  border-radius: 7px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

/* Dropdown Item */
.notif-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s ease;
  border-top: 0;
  border-left: 0;
  border-right: 0;
  background: #fff;
  text-align: left;
}
.notif-item:hover {
  background: #f8fafc;
}
.notif-item.unread {
  background: #f8fbff;
}
.notif-item.unread:hover {
  background: #eff6ff;
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
.notif-icon.ORDER_COMPLETED {
  background: #087b47;
}
.notif-icon.ORDER_CANCELLED {
  background: #ef4444;
}
.notif-icon.ORDER_FAILED {
  background: #f97316;
}
.notif-icon.CONTACT {
  background: #f59e0b;
}
.notif-icon.REFUND {
  background: #8b5cf6;
}
.notif-icon.WARRANTY_REQUEST {
  background: #e60012;
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
.notif-item.unread .notif-title {
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

.notif-chevron {
  align-self: center;
  color: #cbd5e1;
  font-size: 10px;
}
.btn-load-more {
  width: 100%;
  padding: 11px;
  border: 0;
  border-top: 1px solid #f1f5f9;
  background: #fff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.btn-load-more:hover {
  background: #f8fafc;
}
.notif-skeleton-list {
  padding: 8px 14px;
}
.notif-skeleton {
  height: 66px;
  margin: 7px 0;
  border-radius: 9px;
  background: linear-gradient(90deg, #f1f5f9 25%, #f8fafc 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: notif-loading 1.4s infinite;
}
@keyframes notif-loading {
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 720px) {
  .header {
    margin: 10px;
    padding: 12px 14px 12px 58px;
    gap: 12px;
  }
  .greeting-title {
    font-size: 15px;
  }
  .greeting-sub {
    display: none;
  }
  .header-right {
    gap: 6px;
  }
  .header-btn {
    padding: 8px 10px;
  }
  .header-btn > span,
  .profile-btn > span,
  .logout-btn > span {
    display: none;
  }
  .notif-dropdown {
    position: fixed;
    top: 76px;
    right: 12px;
    left: 12px;
    width: auto;
    max-height: calc(100dvh - 92px);
  }
  .notif-body {
    max-height: calc(100dvh - 205px);
  }
}
</style>
