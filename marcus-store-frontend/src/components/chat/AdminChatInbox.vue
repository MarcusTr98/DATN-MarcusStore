<template>
  <div class="admin-chat-widget no-print">
    <!-- NÚT THẢ NỔI DÍNH MÉP BÊN PHẢI (SIDE TAB) -->
    <button
      class="chat-side-tab shadow-lg"
      @click="chatStore.toggleChatPanel"
      :class="{ 'is-hidden': chatStore.isOpen }"
    >
      <span v-if="chatStore.notificationCount > 0" class="chat-badge">
        {{ chatStore.notificationCount }}
      </span>
      <i class="fas fa-headset icon-headset"></i>
      <span class="tab-text">HỖ TRỢ</span>
    </button>

    <!-- KHUNG CHAT (PANEL) -->
    <transition name="panel-slide">
      <div v-show="chatStore.isOpen" class="chat-panel shadow-lg">
        <!-- Header panel -->
        <div class="panel-header">
          <div>
            <h6 class="mb-0 fw-bold">Hỗ trợ khách hàng</h6>
            <small>{{
              chatStore.isConnected ? 'Realtime đã kết nối' : 'Đang kết nối lại...'
            }}</small>
          </div>
          <div class="panel-actions">
            <button
              class="availability-btn"
              :class="{ 'is-ready': chatStore.isAvailable }"
              :disabled="!chatStore.isConnected"
              @click="chatStore.toggleAvailability"
            >
              <span class="availability-dot"></span>
              {{ chatStore.isAvailable ? 'Sẵn sàng nhận chat' : 'Tạm dừng nhận chat' }}
            </button>
            <button class="close-btn" @click="chatStore.toggleChatPanel">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>

        <div v-if="chatStore.errorMessage" class="panel-error">
          {{ chatStore.errorMessage }}
          <button @click="chatStore.errorMessage = ''"><i class="fas fa-times"></i></button>
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
                {{ (room.customerUsername || '?').charAt(0).toUpperCase() }}
                <span v-if="room.unclaimed || room.hasNewMessage" class="ping-dot"></span>
              </div>

              <div class="room-info">
                <div class="room-top-line">
                  <span class="room-name">{{ room.customerUsername || 'Khách hàng' }}</span>
                  <span class="room-time">{{ formatTime(room.lastTimestamp) }}</span>
                </div>
                <p class="room-preview">{{ room.lastMessage }}</p>
                <small class="room-status">{{ roomStatusLabel(room.status) }} · chờ {{ formatWaiting(room.waitingSeconds) }}</small>
              </div>
            </button>
          </aside>

          <!-- CỘT PHẢI: Khung hội thoại -->
          <section class="conversation-panel">
            <template v-if="chatStore.activeRoomId">
              <div class="conv-header">
                <div>
                  <h6 class="mb-0 fw-bold">
                    {{ chatStore.activeRoom?.customerUsername || 'Khách hàng' }}
                  </h6>
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
                <button v-else-if="chatStore.canReply" class="end-room-btn" @click="handleEndRoom">
                  Kết thúc phiên
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

              <!-- Marcus thêm: Admin chọn mẫu rồi chỉnh lại trước khi gửi cho khách. -->
              <div v-if="chatStore.canReply" class="quick-reply-section">
                <div class="quick-reply-heading">
                  <i class="fas fa-bolt"></i>
                  Trả lời nhanh
                </div>
                <div class="quick-reply-list">
                  <button
                    v-for="reply in quickReplies"
                    :key="reply"
                    type="button"
                    class="quick-reply-chip"
                    @click="selectQuickReply(reply)"
                  >
                    {{ reply }}
                  </button>
                </div>
              </div>

              <div class="conv-footer">
                <input
                  v-model="inputMsg"
                  ref="convInput"
                  type="text"
                  :placeholder="
                    chatStore.canReply
                      ? 'Nhập phản hồi...'
                      : activeRoomClaimedBy
                        ? `Phiên do ${activeRoomClaimedBy} phụ trách`
                        : 'Bạn cần nhận hỗ trợ để nhắn tin...'
                  "
                  maxlength="1000"
                  :disabled="!chatStore.canReply"
                  class="conv-input"
                  @keyup.enter="handleSend"
                />
                <button
                  class="conv-send-btn"
                  :disabled="!inputMsg.trim() || !chatStore.canReply"
                  @click="handleSend"
                >
                  <i class="fas fa-paper-plane"></i>
                </button>
              </div>
              <div class="chat-metadata-note">
                Chỉ lưu thời gian chờ/thời lượng/trạng thái trả lời; không lưu nội dung chat vào database.
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

    <BaseModal
      :visible="showEndConfirm"
      type="confirm"
      title="Kết thúc phiên hỗ trợ?"
      message="Phiên sẽ đóng ở cả hai phía và toàn bộ nội dung đang lưu tạm trong RAM sẽ được xóa."
      @close="showEndConfirm = false"
      @confirm="confirmEndRoom"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useAdminChatStore } from '@/stores/adminChatStore'
import BaseModal from '@/components/BaseModal.vue'

const chatStore = useAdminChatStore()
const inputMsg = ref('')
const convBody = ref(null)
const convInput = ref(null)
const showEndConfirm = ref(false)

