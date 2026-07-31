<template>
  <div class="ai-advisor">
    <button
      type="button"
      class="ai-trigger"
      :class="{ 'is-hidden': isOpen }"
      aria-label="Mở Marcus AI"
      @click="openAdvisor"
    >
      <span class="ai-spark"><i class="fas fa-sparkles"></i></span>
      <i class="fas fa-robot"></i>
      <span class="ai-badge">AI</span>
    </button>

    <Transition name="ai-window">
      <section v-show="isOpen" class="ai-panel" aria-label="Marcus AI tư vấn bán hàng">
        <header class="ai-header">
          <div class="ai-identity">
            <div class="ai-avatar"><i class="fas fa-robot"></i></div>
            <div>
              <h6>Marcus AI</h6>
              <span><i class="fas fa-circle"></i> Tư vấn tự động 24/7</span>
            </div>
          </div>
          <button type="button" class="ai-close" aria-label="Đóng" @click="isOpen = false">
            <i class="fas fa-times"></i>
          </button>
        </header>

        <div ref="messageBody" class="ai-body">
          <div class="ai-intro">
            <strong>Xin chào, mình là Marcus AI!</strong>
            <p>Mình có thể tìm và so sánh sản phẩm đang có trên Marcus Store.</p>
          </div>

          <div class="ai-suggestions">
            <span>Gợi ý nhanh</span>
            <div class="ai-suggestion-list">
              <button
                v-for="suggestion in suggestions"
                :key="suggestion"
                type="button"
                @click="selectSuggestion(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="ai-message-row"
            :class="`is-${message.role}`"
          >
            <div class="ai-message">
              <span v-if="message.role === 'assistant'" class="message-author">Marcus AI</span>
              <p>{{ message.content }}</p>

              <div v-if="message.products?.length" class="product-suggestions">
                <router-link
                  v-for="product in message.products"
                  :key="product.productId"
                  :to="`/product/${product.slug}`"
                  class="ai-product"
                  @click="handleProductClick(product)"
                >
                  <img
                    :src="product.thumbnailUrl || '/images/product-placeholder.png'"
                    :alt="product.productName"
                  />
                  <div>
                    <strong>{{ product.productName }}</strong>
                    <span class="product-price">{{ formatPrice(product.price) }}</span>
                    <small :class="{ 'out-of-stock': !product.inStock }">
                      {{ product.inStock ? 'Còn hàng' : 'Tạm hết hàng' }}
                    </small>
                  </div>
                  <i class="fas fa-chevron-right"></i>
                </router-link>
              </div>
            </div>
          </article>

          <div v-if="isLoading && !isStreamingText" class="ai-message-row is-assistant">
            <div class="ai-message typing"><span></span><span></span><span></span></div>
          </div>
        </div>

        <div class="ai-note">
          Marcus AI có thể nhầm lẫn. Giá và tồn kho được đối chiếu tại thời điểm tư vấn.
        </div>

        <footer class="ai-footer">
          <textarea
            ref="messageInput"
            v-model="inputMessage"
            rows="1"
            maxlength="500"
            placeholder="Bạn đang tìm sản phẩm nào?"
            :disabled="isLoading"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <button
            type="button"
            :disabled="!inputMessage.trim() || isLoading"
            aria-label="Gửi câu hỏi"
            @click="sendMessage"
          >
            <i class="fas fa-paper-plane"></i>
          </button>
        </footer>
      </section>
    </Transition>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { streamAiAdvisor, trackAiProductClick } from '@/api/aiAdvisorApi'
import {
  clearFloatingContactPanel,
  setFloatingContactPanelOpen,
} from '@/utils/floatingContactVisibility'

const isOpen = ref(false)
const isLoading = ref(false)
const isStreamingText = ref(false)
const inputMessage = ref('')
const messageBody = ref(null)
const messageInput = ref(null)
const messages = ref([])
let messageId = 0

const getTrackingSessionId = () => {
  const storageKey = 'MARCUS_AI_TRACKING_SESSION'
  let sessionId = sessionStorage.getItem(storageKey)
  if (!sessionId) {
    sessionId = crypto.randomUUID()
    sessionStorage.setItem(storageKey, sessionId)
  }
  return sessionId
}

const handleProductClick = (product) => {
  isOpen.value = false
  // Marcus sửa: tracking không được cản trở thao tác mở sản phẩm nếu API thống kê
  // tạm thời lỗi.
  trackAiProductClick(product.productId, getTrackingSessionId()).catch(() => {})
}

