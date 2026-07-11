import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getAdminPresence, getChatHistory } from '@/api/clientChatApi'

export const useChatStore = defineStore('chat', {
  state: () => ({
    isAdminOnline: false,
    messages: [],
    stompClient: null,
    isConnected: false,
    roomId: null,
    isOpen: false, // Quản lý trạng thái Đóng/Mở khung UI Chat
    unreadCount: 0, // Bộ đếm tin nhắn chưa đọc khi UI đang đóng
  }),

  actions: {
    // 1. Kiểm tra trạng thái trực tuyến của Admin
    async checkPresence() {
      try {
        const res = await getAdminPresence()
        this.isAdminOnline = res.data?.data?.isAdminOnline || false
      } catch (error) {
        console.error('Lỗi kiểm tra trạng thái Admin', error)
        this.isAdminOnline = false
      }
    },

    // 2. Tải lại lịch sử chat từ Backend (Xử lý trường hợp người dùng F5)
    async loadHistory() {
      try {
        const res = await getChatHistory(this.roomId)
        this.messages = res.data?.data ?? []
      } catch (error) {
        console.error('Lỗi tải lịch sử chat', error)
      }
    },

    // 3. Đóng/Mở khung Chat Widget UI
    toggleChat() {
      this.isOpen = !this.isOpen
      if (this.isOpen) {
        this.unreadCount = 0 // Xóa bộ đếm chấm đỏ khi người dùng chủ động mở khung chat
      }
    },

    // 4. Khởi tạo kết nối STOMP/WebSocket
    connectSocket(token, username) {
      if (this.isConnected) return

      this.roomId = username

      // FIX: VITE_WS_URL trong .env đã chứa sẵn "/ws-endpoint",
      // dùng thẳng biến env, KHÔNG nối thêm "/ws-endpoint" nữa
      const socketUrl = import.meta.env.VITE_WS_URL

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,

        onConnect: () => {
          this.isConnected = true

          // lấy ngay dữ liệu lịch sử ngay khi kết nối thành công
          this.loadHistory()

          // Subscribe 1: Lắng nghe trạng thái On/Off của Admin
          this.stompClient.subscribe('/topic/chat.presence', (msg) => {
            const body = JSON.parse(msg.body)
            this.isAdminOnline = body.data.isAdminOnline
          })

          // Subscribe 2: Lắng nghe luồng tin nhắn thuộc về phòng của mình
          this.stompClient.subscribe(`/topic/chat.room.${this.roomId}`, (msg) => {
            const newMessage = JSON.parse(msg.body)
            this.messages.push(newMessage)

            // Kích hoạt bộ đếm chấm đỏ nếu khung UI đang bị ẩn
            if (!this.isOpen) {
              this.unreadCount++
            }
          })
        },
        onStompError: (frame) => {
          console.error('Lỗi STOMP Client:', frame.headers['message'])
        },
      })

      this.stompClient.activate()
    },

    // 5. Gửi tin nhắn từ Client lên Server
    sendMessage(content) {
      if (!this.isConnected || !this.roomId) return

      this.stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({ roomId: this.roomId, content: content }),
      })
    },

    // 6. Ngắt kết nối dọn dẹp RAM
    disconnectSocket() {
      if (this.stompClient) {
        this.stompClient.deactivate()
      }
      this.isConnected = false
      this.messages = []
      this.isOpen = false
      this.unreadCount = 0
    },
  },
})

