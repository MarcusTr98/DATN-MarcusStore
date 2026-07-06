import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getActiveRooms, getChatHistory } from '@/api/adminChatApi'

export const useAdminChatStore = defineStore('adminChat', {
  state: () => ({
    stompClient: null,
    connected: false,

    // Danh sách phòng hiển thị bên Inbox (trái)
    rooms: [], // [{ roomId, lastMessage, lastTimestamp, claimedBy, unclaimed }]

    // Phòng đang mở xem
    activeRoomId: null,
    messages: [], // tin nhắn của activeRoomId

    currentAdmin: null,
  }),

  getters: {
    unclaimedCount: (state) => state.rooms.filter((r) => r.unclaimed).length,
  },

  actions: {
    // Gọi khi Admin đăng nhập vào trang quản lý Chat
    async initInbox() {
      try {
        const res = await getActiveRooms()
        this.rooms = res.data.data ?? []
      } catch (err) {
        console.error('Không tải được danh sách phòng chat:', err)
      }
    },

    connectSocket(token, username) {
      if (this.stompClient?.active) return
      this.currentAdmin = username

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS('/ws-endpoint'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,

        onConnect: () => {
          this.connected = true

          // 1. Lắng nghe chat mới gửi tới (chưa ai nhận)
          this.stompClient.subscribe('/topic/chat.incoming', (msg) => {
            const payload = JSON.parse(msg.body)
            this.upsertRoomSummary(payload)
          })

          // 2. Lắng nghe khi phòng đã được Admin khác nhận (để ẩn khỏi list "chờ")
          this.stompClient.subscribe('/topic/chat.incoming.claimed', (msg) => {
            const { roomId, claimedBy } = JSON.parse(msg.body)
            const room = this.rooms.find((r) => r.roomId === roomId)
            if (room) {
              room.claimedBy = claimedBy
              room.unclaimed = false
            }
          })
        },

        onStompError: (frame) => {
          console.error('STOMP error (Admin):', frame)
        },
      })

      this.stompClient.activate()
    },

    disconnectSocket() {
      this.stompClient?.deactivate()
      this.connected = false
    },

    // Cập nhật preview tin nhắn mới nhất trong danh sách Inbox
    upsertRoomSummary(msg) {
      const existing = this.rooms.find((r) => r.roomId === msg.roomId)
      if (existing) {
        existing.lastMessage = msg.content
        existing.lastTimestamp = msg.timestamp
      } else {
        this.rooms.unshift({
          roomId: msg.roomId,
          lastMessage: msg.content,
          lastTimestamp: msg.timestamp,
          claimedBy: null,
          unclaimed: true,
        })
      }

      // Nếu đang mở đúng phòng này thì đẩy luôn tin nhắn vào khung chat
      if (this.activeRoomId === msg.roomId) {
        this.messages.push(msg)
      }
    },

    // Admin bấm mở 1 phòng chat để xem/trả lời
    async openRoom(roomId) {
      this.activeRoomId = roomId
      this.messages = []

      try {
        const res = await getChatHistory(roomId)
        this.messages = res.data.data ?? []
      } catch (err) {
        console.error('Không tải được lịch sử chat:', err)
      }

      // Subscribe riêng vào topic của phòng này để nhận tin nhắn realtime
      this.stompClient?.subscribe(`/topic/chat.room.${roomId}`, (msg) => {
        this.messages.push(JSON.parse(msg.body))
      })
    },

    // Admin bấm "Nhận hỗ trợ"
    claimRoom(roomId) {
      this.stompClient?.publish({
        destination: '/app/chat.claim',
        body: JSON.stringify({ roomId, adminUsername: this.currentAdmin }),
      })

      const room = this.rooms.find((r) => r.roomId === roomId)
      if (room) {
        room.claimedBy = this.currentAdmin
        room.unclaimed = false
      }
    },

    // Admin gửi tin nhắn trả lời khách
    sendMessage(content) {
      if (!this.activeRoomId || !content.trim()) return

      const message = {
        roomId: this.activeRoomId,
        sender: this.currentAdmin,
        senderRole: 'ADMIN',
        content: content.trim(),
      }

      this.stompClient?.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message),
      })

      // Optimistic UI
      this.messages.push(message)
    },
  },
})