// Marcus thêm: AI và Live Chat có bộ câu hỏi riêng, không trộn trạng thái hai kênh.
const suggestions = [
  'Tư vấn điện thoại phù hợp trong tầm giá 10 triệu',
  'So sánh iPhone và Samsung đang có tại cửa hàng',
  'Màn hình OLED, AMOLED và LTPO khác nhau thế nào?',
  'Chọn điện thoại ưu tiên camera hay hiệu năng?',
  'Địa chỉ Marcus Store ở đâu?',
  'Tìm điện thoại phù hợp để học tập và làm việc',
]

const openAdvisor = async () => {
  isOpen.value = true
  await nextTick()
  messageInput.value?.focus()
}

const selectSuggestion = async (suggestion) => {
  inputMessage.value = suggestion
  await nextTick()
  messageInput.value?.focus()
}

const buildHistory = () =>
  messages.value
    .filter((message) => !message.isError)
    .slice(-6)
    .map(({ role, content }) => ({ role, content: content.slice(0, 500) }))

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  const history = buildHistory()
  messages.value.push({ id: ++messageId, role: 'user', content })
  inputMessage.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    const assistantMessage = {
      id: ++messageId,
      role: 'assistant',
      content: '',
      products: [],
    }
    messages.value.push(assistantMessage)
    await streamAiAdvisor(content, history, getTrackingSessionId(), {
      onToken: (token) => {
        isStreamingText.value = true
        assistantMessage.content += token
        scrollToBottom()
      },
      onDone: (data) => {
        assistantMessage.content =
          data?.answer || assistantMessage.content || 'Mình chưa tìm được câu trả lời phù hợp.'
        assistantMessage.products = data?.products ?? []
      },
    })
  } catch (error) {
    messages.value.push({
      id: ++messageId,
      role: 'assistant',
      content:
        error.response?.data?.message ||
        error.message ||
        'Marcus AI đang gián đoạn. Bạn có thể thử lại hoặc dùng Live Chat với Admin.',
      isError: true,
    })
  } finally {
    isLoading.value = false
    isStreamingText.value = false
    await scrollToBottom()
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageBody.value) messageBody.value.scrollTop = messageBody.value.scrollHeight
}

const formatPrice = (price) =>
  price == null ? 'Liên hệ' : `${Number(price).toLocaleString('vi-VN')} ₫`

watch(isOpen, (opened) => setFloatingContactPanelOpen('ai', opened))
onBeforeUnmount(() => clearFloatingContactPanel('ai'))
</script>

<style scoped>
.ai-advisor {
  position: fixed;
  right: 24px;
  bottom: 228px;
  /* Marcus sửa: AI trên Live Chat nhưng nằm dưới backdrop/offcanvas Trợ giúp. */
  z-index: 1052;
  font-family: 'Be Vietnam Pro', sans-serif;
}

.ai-trigger {
  position: relative;
  width: 56px;
  height: 56px;
  border: 3px solid #fff;
  border-radius: 18px;
  background: linear-gradient(145deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 23px;
  cursor: pointer;
  box-shadow:
    0 10px 24px rgba(79, 70, 229, 0.3),
    0 2px 6px rgba(15, 23, 42, 0.12);
  transition: 0.22s ease;
}

.ai-trigger:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 32px rgba(79, 70, 229, 0.42);
}

.ai-trigger.is-hidden {
  opacity: 0;
  visibility: hidden;
  transform: scale(0.82);
}

.ai-trigger::before {
  content: 'Tư vấn với Marcus AI';
  position: absolute;
  right: 72px;
  top: 50%;
  transform: translateY(-50%);
  padding: 7px 12px;
  border-radius: 8px;
  background: #172033;
  color: #fff;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: 0.2s ease;
}

.ai-trigger:hover::before {
  opacity: 1;
  visibility: visible;
}

.ai-badge {
  position: absolute;
  right: -5px;
  top: -5px;
  min-width: 24px;
  height: 20px;
  padding: 0 5px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #fbbf24;
  color: #4c1d95;
  font-size: 9px;
  font-weight: 800;
  line-height: 16px;
}

.ai-spark {
  position: absolute;
  top: 5px;
  left: 8px;
  color: #fde68a;
  font-size: 9px;
}

.ai-panel {
  position: absolute;
  right: 0;
  bottom: -204px;
  width: 390px;
  height: 540px;
  overflow: hidden;
  border: 1px solid #ddd6fe;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 22px 55px rgba(30, 41, 59, 0.22);
  display: flex;
  flex-direction: column;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #312e81, #6d28d9);
  color: #fff;
}

