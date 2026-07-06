import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getAdminPresence } from '@/api/ChatApi'

export const useChatStore = defineStore('chat', {
  state: () => ({
    isAdminOnline: false,
    messages: [],
    stompClient: null,
    isConnected: false,
    roomId: null,
  }),

  actions: {
    // 1. Hỏi BE xem Admin có nhà không trước khi tính chuyện Connect WS
    async checkPresence() {
      try {
        const res = await getAdminPresence()
        this.isAdminOnline = res.data?.data?.isAdminOnline || false
      } catch (error) {
        console.error('Lỗi kiểm tra trạng thái Admin', error)
        this.isAdminOnline = false
      }
    },

    // 2. Kết nối WebSocket - Chỉ gọi khi user đã login
    connectSocket(token, username) {
      if (this.isConnected) return

      this.roomId = username // Dùng chính username làm roomId cho cá nhân
      const socketUrl = 'http://localhost:8080/ws-endpoint'

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: { Authorization: `Bearer ${token}` },

        reconnectDelay: 5000,

        onConnect: () => {
          this.isConnected = true
          console.log('Đã kết nối Live Chat!')

          // Theo dõi trạng thái Admin (báo tắt/mở UI)
          this.stompClient.subscribe('/topic/chat.presence', (msg) => {
            const body = JSON.parse(msg.body)
            this.isAdminOnline = body.data.isAdminOnline
          })

          // Nhận tin nhắn trả về phòng của mình
          this.stompClient.subscribe(`/topic/chat.room.${this.roomId}`, (msg) => {
            this.messages.push(JSON.parse(msg.body))
          })
        },
        onStompError: (frame) => {
          console.error('STOMP Lỗi:', frame.headers['message'])
        },
      })

      this.stompClient.activate()
    },

    // 3. Gửi tin nhắn
    sendMessage(content) {
      if (!this.isConnected || !this.roomId) return

      const messagePayload = {
        roomId: this.roomId,
        content: content,
      }

      this.stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(messagePayload),
      })
    },

    // 4. Ngắt kết nối
    disconnectSocket() {
      if (this.stompClient) {
        this.stompClient.deactivate()
      }
      this.isConnected = false
      this.messages = []
    },
  },
})
