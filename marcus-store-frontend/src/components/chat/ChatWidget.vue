<template>
  <div class="chat-widget-container">
    <!-- Nút mở khung chat -->
    <button
      class="chat-trigger-btn shadow-lg"
      @click="handleChatTriggerClick"
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

    <!-- Khung Chat Chính (Chỉ hiện khi đã đăng nhập) -->
    <transition name="chat-slide">
      <div v-show="chatStore.isOpen && isLoggedIn" class="chat-window shadow-lg">
        <!-- Header -->
        <div class="chat-header">
          <div class="d-flex align-items-center gap-2">
            <div class="admin-avatar">
              <i class="fas fa-user-tie"></i>
              <span class="status-dot" :class="{ 'is-offline': !chatStore.isAdminOnline }"></span>
            </div>
            <div>
              <h6 class="mb-0 fw-bold text-white">CSKH {{ siteName }}</h6>
              <span class="status-text">
                {{ chatStore.isAdminOnline ? 'Đang trực tuyến' : 'Tạm ngoại tuyến' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <button
              v-if="chatStore.hasActiveSession"
              class="end-chat-btn"
              title="Kết thúc phiên hỗ trợ"
              @click="handleEndSession"
            >
              Kết thúc
            </button>
            <button class="close-btn" @click="chatStore.closeChat">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>

        <!-- Body (Danh sách tin nhắn) -->
        <div class="chat-body" ref="chatBody">
          <div class="chat-welcome">
            <p>
              {{
                chatStore.claimedBy
                  ? `${chatStore.claimedBy} đang hỗ trợ bạn.`
                  : 'Tin nhắn đã được gửi tới đội ngũ CSKH.'
              }}
            </p>
          </div>

          <p v-if="chatStore.errorMessage" class="chat-error">{{ chatStore.errorMessage }}</p>

          <!-- Marcus thêm: câu hỏi phổ biến giúp khách bắt đầu hội thoại nhanh hơn. -->
          <div class="suggestion-section">
            <span class="suggestion-label">
              <i class="fas fa-lightbulb"></i>
              Bạn có thể hỏi
            </span>
            <div class="suggestion-list">
              <button
                v-for="question in suggestedQuestions"
                :key="question"
                type="button"
                class="suggestion-chip"
                @click="selectSuggestedQuestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </div>

          <div
            v-for="(msg, index) in chatStore.messages"
            :key="index"
            class="message-wrapper"
            :class="{
              'is-mine': msg.senderRole === 'CUSTOMER',
              'is-admin': msg.senderRole === 'ADMIN',
              'is-system': msg.senderRole === 'SYSTEM',
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
            ref="chatInput"
            @keyup.enter="handleSend"
            placeholder="Nhập tin nhắn..."
            maxlength="1000"
            :disabled="!chatStore.isConnected"
            class="chat-input"
          />
          <button
            class="send-btn"
            @click="handleSend"
            :disabled="!inputMsg.trim() || !chatStore.isConnected"
          >
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </transition>

    <BaseModal
      :visible="showEndConfirm"
      type="confirm"
      title="Kết thúc phiên hỗ trợ?"
      message="Nội dung trò chuyện chỉ đang lưu tạm thời và sẽ được xóa khỏi hệ thống sau khi kết thúc."
      @close="showEndConfirm = false"
      @confirm="confirmEndSession"
    />

    <!-- Modal Yêu cầu đăng nhập (Hiện khi Guest click) -->
    <transition name="fade">
      <div v-if="showLoginPrompt" class="guest-prompt-overlay" @click="closeLoginPrompt">
        <div class="guest-prompt-modal shadow-lg" @click.stop>
          <!-- Nút tắt tinh tế ở góc -->
          <div class="modal-top-action">
            <button class="close-btn-modal" @click="closeLoginPrompt">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="modal-body text-center pt-0">
            <!-- Khối Logo Marcus Store mô phỏng chính xác ảnh của bạn -->
            <div class="brand-logo-wrapper mb-4">
              <div class="logo-icon-box shadow-sm" :class="{ 'has-site-logo': siteLogoUrl }">
                <img v-if="siteLogoUrl" :src="siteLogoUrl" :alt="siteName" class="site-logo-image" />
                <i v-else class="fas fa-mobile-alt"></i>
              </div>
              <div class="logo-text-box">
                <span class="text-marcus">{{ siteNameParts.primary }}</span>
                <span v-if="siteNameParts.secondary" class="text-store">{{ siteNameParts.secondary }}</span>
              </div>
            </div>

            <h5 class="fw-bold mb-2 text-dark">Trải nghiệm tiện ích</h5>
            <p class="text-muted mb-4 px-2" style="font-size: 14px; line-height: 1.5">
              Vui lòng đăng nhập để kết nối trực tiếp với đội ngũ Chăm sóc khách hàng của chúng tôi.
            </p>

            <div class="action-buttons px-3 pb-2">
              <router-link
                to="/auth/login"
                class="btn btn-primary login-btn w-100 mb-3 py-2 fw-bold shadow-sm"
              >
                Đăng nhập ngay
              </router-link>
              <div class="register-hint text-muted" style="font-size: 13px">
                Chưa có tài khoản?
                <router-link
                  to="/auth/register"
                  class="text-danger fw-bold text-decoration-none ms-1"
                  >Đăng ký</router-link
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import BaseModal from '@/components/BaseModal.vue'
import { useSettings } from '@/composables/useSettings'
import {
  clearFloatingContactPanel,
  setFloatingContactPanelOpen,
} from '@/utils/floatingContactVisibility'

const { siteName, siteLogoUrl, siteNameParts, fetchSettings } = useSettings()
onMounted(fetchSettings)

const props = defineProps({
  isLoggedIn: {
    type: Boolean,
    required: true,
    default: false,
  },
})

const chatStore = useChatStore()
const inputMsg = ref('')
const chatBody = ref(null)
const chatInput = ref(null)
const showLoginPrompt = ref(false)
const showEndConfirm = ref(false)

// Marcus sửa: giữ gợi ý đặc trưng của cửa hàng trong suốt cuộc trò chuyện.
const suggestedQuestions = [
  'Địa chỉ Marcus Store ở đâu?',
  'Tôi có thể nhận hàng trực tiếp tại cửa hàng không?',
  'Marcus Store kiểm tra giúp tôi sản phẩm này còn hàng không?',
  'Chính sách bảo hành sản phẩm tại Marcus Store như thế nào?',
  'Marcus Store có hỗ trợ mua trả góp không?',
  'Marcus Store kiểm tra giúp tôi tình trạng đơn hàng',
]

const handleChatTriggerClick = async () => {
  if (props.isLoggedIn) {
    await chatStore.openChat()
  } else {
    // Nếu là Guest, hiện prompt yêu cầu login
    showLoginPrompt.value = true
  }
}

const closeLoginPrompt = () => {
  showLoginPrompt.value = false
}

const handleSend = () => {
  if (!inputMsg.value.trim()) return
  if (chatStore.sendMessage(inputMsg.value.trim())) inputMsg.value = ''
}

const handleEndSession = () => {
  showEndConfirm.value = true
}

const confirmEndSession = async () => {
  showEndConfirm.value = false
  await chatStore.endSession()
}

const selectSuggestedQuestion = async (question) => {
  inputMsg.value = question
  await nextTick()
  chatInput.value?.focus()
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
    setFloatingContactPanelOpen('live', newVal)
    if (newVal) scrollToBottom()
  },
)

onBeforeUnmount(() => clearFloatingContactPanel('live'))
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap');

.chat-widget-container {
  font-family: 'Be Vietnam Pro', sans-serif;
  position: fixed;
  bottom: 160px;
  right: 24px;
  z-index: 1050;
}

/* Nút Trigger */
.chat-trigger-btn {
  width: 56px;
  height: 56px;
  background: linear-gradient(145deg, #ef233c, #c90019);
  color: #fff;
  border: 3px solid #fff;
  border-radius: 18px;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow:
    0 10px 24px rgba(215, 0, 24, 0.28),
    0 2px 6px rgba(15, 23, 42, 0.12);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

/* Tooltip cho nút Chat Live */
.chat-trigger-btn::before {
  content: 'Chat trực tiếp với Admin';
  position: absolute;
  right: 75px;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  pointer-events: none;
}
.chat-trigger-btn:hover::before {
  opacity: 1;
  visibility: visible;
  right: 70px;
}

.chat-trigger-btn:hover {
  transform: translateY(-3px);
  background: linear-gradient(145deg, #f43f5e, #b80014);
  box-shadow:
    0 14px 28px rgba(215, 0, 24, 0.35),
    0 3px 8px rgba(15, 23, 42, 0.14);
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
  border-radius: 20px;
  border: 2px solid rgba(215, 0, 24, 0.5);
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
  bottom: -136px;
  right: 0;
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
  background: #10b981;
  border: 2px solid #d70018;
  border-radius: 50%;
}
.status-dot.is-offline {
  background: #cbd5e1;
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.end-chat-btn {
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 999px;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
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

.chat-error {
  margin: 0;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 8px 10px;
  background: #fff1f2;
  color: #b91c1c;
  font-size: 12px;
  text-align: center;
}

.suggestion-section {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.suggestion-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
}

.suggestion-label i {
  color: #f59e0b;
}

.suggestion-list {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 3px;
  scrollbar-width: thin;
}

.suggestion-chip {
  flex: 0 0 auto;
  max-width: 245px;
  overflow: hidden;
  border: 1px solid #fecdd3;
  border-radius: 999px;
  padding: 6px 9px;
  background: #fff;
  color: #9f1239;
  font-size: 11px;
  line-height: 1.25;
  text-align: left;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
  transition: all 0.18s ease;
}

.suggestion-chip:hover {
  border-color: #d70018;
  background: #fff1f2;
  transform: translateY(-1px);
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

.is-system {
  justify-content: center;
}

.is-system .message-bubble {
  max-width: 90%;
  padding: 6px 10px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #64748b;
  font-size: 11px;
  text-align: center;
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

/* CSS cho Guest Prompt Modal */
.guest-prompt-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.guest-prompt-modal {
  background: #fff;
  width: 340px;
  border-radius: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.modal-top-action {
  display: flex;
  justify-content: flex-end;
  padding: 16px 16px 0;
}

.close-btn-modal {
  background: #f1f5f9;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 14px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.close-btn-modal:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.modal-body {
  padding: 0 24px 24px;
}

/* Vẽ Logo Marcus Store */
.brand-logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.logo-icon-box {
  width: 48px;
  height: 48px;
  background: #d70018; /* Đỏ chuẩn Marcus */
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
}

/* Marcus sửa: logo cấu hình không đặt trực tiếp trên nền đỏ của fallback icon. */
.logo-icon-box.has-site-logo {
  padding: 7px;
  background: #ffffff;
  border: 1px solid #fee2e2;
  box-shadow: 0 5px 14px rgba(215, 0, 24, 0.12);
}

.logo-icon-box.has-site-logo .site-logo-image {
  border-radius: 6px;
}

.logo-text-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1;
}

.text-marcus {
  font-size: 24px;
  font-weight: 800;
  color: #d70018;
  letter-spacing: -0.5px;
}

.text-store {
  font-size: 13px;
  font-weight: 700;
  color: #d70018;
  letter-spacing: 2px;
  opacity: 0.9;
}

.login-btn {
  background: #d70018;
  border: none;
  border-radius: 12px;
  transition: all 0.3s ease;
}
.login-btn:hover {
  background: #b80014;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(215, 0, 24, 0.3) !important;
}

.register-hint a:hover {
  text-decoration: underline !important;
}

/* Animation cho modal */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.fade-enter-active .guest-prompt-modal {
  animation: modal-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
@keyframes modal-pop {
  0% {
    transform: scale(0.8) translateY(20px);
    opacity: 0;
  }
  100% {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}
</style>
