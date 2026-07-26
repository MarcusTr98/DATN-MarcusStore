import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import {
  endChatSession,
  getAdminPresence,
  getChatHistory,
  getCurrentChatSession,
  startChatSession,
} from '@/api/clientChatApi'

export const useChatStore = defineStore('chat', {
  state: () => ({
    isAdminOnline: false,
    messages: [],
    stompClient: null,
    isConnected: false,
    isConnecting: false,
    roomId: null,
    claimedBy: null,
    isOpen: false,
    unreadCount: 0,
    errorMessage: '',
  }),

  getters: {
    hasActiveSession: (state) => Boolean(state.roomId),
  },

  actions: {
    async checkPresence() {
      try {
        const response = await getAdminPresence()
        this.isAdminOnline = Boolean(response.data?.data?.isAdminOnline)
      } catch {
        this.isAdminOnline = false
      }
    },

    // Marcus thêm: F5 vẫn nối lại đúng phiên RAM nếu phiên chưa hết hạn.
    async restoreSession() {
      try {
        const response = await getCurrentChatSession()
        const session = response.data?.data
        if (!session?.active) return
        this.roomId = session.roomId
        this.claimedBy = session.claimedBy
        await this.loadHistory()
      } catch {
        this.resetSession()
      }
    },

    async startSession() {
      if (this.roomId) return true
      this.errorMessage = ''
      try {
        const response = await startChatSession()
        const session = response.data?.data
        this.roomId = session?.roomId ?? null
        this.claimedBy = session?.claimedBy ?? null
        await this.loadHistory()
        return Boolean(this.roomId)
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không thể bắt đầu phiên hỗ trợ.'
        return false
      }
    },

    async loadHistory() {
      if (!this.roomId) return
      try {
        const response = await getChatHistory()
        this.messages = response.data?.data ?? []
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không thể tải nội dung trò chuyện.'
      }
    },

    async openChat() {
      if (!(await this.startSession())) return
      this.isOpen = true
      this.unreadCount = 0
    },

    closeChat() {
      this.isOpen = false
    },

    connectSocket(token) {
      if (!token || this.stompClient?.active) return
      this.isConnecting = true
      const socketUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws-endpoint'

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: async () => {
          this.isConnected = true
          this.isConnecting = false

          this.stompClient.subscribe('/topic/chat.presence', (frame) => {
            this.isAdminOnline = Boolean(JSON.parse(frame.body)?.data?.isAdminOnline)
          })

          // Marcus sửa: user queue ngăn khách subscribe phòng của tài khoản khác.
          this.stompClient.subscribe('/user/queue/live-chat', (frame) => {
            const message = JSON.parse(frame.body)
            if (message.senderRole === 'SYSTEM' && message.sender) {
              this.claimedBy = message.sender
            }
            if (!this.messages.some((item) => item.id === message.id)) {
              this.messages.push(message)
            }
            if (!this.isOpen && message.senderRole !== 'CUSTOMER') this.unreadCount += 1
          })

          this.stompClient.subscribe('/user/queue/live-chat-ended', () => {
            this.resetSession()
            this.errorMessage = 'Phiên hỗ trợ đã kết thúc.'
          })

          await this.restoreSession()
        },
        onWebSocketClose: () => {
          this.isConnected = false
          this.isConnecting = false
        },
        onStompError: () => {
          this.isConnected = false
          this.isConnecting = false
          this.errorMessage = 'Kết nối Live Chat đang gián đoạn, hệ thống sẽ tự kết nối lại.'
        },
      })
      this.stompClient.activate()
    },

    sendMessage(content) {
      const normalized = content?.trim()
      if (!this.isConnected || !this.roomId || !normalized || normalized.length > 1000) return false
      this.stompClient.publish({
        destination: '/app/chat.customer.send',
        body: JSON.stringify({ content: normalized }),
      })
      return true
    },

    async endSession() {
      if (!this.roomId) return
      try {
        await endChatSession()
        this.resetSession()
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không thể kết thúc phiên hỗ trợ.'
      }
    },

    resetSession() {
      this.roomId = null
      this.claimedBy = null
      this.messages = []
      this.isOpen = false
      this.unreadCount = 0
    },

    disconnectSocket() {
      this.stompClient?.deactivate()
      this.stompClient = null
      this.isConnected = false
      this.isConnecting = false
      this.isOpen = false
    },
  },
})
