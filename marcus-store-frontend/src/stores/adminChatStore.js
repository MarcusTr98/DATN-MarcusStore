import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getActiveRooms, getChatHistory, claimRoomChat } from '@/api/adminChatApi'

export const useAdminChatStore = defineStore('adminChat', {
  state: () => ({
    stompClient: null,
    rooms: [],
    activeRoomId: null,
    messages: [],
    currentAdmin: null,
    currentRoomSubscription: null,
    isOpen: false,
  }),

  getters: {
    unclaimedCount: (state) => state.rooms.filter((r) => r.unclaimed).length,

    // Tổng số phòng cần Admin chú ý: chưa claim HOẶC đã claim nhưng có tin nhắn mới chưa đọc
    notificationCount: (state) => state.rooms.filter((r) => r.unclaimed || r.hasNewMessage).length,
  },

  actions: {
    async initInbox() {
      try {
        const res = await getActiveRooms()
        // Khởi tạo thêm cờ hasNewMessage = false cho mỗi phòng khi mới tải danh sách
        this.rooms = (res.data.data ?? []).map((room) => ({
          ...room,
          hasNewMessage: false,
        }))
      } catch (err) {
        console.error('Lỗi tải danh sách phòng:', err)
      }
    },

    connectSocket(token, username) {
      if (this.stompClient?.active) return
      this.currentAdmin = username
      const socketUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws-endpoint'

      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => {
          this.stompClient.subscribe('/topic/chat.incoming', (msg) =>
            this.upsertRoomSummary(JSON.parse(msg.body)),
          )
          this.stompClient.subscribe('/topic/chat.incoming.claimed', (msg) => {
            const { roomId, claimedBy } = JSON.parse(msg.body)
            const room = this.rooms.find((r) => r.roomId === roomId)
            if (room) {
              room.claimedBy = claimedBy
              room.unclaimed = false
            }
          })
        },
      })
      this.stompClient.activate()
    },

    upsertRoomSummary(msg) {
      const room = this.rooms.find((r) => r.roomId === msg.roomId)

      // Admin có đang xem đúng phòng này không (panel mở + đúng roomId đang active)
      const isViewingThisRoom = this.isOpen && this.activeRoomId === msg.roomId

      if (room) {
        room.lastMessage = msg.content
        room.lastTimestamp = msg.timestamp

        // FIX: chỉ bật chấm đỏ "tin nhắn mới" khi Admin KHÔNG đang xem đúng phòng này,
        // không phụ thuộc việc phòng đã được claim hay chưa
        if (msg.senderRole === 'CUSTOMER' && !isViewingThisRoom) {
          room.hasNewMessage = true
        }
      } else {
        this.rooms.unshift({
          roomId: msg.roomId,
          lastMessage: msg.content,
          lastTimestamp: msg.timestamp,
          claimedBy: null,
          unclaimed: true,
          hasNewMessage: msg.senderRole === 'CUSTOMER',
        })
      }

      if (this.activeRoomId === msg.roomId) this.messages.push(msg)
    },

    async openRoom(roomId) {
      this.activeRoomId = roomId
      this.messages = []
      this.isOpen = true

      // Đã mở phòng để xem -> tắt chấm đỏ "tin nhắn mới" của phòng này
      const room = this.rooms.find((r) => r.roomId === roomId)
      if (room) room.hasNewMessage = false

      this.currentRoomSubscription?.unsubscribe()
      this.currentRoomSubscription = this.stompClient?.subscribe(
        `/topic/chat.room.${roomId}`,
        (msg) => {
          const receivedMsg = JSON.parse(msg.body)
          if (!this.messages.some((m) => m.id === receivedMsg.id && m.id)) {
            this.messages.push(receivedMsg)
          }
        },
      )

      try {
        const res = await getChatHistory(roomId)
        this.messages = res.data.data ?? []
      } catch (err) {
        console.error('Lỗi tải lịch sử:', err)
      }
    },

    async claimRoom(roomId) {
      try {
        await claimRoomChat(roomId)
        const room = this.rooms.find((r) => r.roomId === roomId)
        if (room) {
          room.claimedBy = this.currentAdmin
          room.unclaimed = false
        }
      } catch (e) {
        console.error(e)
      }
    },

    sendMessage(content) {
      this.stompClient?.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({
          roomId: this.activeRoomId,
          sender: this.currentAdmin,
          senderRole: 'ADMIN',
          content,
        }),
      })
    },

    disconnectSocket() {
      this.stompClient?.deactivate()
    },
  },
})
