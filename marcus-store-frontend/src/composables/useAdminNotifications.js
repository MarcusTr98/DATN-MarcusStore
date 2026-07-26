import { computed, onBeforeUnmount, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '@/utils/api'

const PAGE_SIZE = 10
const WS_ENDPOINT_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws-endpoint'

// Marcus thêm: quản lý API + realtime chuông ở một nơi, tránh AdminHeader ôm toàn bộ nghiệp vụ.
export function useAdminNotifications() {
  const notifications = ref([])
  const unreadCount = ref(0)
  const activeFilter = ref('ALL')
  const page = ref(0)
  const hasMore = ref(false)
  const isLoading = ref(false)
  const isLoadingMore = ref(false)
  const errorMessage = ref('')
  const connectionState = ref('CONNECTING')
  let stompClient = null

  const displayUnreadCount = computed(() =>
    unreadCount.value > 99 ? '99+' : String(unreadCount.value),
  )

  const isUnreadOnly = computed(() => activeFilter.value === 'UNREAD')

  const fetchNotifications = async ({ append = false } = {}) => {
    if (append) isLoadingMore.value = true
    else isLoading.value = true
    errorMessage.value = ''

    try {
      const response = await api.get('/admin/notifications', {
        params: {
          page: append ? page.value + 1 : 0,
          size: PAGE_SIZE,
          unreadOnly: isUnreadOnly.value,
        },
      })
      const data = response.data?.data ?? {}
      const incoming = Array.isArray(data.list) ? data.list : []

      if (append) {
        const knownIds = new Set(notifications.value.map((item) => item.id))
        notifications.value.push(...incoming.filter((item) => !knownIds.has(item.id)))
        page.value += 1
      } else {
        notifications.value = incoming
        page.value = 0
      }

      unreadCount.value = Number(data.unreadCount) || 0
      hasMore.value = Boolean(data.hasMore)
    } catch {
      errorMessage.value = 'Không thể tải thông báo. Vui lòng thử lại.'
    } finally {
      isLoading.value = false
      isLoadingMore.value = false
    }
  }

  const setFilter = async (filter) => {
    if (activeFilter.value === filter) return
    activeFilter.value = filter
    await fetchNotifications()
  }

  const loadMore = () => {
    if (!hasMore.value || isLoadingMore.value) return
    return fetchNotifications({ append: true })
  }

  const markAsRead = async (item) => {
    if (item.isRead) return
    try {
      await api.put(`/admin/notifications/${item.id}/read`)
      // Marcus sửa: tránh trừ badge hai lần nếu event READ realtime về trước HTTP response.
      if (!item.isRead) {
        item.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      if (isUnreadOnly.value) {
        notifications.value = notifications.value.filter((entry) => entry.id !== item.id)
      }
    } catch {
      errorMessage.value = 'Không thể đánh dấu thông báo đã đọc.'
    }
  }

  const markAllAsRead = async () => {
    if (unreadCount.value === 0) return
    try {
      await api.put('/admin/notifications/mark-all-read')
      unreadCount.value = 0
      if (isUnreadOnly.value) notifications.value = []
      else notifications.value.forEach((item) => (item.isRead = true))
    } catch {
      errorMessage.value = 'Không thể đánh dấu tất cả thông báo đã đọc.'
    }
  }

  const handleRealtimeEvent = (payload) => {
    if (payload.event === 'NEW' && payload.data) {
      const item = payload.data
      if (!notifications.value.some((entry) => entry.id === item.id)) {
        notifications.value.unshift(item)
        if (notifications.value.length > PAGE_SIZE && !hasMore.value) notifications.value.pop()
        unreadCount.value += 1
      }
      return
    }

    if (payload.event === 'READ') {
      const item = notifications.value.find((entry) => entry.id === payload.id)
      if (!item || !item.isRead) {
        if (item) item.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      if (isUnreadOnly.value) {
        notifications.value = notifications.value.filter((entry) => entry.id !== payload.id)
      }
      return
    }

    if (payload.event === 'READ_ALL') {
      unreadCount.value = 0
      if (isUnreadOnly.value) notifications.value = []
      else notifications.value.forEach((item) => (item.isRead = true))
    }
  }

  const connectRealtime = () => {
    if (stompClient?.active) return
    const token = localStorage.getItem('ACCESS_TOKEN')
    if (!token) {
      connectionState.value = 'DISCONNECTED'
      return
    }

    stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_ENDPOINT_URL),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        connectionState.value = 'CONNECTED'
        stompClient.subscribe('/topic/admin/notifications', (message) => {
          try {
            handleRealtimeEvent(JSON.parse(message.body))
          } catch {
            fetchNotifications()
          }
        })
      },
      onWebSocketClose: () => {
        connectionState.value = 'DISCONNECTED'
      },
      onStompError: () => {
        connectionState.value = 'DISCONNECTED'
      },
    })
    stompClient.activate()
  }

  const disconnectRealtime = () => {
    stompClient?.deactivate()
    stompClient = null
    connectionState.value = 'DISCONNECTED'
  }

  onBeforeUnmount(disconnectRealtime)

  return {
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
  }
}
