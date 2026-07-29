import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import {
  claimRoomChat,
  endRoomChat,
  getActiveRooms,
  getChatAvailability,
  getChatHistory,
  updateChatAvailability,
} from '@/api/adminChatApi'

export const useAdminChatStore = defineStore('adminChat', {
  state: () => ({
    stompClient: null,
    rooms: [],
    activeRoomId: null,
    messages: [],
    currentAdmin: null,
    currentRoomSubscription: null,
    endedSubscription: null,
    isOpen: false,
    isConnected: false,
    isAvailable: false,
    errorMessage: '',
  }),

  getters: {
    notificationCount: (state) =>
      state.rooms.filter((room) => room.unclaimed || room.hasNewMessage).length,
    activeRoom: (state) => state.rooms.find((room) => room.roomId === state.activeRoomId) ?? null,
    canReply() {
      return Boolean(this.activeRoom?.claimedBy === this.currentAdmin)
    },
  },

  actions: {
    toggleChatPanel() {
      this.isOpen = !this.isOpen
      if (this.isOpen && this.activeRoom) this.activeRoom.hasNewMessage = false
    },

    async initInbox() {
      try {
        const response = await getActiveRooms()
        this.rooms = (response.data?.data ?? []).map((room) => ({ ...room, hasNewMessage: false }))
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không tải được danh sách hỗ trợ.'
      }
    },

    async loadAvailability() {
      try {
        const response = await getChatAvailability()
        this.isAvailable = Boolean(response.data?.data?.available)
      } catch {
        this.isAvailable = false
      }
    },

    async toggleAvailability() {
      if (!this.isConnected) return
      try {
        const response = await updateChatAvailability(!this.isAvailable)
        this.isAvailable = Boolean(response.data?.data?.available)
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không thể đổi trạng thái nhận chat.'
      }
    },

    connectSocket(token, username) {
      if (!token || this.stompClient?.active) return
      this.currentAdmin = username
      const socketUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws-endpoint'
      this.stompClient = new Client({
        webSocketFactory: () => new SockJS(socketUrl),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: async () => {
          this.isConnected = true
          await this.loadAvailability()
          this.stompClient.subscribe('/topic/chat.incoming', (frame) => {
            this.upsertRoomSummary(JSON.parse(frame.body))
          })
          this.stompClient.subscribe('/topic/chat.incoming.claimed', (frame) => {
            const { roomId, claimedBy } = JSON.parse(frame.body)
            const room = this.rooms.find((item) => item.roomId === roomId)
            if (room) Object.assign(room, { claimedBy, unclaimed: false })
          })
          this.stompClient.subscribe('/topic/chat.incoming.ended', (frame) => {
            this.removeRoom(JSON.parse(frame.body).roomId)
          })
          await this.initInbox()
        },
        onWebSocketClose: () => {
          this.isConnected = false
          this.isAvailable = false
        },
        onStompError: () => {
          this.errorMessage = 'Kết nối Live Chat bị gián đoạn.'
        },
      })
      this.stompClient.activate()
    },

    upsertRoomSummary(summary) {
      const room = this.rooms.find((item) => item.roomId === summary.roomId)
      const isViewing = this.isOpen && this.activeRoomId === summary.roomId
      if (room) {
        Object.assign(room, summary)
        if (!isViewing && summary.lastMessage) room.hasNewMessage = true
      } else {
        this.rooms.unshift({ ...summary, hasNewMessage: !isViewing })
      }
    },

    async openRoom(roomId) {
      this.activeRoomId = roomId
      this.messages = []
      this.isOpen = true
      if (this.activeRoom) this.activeRoom.hasNewMessage = false
      this.currentRoomSubscription?.unsubscribe()
      this.endedSubscription?.unsubscribe()

      this.currentRoomSubscription = this.stompClient?.subscribe(
        `/topic/chat.room.${roomId}`,
        (frame) => {
          const message = JSON.parse(frame.body)
          if (!this.messages.some((item) => item.id === message.id)) this.messages.push(message)
        },
      )
      this.endedSubscription = this.stompClient?.subscribe(
        `/topic/chat.room.${roomId}.ended`,
        () => {
          this.removeRoom(roomId)
        },
      )

      try {
        const response = await getChatHistory(roomId)
        this.messages = response.data?.data ?? []
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không tải được nội dung trò chuyện.'
      }
    },

    async claimRoom(roomId) {
      try {
        const response = await claimRoomChat(roomId)
        const session = response.data?.data
        const room = this.rooms.find((item) => item.roomId === roomId)
        if (room) Object.assign(room, { claimedBy: session?.claimedBy, unclaimed: false })
      } catch (error) {
        this.errorMessage =
          error.response?.data?.message || 'Phiên đã được nhân viên khác tiếp nhận.'
        await this.initInbox()
      }
    },

    sendMessage(content) {
      const normalized = content?.trim()
      if (!this.canReply || !this.isConnected || !normalized || normalized.length > 1000)
        return false
      this.stompClient.publish({
        destination: '/app/chat.admin.send',
        body: JSON.stringify({ roomId: this.activeRoomId, content: normalized }),
      })
      return true
    },

    async endActiveRoom() {
      if (!this.canReply) return
      const roomId = this.activeRoomId
      try {
        await endRoomChat(roomId)
        this.removeRoom(roomId)
      } catch (error) {
        this.errorMessage = error.response?.data?.message || 'Không thể kết thúc phiên hỗ trợ.'
      }
    },

    removeRoom(roomId) {
      this.rooms = this.rooms.filter((room) => room.roomId !== roomId)
      if (this.activeRoomId === roomId) {
        this.currentRoomSubscription?.unsubscribe()
        this.endedSubscription?.unsubscribe()
        this.activeRoomId = null
        this.messages = []
      }
    },

    disconnectSocket() {
      this.currentRoomSubscription?.unsubscribe()
      this.endedSubscription?.unsubscribe()
      this.stompClient?.deactivate()
      this.stompClient = null
      this.isConnected = false
      this.isAvailable = false
    },
  },
})