.ai-identity {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.15);
}

.ai-identity h6 {
  margin: 0 0 2px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.ai-identity span {
  color: #ddd6fe;
  font-size: 10px;
}

.ai-identity span i {
  margin-right: 4px;
  color: #4ade80;
  font-size: 7px;
}

.ai-close {
  border: 0;
  background: transparent;
  color: #ddd6fe;
  font-size: 18px;
  cursor: pointer;
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: #f8fafc;
}

.ai-intro {
  margin-bottom: 12px;
  border: 1px solid #e9d5ff;
  border-radius: 14px;
  padding: 10px 12px;
  background: #faf5ff;
  color: #4c1d95;
  font-size: 12px;
}

.ai-intro p {
  margin: 3px 0 0;
  color: #6b7280;
}

.ai-suggestions > span {
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
}

.ai-suggestion-list {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  margin: 6px 0 14px;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.ai-suggestion-list button {
  flex: 0 0 auto;
  max-width: 260px;
  overflow: hidden;
  border: 1px solid #ddd6fe;
  border-radius: 999px;
  padding: 6px 10px;
  background: #fff;
  color: #5b21b6;
  font-size: 10px;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
}

.ai-message-row {
  display: flex;
  margin: 8px 0;
}

.ai-message-row.is-user {
  justify-content: flex-end;
}

.ai-message {
  max-width: 88%;
  border: 1px solid #e2e8f0;
  border-radius: 15px 15px 15px 4px;
  padding: 9px 11px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.04);
}

.is-user .ai-message {
  border: 0;
  border-radius: 15px 15px 4px 15px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
}

.ai-message p {
  margin: 0;
  white-space: pre-line;
}

.message-author {
  display: block;
  margin-bottom: 3px;
  color: #6d28d9;
  font-size: 9px;
  font-weight: 700;
}

.product-suggestions {
  display: grid;
  gap: 6px;
  margin-top: 9px;
}

.ai-product {
  display: grid;
  grid-template-columns: 45px 1fr 12px;
  align-items: center;
  gap: 8px;
  border: 1px solid #ede9fe;
  border-radius: 11px;
  padding: 7px;
  background: #fafafa;
  color: inherit;
  text-decoration: none;
}

.ai-product img {
  width: 45px;
  height: 45px;
  object-fit: contain;
  border-radius: 8px;
  background: #fff;
}

.ai-product strong,
.ai-product span,
.ai-product small {
  display: block;
}

.ai-product strong {
  display: -webkit-box;
  overflow: hidden;
  color: #1e293b;
  font-size: 10px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-price {
  color: #dc2626;
  font-size: 11px;
  font-weight: 700;
}

.ai-product small {
  color: #15803d;
  font-size: 9px;
}

.ai-product small.out-of-stock {
  color: #94a3b8;
}

.ai-product > i {
  color: #8b5cf6;
  font-size: 9px;
}

.typing {
  display: flex;
  gap: 4px;
  padding: 12px 14px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8b5cf6;
  animation: typing-dot 1s infinite alternate;
}

.typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing span:nth-child(3) {
  animation-delay: 0.4s;
}

.ai-note {
  padding: 6px 12px;
  border-top: 1px solid #ede9fe;
  background: #faf5ff;
  color: #7c3aed;
  font-size: 9px;
  text-align: center;
}

.ai-footer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.ai-footer textarea {
  flex: 1;
  min-height: 39px;
  max-height: 80px;
  resize: none;
  border: 1px solid #ddd6fe;
  border-radius: 14px;
  padding: 10px 12px;
  outline: none;
  color: #334155;
  font: inherit;
  font-size: 12px;
}

.ai-footer textarea:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.ai-footer button {
  width: 39px;
  height: 39px;
  border: 0;
  border-radius: 13px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  cursor: pointer;
}

.ai-footer button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.ai-window-enter-active,
.ai-window-leave-active {
  transition: 0.24s ease;
}

.ai-window-enter-from,
.ai-window-leave-to {
  opacity: 0;
  transform: translateY(15px) scale(0.94);
}

@keyframes typing-dot {
  to {
    opacity: 0.25;
    transform: translateY(-3px);
  }
}

@media (max-width: 520px) {
  .ai-advisor {
    right: 16px;
  }

  .ai-panel {
    position: fixed;
    right: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
    height: min(620px, calc(100vh - 24px));
  }
}
</style>
