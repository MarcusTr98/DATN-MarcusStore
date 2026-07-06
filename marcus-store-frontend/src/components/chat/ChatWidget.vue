<template>
  <div class="chat-widget-container">
    <!-- Nút mở khung chat -->
    <button
      class="chat-trigger-btn shadow-lg"
      @click="chatStore.toggleChat"
      :class="{ 'is-hidden': chatStore.isOpen }"
    >
      <i class="fas fa-headset"></i>
      <span class="pulse-ring"></span>

      <!-- CHẤM ĐỎ BÁO TIN NHẮN CHƯA ĐỌC -->
      <span
        v-if="chatStore.unreadCount > 0"
        class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
        style="font-size: 12px; border: 2px solid #fff"
      >
        {{ chatStore.unreadCount }}
      </span>
    </button>

    <!-- Khung Chat Chính -->
    <transition name="chat-slide">
      <div v-show="chatStore.isOpen" class="chat-window shadow-lg">
        <!-- Header -->
        <div class="chat-header">
          <div class="d-flex align-items-center gap-2">
            <div class="admin-avatar">
              <i class="fas fa-user-tie"></i>
              <span class="status-dot"></span>
            </div>
            <div>
              <h6 class="mb-0 fw-bold text-white">CSKH Marcus Store</h6>
              <span class="status-text">Đang trực tuyến</span>
            </div>
          </div>
          <button class="close-btn" @click="chatStore.toggleChat">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <!-- Body (Danh sách tin nhắn) -->
        <div class="chat-body" ref="chatBody">
          <div class="chat-welcome">
            <p>Xin chào! Chúng tôi có thể giúp gì cho bạn hôm nay?</p>
          </div>

          <div
            v-for="(msg, index) in chatStore.messages"
            :key="index"
            class="message-wrapper"
            :class="{
              'is-mine': msg.senderRole === 'CUSTOMER',
              'is-admin': msg.senderRole === 'ADMIN',
            }"
          >
            <div class="message-bubble">
              {{ msg.content }}
            </div>
          </div>
        </div>

        <!-- Footer (Nhập liệu) -->
        <div class="chat-footer">
          <input
            type="text"
            v-model="inputMsg"
            @keyup.enter="handleSend"
            placeholder="Nhập tin nhắn..."
            class="chat-input"
          />
          <button class="send-btn" @click="handleSend" :disabled="!inputMsg.trim()">
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chatStore'

const chatStore = useChatStore()
const inputMsg = ref('')
const chatBody = ref(null)

const handleSend = () => {
  if (!inputMsg.value.trim()) return
  chatStore.sendMessage(inputMsg.value.trim())
  inputMsg.value = '' // Tự động xóa input sau khi ấn gửi
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}

watch(
  () => chatStore.messages.length,
  () => {
    if (chatStore.isOpen) scrollToBottom()
  },
)

watch(
  () => chatStore.isOpen,
  (newVal) => {
    if (newVal) scrollToBottom()
  },
)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap');

.chat-widget-container {
  font-family: 'Be Vietnam Pro', sans-serif;
  position: fixed;
  bottom: 24px;
  right: 100px; /* Đẩy sang trái 100px để không đè lên Zalo */
  z-index: 1050;
}

/* Nút Trigger */
.chat-trigger-btn {
  width: 60px;
  height: 60px;
  background: #d70018; /* Màu đỏ Marcus */
  color: #fff;
  border: none;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
}

.chat-trigger-btn:hover {
  transform: translateY(-5px);
  background: #b80014;
}

.chat-trigger-btn.is-hidden {
  opacity: 0;
  visibility: hidden;
  transform: scale(0.8);
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid #d70018;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.6);
    opacity: 0;
  }
}

/* Cửa sổ Chat */
.chat-window {
  position: absolute;
  bottom: 70px;
  right: -76px;
  width: 350px;
  height: 480px;
  background: #fff;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  transform-origin: bottom right;
}

/* Hiệu ứng trượt lên */
.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: scale(0.8) translateY(20px);
}

/* Header */
.chat-header {
  background: linear-gradient(135deg, #d70018, #9e0012);
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}

.admin-avatar {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  position: relative;
}

.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #10b981; /* Màu xanh lá online */
  border: 2px solid #d70018;
  border-radius: 50%;
}

.status-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 20px;
  cursor: pointer;
  transition: color 0.2s;
}
.close-btn:hover {
  color: #fff;
}

/* Body */
.chat-body {
  flex: 1;
  padding: 16px;
  background: #f8fafc;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-welcome p {
  font-size: 13px;
  color: #64748b;
  text-align: center;
  margin: 10px 0;
  background: #f1f5f9;
  padding: 8px;
  border-radius: 12px;
}

.message-wrapper {
  display: flex;
  width: 100%;
}

.message-wrapper.is-mine {
  justify-content: flex-end;
}

.message-wrapper.is-admin {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
}

.is-mine .message-bubble {
  background: #d70018;
  color: #fff;
  border-radius: 16px 16px 0 16px;
  box-shadow: 0 2px 8px rgba(215, 0, 24, 0.2);
}

.is-admin .message-bubble {
  background: #fff;
  color: #334155;
  border-radius: 16px 16px 16px 0;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* Footer */
.chat-footer {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-input {
  flex: 1;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 999px;
  padding: 10px 16px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: #d70018;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #d70018;
  color: #fff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  background: #b80014;
}
</style>
