<template>
  <div class="admin-chat-widget">
    <!-- Nút thả nổi -->
    <button
      class="chat-trigger-btn shadow-lg"
      @click="togglePanel"
      :class="{ 'is-hidden': isOpen }"
    >
      <i class="fas fa-headset"></i>
      <span v-if="chatStore.unclaimedCount > 0" class="unclaimed-dot">{{
        chatStore.unclaimedCount
      }}</span>
    </button>

    <!-- Panel nổi -->
    <transition name="panel-slide">
      <div v-show="isOpen" class="chat-panel shadow-lg">
        <!-- Header panel -->
        <div class="panel-header">
          <h6 class="mb-0 fw-bold">Hỗ trợ khách hàng</h6>
          <button class="close-btn" @click="togglePanel">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="panel-body">
          <!-- CỘT TRÁI: Danh sách phòng chat -->
          <aside class="room-list">
            <div v-if="chatStore.rooms.length === 0" class="empty-state">
              <i class="fas fa-comments"></i>
              <p>Chưa có cuộc trò chuyện nào</p>
            </div>

            <button
              v-for="room in chatStore.rooms"
              :key="room.roomId"
              class="room-item"
              :class="{
                'is-active': chatStore.activeRoomId === room.roomId,
                'is-unclaimed': room.unclaimed,
              }"
              @click="chatStore.openRoom(room.roomId)"
            >
              <div class="room-avatar">
                {{ room.roomId.charAt(0).toUpperCase() }}
                <span v-if="room.unclaimed" class="ping-dot"></span>
              </div>

              <div class="room-info">
                <div class="room-top-line">
                  <span class="room-name">{{ room.roomId }}</span>
                  <span class="room-time">{{ formatTime(room.lastTimestamp) }}</span>
                </div>
                <p class="room-preview">{{ room.lastMessage }}</p>
              </div>
            </button>
          </aside>

          <!-- CỘT PHẢI: Khung hội thoại -->
          <section class="conversation-panel">
            <template v-if="chatStore.activeRoomId">
              <div class="conv-header">
                <div>
                  <h6 class="mb-0 fw-bold">{{ chatStore.activeRoomId }}</h6>
                  <span class="conv-sub">
                    {{
                      activeRoomClaimedBy
                        ? `Đang hỗ trợ: ${activeRoomClaimedBy}`
                        : 'Chưa có ai tiếp nhận'
                    }}
                  </span>
                </div>

                <button
                  v-if="!activeRoomClaimedBy"
                  class="claim-btn"
                  @click="chatStore.claimRoom(chatStore.activeRoomId)"
                >
                  <i class="fas fa-hand-paper"></i> Nhận hỗ trợ
                </button>
              </div>

              <div class="conv-body" ref="convBody">
                <div
                  v-for="(msg, idx) in chatStore.messages"
                  :key="idx"
                  class="msg-row"
                  :class="{
                    'is-admin': msg.senderRole === 'ADMIN',
                    'is-customer': msg.senderRole === 'CUSTOMER',
                    'is-system': msg.senderRole === 'SYSTEM',
                  }"
                >
                  <div v-if="msg.senderRole === 'SYSTEM'" class="system-note">
                    {{ msg.content }}
                  </div>
                  <div v-else class="msg-bubble">{{ msg.content }}</div>
                </div>
              </div>

              <div class="conv-footer">
                <input
                  v-model="inputMsg"
                  type="text"
                  placeholder="Nhập phản hồi cho khách..."
                  class="conv-input"
                  @keyup.enter="handleSend"
                />
                <button class="conv-send-btn" :disabled="!inputMsg.trim()" @click="handleSend">
                  <i class="fas fa-paper-plane"></i>
                </button>
              </div>
            </template>

            <div v-else class="no-room-selected">
              <i class="fas fa-headset"></i>
              <p>Chọn một cuộc trò chuyện bên trái</p>
            </div>
          </section>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useAdminChatStore } from '@/stores/adminChatStore'

const chatStore = useAdminChatStore()
const isOpen = ref(false)
const inputMsg = ref('')
const convBody = ref(null)

const togglePanel = () => {
  isOpen.value = !isOpen.value
}

// Lấy thông tin ai đang phụ trách phòng đang mở
const activeRoomClaimedBy = computed(() => {
  const room = chatStore.rooms.find((r) => r.roomId === chatStore.activeRoomId)
  return room?.claimedBy ?? null
})

const handleSend = () => {
  if (!inputMsg.value.trim()) return
  chatStore.sendMessage(inputMsg.value)
  inputMsg.value = ''
}

const scrollToBottom = async () => {
  await nextTick()
  if (convBody.value) convBody.value.scrollTop = convBody.value.scrollHeight
}

watch(() => chatStore.messages.length, scrollToBottom)

const formatTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  const token = localStorage.getItem('ACCESS_TOKEN')
  const username = localStorage.getItem('USERNAME')

  // Load danh sách phòng + connect socket ngay khi Admin vào trang,
  // không cần đợi Admin bấm mở panel mới bắt đầu lắng nghe
  await chatStore.initInbox()
  chatStore.connectSocket(token, username)
})

onBeforeUnmount(() => {
  chatStore.disconnectSocket()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap');

.admin-chat-widget {
  font-family: 'Be Vietnam Pro', sans-serif;
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1050;
}

/* ===== Nút thả nổi ===== */
.chat-trigger-btn {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  border: none;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.chat-trigger-btn:hover {
  transform: translateY(-4px);
  background: linear-gradient(135deg, #1e40af, #2563eb);
}

.chat-trigger-btn.is-hidden {
  opacity: 0;
  visibility: hidden;
  transform: scale(0.8);
}

.unclaimed-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 22px;
  height: 22px;
  padding: 0 5px;
  background: #ef4444;
  border: 2px solid #fff;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: ping-badge 1.6s infinite;
}

@keyframes ping-badge {
  0% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.5);
  }
  100% {
    box-shadow: 0 0 0 8px rgba(239, 68, 68, 0);
  }
}

/* ===== Panel nổi ===== */
.chat-panel {
  position: absolute;
  bottom: 74px;
  right: 0;
  width: 640px;
  height: 480px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transform-origin: bottom right;
  border: 1px solid #dbeafe;
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}

.panel-header {
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.85);
  font-size: 18px;
  cursor: pointer;
}
.close-btn:hover {
  color: #fff;
}

.panel-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* ===== Cột trái: danh sách phòng ===== */
.room-list {
  width: 220px;
  min-width: 220px;
  border-right: 1px solid #dbeafe;
  background: #f5f9ff;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  color: #93b4e0;
  padding: 40px 16px;
}
.empty-state i {
  font-size: 26px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.room-item {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  gap: 10px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  border-bottom: 1px solid #eaf2ff;
  transition: background 0.15s;
}

.room-item:hover {
  background: #eaf2ff;
}

.room-item.is-active {
  background: #dbeafe;
}

.room-item.is-unclaimed {
  background: #f0f7ff;
}

.room-avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  position: relative;
}

.ping-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background: #ef4444;
  border: 2px solid #f5f9ff;
  border-radius: 50%;
}

.room-info {
  flex: 1;
  min-width: 0;
}

.room-top-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 4px;
}

.room-name {
  font-weight: 600;
  font-size: 13px;
  color: #1e3a5f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-time {
  font-size: 10px;
  color: #93b4e0;
  flex-shrink: 0;
}

.room-preview {
  margin: 2px 0 0;
  font-size: 12px;
  color: #5b7ba3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== Cột phải: hội thoại ===== */
.conversation-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.conv-header {
  padding: 12px 16px;
  border-bottom: 1px solid #dbeafe;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-sub {
  font-size: 11px;
  color: #93b4e0;
}

.claim-btn {
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.claim-btn:hover {
  opacity: 0.9;
}

.conv-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f9ff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg-row {
  display: flex;
}
.msg-row.is-admin {
  justify-content: flex-end;
}
.msg-row.is-customer {
  justify-content: flex-start;
}
.msg-row.is-system {
  justify-content: center;
}

.msg-bubble {
  max-width: 75%;
  padding: 9px 13px;
  font-size: 13px;
  border-radius: 14px;
  word-wrap: break-word;
}

.is-admin .msg-bubble {
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.is-customer .msg-bubble {
  background: #fff;
  color: #1e3a5f;
  border: 1px solid #dbeafe;
  border-bottom-left-radius: 4px;
}

.system-note {
  font-size: 11px;
  color: #5b7ba3;
  background: #dbeafe;
  padding: 4px 10px;
  border-radius: 999px;
}

.conv-footer {
  padding: 12px 16px;
  border-top: 1px solid #dbeafe;
  display: flex;
  gap: 8px;
}

.conv-input {
  flex: 1;
  border: 1px solid #dbeafe;
  background: #f5f9ff;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
}
.conv-input:focus {
  border-color: #3b82f6;
}

.conv-send-btn {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  border: none;
  cursor: pointer;
}
.conv-send-btn:disabled {
  background: #bfd6f5;
  cursor: not-allowed;
}

.no-room-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #93b4e0;
}
.no-room-selected i {
  font-size: 34px;
  margin-bottom: 10px;
  opacity: 0.4;
}

/* Responsive: panel co lại trên màn hình nhỏ */
@media (max-width: 700px) {
  .chat-panel {
    width: calc(100vw - 32px);
    right: -8px;
  }
  p .room-list {
    width: 160px;
    min-width: 160px;
  }
}
</style>
