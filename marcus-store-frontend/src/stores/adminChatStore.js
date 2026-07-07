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
    isOpen: false, // Biến duy nhất quản lý trạng thái Đóng/Mở Panel
  }),

  getters: {
    notificationCount: (state) => state.rooms.filter((r) => r.unclaimed || r.hasNewMessage).length,
  },

  actions: {
    // Hàm mới: Đóng mở panel và reset thông báo
    toggleChatPanel() {
      this.isOpen = !this.isOpen
      if (this.isOpen && this.activeRoomId) {
        const room = this.rooms.find((r) => r.roomId === this.activeRoomId)
        if (room) room.hasNewMessage = false
      }
    },

    async initInbox() {
      try {
        const res = await getActiveRooms()
        this.rooms = (res.data.data ?? []).map((room) => ({
          ...room,
          hasNewMessage: false,
        }))
      } catch (err) {
        console.error('Không tải được danh sách phòng:', err)
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
          this.stompClient.subscribe('/topic/chat.incoming', (msg) => {
            this.upsertRoomSummary(JSON.parse(msg.body))
          })

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
      const isViewingThisRoom = this.isOpen && this.activeRoomId === msg.roomId
      const isFromCustomer = msg.senderRole === 'CUSTOMER'

      if (room) {
        room.lastMessage = msg.content
        room.lastTimestamp = msg.timestamp
        if (isFromCustomer && !isViewingThisRoom) {
          room.hasNewMessage = true
        }
      } else {
        this.rooms.unshift({
          roomId: msg.roomId,
          lastMessage: msg.content,
          lastTimestamp: msg.timestamp,
          claimedBy: null,
          unclaimed: true,
          hasNewMessage: isFromCustomer && !isViewingThisRoom,
        })
      }

      if (this.activeRoomId === msg.roomId) {
        if (!this.messages.some((m) => m.id === msg.id && m.id != null)) {
          this.messages.push(msg)
        }
      }
    },

    async openRoom(roomId) {
      this.activeRoomId = roomId
      this.messages = []
      this.isOpen = true

      const room = this.rooms.find((r) => r.roomId === roomId)
      if (room) room.hasNewMessage = false

      if (this.currentRoomSubscription) {
        this.currentRoomSubscription.unsubscribe()
      }

      this.currentRoomSubscription = this.stompClient?.subscribe(
        `/topic/chat.room.${roomId}`,
        (msg) => {
          const receivedMsg = JSON.parse(msg.body)
          if (!this.messages.some((m) => m.id === receivedMsg.id && m.id != null)) {
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
      } catch (error) {
        console.error('Lỗi khi claim phòng:', error)
      }
    },

    sendMessage(content) {
      if (!this.activeRoomId || !content.trim()) return

      this.stompClient?.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({
          roomId: this.activeRoomId,
          sender: this.currentAdmin,
          senderRole: 'ADMIN',
          content: content.trim(),
        }),
      })
    },

    disconnectSocket() {
      this.stompClient?.deactivate()
    },
  },
})
