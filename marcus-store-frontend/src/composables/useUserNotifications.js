import { computed, onBeforeUnmount, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '@/utils/api'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws-endpoint'

// Marcus thêm: chuông riêng của khách, chỉ đọc notification gắn với user đang
// đăng nhập. API không nhận userId từ frontend.
export function useUserNotifications() {
  const notifications = ref([])
  const unreadCount = ref(0)
  const isLoading = ref(false)
  const errorMessage = ref('')
  let stompClient = null

  const displayUnreadCount = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value))

  const fetchNotifications = async () => {
    if (!localStorage.getItem('ACCESS_TOKEN') || isLoading.value) return
    isLoading.value = true
    try {
      const response = await api.get('/user/notifications', {
        params: { page: 0, size: 10 },
        skipGlobalLoading: true,
      })
      const data = response.data?.data || {}
      notifications.value = Array.isArray(data.list) ? data.list : []
      unreadCount.value = Number(data.unreadCount) || 0
      connectRealtime()
      errorMessage.value = ''
    } catch {
      errorMessage.value = 'Không thể tải thông báo.'
    } finally {
      isLoading.value = false
    }
  }

  const connectRealtime = () => {
    if (stompClient?.active) return
    stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${localStorage.getItem('ACCESS_TOKEN')}` },
      onConnect: () => {
        stompClient.subscribe('/user/queue/notifications', (message) => {
          try {
            const payload = JSON.parse(message.body)
            if (payload.event === 'NEW' && payload.data) {
              notifications.value.unshift(payload.data)
              notifications.value = notifications.value.slice(0, 10)
              unreadCount.value += 1
            }
          } catch {
            fetchNotifications()
          }
        })
      },
    })
    stompClient.activate()
  }

  const markRead = async (item) => {
    if (!item.isRead) {
      await api.put(`/user/notifications/${item.id}/read`, null, { skipGlobalLoading: true })
      item.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    if (item.deepLink) return item.deepLink
    if (item.referenceId) return `/profile/orders/${item.referenceId}`
    return null
  }

  const markAllRead = async () => {
    await api.put('/user/notifications/mark-all-read', null, { skipGlobalLoading: true })
    notifications.value.forEach((item) => (item.isRead = true))
    unreadCount.value = 0
  }

  const disconnectRealtime = () => {
    stompClient?.deactivate()
    stompClient = null
    notifications.value = []
    unreadCount.value = 0
  }

  onBeforeUnmount(disconnectRealtime)
  return {
    notifications,
    unreadCount,
    displayUnreadCount,
    isLoading,
    errorMessage,
    fetchNotifications,
    markRead,
    markAllRead,
    disconnectRealtime,
  }
}