// Marcus thêm: mẫu phản hồi chung, không tự gửi để Admin có thể bổ sung thông tin cụ thể.
const quickReplies = [
  'Chào bạn, Marcus Store có thể hỗ trợ gì cho bạn?',
  'Dạ, 118 Cát Bi, Hải An, Hải Phòng ạ.',
  'Dạ, bạn có thể chọn nhận hàng tại cửa hàng và đến Marcus Store nhận trực tiếp ạ.',
  'Bạn gửi giúp mình tên hoặc đường link sản phẩm, mình kiểm tra tồn kho ngay nhé.',
  'Bạn vui lòng cung cấp mã đơn hàng giúp mình nhé.',
  'Mình đang kiểm tra thông tin, bạn chờ mình một chút nhé.',
  'Bạn gửi giúp mình tên sản phẩm để mình kiểm tra chính sách bảo hành cụ thể nhé.',
  'Bạn gửi giúp mình sản phẩm và phương thức trả góp mong muốn để mình tư vấn chính xác nhé.',
  'Cảm ơn bạn đã liên hệ Marcus Store!',
]

const activeRoomClaimedBy = computed(() => {
  const room = chatStore.rooms.find((r) => r.roomId === chatStore.activeRoomId)
  return room?.claimedBy ?? null
})

const handleSend = () => {
  if (!inputMsg.value.trim()) return
  if (chatStore.sendMessage(inputMsg.value)) inputMsg.value = ''
}

const handleEndRoom = () => {
  showEndConfirm.value = true
}

const confirmEndRoom = async () => {
  showEndConfirm.value = false
  await chatStore.endActiveRoom()
}

const selectQuickReply = async (reply) => {
  inputMsg.value = reply
  await nextTick()
  convInput.value?.focus()
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
const roomStatusLabel = (status) => ({ WAITING_ADMIN: 'Đang chờ Admin', CLAIMED: 'Đã tiếp nhận', ACTIVE: 'Đang trả lời', ENDED: 'Đã kết thúc' })[status] || 'Đang hoạt động'
const formatWaiting = (seconds) => seconds >= 60 ? `${Math.floor(seconds / 60)} phút` : `${seconds || 0} giây`

onMounted(async () => {
  const token = localStorage.getItem('ACCESS_TOKEN')
  const username = localStorage.getItem('USERNAME')

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
  z-index: 1050;
}
.room-status { color: #64748b; font-size: 9px; }
.chat-metadata-note { padding: 5px 12px 8px; color: #64748b; background: #fff; font-size: 9px; text-align: center; }

/* NÚT THẢ NỔI DÍNH MÉP (SIDE TAB) */
.chat-side-tab {
  position: fixed;
  top: 50%;
  right: -5px;
  transform: translateY(-50%);
  width: 46px;
  padding: 16px 8px;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #fff;
  border: none;
  border-radius: 12px 0 0 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: -4px 0 15px rgba(29, 78, 216, 0.3);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 1050;
}

.chat-side-tab:hover {
  right: 0;
  padding-right: 12px;
  background: linear-gradient(135deg, #1e40af, #2563eb);
}

.chat-side-tab.is-hidden {
  right: -60px;
  opacity: 0;
  visibility: hidden;
}

.icon-headset {
  font-size: 20px;
}

.tab-text {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  transform: rotate(180deg);
}

/* CHẤM ĐỎ CẢNH BÁO TIN NHẮN MỚI */
.chat-badge {
  position: absolute;
  top: -6px;
  left: -6px;
  min-width: 22px;
  height: 22px;
  padding: 0 5px;
  background: #ef4444;
  border: 2px solid #fff;
  border-radius: 999px;
  font-size: 11px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(239, 68, 68, 0.4);
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

/* KHUNG CHAT BÊN TRONG */
.chat-panel {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 640px;
  height: 480px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transform-origin: bottom right;
  border: 1px solid #dbeafe;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  z-index: 1050;
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
.panel-header small {
  color: rgba(255, 255, 255, 0.75);
  font-size: 10px;
}
.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.availability-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 999px;
  padding: 6px 10px;
  background: rgba(15, 23, 42, 0.2);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
}
.availability-btn.is-ready {
  background: rgba(22, 163, 74, 0.3);
}
.availability-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd5e1;
}
.availability-btn.is-ready .availability-dot {
  background: #4ade80;
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
.panel-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 12px;
  border-bottom: 1px solid #fecaca;
  background: #fff1f2;
  color: #be123c;
  font-size: 11px;
}
.panel-error button {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

/* CỘT TRÁI (DANH SÁCH PHÒNG) */
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

/* CỘT PHẢI (KHUNG CHAT) */
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

.end-room-btn {
  border: 1px solid #fecaca;
  border-radius: 999px;
  padding: 6px 11px;
  background: #fff1f2;
  color: #be123c;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
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

.quick-reply-section {
  padding: 9px 12px 0;
  border-top: 1px solid #dbeafe;
  background: #fff;
}

.quick-reply-heading {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 7px;
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.35px;
}

.quick-reply-heading i {
  color: #f59e0b;
}

.quick-reply-list {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 7px;
  scrollbar-width: thin;
}

.quick-reply-chip {
  max-width: 210px;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  padding: 6px 9px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 10px;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
  transition: all 0.18s ease;
}

.quick-reply-chip:hover {
  border-color: #3b82f6;
  background: #dbeafe;
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

@media (max-width: 700px) {
  .chat-panel {
    width: calc(100vw - 32px);
    right: 16px;
  }
  .room-list {
    width: 160px;
    min-width: 160px;
  }
}
</style>
